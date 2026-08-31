package com.powercity.power_city_platform.enums;

public enum EmailType {
    WELCOME("WELCOME"),
    EMAIL_VERIFICATION("EMAIL_VERIFICATION"),
    PASSWORD_RESET("PASSWORD_RESET"),
    COURSE_ENROLLMENT("COURSE_ENROLLMENT"),
    EVENT_REGISTRATION("EVENT_REGISTRATION"),
    BOOK_PURCHASE("BOOK_PURCHASE"),
    NEWSLETTER("NEWSLETTER"),
    PROMOTIONAL("PROMOTIONAL"),
    SYSTEM_NOTIFICATION("SYSTEM_NOTIFICATION");

    private final String value;

    EmailType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}