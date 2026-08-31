package com.powercity.power_city_platform.dto.response.user;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RoleAssignmentResponse(
        Long id,
        String roleName,
        String roleDescription,
        Boolean isActive,
        LocalDateTime assignedAt,
        Long assignedBy,
        String reason
) {}