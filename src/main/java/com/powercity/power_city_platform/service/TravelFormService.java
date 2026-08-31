package com.powercity.power_city_platform.service;

import com.powercity.power_city_platform.dto.request.travel.TravelFormCreateRequest;
import com.powercity.power_city_platform.dto.response.travel.PowerPortalDashboardResponse;
import com.powercity.power_city_platform.dto.response.travel.TravelFormResponse;
import com.powercity.power_city_platform.entity.TravelForm;
import com.powercity.power_city_platform.entity.User;
import com.powercity.power_city_platform.entity.UserRole;
import com.powercity.power_city_platform.exception.ResourceNotFoundException;
import com.powercity.power_city_platform.entity.ChildDedication;
import com.powercity.power_city_platform.repository.ChildDedicationRepository;
import com.powercity.power_city_platform.repository.MarriageCertificateRepository;
import com.powercity.power_city_platform.repository.TravelFormRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class TravelFormService {

    private final TravelFormRepository travelFormRepository;
    private final ChildDedicationRepository childDedicationRepository;
    private final MarriageCertificateRepository marriageCertificateRepository;
    private final UserService userService;

    public TravelFormResponse createTravelForm(TravelFormCreateRequest request) {
        User currentUser = userService.getCurrentUser();

        TravelForm form = new TravelForm();
        form.setUser(currentUser);
        form.setCountry(request.getCountry());
        form.setTravelDate(request.getTravelDate());
        form.setReturnDate(request.getReturnDate());
        form.setDays(request.getDays());
        form.setReason(request.getReason());
        form.setCampus(request.getCampus() != null ? request.getCampus() : currentUser.getRegion());
        form.setStatus("Pending");
        form.setSubmittedAt(LocalDateTime.now());

        TravelForm saved = travelFormRepository.save(form);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<TravelFormResponse> getAllTravelForms() {
        User currentUser = userService.getCurrentUser();
        List<TravelForm> forms;

        if (isAdminOrReviewer(currentUser)) {
            forms = travelFormRepository.findAllByOrderBySubmittedAtDesc();
        } else {
            forms = travelFormRepository.findByUserIdOrderBySubmittedAtDesc(currentUser.getId());
        }

        return forms.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TravelFormResponse getTravelFormById(Long id) {
        User currentUser = userService.getCurrentUser();
        TravelForm form = travelFormRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Travel form not found with id: " + id));

        if (!isAdminOrReviewer(currentUser) && !form.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Access denied");
        }

        return mapToResponse(form);
    }

    public TravelFormResponse updateStatus(Long id, String status, String rejectionReason) {
        User currentUser = userService.getCurrentUser();
        TravelForm form = travelFormRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Travel form not found with id: " + id));

        String normalizedStatus = status.toUpperCase();
        if (!"APPROVED".equals(normalizedStatus) && !"REJECTED".equals(normalizedStatus)) {
            throw new IllegalArgumentException("Status must be APPROVED or REJECTED");
        }

        String displayStatus = "APPROVED".equals(normalizedStatus) ? "Approved" : "Rejected";
        form.setStatus(displayStatus);
        form.setReviewedBy(currentUser);
        form.setReviewedAt(LocalDateTime.now());
        if ("REJECTED".equals(normalizedStatus)) {
            form.setRejectionReason(rejectionReason);
        }

        TravelForm saved = travelFormRepository.save(form);
        return mapToResponse(saved);
    }

    public void deleteTravelForm(Long id) {
        User currentUser = userService.getCurrentUser();
        TravelForm form = travelFormRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Travel form not found with id: " + id));

        if (!isAdminOrReviewer(currentUser) && !form.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Access denied");
        }

        travelFormRepository.delete(form);
    }

    @Transactional(readOnly = true)
    public PowerPortalDashboardResponse getDashboardStats() {
        long totalTravel = travelFormRepository.countTotal();
        long pendingTravelCount = travelFormRepository.countByStatus("Pending");
        long approvedTravelCount = travelFormRepository.countByStatus("Approved");

        List<TravelForm> recentTravel = travelFormRepository.findRecent();
        List<TravelForm> pendingTravelForms = travelFormRepository.findPendingApprovals();

        List<PowerPortalDashboardResponse.RecentActivity> activities = new ArrayList<>();

        for (TravelForm tf : recentTravel.stream().limit(3).toList()) {
            activities.add(new PowerPortalDashboardResponse.RecentActivity(
                    "Travelling Form",
                    tf.getStatus().equals("Pending") ? "Submitted" : tf.getStatus().equals("Approved") ? "Approved" : "Rejected",
                    tf.getSubmittedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy, h:mm a")),
                    tf.getStatus()
            ));
        }

        for (ChildDedication cd : childDedicationRepository.findRecent().stream().limit(3).toList()) {
            activities.add(new PowerPortalDashboardResponse.RecentActivity(
                    "Child Dedication",
                    cd.getStatus().equals("Pending") ? "Submitted" : cd.getStatus().equals("Approved") ? "Approved" : "Rejected",
                    cd.getSubmittedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy, h:mm a")),
                    cd.getStatus()
            ));
        }

        activities.sort(Comparator.comparing(PowerPortalDashboardResponse.RecentActivity::date).reversed());
        List<PowerPortalDashboardResponse.RecentActivity> finalActivities = activities.size() > 5
                ? new ArrayList<>(activities.subList(0, 5)) : activities;

        List<PowerPortalDashboardResponse.PendingApproval> approvals = new ArrayList<>();

        for (TravelForm tf : pendingTravelForms.stream().limit(5).toList()) {
            approvals.add(new PowerPortalDashboardResponse.PendingApproval(
                    "TRV-" + String.format("%05d", tf.getId()),
                    tf.getUser().getFirstName() + " " + tf.getUser().getLastName(),
                    tf.getCampus() != null ? tf.getCampus() : "",
                    tf.getSubmittedAt().format(DateTimeFormatter.ofPattern("dd MMM"))
            ));
        }

        for (ChildDedication cd : childDedicationRepository.findPendingApprovals().stream().limit(5).toList()) {
            approvals.add(new PowerPortalDashboardResponse.PendingApproval(
                    cd.getCertificateNumber(),
                    cd.getUser().getFirstName() + " " + cd.getUser().getLastName(),
                    cd.getCampus() != null ? cd.getCampus() : "",
                    cd.getSubmittedAt().format(DateTimeFormatter.ofPattern("dd MMM"))
            ));
        }

        List<PowerPortalDashboardResponse.PendingApproval> finalApprovals = approvals.size() > 10
                ? new ArrayList<>(approvals.subList(0, 10)) : approvals;

        int childTotal = (int) childDedicationRepository.countTotal();
        int childPending = (int) childDedicationRepository.countByStatus("Pending");
        int childApproved = (int) childDedicationRepository.countByStatus("Approved");

        int marriageTotal = (int) marriageCertificateRepository.countTotal();
        int marriagePending = (int) marriageCertificateRepository.countByStatus("Pending");
        int marriageApproved = (int) marriageCertificateRepository.countByStatus("Approved");

        return new PowerPortalDashboardResponse(
                new PowerPortalDashboardResponse.FormTypeStats((int) totalTravel, (int) pendingTravelCount, (int) approvedTravelCount),
                new PowerPortalDashboardResponse.FormTypeStats(childTotal, childPending, childApproved),
                new PowerPortalDashboardResponse.FormTypeStats(marriageTotal, marriagePending, marriageApproved),
                finalActivities,
                finalApprovals
        );
    }

    private TravelFormResponse mapToResponse(TravelForm form) {
        User u = form.getUser();
        String submitterName = u.getFirstName() + " " + u.getLastName();
        String roleName = u.getUserRoles().stream()
                .filter(UserRole::getIsActive)
                .map(ur -> ur.getRole().getName())
                .findFirst()
                .orElse("USER");

        String travelDateStr = form.getTravelDate() != null
                ? form.getTravelDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy")) : null;
        String returnDateStr = form.getReturnDate() != null
                ? form.getReturnDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy")) : null;
        String submittedStr = form.getSubmittedAt() != null
                ? form.getSubmittedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy")) : null;
        String reviewedStr = form.getReviewedAt() != null
                ? form.getReviewedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy, h:mm a")) : null;
        String reviewedByName = form.getReviewedBy() != null
                ? form.getReviewedBy().getFirstName() + " " + form.getReviewedBy().getLastName() : null;

        return new TravelFormResponse(
                form.getId(),
                submitterName,
                roleName,
                form.getCountry(),
                travelDateStr,
                returnDateStr,
                form.getDays(),
                form.getReason(),
                form.getCampus(),
                form.getStatus(),
                submittedStr,
                reviewedByName,
                reviewedStr,
                form.getRejectionReason()
        );
    }

    private boolean isAdminOrReviewer(User user) {
        return user.getUserRoles().stream()
                .filter(UserRole::getIsActive)
                .anyMatch(ur -> {
                    String role = ur.getRole().getName();
                    return role.equals("SUPER_ADMIN") || role.equals("ADMIN") || role.equals("NATIONAL_LEADER");
                });
    }
}
