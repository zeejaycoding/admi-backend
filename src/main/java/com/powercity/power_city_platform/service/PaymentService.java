package com.powercity.power_city_platform.service;

import com.powercity.power_city_platform.dto.request.payment.CreatePaymentRequest;
import com.powercity.power_city_platform.dto.response.payment.PaymentResponse;
import com.powercity.power_city_platform.entity.Order;
import com.powercity.power_city_platform.entity.Payment;
import com.powercity.power_city_platform.entity.User;
import com.powercity.power_city_platform.enums.OrderStatus;
import com.powercity.power_city_platform.enums.PaymentStatus;
import com.powercity.power_city_platform.exception.ResourceNotFoundException;
import com.powercity.power_city_platform.repository.OrderRepository;
import com.powercity.power_city_platform.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.HashMap;

@Service
@Transactional
@RequiredArgsConstructor
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;

    private final OrderRepository orderRepository;

    private final StripeService stripeService;

    // Create a new payment
    public PaymentResponse createPayment(CreatePaymentRequest request, User currentUser) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        // Check if user has access to this order
        if (!order.getUser().getId().equals(currentUser.getId()) &&
            !currentUser.getRoles().stream().anyMatch(role ->
                role.getRole().getName().equals("ADMIN") ||
                role.getRole().getName().equals("SUPER_ADMIN"))) {
            throw new RuntimeException("Access denied");
        }

        Payment payment = new Payment(order, currentUser, request.getAmount(),
                request.getCurrency(), request.getPaymentMethod(), request.getPaymentGateway());
        payment.setNotes(request.getNotes());
        payment.setMetadata(request.getMetadata());
        payment.setCreatedBy(currentUser.getId());
        payment.setUpdatedBy(currentUser.getId());

        Payment savedPayment = paymentRepository.save(payment);
        return convertToPaymentResponse(savedPayment);
    }

    // Get payment by ID
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(Long paymentId, User currentUser) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        // Check if user has access to this payment
        if (!payment.getUser().getId().equals(currentUser.getId()) &&
            !currentUser.getRoles().stream().anyMatch(role ->
                role.getRole().getName().equals("ADMIN") ||
                role.getRole().getName().equals("SUPER_ADMIN"))) {
            throw new RuntimeException("Access denied");
        }

        return convertToPaymentResponse(payment);
    }

    // Get payment by gateway transaction ID
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByGatewayTransactionId(String gatewayTransactionId, User currentUser) {
        Payment payment = paymentRepository.findByGatewayTransactionId(gatewayTransactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        // Check if user has access to this payment
        if (!payment.getUser().getId().equals(currentUser.getId()) &&
            !currentUser.getRoles().stream().anyMatch(role ->
                role.getRole().getName().equals("ADMIN") ||
                role.getRole().getName().equals("SUPER_ADMIN"))) {
            throw new RuntimeException("Access denied");
        }

        return convertToPaymentResponse(payment);
    }

    // Get user payments
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getUserPayments(User currentUser, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Payment> paymentsPage = paymentRepository.findByUserOrderByCreatedAtDesc(currentUser, pageable);
        return paymentsPage.map(this::convertToPaymentResponse);
    }

    // Get user payments by status
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getUserPaymentsByStatus(User currentUser, PaymentStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Payment> paymentsPage = paymentRepository.findByUserAndStatus(currentUser, status, pageable);
        return paymentsPage.map(this::convertToPaymentResponse);
    }

    // Get all payments (admin only)
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getAllPayments(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Payment> paymentsPage = paymentRepository.findAll(pageable);
        return paymentsPage.map(this::convertToPaymentResponse);
    }

    // Get payments by status (admin only)
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getPaymentsByStatus(PaymentStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Payment> paymentsPage = paymentRepository.findByStatus(status, pageable);
        return paymentsPage.map(this::convertToPaymentResponse);
    }

    // Search payments (admin only)
    @Transactional(readOnly = true)
    public Page<PaymentResponse> searchPayments(String searchTerm, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Payment> paymentsPage = paymentRepository.searchPayments(searchTerm, pageable);
        return paymentsPage.map(this::convertToPaymentResponse);
    }

    // Process payment with Stripe
    public PaymentResponse processPaymentWithStripe(Long paymentId, String stripeToken, User currentUser) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        // Check if user has access to this payment
        if (!payment.getUser().getId().equals(currentUser.getId()) &&
            !currentUser.getRoles().stream().anyMatch(role ->
                role.getRole().getName().equals("ADMIN") ||
                role.getRole().getName().equals("SUPER_ADMIN"))) {
            throw new RuntimeException("Access denied");
        }

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new RuntimeException("Payment is not in pending status");
        }

        try {
            // Mark payment as processing
            payment.markAsProcessing();
            payment.setUpdatedBy(currentUser.getId());
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(payment);

            // Process payment with Stripe
            Map<String, Object> stripeResponse = stripeService.processPayment(
                payment.getAmount(),
                payment.getCurrency().getCode(),
                stripeToken,
                payment.getOrder().getOrderNumber()
            );

            // Update payment with Stripe response
            String transactionId = (String) stripeResponse.get("transactionId");
            String status = (String) stripeResponse.get("status");
            BigDecimal gatewayFee = new BigDecimal(stripeResponse.get("gatewayFee").toString());
            BigDecimal processingFee = new BigDecimal(stripeResponse.get("processingFee").toString());

            payment.setGatewayTransactionId(transactionId);
            payment.setGatewayFee(gatewayFee);
            payment.setProcessingFee(processingFee);

            if ("succeeded".equals(status)) {
                payment.markAsCompleted(transactionId);
            } else {
                payment.markAsFailed("failed", "Payment processing failed");
            }

            payment.setUpdatedBy(currentUser.getId());
            payment.setUpdatedAt(LocalDateTime.now());

            Payment savedPayment = paymentRepository.save(payment);
            return convertToPaymentResponse(savedPayment);

        } catch (Exception e) {
            payment.markAsFailed("error", e.getMessage());
            payment.setUpdatedBy(currentUser.getId());
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(payment);
            logger.error("Payment processing failed for paymentId {}: {}", paymentId, e.getMessage(), e);
            throw new RuntimeException("Payment processing failed: " + e.getMessage());
        }
    }

    // Refund payment
    public PaymentResponse refundPayment(Long paymentId, BigDecimal refundAmount, String reason, User currentUser) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        // Check if user has access to this payment
        if (!payment.getUser().getId().equals(currentUser.getId()) &&
            !currentUser.getRoles().stream().anyMatch(role ->
                role.getRole().getName().equals("ADMIN") ||
                role.getRole().getName().equals("SUPER_ADMIN"))) {
            throw new RuntimeException("Access denied");
        }

        if (!payment.canBeRefunded()) {
            throw new RuntimeException("Payment cannot be refunded");
        }

        if (refundAmount.compareTo(payment.getAmount()) > 0) {
            throw new RuntimeException("Refund amount cannot exceed payment amount");
        }

        try {
            // Process refund with Stripe if it's a Stripe payment
            if ("stripe".equals(payment.getPaymentGateway())) {
                Map<String, Object> refundResponse = stripeService.processRefund(
                    payment.getGatewayTransactionId(),
                    refundAmount,
                    reason
                );

                String refundId = (String) refundResponse.get("refundId");
                payment.setGatewayTransactionId(refundId);
            }

            payment.markAsRefunded(refundAmount, reason);
            payment.setUpdatedBy(currentUser.getId());
            payment.setUpdatedAt(LocalDateTime.now());

            Payment savedPayment = paymentRepository.save(payment);
            return convertToPaymentResponse(savedPayment);

        } catch (Exception e) {
            logger.error("Refund processing failed for paymentId {}: {}", paymentId, e.getMessage(), e);
            throw new RuntimeException("Refund processing failed: " + e.getMessage());
        }
    }

    // Handle payment webhook
    public void handlePaymentWebhook(String paymentGateway, Map<String, Object> webhookData) {
        String transactionId = (String) webhookData.get("transactionId");
        String status = (String) webhookData.get("status");

        Payment payment = paymentRepository.findByGatewayTransactionId(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        switch (status) {
            case "succeeded":
                payment.markAsCompleted(transactionId);
                break;
            case "failed":
                payment.markAsFailed("webhook", "Payment failed via webhook");
                break;
            case "refunded":
                BigDecimal refundAmount = new BigDecimal(webhookData.get("refundAmount").toString());
                String refundReason = (String) webhookData.get("refundReason");
                payment.markAsRefunded(refundAmount, refundReason);
                break;
            default:
                // Handle other statuses as needed
                break;
        }

        payment.setUpdatedAt(LocalDateTime.now());
        paymentRepository.save(payment);
    }

    // Get payment analytics
    @Transactional(readOnly = true)
    public Map<String, Object> getPaymentAnalytics() {
        Map<String, Object> analytics = new HashMap<>();

        analytics.put("totalPayments", paymentRepository.count());
        analytics.put("totalRevenue", paymentRepository.getTotalRevenue());
        analytics.put("totalGatewayFees", paymentRepository.getTotalGatewayFees());
        analytics.put("totalProcessingFees", paymentRepository.getTotalProcessingFees());
        analytics.put("totalRefunds", paymentRepository.getTotalRefunds());
        analytics.put("averagePaymentValue", paymentRepository.getAveragePaymentValue());
        analytics.put("paymentsByStatus", paymentRepository.getPaymentCountByStatus());
        analytics.put("paymentsByPaymentGateway", paymentRepository.getPaymentCountByPaymentGateway());
        analytics.put("paymentsByPaymentMethod", paymentRepository.getPaymentCountByPaymentMethod());
        analytics.put("paymentsByCurrency", paymentRepository.getPaymentCountByCurrency());
        analytics.put("monthlyRevenue", paymentRepository.getMonthlyRevenue());
        analytics.put("dailyRevenue", paymentRepository.getDailyRevenue());

  
        BigDecimal ngnRevenue = paymentRepository.getTotalRevenueByCurrency(com.powercity.power_city_platform.enums.Currency.NGN);
        BigDecimal usdRevenue = paymentRepository.getTotalRevenueByCurrency(com.powercity.power_city_platform.enums.Currency.USD);
        analytics.put("totalRevenueNGN", ngnRevenue != null ? ngnRevenue : BigDecimal.ZERO);
        analytics.put("totalRevenueUSD", usdRevenue != null ? usdRevenue : BigDecimal.ZERO);

        return analytics;
    }

    // Get user payment analytics
    @Transactional(readOnly = true)
    public Map<String, Object> getUserPaymentAnalytics(User currentUser) {
        Map<String, Object> analytics = new HashMap<>();

        analytics.put("totalPayments", paymentRepository.countByUser(currentUser));
        analytics.put("totalRevenue", paymentRepository.getTotalRevenueByUser(currentUser));
        analytics.put("averagePaymentValue", paymentRepository.getAveragePaymentValueByUser(currentUser));

        return analytics;
    }

    // Confirm payment and complete order (Admin function)
    public PaymentResponse confirmPayment(Long paymentId, User adminUser) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        // Check if admin has permission
        if (!adminUser.getRoles().stream().anyMatch(role ->
            role.getRole().getName().equals("ADMIN") ||
            role.getRole().getName().equals("SUPER_ADMIN"))) {
            throw new RuntimeException("Access denied - Admin privileges required");
        }

        if (payment.getStatus() != PaymentStatus.PENDING && payment.getStatus() != PaymentStatus.PROCESSING) {
            throw new RuntimeException("Payment is not in pending or processing status");
        }

        // Mark payment as completed
        payment.markAsCompleted("ADMIN_CONFIRMED_" + System.currentTimeMillis());
        payment.setUpdatedBy(adminUser.getId());
        payment.setUpdatedAt(LocalDateTime.now());

        Payment savedPayment = paymentRepository.save(payment);

        // Complete the associated order
        Order order = payment.getOrder();
        if (order != null && order.getStatus() == OrderStatus.PROCESSING) {
            order.markAsCompleted();
            order.setUpdatedBy(adminUser.getId());
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);
        }

        return convertToPaymentResponse(savedPayment);
    }

    // Get failed payments (admin only)
    @Transactional(readOnly = true)
    public List<PaymentResponse> getFailedPayments() {
        List<Payment> payments = paymentRepository.findFailedPayments();
        return payments.stream()
                .map(this::convertToPaymentResponse)
                .collect(Collectors.toList());
    }

    // Get refundable payments (admin only)
    @Transactional(readOnly = true)
    public List<PaymentResponse> getRefundablePayments() {
        List<Payment> payments = paymentRepository.findRefundablePayments();
        return payments.stream()
                .map(this::convertToPaymentResponse)
                .collect(Collectors.toList());
    }

    // Convert Payment entity to PaymentResponse DTO
    private PaymentResponse convertToPaymentResponse(Payment payment) {
        PaymentResponse response = new PaymentResponse();
        response.setId(payment.getId());
        response.setOrderId(payment.getOrder().getId());
        response.setOrderNumber(payment.getOrder().getOrderNumber());
        response.setUserId(payment.getUser().getId());
        response.setUserEmail(payment.getUser().getEmail());
        response.setUserName(payment.getUser().getFullName());
        response.setAmount(payment.getAmount());
        response.setCurrency(payment.getCurrency());
        response.setStatus(payment.getStatus());
        response.setPaymentMethod(payment.getPaymentMethod());
        response.setPaymentGateway(payment.getPaymentGateway());
        response.setGatewayTransactionId(payment.getGatewayTransactionId());
        response.setGatewayResponseCode(payment.getGatewayResponseCode());
        response.setGatewayResponseMessage(payment.getGatewayResponseMessage());
        response.setGatewayFee(payment.getGatewayFee());
        response.setProcessingFee(payment.getProcessingFee());
        response.setRefundAmount(payment.getRefundAmount());
        response.setRefundReason(payment.getRefundReason());
        response.setProcessedAt(payment.getProcessedAt());
        response.setCompletedAt(payment.getCompletedAt());
        response.setFailedAt(payment.getFailedAt());
        response.setRefundedAt(payment.getRefundedAt());
        response.setNotes(payment.getNotes());
        response.setMetadata(payment.getMetadata());
        response.setCreatedAt(payment.getCreatedAt());
        response.setUpdatedAt(payment.getUpdatedAt());

        // Add user-specific information
        response.setCanBeRefunded(payment.canBeRefunded());
        response.setRemainingRefundableAmount(payment.getRemainingRefundableAmount());

        return response;
    }
}
