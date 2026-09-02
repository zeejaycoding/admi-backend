package com.powercity.power_city_platform.service;

import com.powercity.power_city_platform.dto.request.email.SendEmailRequest;
import com.powercity.power_city_platform.dto.request.marriage.MarriageCertificateCreateRequest;
import com.powercity.power_city_platform.dto.response.marriage.MarriageCertificateResponse;
import com.powercity.power_city_platform.entity.MarriageCertificate;
import com.powercity.power_city_platform.entity.User;
import com.powercity.power_city_platform.entity.UserRole;
import com.powercity.power_city_platform.exception.ResourceNotFoundException;
import com.powercity.power_city_platform.repository.MarriageCertificateRepository;
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
public class MarriageCertificateService {

    private final MarriageCertificateRepository marriageCertificateRepository;
    private final UserService userService;
    private final EmailService emailService;

    public MarriageCertificateResponse createCertificate(MarriageCertificateCreateRequest request) {
        User currentUser = userService.getCurrentUser();

        boolean duplicate = marriageCertificateRepository.existsDuplicate(
                request.getGroomName(), request.getBrideName(), request.getMarriageDate());
        if (duplicate) {
            throw new RuntimeException("A marriage certificate for this couple on the same date already exists");
        }

        MarriageCertificate certificate = new MarriageCertificate();
        certificate.setUser(currentUser);
        certificate.setGroomName(request.getGroomName());
        certificate.setGroomEmail(request.getGroomEmail());
        certificate.setBrideName(request.getBrideName());
        certificate.setBrideEmail(request.getBrideEmail());
        certificate.setMarriageDate(request.getMarriageDate());
        certificate.setCampus(request.getCampus());
        certificate.setMinister(request.getMinister());
        certificate.setMaidOfHonor(request.getMaidOfHonor());
        certificate.setBestMan(request.getBestMan());
        certificate.setSubject(request.getSubject());
        certificate.setAdditionalMessage(request.getAdditionalMessage());
        certificate.setCertificateNumber(generateCertificateNumber());
        certificate.setStatus("Sent");
        certificate.setSubmittedAt(LocalDateTime.now());

        MarriageCertificate saved = marriageCertificateRepository.save(certificate);

        sendCertificateEmails(saved, request.getAdditionalMessage());

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<MarriageCertificateResponse> getAllCertificates() {
        User currentUser = userService.getCurrentUser();

        List<MarriageCertificate> certificates;
        if (isSuperAdminOrAdmin(currentUser)) {
            certificates = marriageCertificateRepository.findAllByOrderBySubmittedAtDesc();
        } else if (isNationalLeader(currentUser)) {
            certificates = marriageCertificateRepository.findByRegionForNationalLeader(currentUser.getRegion());
        } else if (isNationalLeaderOrCoordinator(currentUser)) {
            certificates = marriageCertificateRepository.findByUserIdOrSameCampusCoordinators(
                    currentUser.getId(), userService.resolveCampusName(currentUser.getEmail()));
        } else {
            certificates = marriageCertificateRepository.findByUserIdOrderBySubmittedAtDesc(currentUser.getId());
        }

        return certificates.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MarriageCertificateResponse getCertificateById(Long id) {
        MarriageCertificate certificate = marriageCertificateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Marriage certificate not found with id: " + id));

        User currentUser = userService.getCurrentUser();
        boolean isCreator = certificate.getUser() != null
                && certificate.getUser().getId().equals(currentUser.getId());
        boolean sameRegion = isNationalLeader(currentUser)
                && currentUser.getRegion() != null
                && certificate.getUser() != null
                && currentUser.getRegion().equalsIgnoreCase(certificate.getUser().getRegion());
        boolean sameCampus = !isNationalLeader(currentUser)
                && !isSuperAdminOrAdmin(currentUser)
                && isSameCampusCertificate(currentUser, certificate);
        if (!isSuperAdminOrAdmin(currentUser) && !isCreator && !sameRegion && !sameCampus) {
            throw new RuntimeException("Access denied");
        }

        return mapToResponse(certificate);
    }

    public void deleteCertificate(Long id) {
        MarriageCertificate certificate = marriageCertificateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Marriage certificate not found with id: " + id));

        User currentUser = userService.getCurrentUser();
        boolean isCreator = certificate.getUser() != null
                && certificate.getUser().getId().equals(currentUser.getId());
        boolean sameRegion = isNationalLeader(currentUser)
                && currentUser.getRegion() != null
                && certificate.getUser() != null
                && currentUser.getRegion().equalsIgnoreCase(certificate.getUser().getRegion());
        boolean sameCampus = !isNationalLeader(currentUser)
                && !isSuperAdminOrAdmin(currentUser)
                && isSameCampusCertificate(currentUser, certificate);
        if (!isSuperAdminOrAdmin(currentUser) && !isCreator && !sameRegion && !sameCampus) {
            throw new RuntimeException("Access denied");
        }

        marriageCertificateRepository.delete(certificate);
    }

    public MarriageCertificateResponse updateStatus(Long id, String status, String rejectionReason) {
        User currentUser = userService.getCurrentUser();
        if (!isSuperAdminOrAdmin(currentUser)) {
            throw new RuntimeException("Only admins can approve or reject marriage certificates");
        }

        MarriageCertificate certificate = marriageCertificateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Marriage certificate not found with id: " + id));

        String normalizedStatus = status.toUpperCase();
        if (!"APPROVED".equals(normalizedStatus) && !"REJECTED".equals(normalizedStatus)) {
            throw new IllegalArgumentException("Status must be APPROVED or REJECTED");
        }

        String displayStatus = "APPROVED".equals(normalizedStatus) ? "Approved" : "Rejected";
        certificate.setStatus(displayStatus);
        if ("REJECTED".equals(normalizedStatus)) {
            certificate.setRejectionReason(rejectionReason);
        }

        MarriageCertificate saved = marriageCertificateRepository.save(certificate);
        return mapToResponse(saved);
    }

    private void sendCertificateEmails(MarriageCertificate certificate, String additionalMessage) {
        String marriageDateStr = certificate.getMarriageDate() != null
                ? certificate.getMarriageDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy")) : "";

        Map<String, Object> templateData = Map.of(
                "groomName", certificate.getGroomName(),
                "brideName", certificate.getBrideName(),
                "marriageDate", marriageDateStr,
                "campus", certificate.getCampus(),
                "minister", certificate.getMinister(),
                "maidOfHonor", certificate.getMaidOfHonor() != null ? certificate.getMaidOfHonor() : "",
                "bestMan", certificate.getBestMan() != null ? certificate.getBestMan() : "",
                "certificateNumber", certificate.getCertificateNumber(),
                "status", certificate.getStatus(),
                "additionalMessage", additionalMessage != null ? additionalMessage : ""
        );

        String subject = certificate.getSubject() != null && !certificate.getSubject().isBlank()
                ? certificate.getSubject()
                : "Marriage Certificate - " + certificate.getGroomName() + " & " + certificate.getBrideName();

        SendEmailRequest request = new SendEmailRequest(
                List.of(certificate.getGroomEmail(), certificate.getBrideEmail()),
                subject,
                "marriage-certificate",
                templateData,
                null,
                null
        );

        boolean sent = emailService.sendEmail(request);
        if (!sent) {
            throw new RuntimeException("Emails could not be sent to both partners. Please check the server logs for details.");
        }
    }

    private String generateCertificateNumber() {
        String prefix = "MC";
        long count = marriageCertificateRepository.countTotal() + 1;
        return prefix + "-" + String.format("%05d", count);
    }

    private MarriageCertificateResponse mapToResponse(MarriageCertificate certificate) {
        User u = certificate.getUser();
        String submitterName = u.getFirstName() + " " + u.getLastName();
        String roleName = u.getUserRoles().stream()
                .filter(UserRole::getIsActive)
                .map(ur -> ur.getRole().getName())
                .findFirst()
                .orElse("USER");

        String marriageDateStr = certificate.getMarriageDate() != null
                ? certificate.getMarriageDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy")) : null;
        String submittedStr = certificate.getSubmittedAt() != null
                ? certificate.getSubmittedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy, h:mm a")) : null;

        return new MarriageCertificateResponse(
                certificate.getId(),
                submitterName,
                roleName,
                certificate.getGroomName(),
                certificate.getGroomEmail(),
                certificate.getBrideName(),
                certificate.getBrideEmail(),
                marriageDateStr,
                certificate.getCampus(),
                certificate.getMinister(),
                certificate.getMaidOfHonor(),
                certificate.getBestMan(),
                certificate.getSubject(),
                certificate.getAdditionalMessage(),
                certificate.getCertificateNumber(),
                certificate.getStatus(),
                certificate.getRejectionReason(),
                submittedStr
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

    private boolean isSameCampusCertificate(User user, MarriageCertificate certificate) {
        if (certificate.getUser() == null || isNationalLeader(user) || isSuperAdminOrAdmin(user)) {
            return false;
        }
        if (certificate.getUser().getId().equals(user.getId())) {
            return true;
        }
        boolean submitterIsCoordinator = certificate.getUser().getUserRoles().stream()
                .filter(UserRole::getIsActive)
                .anyMatch(ur -> ur.getRole().getName().equals("COORDINATOR"));
        if (!submitterIsCoordinator) {
            return false;
        }
        String campus = userService.resolveCampusName(user.getEmail());
        if (campus == null) {
            return false;
        }
        return campus.equalsIgnoreCase(userService.resolveCampusName(certificate.getUser().getEmail()));
    }
}
