package com.powercity.power_city_platform.dto.response.user;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Set;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserListResponse(
        Long id,
        String email,
        String fullName,
        String region,
        Set<String> roles,
        Boolean isActive,
        Boolean isEmailVerified,
        Boolean createdByAdmin,
        LocalDateTime lastLogin,
        LocalDateTime createdAt,
        Boolean flaggedForReview,
        String reviewTagReason,
        LocalDateTime reviewTaggedAt,
        String campusName
) {}
