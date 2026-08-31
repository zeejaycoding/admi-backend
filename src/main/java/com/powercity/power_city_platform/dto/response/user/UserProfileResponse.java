package com.powercity.power_city_platform.dto.response.user;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Set;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserProfileResponse(
        Long id,
        String email,
        String fullName,
        String phoneNumber,
        String region,
        String currency,
        String bio,
        String profileImageUrl,
        Boolean isEmailVerified,
        Boolean isActive,
        Set<String> roles,
        Set<String> permissions,
        LocalDateTime lastLogin,
        LocalDateTime createdAt,
        UserStatsResponse stats
) {}