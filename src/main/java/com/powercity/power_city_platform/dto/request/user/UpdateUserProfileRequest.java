package com.powercity.power_city_platform.dto.request.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserProfileRequest(
        @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
        String fullName,

        @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Phone number should be valid")
        String phoneNumber,

        @Pattern(regexp = "^(UK|SOUTH_AFRICA|USA|GHANA|NIGERIA)$",
                message = "Region must be one of: UK, SOUTH_AFRICA, USA, GHANA, NIGERIA")
        String region,

        String bio,

        String profileImageUrl
) {}