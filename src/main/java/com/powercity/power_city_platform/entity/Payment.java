package com.powercity.power_city_platform.entity;

import com.powercity.power_city_platform.enums.Currency;
import com.powercity.power_city_platform.enums.PaymentStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
public class Payment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @NotNull(message = "Order is required")
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "User is required")
    private User user;

    @Column(name = "amount", nullable = false)
    @NotNull(message = "Payment amount is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Payment amount must be greater than 0")
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false)
    private Currency currency = Currency.USD;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "payment_method", nullable = false)
    @NotBlank(message = "Payment method is required")
    @Size(max = 50, message = "Payment method must be less than 50 characters")
    private String paymentMethod;

    @Column(name = "payment_gateway", nullable = false)
    @NotBlank(message = "Payment gateway is required")
    @Size(max = 50, message = "Payment gateway must be less than 50 characters")
    private String paymentGateway;

    @Column(name = "gateway_transaction_id")
    @Size(max = 255, message = "Gateway transaction ID must be less than 255 characters")
    private String gatewayTransactionId;

    @Column(name = "gateway_response_code")
    @Size(max = 50, message = "Gateway response code must be less than 50 characters")
    private String gatewayResponseCode;

    @Column(name = "gateway_response_message")
    @Size(max = 500, message = "Gateway response message must be less than 500 characters")
    private String gatewayResponseMessage;

    @Column(name = "gateway_fee")
    @DecimalMin(value = "0.0", message = "Gateway fee cannot be negative")
    private BigDecimal gatewayFee = BigDecimal.ZERO;

    @Column(name = "processing_fee")
    @DecimalMin(value = "0.0", message = "Processing fee cannot be negative")
    private BigDecimal processingFee = BigDecimal.ZERO;

    @Column(name = "refund_amount")
    @DecimalMin(value = "0.0", message = "Refund amount cannot be negative")
    private BigDecimal refundAmount = BigDecimal.ZERO;

    @Column(name = "refund_reason")
    @Size(max = 500, message = "Refund reason must be less than 500 characters")
    private String refundReason;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "failed_at")
    private LocalDateTime failedAt;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    @Column(name = "notes")
    @Size(max = 1000, message = "Payment notes must be less than 1000 characters")
    private String notes;

    @Column(name = "metadata")
    private String metadata;

    public Payment() {}

    public Payment(Order order, User user, BigDecimal amount, Currency currency, String paymentMethod, String paymentGateway) {
        this.order = order;
        this.user = user;
        this.amount = amount;
        this.currency = currency;
        this.paymentMethod = paymentMethod;
        this.paymentGateway = paymentGateway;
        this.status = PaymentStatus.PENDING;
    }

    public void markAsProcessing() {
        this.status = PaymentStatus.PROCESSING;
        this.processedAt = LocalDateTime.now();
    }

    public void markAsCompleted(String gatewayTransactionId) {
        this.status = PaymentStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.gatewayTransactionId = gatewayTransactionId;
    }

    public void markAsFailed(String responseCode, String responseMessage) {
        this.status = PaymentStatus.FAILED;
        this.failedAt = LocalDateTime.now();
        this.gatewayResponseCode = responseCode;
        this.gatewayResponseMessage = responseMessage;
    }

    public void markAsRefunded(BigDecimal refundAmount, String reason) {
        this.status = PaymentStatus.REFUNDED;
        this.refundedAt = LocalDateTime.now();
        this.refundAmount = refundAmount;
        this.refundReason = reason;
    }

    public boolean canBeRefunded() {
        return status == PaymentStatus.COMPLETED && refundAmount.compareTo(amount) < 0;
    }

    public BigDecimal getNetAmount() {
        return amount.subtract(gatewayFee).subtract(processingFee);
    }

    public BigDecimal getRemainingRefundableAmount() {
        return amount.subtract(refundAmount);
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
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

    public String getGatewayTransactionId() {
        return gatewayTransactionId;
    }

    public void setGatewayTransactionId(String gatewayTransactionId) {
        this.gatewayTransactionId = gatewayTransactionId;
    }

    public String getGatewayResponseCode() {
        return gatewayResponseCode;
    }

    public void setGatewayResponseCode(String gatewayResponseCode) {
        this.gatewayResponseCode = gatewayResponseCode;
    }

    public String getGatewayResponseMessage() {
        return gatewayResponseMessage;
    }

    public void setGatewayResponseMessage(String gatewayResponseMessage) {
        this.gatewayResponseMessage = gatewayResponseMessage;
    }

    public BigDecimal getGatewayFee() {
        return gatewayFee;
    }

    public void setGatewayFee(BigDecimal gatewayFee) {
        this.gatewayFee = gatewayFee;
    }

    public BigDecimal getProcessingFee() {
        return processingFee;
    }

    public void setProcessingFee(BigDecimal processingFee) {
        this.processingFee = processingFee;
    }

    public BigDecimal getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(BigDecimal refundAmount) {
        this.refundAmount = refundAmount;
    }

    public String getRefundReason() {
        return refundReason;
    }

    public void setRefundReason(String refundReason) {
        this.refundReason = refundReason;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public LocalDateTime getFailedAt() {
        return failedAt;
    }

    public void setFailedAt(LocalDateTime failedAt) {
        this.failedAt = failedAt;
    }

    public LocalDateTime getRefundedAt() {
        return refundedAt;
    }

    public void setRefundedAt(LocalDateTime refundedAt) {
        this.refundedAt = refundedAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }
}
