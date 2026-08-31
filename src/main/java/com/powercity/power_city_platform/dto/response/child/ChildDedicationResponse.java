package com.powercity.power_city_platform.dto.response.child;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChildDedicationResponse(
        Long id,
        String submitter,
        String role,
        String childName,
        String dedicationDate,
        String parentName,
        String campus,
        String minister,
        String certificateNumber,
        String status,
        String submitted,
        String reviewedBy,
        String reviewedAt,
        String rejectionReason
) {}
