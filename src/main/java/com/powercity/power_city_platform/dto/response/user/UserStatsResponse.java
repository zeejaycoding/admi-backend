package com.powercity.power_city_platform.dto.response.user;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserStatsResponse(
        Integer coursesEnrolled,
        Integer coursesCompleted,
        Integer eventsAttended,
        Integer totalOrders,
        BigDecimal totalSpent,
        BigDecimal totalDonated,
        Integer completionPercentage
) {}