package com.powercity.power_city_platform.dto.response.user;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserDashboardResponse(
        UserProfileResponse profile,
        List<RecentActivityResponse> recentActivities,
        UserStatsResponse stats,
        List<String> recommendations
) {}