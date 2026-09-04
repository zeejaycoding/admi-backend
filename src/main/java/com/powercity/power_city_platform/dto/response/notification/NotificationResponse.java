package com.powercity.power_city_platform.dto.response.notification;

import com.powercity.power_city_platform.entity.Notification;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

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
    private Instant readAt;
    private Instant createdAt;

    public NotificationResponse(Notification n) {
        this.id = n.getId();
        this.userId = n.getUserId();
        this.type = n.getType();
        this.title = n.getTitle();
        this.body = n.getBody();
        this.link = n.getLink();
        this.announcementId = n.getAnnouncementId();
        this.read = n.getRead();
        this.readAt = toUtcInstant(n.getReadAt());
        this.createdAt = toUtcInstant(n.getCreatedAt());
    }

    // LocalDateTime is stored using the JVM's local wall clock (no timezone). Convert it
    // to the true UTC instant so clients can parse it unambiguously regardless of their
    // own timezone (avoids off-by-offset relative timestamps like "5 hours ago").
    private static Instant toUtcInstant(java.time.LocalDateTime dateTime) {
        if (dateTime == null) return null;
        OffsetDateTime odt = dateTime.atZone(ZoneId.systemDefault()).toOffsetDateTime();
        return odt.withOffsetSameInstant(ZoneOffset.UTC).toInstant();
    }
}
