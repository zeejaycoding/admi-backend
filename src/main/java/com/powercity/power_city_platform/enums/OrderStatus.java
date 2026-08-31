package com.powercity.power_city_platform.enums;

public enum OrderStatus {
    PENDING("PENDING", "Order created, awaiting payment"),
    PROCESSING("PROCESSING", "Payment received, processing order"),
    COMPLETED("COMPLETED", "Order completed successfully"),
    CANCELLED("CANCELLED", "Order cancelled"),
    REFUNDED("REFUNDED", "Order refunded"),
    FAILED("FAILED", "Order failed"),
    EXPIRED("EXPIRED", "Order expired");

    private final String value;
    private final String description;

    OrderStatus(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public String getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    public static OrderStatus fromValue(String value) {
        for (OrderStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid order status: " + value);
    }
}
