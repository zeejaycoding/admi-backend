package com.powercity.power_city_platform.service;

import com.powercity.power_city_platform.dto.request.child.ChildDedicationCreateRequest;
import com.powercity.power_city_platform.dto.request.email.SendEmailRequest;
import com.powercity.power_city_platform.dto.response.child.ChildDedicationResponse;
import com.powercity.power_city_platform.entity.ChildDedication;
import com.powercity.power_city_platform.entity.User;
import com.powercity.power_city_platform.entity.UserRole;
import com.powercity.power_city_platform.exception.ResourceNotFoundException;
import com.powercity.power_city_platform.repository.ChildDedicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class ChildDedicationService {

    private final ChildDedicationRepository childDedicationRepository;
    private final UserService userService;
    private final EmailService emailService;

    public ChildDedicationResponse createDedication(ChildDedicationCreateRequest request) {
        User currentUser = userService.getCurrentUser();

        boolean duplicate = childDedicationRepository.existsDuplicate(
                request.getChildName(), request.getDedicationDate(), request.getParentName());
        if (duplicate) {
            throw new RuntimeException("A certificate for this child on the same date already exists");
        }

        ChildDedication dedication = new ChildDedication();
        dedication.setUser(currentUser);
        dedication.setChildName(request.getChildName());
        dedication.setDedicationDate(request.getDedicationDate());
        dedication.setParentName(request.getParentName());
        dedication.setCampus(request.getCampus());
        dedication.setMinister(request.getMinister());
        dedication.setCertificateNumber(generateCertificateNumber());
        dedication.setStatus("Pending");
        dedication.setSubmittedAt(LocalDateTime.now());

        ChildDedication saved = childDedicationRepository.save(dedication);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ChildDedicationResponse> getAllDedications() {
        User currentUser = userService.getCurrentUser();
        List<ChildDedication> dedications;

        if (isSuperAdminOrAdmin(currentUser)) {
            dedications = childDedicationRepository.findAllByOrderBySubmittedAtDesc();
        } else if (isNationalLeader(currentUser)) {
            dedications = childDedicationRepository.findByRegionForNationalLeader(currentUser.getRegion());
        } else if (isNationalLeaderOrCoordinator(currentUser)) {
            dedications = childDedicationRepository.findByUserIdOrSameCampusCoordinators(
                    currentUser.getId(), userService.resolveCampusName(currentUser.getEmail()));
        } else {
            dedications = childDedicationRepository.findByUserIdOrderBySubmittedAtDesc(currentUser.getId());
        }

        return dedications.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ChildDedicationResponse getDedicationById(Long id) {
        User currentUser = userService.getCurrentUser();
        ChildDedication dedication = childDedicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Child dedication not found with id: " + id));

        if (!canAccess(currentUser, dedication)) {
            throw new RuntimeException("Access denied");
        }

        return mapToResponse(dedication);
    }

    public void sendCertificateEmail(Long id, String recipientEmail) {
        User currentUser = userService.getCurrentUser();
        ChildDedication dedication = childDedicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Child dedication not found with id: " + id));

        if (!canAccess(currentUser, dedication)) {
            throw new RuntimeException("Access denied");
        }

        User submitter = dedication.getUser();
        String dedicationDateStr = dedication.getDedicationDate() != null
                ? dedication.getDedicationDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy")) : "N/A";

        Map<String, Object> templateData = Map.of(
                "firstName", submitter.getFirstName() != null ? submitter.getFirstName() : "",
                "lastName", submitter.getLastName() != null ? submitter.getLastName() : "",
                "email", recipientEmail,
                "childName", dedication.getChildName(),
                "dedicationDate", dedicationDateStr,
                "parentName", dedication.getParentName(),
                "campus", dedication.getCampus(),
                "minister", dedication.getMinister(),
                "certificateNumber", dedication.getCertificateNumber(),
                "status", dedication.getStatus()
        );

        SendEmailRequest request = new SendEmailRequest(
                List.of(recipientEmail),
                "Child Dedication Certificate - " + dedication.getChildName(),
                "child-dedication-certificate",
                templateData,
                null,
                null
        );

        boolean sent = emailService.sendEmail(request);
        if (!sent) {
            throw new RuntimeException("Email could not be sent to " + recipientEmail
                    + ". Please check the server logs for details.");
        }
    }

    public ChildDedicationResponse updateStatus(Long id, String status, String rejectionReason) {
        User currentUser = userService.getCurrentUser();
        ChildDedication dedication = childDedicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Child dedication not found with id: " + id));

        String normalizedStatus = status.toUpperCase();
        if (!"APPROVED".equals(normalizedStatus) && !"REJECTED".equals(normalizedStatus)) {
            throw new IllegalArgumentException("Status must be APPROVED or REJECTED");
        }

        String displayStatus = "APPROVED".equals(normalizedStatus) ? "Approved" : "Rejected";
        dedication.setStatus(displayStatus);
        dedication.setReviewedBy(currentUser);
        dedication.setReviewedAt(LocalDateTime.now());
        if ("REJECTED".equals(normalizedStatus)) {
            dedication.setRejectionReason(rejectionReason);
        }

        ChildDedication saved = childDedicationRepository.save(dedication);
        return mapToResponse(saved);
    }

    public void deleteDedication(Long id) {
        User currentUser = userService.getCurrentUser();
        ChildDedication dedication = childDedicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Child dedication not found with id: " + id));

        if (!canAccess(currentUser, dedication)) {
            throw new RuntimeException("Access denied");
        }

        childDedicationRepository.delete(dedication);
    }

    private String generateCertificateNumber() {
        String prefix = "CDC";
        long count = childDedicationRepository.countTotal() + 1;
        return prefix + "-" + String.format("%05d", count);
    }

    private ChildDedicationResponse mapToResponse(ChildDedication dedication) {
        User u = dedication.getUser();
        String submitterName = u.getFirstName() + " " + u.getLastName();
        String roleName = u.getUserRoles().stream()
                .filter(UserRole::getIsActive)
                .map(ur -> ur.getRole().getName())
                .findFirst()
                .orElse("USER");

        String dedicationDateStr = dedication.getDedicationDate() != null
                ? dedication.getDedicationDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy")) : null;
        String submittedStr = dedication.getSubmittedAt() != null
                ? dedication.getSubmittedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy")) : null;
        String reviewedStr = dedication.getReviewedAt() != null
                ? dedication.getReviewedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy, h:mm a")) : null;
        String reviewedByName = dedication.getReviewedBy() != null
                ? dedication.getReviewedBy().getFirstName() + " " + dedication.getReviewedBy().getLastName() : null;

        return new ChildDedicationResponse(
                dedication.getId(),
                submitterName,
                roleName,
                dedication.getChildName(),
                dedicationDateStr,
                dedication.getParentName(),
                dedication.getCampus(),
                dedication.getMinister(),
                dedication.getCertificateNumber(),
                dedication.getStatus(),
                submittedStr,
                reviewedByName,
                reviewedStr,
                dedication.getRejectionReason()
        );
    }

    private boolean isSuperAdminOrAdmin(User user) {
        return user.getUserRoles().stream()
                .filter(UserRole::getIsActive)
                .anyMatch(ur -> {
                    String role = ur.getRole().getName();
                    return role.equals("SUPER_ADMIN") || role.equals("ADMIN");
                });
    }

    private boolean isNationalLeaderOrCoordinator(User user) {
        return user.getUserRoles().stream()
                .filter(UserRole::getIsActive)
                .anyMatch(ur -> {
                    String role = ur.getRole().getName();
                    return role.equals("NATIONAL_LEADER") || role.equals("COORDINATOR");
                });
    }

    private boolean isNationalLeader(User user) {
        return user.getUserRoles().stream()
                .filter(UserRole::getIsActive)
                .anyMatch(ur -> {
                    String role = ur.getRole().getName();
                    return role.equals("NATIONAL_LEADER");
                });
    }

    private boolean canAccess(User user, ChildDedication dedication) {
        if (isSuperAdminOrAdmin(user)) return true;
        if (isNationalLeaderOrCoordinator(user)) {
            boolean isCreator = dedication.getUser() != null
                    && dedication.getUser().getId().equals(user.getId());
            if (isCreator) return true;
            if (isNationalLeader(user)) {
                return user.getRegion() != null
                        && dedication.getUser() != null
                        && user.getRegion().equalsIgnoreCase(dedication.getUser().getRegion());
            }
            return isSameCampusDedication(user, dedication);
        }
        return dedication.getUser().getId().equals(user.getId());
    }

    private boolean isSameCampusDedication(User user, ChildDedication dedication) {
        if (dedication.getUser() == null) {
            return false;
        }
        if (dedication.getUser().getId().equals(user.getId())) {
            return true;
        }
        boolean submitterIsCoordinator = dedication.getUser().getUserRoles().stream()
                .filter(UserRole::getIsActive)
                .anyMatch(ur -> ur.getRole().getName().equals("COORDINATOR"));
        if (!submitterIsCoordinator) {
            return false;
        }
        String campus = userService.resolveCampusName(user.getEmail());
        if (campus == null) {
            return false;
        }
        return campus.equalsIgnoreCase(userService.resolveCampusName(dedication.getUser().getEmail()));
    }
}
