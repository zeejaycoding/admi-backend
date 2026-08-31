package com.powercity.power_city_platform.dto.response.user;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserPlatformStatsResponse(
        Long totalUsers,
        Long activeUsers,
        Long verifiedUsers,
        Long newUsersThisMonth,
        Long newUsersToday,
        Map<String, Long> usersByRegion,
        Map<String, Long> usersByRole,
        Double averageLoginFrequency,
        LocalDateTime lastUpdated
) {}
