package com.powercity.power_city_platform.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "announcements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Announcement extends BaseEntity {

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "body", columnDefinition = "TEXT", nullable = false)
    private String body;

    @Column(name = "region")
    private String region;

    @Column(name = "target")
    private String target;

    @Column(name = "target_value")
    private String targetValue;

    @Column(name = "recipient_count")
    private Integer recipientCount;

    @Column(name = "portal_notification", nullable = false)
    private Boolean portalNotification = true;

    @Column(name = "email_alert", nullable = false)
    private Boolean emailAlert = false;

    @Column(name = "sender_name")
    private String senderName;
}
