package com.powercity.power_city_platform.enums;

public enum Currency {
    GBP("GBP", "£", "British Pound Sterling", "UK"),
    ZAR("ZAR", "R", "South African Rand", "South Africa"),
    USD("USD", "$", "US Dollar", "USA"),
    GHS("GHS", "₵", "Ghanaian Cedi", "Ghana"),
    NGN("NGN", "₦", "Nigerian Naira", "Nigeria");

    private final String code;
    private final String symbol;
    private final String displayName;
    private final String region;

    Currency(String code, String symbol, String displayName, String region) {
        this.code = code;
        this.symbol = symbol;
        this.displayName = displayName;
        this.region = region;
    }

    public String getCode() {
        return code;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getRegion() {
        return region;
    }

    public static Currency fromCode(String code) {
        for (Currency currency : values()) {
            if (currency.code.equals(code)) {
                return currency;
            }
        }
        throw new IllegalArgumentException("Invalid currency code: " + code);
    }

    public static Currency fromRegion(String region) {
        for (Currency currency : values()) {
            if (currency.region.equalsIgnoreCase(region)) {
                return currency;
            }
        }
        return USD; // Default to USD if region not found
    }

    @Override
    public String toString() {
        return code;
    }
}
