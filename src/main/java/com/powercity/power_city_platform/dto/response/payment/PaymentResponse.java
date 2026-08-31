package com.powercity.power_city_platform.dto.response.payment;

import com.powercity.power_city_platform.enums.Currency;
import com.powercity.power_city_platform.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaymentResponse {

    private Long id;
    private Long orderId;
    private String orderNumber;
    private Long userId;
    private String userEmail;
    private String userName;
    private BigDecimal amount;
    private Currency currency;
    private PaymentStatus status;
    private String paymentMethod;
    private String paymentGateway;
    private String gatewayTransactionId;
    private String gatewayResponseCode;
    private String gatewayResponseMessage;
    private BigDecimal gatewayFee;
    private BigDecimal processingFee;
    private BigDecimal refundAmount;
    private String refundReason;
    private LocalDateTime processedAt;
    private LocalDateTime completedAt;
    private LocalDateTime failedAt;
    private LocalDateTime refundedAt;
    private String notes;
    private String metadata;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // User-specific information
    private Boolean canBeRefunded;
    private BigDecimal remainingRefundableAmount;

    public PaymentResponse() {}

    public boolean isCompleted() {
        return PaymentStatus.COMPLETED.equals(status);
    }

    public boolean isFailed() {
        return PaymentStatus.FAILED.equals(status);
    }

    public boolean isRefunded() {
        return PaymentStatus.REFUNDED.equals(status);
    }

    public boolean isPending() {
        return PaymentStatus.PENDING.equals(status);
    }

    public boolean isProcessing() {
        return PaymentStatus.PROCESSING.equals(status);
    }

    public String getFormattedAmount() {
        return currency.getSymbol() + amount;
    }

    public String getFormattedGatewayFee() {
        return currency.getSymbol() + gatewayFee;
    }

    public String getFormattedProcessingFee() {
        return currency.getSymbol() + processingFee;
    }

    public String getFormattedRefundAmount() {
        return currency.getSymbol() + refundAmount;
    }

    public String getFormattedRemainingRefundableAmount() {
        return currency.getSymbol() + remainingRefundableAmount;
    }

    public BigDecimal getNetAmount() {
        return amount.subtract(gatewayFee != null ? gatewayFee : BigDecimal.ZERO)
                    .subtract(processingFee != null ? processingFee : BigDecimal.ZERO);
    }

    public String getFormattedNetAmount() {
        return currency.getSymbol() + getNetAmount();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Boolean getCanBeRefunded() {
        return canBeRefunded;
    }

    public void setCanBeRefunded(Boolean canBeRefunded) {
        this.canBeRefunded = canBeRefunded;
    }

    public BigDecimal getRemainingRefundableAmount() {
        return remainingRefundableAmount;
    }

    public void setRemainingRefundableAmount(BigDecimal remainingRefundableAmount) {
        this.remainingRefundableAmount = remainingRefundableAmount;
    }
} 