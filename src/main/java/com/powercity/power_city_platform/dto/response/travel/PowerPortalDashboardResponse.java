package com.powercity.power_city_platform.dto.response.travel;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PowerPortalDashboardResponse(
        FormTypeStats travelForms,
        FormTypeStats childDedications,
        FormTypeStats marriageCertificates,
        List<RecentActivity> recentActivity,
        List<PendingApproval> pendingApprovals
) {
    public record FormTypeStats(int total, int pending, int approved) {}

    public record RecentActivity(String type, String action, String date, String status) {}

    public record PendingApproval(String formNo, String person, String campus, String date) {}
}
