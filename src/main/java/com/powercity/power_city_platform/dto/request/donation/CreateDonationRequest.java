package com.powercity.power_city_platform.dto.request.donation;

import com.powercity.power_city_platform.enums.Currency;
import com.powercity.power_city_platform.enums.OfferingType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateDonationRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
    private BigDecimal amount;

    @NotNull(message = "Currency is required")
    private Currency currency;

    @NotNull(message = "Offering type is required")
    private OfferingType offeringType;

    @NotBlank(message = "Donor email is required")
    @Email(message = "Invalid email format")
    private String donorEmail;

    // Optional custom success/cancel URLs (for region-aware redirects)
    private String successUrl;
    private String cancelUrl;
}
