package com.powercity.power_city_platform.dto.response.user;

import java.util.List;
import java.util.Map;

public record PersonnelRecordsResponse(
        List<Map<String, Object>> travelForms,
        List<Map<String, Object>> childDedications,
        List<Map<String, Object>> marriageCertificates,
        List<Map<String, Object>> reports
) {}
