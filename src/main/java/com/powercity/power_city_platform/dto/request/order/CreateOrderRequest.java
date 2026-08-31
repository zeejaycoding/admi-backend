package com.powercity.power_city_platform.dto.request.order;

import com.powercity.power_city_platform.enums.Currency;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public class CreateOrderRequest {

    @NotNull(message = "Order items are required")
    @Size(min = 1, message = "At least one order item is required")
    private List<OrderItemRequest> orderItems;

    @NotNull(message = "Currency is required")
    private Currency currency = Currency.USD;

    @DecimalMin(value = "0.0", message = "Tax amount cannot be negative")
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @DecimalMin(value = "0.0", message = "Discount amount cannot be negative")
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Size(max = 1000, message = "Order notes must be less than 1000 characters")
    private String notes;

    @Email(message = "Customer email must be valid")
    private String customerEmail;

    @Size(max = 255, message = "Customer name must be less than 255 characters")
    private String customerName;

    @Size(max = 20, message = "Customer phone must be less than 20 characters")
    private String customerPhone;

    @Size(max = 500, message = "Billing address must be less than 500 characters")
    private String billingAddress;

    @Size(max = 500, message = "Shipping address must be less than 500 characters")
    private String shippingAddress;

    @Size(max = 50, message = "Region must be less than 50 characters")
    private String region;

    @Size(max = 50, message = "Payment method must be less than 50 characters")
    private String paymentMethod;

    @Size(max = 50, message = "Payment gateway must be less than 50 characters")
    private String paymentGateway;

    public CreateOrderRequest() {}

    public CreateOrderRequest(List<OrderItemRequest> orderItems, Currency currency) {
        this.orderItems = orderItems;
        this.currency = currency;
    }

    public BigDecimal calculateSubtotal() {
        return orderItems.stream()
                .map(OrderItemRequest::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal calculateTotal() {
        return calculateSubtotal().add(taxAmount).subtract(discountAmount);
    }

    public List<OrderItemRequest> getOrderItems() {
        return orderItems;
    }

    public void setOrderItems(List<OrderItemRequest> orderItems) {
        this.orderItems = orderItems;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public String getBillingAddress() {
        return billingAddress;
    }

    public void setBillingAddress(String billingAddress) {
        this.billingAddress = billingAddress;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getPaymentGateway() {
        return paymentGateway;
    }

    public void setPaymentGateway(String paymentGateway) {
        this.paymentGateway = paymentGateway;
    }

    // Inner class for order items
    public static class OrderItemRequest {
        
        @NotNull(message = "Book ID is required")
        private Long bookId;

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        private Integer quantity = 1;

        @NotNull(message = "Unit price is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Unit price must be greater than 0")
        private BigDecimal unitPrice;

        @DecimalMin(value = "0.0", message = "Discount amount cannot be negative")
        private BigDecimal discountAmount = BigDecimal.ZERO;

        @Size(max = 500, message = "Item notes must be less than 500 characters")
        private String notes;

        public OrderItemRequest() {}

        public OrderItemRequest(Long bookId, Integer quantity, BigDecimal unitPrice) {
            this.bookId = bookId;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
        }

        public BigDecimal getSubtotal() {
            return unitPrice.multiply(new BigDecimal(quantity)).subtract(discountAmount);
        }

        public Long getBookId() {
            return bookId;
        }

        public void setBookId(Long bookId) {
            this.bookId = bookId;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }

        public BigDecimal getUnitPrice() {
            return unitPrice;
        }

        public void setUnitPrice(BigDecimal unitPrice) {
            this.unitPrice = unitPrice;
        }

        public BigDecimal getDiscountAmount() {
            return discountAmount;
        }

        public void setDiscountAmount(BigDecimal discountAmount) {
            this.discountAmount = discountAmount;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }
    }
} 