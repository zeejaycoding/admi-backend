package com.powercity.power_city_platform.dto.request.user;

import jakarta.validation.constraints.NotBlank;

public record TagForReviewRequest(
        @NotBlank(message = "Reason for tagging is required")
        String reason
) {}
