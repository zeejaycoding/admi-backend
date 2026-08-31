package com.powercity.power_city_platform.dto.response.user;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RecentActivityResponse(
        String type, // COURSE_ENROLLMENT, EVENT_REGISTRATION, PURCHASE, DONATION
        String title,
        String description,
        LocalDateTime timestamp,
        String relatedId,
        String status
) {}