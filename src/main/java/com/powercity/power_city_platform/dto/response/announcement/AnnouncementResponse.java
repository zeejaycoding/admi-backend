package com.powercity.power_city_platform.dto.response.announcement;

import com.powercity.power_city_platform.entity.Announcement;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AnnouncementResponse {

    private Long id;
    private String title;
    private String body;
    private String region;
    private String target;
    private String targetValue;
    private Integer recipientCount;
    private Boolean portalNotification;
    private Boolean emailAlert;
    private String senderName;
    private Long createdBy;
    private LocalDateTime createdAt;

    public AnnouncementResponse(Announcement a) {
        this.id = a.getId();
        this.title = a.getTitle();
        this.body = a.getBody();
        this.region = a.getRegion();
        this.target = a.getTarget();
        this.targetValue = a.getTargetValue();
        this.recipientCount = a.getRecipientCount();
        this.portalNotification = a.getPortalNotification();
        this.emailAlert = a.getEmailAlert();
        this.senderName = a.getSenderName();
        this.createdBy = a.getCreatedBy();
        this.createdAt = a.getCreatedAt();
    }
}
