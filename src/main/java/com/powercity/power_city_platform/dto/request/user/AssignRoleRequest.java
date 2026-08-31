package com.powercity.power_city_platform.dto.request.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AssignRoleRequest(
        @NotNull(message = "User ID is required")
        Long userId,

        @NotBlank(message = "Role name is required")
        String roleName,

        String reason // Optional reason for role assignment
) {}
