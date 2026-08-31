package com.powercity.power_city_platform.dto.response.email;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record EmailStatusResponse(
        String messageId,
        String status,
        String recipient,
        String subject,
        LocalDateTime sentAt,
        String errorMessage
) {}