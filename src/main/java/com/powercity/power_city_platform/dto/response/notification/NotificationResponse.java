package com.powercity.power_city_platform.dto.response.notification;

import com.powercity.power_city_platform.entity.Notification;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private Long id;
    private Long userId;
    private String type;
    private String title;
    private String body;
    private String link;
    private Long announcementId;
    private Boolean read;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;

    public NotificationResponse(Notification n) {
        this.id = n.getId();
        this.userId = n.getUserId();
        this.type = n.getType();
        this.title = n.getTitle();
        this.body = n.getBody();
        this.link = n.getLink();
        this.announcementId = n.getAnnouncementId();
        this.read = n.getRead();
        this.readAt = n.getReadAt();
        this.createdAt = n.getCreatedAt();
    }
}
