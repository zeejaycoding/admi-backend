package com.powercity.power_city_platform.enums;

public enum PaymentStatus {
    PENDING("PENDING", "Payment initiated, awaiting confirmation"),
    PROCESSING("PROCESSING", "Payment being processed"),
    COMPLETED("COMPLETED", "Payment completed successfully"),
    FAILED("FAILED", "Payment failed"),
    CANCELLED("CANCELLED", "Payment cancelled"),
    REFUNDED("REFUNDED", "Payment refunded"),
    PARTIALLY_REFUNDED("PARTIALLY_REFUNDED", "Payment partially refunded"),
    EXPIRED("EXPIRED", "Payment expired");

    private final String value;
    private final String description;

    PaymentStatus(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public String getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    public static PaymentStatus fromValue(String value) {
        for (PaymentStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid payment status: " + value);
    }
}
