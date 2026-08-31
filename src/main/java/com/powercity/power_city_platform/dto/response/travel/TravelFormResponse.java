package com.powercity.power_city_platform.dto.response.travel;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TravelFormResponse(
        Long id,
        String submitter,
        String role,
        String country,
        String travelDate,
        String returnDate,
        Integer days,
        String reason,
        String campus,
        String status,
        String submitted,
        String reviewedBy,
        String reviewedAt,
        String rejectionReason
) {}
