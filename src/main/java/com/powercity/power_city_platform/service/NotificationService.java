package com.powercity.power_city_platform.service;

import com.powercity.power_city_platform.dto.response.notification.NotificationResponse;
import com.powercity.power_city_platform.entity.Announcement;
import com.powercity.power_city_platform.entity.Notification;
import com.powercity.power_city_platform.entity.User;
import com.powercity.power_city_platform.exception.ResourceNotFoundException;
import com.powercity.power_city_platform.repository.NotificationRepository;
import com.powercity.power_city_platform.repository.UserRepository;
import com.powercity.power_city_platform.websocket.NotificationWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationWebSocketHandler webSocketHandler;

    @Transactional
    public void notifyAnnouncement(Announcement announcement) {
        if (!Boolean.TRUE.equals(announcement.getPortalNotification())) {
            return;
        }
        String region = announcement.getRegion();
        if (region == null) {
            return;
        }
        List<Long> recipientIds;
        try {
            recipientIds = userRepository.findCoordinators(region, "").stream()
                    .map(User::getId)
                    .distinct()
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error resolving announcement recipients in region {}", region, e);
            return;
        }
        if (recipientIds.isEmpty()) {
            return;
        }

        List<User> recipients = userRepository.findAllById(recipientIds);
        for (User recipient : recipients) {
            Notification notification = Notification.builder()
                    .userId(recipient.getId())
                    .type("ANNOUNCEMENT")
                    .title(announcement.getTitle())
                    .body(announcement.getBody())
                    .link(null)
                    .announcementId(announcement.getId())
                    .read(false)
                    .build();
            notification.setCreatedBy(announcement.getCreatedBy());
            notification.setUpdatedBy(announcement.getUpdatedBy());
            Notification saved = notificationRepository.save(notification);

            NotificationResponse response = new NotificationResponse(saved);
            webSocketHandler.sendToUser(recipient.getId(),
                    Map.of("type", "new_notification", "data", response));
            logger.info("Notification delivered in real-time to user {}", recipient.getId());
        }
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getForUser(User currentUser) {
        return notificationRepository.findTop50ByUserIdOrderByCreatedAtDesc(currentUser.getId())
                .stream().map(NotificationResponse::new).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(User currentUser) {
        return notificationRepository.countUnreadByUserId(currentUser.getId());
    }

    @Transactional
    public void markAllRead(User currentUser) {
        notificationRepository.markAllReadForUser(currentUser.getId());
        pushUnreadCount(currentUser.getId());
    }

    @Transactional
    public void markRead(Long id, User currentUser) {
        int updated = notificationRepository.markReadByIdAndUser(id, currentUser.getId());
        if (updated == 0) {
            throw new ResourceNotFoundException("Notification not found");
        }
        pushUnreadCount(currentUser.getId());
    }

    private void pushUnreadCount(Long userId) {
        try {
            long count = notificationRepository.countUnreadByUserId(userId);
            webSocketHandler.sendToUser(userId,
                    Map.of("type", "notifications_updated", "data", Map.of("unread", count)));
        } catch (Exception e) {
            logger.warn("Error pushing unread count to user {}", userId, e);
        }
    }
}
