package com.powercity.power_city_platform.dto.request.order;

import com.powercity.power_city_platform.enums.Currency;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public class BookPurchaseRequest {

    @NotNull(message = "Book ID is required")
    private Long bookId;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    private BigDecimal price;

    @NotNull(message = "Currency is required")
    private Currency currency = Currency.USD;

    public BookPurchaseRequest() {}

    public BookPurchaseRequest(Long bookId, BigDecimal price, Currency currency) {
        this.bookId = bookId;
        this.price = price;
        this.currency = currency;
    }

    public Long getBookId() {
        return bookId;
    }

    public void setBookId(Long bookId) {
        this.bookId = bookId;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }
} 