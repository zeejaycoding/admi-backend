package com.powercity.power_city_platform.dto.response.auth;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Set;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserInfo(
        Long id,
        String email,
        String fullName,
        String phoneNumber,
        String region,
        String currency,
        Boolean isEmailVerified,
        Set<String> roles,
        Set<String> permissions,
        LocalDateTime lastLogin
) {}