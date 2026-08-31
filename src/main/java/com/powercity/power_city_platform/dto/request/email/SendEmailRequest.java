package com.powercity.power_city_platform.dto.request.email;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.Map;

public record SendEmailRequest(
        @NotEmpty(message = "Recipients list cannot be empty")
        List<@Email(message = "Invalid email format") String> to,

        @NotBlank(message = "Subject is required")
        String subject,

        @NotBlank(message = "Template name is required")
        String templateName,

        Map<String, Object> templateData,

        List<String> cc,
        List<String> bcc
) {}