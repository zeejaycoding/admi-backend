package com.powercity.power_city_platform.dto.request.child;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class ChildDedicationEmailRequest {

    @NotBlank(message = "Recipient email is required")
    @Email(message = "Invalid email format")
    private String email;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
