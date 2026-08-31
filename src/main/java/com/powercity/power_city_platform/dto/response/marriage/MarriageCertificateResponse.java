package com.powercity.power_city_platform.dto.response.marriage;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MarriageCertificateResponse(
        Long id,
        String submitter,
        String role,
        String groomName,
        String groomEmail,
        String brideName,
        String brideEmail,
        String marriageDate,
        String campus,
        String minister,
        String maidOfHonor,
        String bestMan,
        String subject,
        String additionalMessage,
        String certificateNumber,
        String status,
        String submitted
) {}
