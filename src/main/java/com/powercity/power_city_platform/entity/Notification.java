package com.powercity.power_city_platform.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "body", columnDefinition = "TEXT")
    private String body;

    @Column(name = "link")
    private String link;

    @Column(name = "announcement_id")
    private Long announcementId;

    @Column(name = "is_read", nullable = false)
    private Boolean read = false;

    @Column(name = "read_at")
    private LocalDateTime readAt;
}
