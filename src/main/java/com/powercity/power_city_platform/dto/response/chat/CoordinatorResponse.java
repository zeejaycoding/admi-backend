package com.powercity.power_city_platform.dto.response.chat;

import java.time.LocalDateTime;

public record CoordinatorResponse(
        Long id,
        String fullName,
        String email,
        String region,
        String profileImageUrl
) {
}
