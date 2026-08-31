package com.powercity.power_city_platform.dto.response.donation;

import com.powercity.power_city_platform.enums.Currency;
import com.powercity.power_city_platform.enums.OfferingType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DonationResponse {

    private Long id;
    private String donorEmail;
    private String donorName;
    private BigDecimal amount;
    private Currency currency;
    private OfferingType offeringType;
    private String paymentGateway;
    private String paypalOrderId;
    private String paypalCaptureId;
    private String status;
    private LocalDateTime donationDate;
    private String message;
    private Boolean isAnonymous;
    private Boolean receiptSent;
    private LocalDateTime receiptSentAt;
    private LocalDateTime createdAt;
}
