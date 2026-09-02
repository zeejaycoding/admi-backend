package com.powercity.power_city_platform.dto.request.marriage;

import jakarta.validation.constraints.NotBlank;

public class MarriageCertificateStatusRequest {

    @NotBlank(message = "Status is required")
    private String status;

    private String rejectionReason;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
}
