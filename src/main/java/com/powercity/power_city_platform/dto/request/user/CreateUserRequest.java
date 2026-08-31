package com.powercity.power_city_platform.dto.request.user;

import jakarta.validation.constraints.*;

import java.util.Set;

public record CreateUserRequest(
        @NotBlank(message = "Full name is required")
        @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
        String fullName,

        @NotBlank(message = "Email is required")
        @Email(message = "Please provide a valid email address")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 6, message = "Password must be at least 6 characters")
        String password,

        @Size(max = 20, message = "Phone number must not exceed 20 characters")
        String phoneNumber,

        @NotEmpty(message = "At least one role is required")
        Set<String> roles,

        Boolean emailVerified,
        Boolean enabled
) {}
