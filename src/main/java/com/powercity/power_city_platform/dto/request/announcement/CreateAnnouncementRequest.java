package com.powercity.power_city_platform.dto.request.announcement;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateAnnouncementRequest {

    @NotBlank(message = "Announcement title is required")
    private String title;

    @NotBlank(message = "Announcement message is required")
    private String body;

    private String target;

    private String targetValue;

    @NotNull(message = "Portal notification flag is required")
    private Boolean portalNotification;

    @NotNull(message = "Email alert flag is required")
    private Boolean emailAlert;
}
