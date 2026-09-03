package com.powercity.power_city_platform.service;

import com.powercity.power_city_platform.dto.request.announcement.CreateAnnouncementRequest;
import com.powercity.power_city_platform.dto.response.announcement.AnnouncementResponse;
import com.powercity.power_city_platform.entity.Announcement;
import com.powercity.power_city_platform.entity.User;
import com.powercity.power_city_platform.exception.ResourceNotFoundException;
import com.powercity.power_city_platform.repository.AnnouncementRepository;
import com.powercity.power_city_platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;

    private boolean isAdminOrSuperAdmin(User user) {
        User managed = getManagedUser(user);
        return managed.getUserRoles() != null && managed.getUserRoles().stream()
                .anyMatch(ur -> Boolean.TRUE.equals(ur.getIsActive()) && (
                        ur.getRole() != null && (ur.getRole().getName().equals("ADMIN")
                                || ur.getRole().getName().equals("SUPER_ADMIN"))));
    }

    private boolean isNationalLeader(User user) {
        User managed = getManagedUser(user);
        return managed.getUserRoles() != null && managed.getUserRoles().stream()
                .anyMatch(ur -> Boolean.TRUE.equals(ur.getIsActive())
                        && ur.getRole() != null && ur.getRole().getName().equals("NATIONAL_LEADER"));
    }

    private boolean isCoordinator(User user) {
        User managed = getManagedUser(user);
        return managed.getUserRoles() != null && managed.getUserRoles().stream()
                .anyMatch(ur -> Boolean.TRUE.equals(ur.getIsActive())
                        && ur.getRole() != null && ur.getRole().getName().equals("COORDINATOR"));
    }

    // The User passed from the controller is detached (loaded in its own read-only
    // transaction), so its lazy userRoles collection cannot be read without throwing
    // a LazyInitializationException. Re-fetch it within the current transaction so
    // the roles are initialized and role-based checks work reliably.
    private User getManagedUser(User user) {
        if (user == null || user.getId() == null) return user;
        User refreshed = userRepository.findById(user.getId()).orElse(null);
        return refreshed != null ? refreshed : user;
    }

    public AnnouncementResponse createAnnouncement(CreateAnnouncementRequest request, User currentUser) {
        String region = currentUser.getRegion();

        // Determine recipient count based on target
        Integer recipientCount;
        if (request.getTargetValue() != null && !request.getTargetValue().isBlank()
                && !"all".equalsIgnoreCase(request.getTargetValue())) {
            recipientCount = 1;
        } else if (region != null) {
            try {
                recipientCount = userRepository.findCoordinators(region, "").size();
            } catch (Exception e) {
                recipientCount = 0;
            }
        } else {
            recipientCount = 0;
        }

        Announcement announcement = Announcement.builder()
                .title(request.getTitle())
                .body(request.getBody())
                .region(region)
                .target(request.getTarget())
                .targetValue(request.getTargetValue())
                .recipientCount(recipientCount)
                .portalNotification(request.getPortalNotification())
                .emailAlert(request.getEmailAlert())
                .senderName(currentUser.getFullName())
                .build();

        announcement.setCreatedBy(currentUser.getId());
        announcement.setUpdatedBy(currentUser.getId());

        Announcement saved = announcementRepository.save(announcement);

        fireNotificationsAndEmails(saved, currentUser);

        return new AnnouncementResponse(saved);
    }

    private void fireNotificationsAndEmails(Announcement saved, User sender) {
        try {
            notificationService.notifyAnnouncement(saved);
        } catch (Exception e) {
            log.error("Error creating notifications for announcement {}", saved.getId(), e);
        }

        if (Boolean.TRUE.equals(saved.getEmailAlert())) {
            try {
                List<User> recipients = userRepository.findCoordinators(saved.getRegion(), "");
                for (User recipient : recipients) {
                    emailService.sendAnnouncementEmail(
                            recipient.getEmail(),
                            recipient.getFirstName() != null ? recipient.getFirstName() : recipient.getFullName(),
                            saved.getTitle(),
                            saved.getBody(),
                            saved.getSenderName() != null ? saved.getSenderName() : sender.getFullName()
                    );
                }
            } catch (Exception e) {
                log.error("Error sending announcement emails for {}", saved.getId(), e);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<AnnouncementResponse> getAllAnnouncements(User currentUser) {
        List<Announcement> announcements;
        if (isAdminOrSuperAdmin(currentUser)) {
            announcements = announcementRepository.findAllOrderByCreatedAtDesc();
        } else if (isNationalLeader(currentUser) && currentUser.getRegion() != null) {
            announcements = announcementRepository.findByRegionOrderByCreatedAtDesc(currentUser.getRegion());
        } else if (currentUser.getRegion() != null) {
            announcements = announcementRepository.findByRegionOrderByCreatedAtDesc(currentUser.getRegion());
        } else {
            return Collections.emptyList();
        }
        return announcements.stream().map(AnnouncementResponse::new).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AnnouncementResponse> getAnnouncementsForRecipient(User currentUser) {
        if (currentUser.getRegion() == null) {
            return Collections.emptyList();
        }
        List<Announcement> announcements = announcementRepository
                .findByRegionOrderByCreatedAtDesc(currentUser.getRegion());
        return announcements.stream().map(AnnouncementResponse::new).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AnnouncementResponse getAnnouncementById(Long id, User currentUser) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement not found"));
        if (!isAdminOrSuperAdmin(currentUser)) {
            boolean sameRegion = currentUser.getRegion() != null
                    && announcement.getRegion() != null
                    && currentUser.getRegion().equalsIgnoreCase(announcement.getRegion());
            if (!sameRegion) {
                throw new RuntimeException("Unauthorized access to this announcement");
            }
        }
        return new AnnouncementResponse(announcement);
    }

    public void deleteAnnouncement(Long id, User currentUser) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement not found"));
        if (!isAdminOrSuperAdmin(currentUser)) {
            boolean isCreator = announcement.getCreatedBy() != null
                    && announcement.getCreatedBy().equals(currentUser.getId());
            boolean sameRegion = currentUser.getRegion() != null
                    && announcement.getRegion() != null
                    && currentUser.getRegion().equalsIgnoreCase(announcement.getRegion());
            if (!(isNationalLeader(currentUser) && sameRegion) && !isCreator) {
                throw new RuntimeException("Unauthorized access to this announcement");
            }
        }
        announcementRepository.delete(announcement);
    }
}
