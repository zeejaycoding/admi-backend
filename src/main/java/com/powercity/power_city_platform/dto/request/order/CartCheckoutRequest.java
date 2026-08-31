package com.powercity.power_city_platform.dto.request.order;

import com.powercity.power_city_platform.enums.Currency;
import jakarta.validation.constraints.*;

public class CartCheckoutRequest {

    @NotNull(message = "Currency is required")
    private Currency currency;

    public CartCheckoutRequest() {}

    public CartCheckoutRequest(Currency currency) {
        this.currency = currency;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }
}
