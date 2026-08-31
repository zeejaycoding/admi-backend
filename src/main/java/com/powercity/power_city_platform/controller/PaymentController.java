package com.powercity.power_city_platform.controller;

import com.powercity.power_city_platform.dto.request.payment.CreatePaymentRequest;
import com.powercity.power_city_platform.dto.response.payment.PaymentResponse;
import com.powercity.power_city_platform.dto.response.order.OrderResponse;
import com.powercity.power_city_platform.dto.response.common.ApiResponse;
import com.powercity.power_city_platform.entity.Order;
import com.powercity.power_city_platform.entity.Payment;
import com.powercity.power_city_platform.entity.User;
import com.powercity.power_city_platform.enums.OrderStatus;
import com.powercity.power_city_platform.enums.PaymentStatus;
import com.powercity.power_city_platform.repository.OrderRepository;
import com.powercity.power_city_platform.repository.PaymentRepository;
import com.powercity.power_city_platform.service.PaymentService;
import com.powercity.power_city_platform.service.UserService;
import com.powercity.power_city_platform.service.StripeService;
import com.powercity.power_city_platform.service.OrderService;
import com.powercity.power_city_platform.service.DonationService;
import com.powercity.power_city_platform.service.FormSubmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "Payment Management", description = "APIs for managing payment processing")
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService paymentService;
    private final UserService userService;
    private final StripeService stripeService;
    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final DonationService donationService;
    private final FormSubmissionService formSubmissionService;

    // Constructor injection (Spring auto-detects, no @Autowired needed)
    public PaymentController(
            PaymentService paymentService,
            UserService userService,
            StripeService stripeService,
            OrderService orderService,
            OrderRepository orderRepository,
            PaymentRepository paymentRepository,
            DonationService donationService,
            FormSubmissionService formSubmissionService) {
        this.paymentService = paymentService;
        this.userService = userService;
        this.stripeService = stripeService;
        this.orderService = orderService;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.donationService = donationService;
        this.formSubmissionService = formSubmissionService;
    }


    @PostMapping
    @Operation(summary = "Create payment", description = "Create a new payment for an order")
    public ResponseEntity<ApiResponse<PaymentResponse>> createPayment(
            @Parameter(description = "Payment creation request", required = true)
            @Valid @RequestBody CreatePaymentRequest request) {
        User currentUser = userService.getCurrentUser();
        PaymentResponse payment = paymentService.createPayment(request, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Payment created successfully", payment));
    }

    @GetMapping("/{paymentId}")
    @Operation(summary = "Get payment by ID", description = "Get detailed information about a specific payment")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentById(
            @Parameter(description = "Payment ID", required = true) @PathVariable Long paymentId) {
        User currentUser = userService.getCurrentUser();
        PaymentResponse payment = paymentService.getPaymentById(paymentId, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Payment retrieved successfully", payment));
    }

    @GetMapping("/transaction/{transactionId}")
    @Operation(summary = "Get payment by transaction ID", description = "Get payment details using gateway transaction ID")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentByTransactionId(
            @Parameter(description = "Gateway transaction ID", required = true) @PathVariable String transactionId) {
        User currentUser = userService.getCurrentUser();
        PaymentResponse payment = paymentService.getPaymentByGatewayTransactionId(transactionId, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Payment retrieved successfully", payment));
    }

    @GetMapping("/my-payments")
    @Operation(summary = "Get user payments", description = "Get paginated list of current user's payments")
    public ResponseEntity<ApiResponse<Page<PaymentResponse>>> getMyPayments(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        User currentUser = userService.getCurrentUser();
        Page<PaymentResponse> payments = paymentService.getUserPayments(currentUser, page, size);
        return ResponseEntity.ok(ApiResponse.success("Payments retrieved successfully", payments));
    }

    @GetMapping("/my-payments/status/{status}")
    @Operation(summary = "Get user payments by status", description = "Get user payments filtered by status")
    public ResponseEntity<ApiResponse<Page<PaymentResponse>>> getMyPaymentsByStatus(
            @Parameter(description = "Payment status", required = true) @PathVariable PaymentStatus status,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        User currentUser = userService.getCurrentUser();
        Page<PaymentResponse> payments = paymentService.getUserPaymentsByStatus(currentUser, status, page, size);
        return ResponseEntity.ok(ApiResponse.success("Payments retrieved successfully", payments));
    }

    @PostMapping("/{paymentId}/process-stripe")
    @Operation(summary = "Process payment with Stripe", description = "Process payment using Stripe payment gateway")
    public ResponseEntity<ApiResponse<PaymentResponse>> processPaymentWithStripe(
            @Parameter(description = "Payment ID", required = true) @PathVariable Long paymentId,
            @Parameter(description = "Stripe token", required = true) @RequestParam String stripeToken) {
        User currentUser = userService.getCurrentUser();
        PaymentResponse payment = paymentService.processPaymentWithStripe(paymentId, stripeToken, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Payment processed successfully", payment));
    }

    @GetMapping("/my-payments/analytics")
    @Operation(summary = "Get user payment analytics", description = "Get analytics for current user's payments")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMyPaymentAnalytics() {
        User currentUser = userService.getCurrentUser();
        Map<String, Object> analytics = paymentService.getUserPaymentAnalytics(currentUser);
        return ResponseEntity.ok(ApiResponse.success("Payment analytics retrieved successfully", analytics));
    }


    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get all payments", description = "Get all payments with pagination (Admin/Super Admin only)")
    public ResponseEntity<ApiResponse<Page<PaymentResponse>>> getAllPayments(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        Page<PaymentResponse> payments = paymentService.getAllPayments(page, size);
        return ResponseEntity.ok(ApiResponse.success("Payments retrieved successfully", payments));
    }

    @GetMapping("/admin/status/{status}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get payments by status", description = "Get payments filtered by status (Super Admin only)")
    public ResponseEntity<ApiResponse<Page<PaymentResponse>>> getPaymentsByStatus(
            @Parameter(description = "Payment status", required = true) @PathVariable PaymentStatus status,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        Page<PaymentResponse> payments = paymentService.getPaymentsByStatus(status, page, size);
        return ResponseEntity.ok(ApiResponse.success("Payments retrieved successfully", payments));
    }

    @GetMapping("/admin/search")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Search payments", description = "Search payments by transaction ID, payment method, or gateway (Super Admin only)")
    public ResponseEntity<ApiResponse<Page<PaymentResponse>>> searchPayments(
            @Parameter(description = "Search term", required = true) @RequestParam String searchTerm,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        Page<PaymentResponse> payments = paymentService.searchPayments(searchTerm, page, size);
        return ResponseEntity.ok(ApiResponse.success("Payments retrieved successfully", payments));
    }

    @GetMapping("/admin/failed")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get failed payments", description = "Get list of failed payments (Super Admin only)")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getFailedPayments() {
        List<PaymentResponse> payments = paymentService.getFailedPayments();
        return ResponseEntity.ok(ApiResponse.success("Failed payments retrieved successfully", payments));
    }

    @GetMapping("/admin/analytics")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get payment analytics", description = "Get comprehensive payment analytics (Admin/Super Admin only)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPaymentAnalytics() {
        Map<String, Object> analytics = paymentService.getPaymentAnalytics();
        return ResponseEntity.ok(ApiResponse.success("Payment analytics retrieved successfully", analytics));
    }

    @PostMapping("/admin/{paymentId}/confirm")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Confirm payment", description = "Confirm payment and complete order (Admin/Super Admin only)")
    public ResponseEntity<ApiResponse<PaymentResponse>> confirmPayment(
            @Parameter(description = "Payment ID", required = true) @PathVariable Long paymentId) {
        User currentUser = userService.getCurrentUser();
        PaymentResponse payment = paymentService.confirmPayment(paymentId, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Payment confirmed successfully", payment));
    }

    @PostMapping("/checkout-cart-item")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Checkout single cart item", description = "Create order from a single cart item and return Stripe checkout session")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkoutCartItem(
            @Parameter(description = "Cart item checkout request", required = true)
            @RequestBody Map<String, Object> request) {
        User currentUser = userService.getCurrentUser();

        // Extract parameters
        Long cartItemId = Long.valueOf(request.get("cartItemId").toString());
        String currencyStr = request.get("currency").toString();
        com.powercity.power_city_platform.enums.Currency currency =
            com.powercity.power_city_platform.enums.Currency.valueOf(currencyStr);

        // Validate currency - Only USD is currently supported
        if (currency != com.powercity.power_city_platform.enums.Currency.USD) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Payment not available",
                        "Payment processing is currently only available for USD. Please switch currency to proceed."));
        }

        // Create order from single cart item
        OrderResponse order = orderService.createOrderFromCartItem(cartItemId, currency, currentUser);

        // Create Stripe checkout session (USD only)
        Map<String, Object> checkoutSession;
        try {
            checkoutSession = stripeService.createCheckoutSession(
                    order.getId(),
                    order.getTotalAmount(),
                    currency.name(),
                    currentUser.getEmail(),
                    "Order: " + order.getOrderNumber()
            );
        } catch (Exception stripeException) {
            // If Stripe fails, we should clean up the order or mark it as failed
            logger.error("Stripe checkout session creation failed: {}", stripeException.getMessage());
            throw new RuntimeException("Failed to initialize payment gateway", stripeException);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("checkoutSession", checkoutSession);
        response.put("order", order);

        return ResponseEntity.ok(ApiResponse.success("Checkout session created successfully", response));
    }

    @GetMapping("/session/{sessionId}")
    @Operation(summary = "Get order by Stripe session ID", description = "Retrieve order details using Stripe checkout session ID")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderBySessionId(
            @Parameter(description = "Stripe session ID", required = true) @PathVariable String sessionId) {
        // Get session details from Stripe to extract order ID
        Map<String, Object> sessionDetails = stripeService.getSessionDetails(sessionId);
        Map<String, Object> metadata = (Map<String, Object>) sessionDetails.get("metadata");

        if (metadata == null || !metadata.containsKey("orderId")) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid session", "No order found for this session"));
        }

        String orderIdStr = (String) metadata.get("orderId");
        Long orderId = Long.parseLong(orderIdStr);

        // Get current user (may be null for unauthenticated access)
        User currentUser = null;
        try {
            currentUser = userService.getCurrentUser();
        } catch (Exception e) {
            // User not authenticated - that's okay, we'll verify via session
        }

        // Fetch order details
        OrderResponse order = orderService.getOrderByIdPublic(orderId);

        return ResponseEntity.ok(ApiResponse.success("Order retrieved successfully", order));
    }

    @PostMapping("/webhook/stripe")
    @Operation(summary = "Stripe webhook", description = "Handle Stripe webhook events for payment confirmation")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String signature) {
        logger.info("=== STRIPE WEBHOOK RECEIVED ===");
        logger.debug("Payload length: {}", (payload != null ? payload.length() : "null"));

        try {
            // Signature validation - CRITICAL SECURITY: Always verify signature
            if (signature == null || signature.trim().isEmpty()) {
                logger.warn("ERROR: Missing signature");
                return ResponseEntity.badRequest().body("Missing signature");
            }

            // Validate webhook signature using Stripe's cryptographic verification
            boolean isValidSignature = stripeService.validateWebhookSignature(payload, signature);

            logger.info("Signature valid: {}", isValidSignature);

            if (!isValidSignature) {
                logger.warn("ERROR: Invalid signature - potential attack attempt");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature");
            }

            // Parse webhook event
            logger.info("Parsing webhook event...");
            Map<String, Object> event = stripeService.parseWebhookEvent(payload);
            String eventType = (String) event.get("type");
            logger.info("Event type: {}", eventType);

            if ("checkout.session.completed".equals(eventType)) {
                logger.info("Processing checkout.session.completed...");
                Map<String, Object> data = (Map<String, Object>) event.get("data");
                Map<String, Object> session = (Map<String, Object>) data.get("object");

                // Extract metadata to determine if this is a donation or order
                Map<String, Object> metadata = (Map<String, Object>) session.get("metadata");
                logger.debug("Metadata: {}", metadata);

                // Check if this is a donation checkout
                String donationIdStr = (String) metadata.get("donationId");
                if (donationIdStr != null) {
                    logger.info("Processing donation checkout for donation ID: {}", donationIdStr);
                    String sessionId = (String) session.get("id");
                    String paymentIntentId = (String) session.get("payment_intent");
                    String paymentStatus = (String) session.get("payment_status");

                    if ("paid".equals(paymentStatus)) {
                        donationService.handleStripeCheckoutComplete(sessionId, paymentIntentId);
                        logger.info("Donation processed successfully");
                    } else {
                        logger.info("Donation payment not completed, status: {}", paymentStatus);
                    }

                    return ResponseEntity.ok("Donation webhook processed");
                }

                String formSubmissionIdStr = (String) metadata.get("formSubmissionId");
                if (formSubmissionIdStr != null) {
                    String sessionId = (String) session.get("id");
                    String formPaymentStatus = (String) session.get("payment_status");
                    if ("paid".equals(formPaymentStatus)) {
                        formSubmissionService.completeStripeFormPayment(sessionId, formSubmissionIdStr);
                    }
                    return ResponseEntity.ok("Form payment webhook processed");
                }

                // Otherwise, process as order
                String orderIdStr = (String) metadata.get("orderId");
                logger.info("Order ID from metadata: {}", orderIdStr);
                Long orderId = Long.parseLong(orderIdStr);

                // Get payment details
                String paymentIntentId = (String) session.get("payment_intent");
                String customerEmail = (String) session.get("customer_email");
                String paymentStatus = (String) session.get("payment_status");

                // payment_method_types is an array in Stripe, get first element
                List<String> paymentMethodTypes = (List<String>) session.get("payment_method_types");
                String paymentMethodType = (paymentMethodTypes != null && !paymentMethodTypes.isEmpty())
                    ? paymentMethodTypes.get(0)
                    : "card";

                logger.info("Payment status: {}", paymentStatus);
                logger.info("Payment intent ID: {}", paymentIntentId);
                logger.info("Payment method type: {}", paymentMethodType);

                // Only process if payment is successful
                if ("paid".equals(paymentStatus)) {
                    logger.info("Payment is paid, processing order...");
                    try {
                        // Get system user or use the order owner
                        logger.info("Fetching order {}", orderId);
                        // Use findByIdWithItems to eagerly fetch orderItems and avoid LazyInitializationException
                        Order order = orderRepository.findByIdWithItems(orderId)
                                .orElseThrow(() -> new RuntimeException("Order not found"));

                        logger.info("Order found: {}", order.getOrderNumber());

                        User systemUser;
                        try {
                            systemUser = userService.getSystemUser();
                            logger.info("Using system user");
                        } catch (Exception e) {
                            // If no system user, use the order owner
                            systemUser = order.getUser();
                            logger.info("Using order owner as system user");
                        }

                        // First, update the order status to PROCESSING if it's PENDING
                        if (order.getStatus() == OrderStatus.PENDING) {
                            logger.info("Updating order status to PROCESSING");
                            order.setStatus(OrderStatus.PROCESSING);
                            order.setUpdatedBy(systemUser.getId());
                            order.setUpdatedAt(LocalDateTime.now());
                            orderRepository.save(order);
                        }

                        // Complete the order automatically
                        logger.info("Calling orderService.completeOrder");
                        orderService.completeOrder(orderId, systemUser);

                        // Create Payment record for audit trail
                        try {
                            logger.info("Creating payment record...");
                            Payment payment = new Payment();
                            payment.setOrder(order);
                            payment.setUser(order.getUser());
                            payment.setAmount(order.getTotalAmount());
                            payment.setCurrency(order.getCurrency());
                            // Extract payment method from Stripe or use gateway name
                            payment.setPaymentMethod(paymentMethodType != null ? paymentMethodType.toUpperCase() : "CARD");
                            payment.setPaymentGateway("STRIPE");
                            payment.setGatewayTransactionId(paymentIntentId);
                            // Use helper method to properly set completed status and timestamp
                            payment.markAsCompleted(paymentIntentId);
                            payment.setCreatedBy(systemUser.getId());
                            payment.setUpdatedBy(systemUser.getId());
                            payment.setCreatedAt(LocalDateTime.now());
                            payment.setUpdatedAt(LocalDateTime.now());
                            paymentRepository.save(payment);
                            logger.info("Payment record created successfully");
                        } catch (Exception paymentException) {
                            // Log the error but don't fail the order
                            logger.error("Failed to create payment record: {}", paymentException.getMessage(), paymentException);
                        }

                        // Handle post-purchase actions based on what was purchased
                        logger.info("Processing post-purchase actions...");

                        // Check what types of products are in the order
                        boolean hasBooks = order.getOrderItems().stream()
                                .anyMatch(item -> item.getProductType() == com.powercity.power_city_platform.enums.ProductType.BOOK);
                        boolean hasCourses = order.getOrderItems().stream()
                                .anyMatch(item -> item.getProductType() == com.powercity.power_city_platform.enums.ProductType.COURSE);

                        // Handle email notifications based on order contents
                        if (hasBooks && hasCourses) {
                            // Both books and courses - send ONE combined email
                            logger.info("Order has both books and courses, sending combined email...");
                            try {
                                orderService.sendCombinedOrderEmailAndGrantAccess(orderId, systemUser);
                                logger.info("Combined email sent and course access granted successfully");
                            } catch (Exception e) {
                                logger.error("Failed to process combined order: {}", e.getMessage());
                                // Continue processing - don't fail the entire order
                            }
                        } else if (hasBooks) {
                            // Only books - send book download email
                            logger.info("Order has books only, sending download email...");
                            try {
                                orderService.sendBookDownloadEmail(orderId, systemUser);
                                logger.info("Download email sent successfully");
                            } catch (Exception e) {
                                logger.error("Failed to send book download email: {}", e.getMessage());
                                // Continue processing - don't fail the entire order
                            }
                        } else if (hasCourses) {
                            // Only courses - grant access and send course enrollment email
                            logger.info("Order has courses only, granting access...");
                            try {
                                orderService.grantCourseAccess(orderId, systemUser);
                                logger.info("Course access granted successfully");
                            } catch (Exception e) {
                                logger.error("Failed to grant course access: {}", e.getMessage());
                                // Continue processing - don't fail the entire order
                            }
                        }

                        logger.info("=== ORDER COMPLETED SUCCESSFULLY ===");
                        return ResponseEntity.ok("Order completed successfully");
                    } catch (Exception e) {
                        // Log the error but return 200 so Stripe doesn't retry
                        logger.error("ERROR completing order: {}", e.getMessage(), e);
                        return ResponseEntity.ok("Webhook received but order completion failed: " + e.getMessage());
                    }
                } else {
                    logger.info("Payment not completed - status: {}", paymentStatus);
                    return ResponseEntity.ok("Payment not completed - status: " + paymentStatus);
                }
            }

            logger.info("Event type not checkout.session.completed, returning OK");
            return ResponseEntity.ok("Webhook received");
        } catch (Exception e) {
            logger.error("ERROR in webhook handler: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("Webhook processing failed: " + e.getMessage());
        }
    }
}
