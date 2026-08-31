package com.powercity.power_city_platform.dto.response.order;

import com.powercity.power_city_platform.enums.Currency;
import com.powercity.power_city_platform.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class OrderResponse {

    private Long id;
    private String orderNumber;
    private Long userId;
    private String userEmail;
    private String userName;
    private OrderStatus status;
    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private Currency currency;
    private String notes;
    private String customerEmail;
    private String customerName;
    private String customerPhone;
    private String billingAddress;
    private String shippingAddress;
    private String region;
    private String paymentMethod;
    private String paymentGateway;
    private String paymentGatewayTransactionId;
    private LocalDateTime processedAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime refundedAt;
    private BigDecimal refundAmount;
    private String refundReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Order items
    private List<OrderItemResponse> orderItems;
    
    // Payments
    private List<PaymentResponse> payments;
    
    // User-specific information
    private Boolean canBeCancelled;
    private Boolean canBeRefunded;
    private String downloadUrl;
    private List<String> downloadUrls;

    public OrderResponse() {}

    public boolean isCompleted() {
        return OrderStatus.COMPLETED.equals(status);
    }

    public boolean isCancelled() {
        return OrderStatus.CANCELLED.equals(status);
    }

    public boolean isRefunded() {
        return OrderStatus.REFUNDED.equals(status);
    }

    public boolean isPending() {
        return OrderStatus.PENDING.equals(status);
    }

    public boolean isProcessing() {
        return OrderStatus.PROCESSING.equals(status);
    }

    public String getFormattedTotal() {
        return currency.getSymbol() + totalAmount;
    }

    public String getFormattedSubtotal() {
        return currency.getSymbol() + subtotal;
    }

    public String getFormattedTaxAmount() {
        return currency.getSymbol() + taxAmount;
    }

    public String getFormattedDiscountAmount() {
        return currency.getSymbol() + discountAmount;
    }

    public String getFormattedRefundAmount() {
        return currency.getSymbol() + refundAmount;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
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

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
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

    public String getPaymentGatewayTransactionId() {
        return paymentGatewayTransactionId;
    }

    public void setPaymentGatewayTransactionId(String paymentGatewayTransactionId) {
        this.paymentGatewayTransactionId = paymentGatewayTransactionId;
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

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public LocalDateTime getRefundedAt() {
        return refundedAt;
    }

    public void setRefundedAt(LocalDateTime refundedAt) {
        this.refundedAt = refundedAt;
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

    public List<OrderItemResponse> getOrderItems() {
        return orderItems;
    }

    public void setOrderItems(List<OrderItemResponse> orderItems) {
        this.orderItems = orderItems;
    }

    public List<PaymentResponse> getPayments() {
        return payments;
    }

    public void setPayments(List<PaymentResponse> payments) {
        this.payments = payments;
    }

    public Boolean getCanBeCancelled() {
        return canBeCancelled;
    }

    public void setCanBeCancelled(Boolean canBeCancelled) {
        this.canBeCancelled = canBeCancelled;
    }

    public Boolean getCanBeRefunded() {
        return canBeRefunded;
    }

    public void setCanBeRefunded(Boolean canBeRefunded) {
        this.canBeRefunded = canBeRefunded;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public List<String> getDownloadUrls() {
        return downloadUrls;
    }

    public void setDownloadUrls(List<String> downloadUrls) {
        this.downloadUrls = downloadUrls;
    }

    // Inner class for order items
    public static class OrderItemResponse {

        private Long id;
        private String productType;  // BOOK or COURSE
        private Long bookId;
        private String bookTitle;
        private String bookCoverImageUrl;
        private Long courseId;
        private String courseTitle;
        private String courseThumbnailUrl;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal subtotal;
        private Currency currency;
        private BigDecimal discountAmount;
        private String notes;

        public OrderItemResponse() {}

        public String getFormattedUnitPrice() {
            return currency.getSymbol() + unitPrice;
        }

        public String getFormattedSubtotal() {
            return currency.getSymbol() + subtotal;
        }

        public String getFormattedDiscountAmount() {
            return currency.getSymbol() + discountAmount;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Long getBookId() {
            return bookId;
        }

        public void setBookId(Long bookId) {
            this.bookId = bookId;
        }

        public String getBookTitle() {
            return bookTitle;
        }

        public void setBookTitle(String bookTitle) {
            this.bookTitle = bookTitle;
        }

        public String getBookCoverImageUrl() {
            return bookCoverImageUrl;
        }

        public void setBookCoverImageUrl(String bookCoverImageUrl) {
            this.bookCoverImageUrl = bookCoverImageUrl;
        }

        public String getProductType() {
            return productType;
        }

        public void setProductType(String productType) {
            this.productType = productType;
        }

        public Long getCourseId() {
            return courseId;
        }

        public void setCourseId(Long courseId) {
            this.courseId = courseId;
        }

        public String getCourseTitle() {
            return courseTitle;
        }

        public void setCourseTitle(String courseTitle) {
            this.courseTitle = courseTitle;
        }

        public String getCourseThumbnailUrl() {
            return courseThumbnailUrl;
        }

        public void setCourseThumbnailUrl(String courseThumbnailUrl) {
            this.courseThumbnailUrl = courseThumbnailUrl;
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

        public BigDecimal getSubtotal() {
            return subtotal;
        }

        public void setSubtotal(BigDecimal subtotal) {
            this.subtotal = subtotal;
        }

        public Currency getCurrency() {
            return currency;
        }

        public void setCurrency(Currency currency) {
            this.currency = currency;
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

    // Inner class for payments
    public static class PaymentResponse {
        
        private Long id;
        private BigDecimal amount;
        private Currency currency;
        private String status;
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

        public PaymentResponse() {}

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

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
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

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
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
    }
} 