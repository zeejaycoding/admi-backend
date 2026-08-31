package com.powercity.power_city_platform.enums;

public enum RoleType {
    SUPER_ADMIN("SUPER_ADMIN"),
    ADMIN("ADMIN"),
    COORDINATOR("COORDINATOR"),
    USER("USER"),
    STUDENT("STUDENT"),
    CUSTOMER("CUSTOMER"),
    NATIONAL_LEADER("NATIONAL_LEADER");

    private final String value;

    RoleType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}