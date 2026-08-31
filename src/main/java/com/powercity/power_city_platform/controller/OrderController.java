package com.powercity.power_city_platform.controller;

import com.powercity.power_city_platform.dto.request.order.CreateOrderRequest;
import com.powercity.power_city_platform.dto.request.order.BookPurchaseRequest;
import com.powercity.power_city_platform.dto.response.order.OrderResponse;
import com.powercity.power_city_platform.dto.response.common.ApiResponse;
import com.powercity.power_city_platform.entity.User;
import com.powercity.power_city_platform.enums.OrderStatus;
import com.powercity.power_city_platform.service.OrderService;
import com.powercity.power_city_platform.service.UserService;
import com.powercity.power_city_platform.service.StripeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Order Management", description = "APIs for managing digital book orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final UserService userService;
    private final StripeService stripeService;

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);


    @PostMapping
    @Operation(summary = "Create order", description = "Create a new order for digital books")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Parameter(description = "Order creation request", required = true)
            @Valid @RequestBody CreateOrderRequest request) {
        User currentUser = userService.getCurrentUser();
        OrderResponse order = orderService.createOrder(request, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Order created successfully", order));
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Get order by ID", description = "Get detailed information about a specific order")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(
            @Parameter(description = "Order ID", required = true) @PathVariable Long orderId) {
        User currentUser = userService.getCurrentUser();
        OrderResponse order = orderService.getOrderById(orderId, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Order retrieved successfully", order));
    }

    @GetMapping("/number/{orderNumber}")
    @Operation(summary = "Get order by order number", description = "Get order details using order number")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderByOrderNumber(
            @Parameter(description = "Order number", required = true) @PathVariable String orderNumber) {
        User currentUser = userService.getCurrentUser();
        OrderResponse order = orderService.getOrderByOrderNumber(orderNumber, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Order retrieved successfully", order));
    }

    @GetMapping("/my-orders")
    @Operation(summary = "Get user orders", description = "Get paginated list of current user's orders")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getMyOrders(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        User currentUser = userService.getCurrentUser();
        Page<OrderResponse> orders = orderService.getUserOrders(currentUser, page, size);
        return ResponseEntity.ok(ApiResponse.success("Orders retrieved successfully", orders));
    }

    @GetMapping("/my-orders/status/{status}")
    @Operation(summary = "Get user orders by status", description = "Get user orders filtered by status")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getMyOrdersByStatus(
            @Parameter(description = "Order status", required = true) @PathVariable OrderStatus status,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        User currentUser = userService.getCurrentUser();
        Page<OrderResponse> orders = orderService.getUserOrdersByStatus(currentUser, status, page, size);
        return ResponseEntity.ok(ApiResponse.success("Orders retrieved successfully", orders));
    }

    @PostMapping("/{orderId}/cancel")
    @Operation(summary = "Cancel order", description = "Cancel an order (only if it's pending or processing)")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @Parameter(description = "Order ID", required = true) @PathVariable Long orderId,
            @Parameter(description = "Cancellation reason") @RequestParam(required = false) String reason) {
        User currentUser = userService.getCurrentUser();
        OrderResponse order = orderService.cancelOrder(orderId, reason, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Order cancelled successfully", order));
    }

    @GetMapping("/my-orders/analytics")
    @Operation(summary = "Get user order analytics", description = "Get analytics for current user's orders")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMyOrderAnalytics() {
        User currentUser = userService.getCurrentUser();
        Map<String, Object> analytics = orderService.getUserOrderAnalytics(currentUser);
        return ResponseEntity.ok(ApiResponse.success("Order analytics retrieved successfully", analytics));
    }


    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get all orders", description = "Get all orders with pagination (Admin/Super Admin only)")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getAllOrders(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        Page<OrderResponse> orders = orderService.getAllOrders(page, size);
        return ResponseEntity.ok(ApiResponse.success("Orders retrieved successfully", orders));
    }

    @GetMapping("/admin/status/{status}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get orders by status", description = "Get orders filtered by status (Super Admin only)")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getOrdersByStatus(
            @Parameter(description = "Order status", required = true) @PathVariable OrderStatus status,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        Page<OrderResponse> orders = orderService.getOrdersByStatus(status, page, size);
        return ResponseEntity.ok(ApiResponse.success("Orders retrieved successfully", orders));
    }

    @GetMapping("/admin/search")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Search orders", description = "Search orders by order number, customer name, or email (Super Admin only)")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> searchOrders(
            @Parameter(description = "Search term", required = true) @RequestParam String searchTerm,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        Page<OrderResponse> orders = orderService.searchOrders(searchTerm, page, size);
        return ResponseEntity.ok(ApiResponse.success("Orders retrieved successfully", orders));
    }

    @PostMapping("/admin/{orderId}/process-payment")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Process order payment", description = "Mark order as processing with payment gateway transaction ID (Super Admin only)")
    public ResponseEntity<ApiResponse<OrderResponse>> processOrderPayment(
            @Parameter(description = "Order ID", required = true) @PathVariable Long orderId,
            @Parameter(description = "Payment gateway transaction ID", required = true) @RequestParam String transactionId) {
        User currentUser = userService.getCurrentUser();
        OrderResponse order = orderService.processOrderPayment(orderId, transactionId, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Order payment processed successfully", order));
    }

    @PostMapping("/admin/{orderId}/complete")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Complete order", description = "Mark order as completed and send download links (Super Admin only)")
    public ResponseEntity<ApiResponse<OrderResponse>> completeOrder(
            @Parameter(description = "Order ID", required = true) @PathVariable Long orderId) {
        User currentUser = userService.getCurrentUser();
        OrderResponse order = orderService.completeOrder(orderId, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Order completed successfully", order));
    }

    @PostMapping("/purchase-book")
    @Operation(summary = "Purchase book", description = "Create order and return Stripe checkout session for book purchase")
    public ResponseEntity<ApiResponse<Map<String, Object>>> purchaseBook(
            @Parameter(description = "Book purchase request", required = true)
            @Valid @RequestBody BookPurchaseRequest request) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required", "Please login to purchase books"));
        }

        // Create simplified order request
        CreateOrderRequest orderRequest = new CreateOrderRequest();
        orderRequest.setOrderItems(List.of(new CreateOrderRequest.OrderItemRequest(
            request.getBookId(),
            1,
            request.getPrice()
        )));
        orderRequest.setCurrency(request.getCurrency());
        orderRequest.setCustomerEmail(currentUser.getEmail());
        orderRequest.setCustomerName(currentUser.getFullName());
        orderRequest.setRegion(currentUser.getRegion());
        orderRequest.setNotes("Digital book purchase");

        // Create order
        OrderResponse order = orderService.createOrder(orderRequest, currentUser);

        // Create Stripe checkout session with improved error handling
        Map<String, Object> checkoutSession;
        try {
            checkoutSession = stripeService.createCheckoutSession(
                order.getId(),
                request.getPrice(),
                request.getCurrency().name(),
                currentUser.getEmail(),
                "Digital Book Purchase - Order #" + order.getOrderNumber()
            );
        } catch (Exception stripeException) {
            // If Stripe fails, we should handle the order appropriately
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Payment processing unavailable",
                            "Unable to create checkout session. Please try again later."));
        }

        // Include both order and checkout session info in response
        Map<String, Object> response = new HashMap<>();
        response.put("order", order);
        response.put("checkoutSession", checkoutSession);

        return ResponseEntity.ok(ApiResponse.success("Checkout session created successfully", response));
    }

    @PostMapping("/checkout-cart")
    @Operation(summary = "Checkout cart", description = "Create order from all cart items and return Stripe checkout session")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkoutCart(
            @Parameter(description = "Cart checkout request", required = true)
            @Valid @RequestBody com.powercity.power_city_platform.dto.request.order.CartCheckoutRequest request) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required", "Please login to checkout"));
        }

        // Validate currency - Only USD is currently supported
        if (request.getCurrency() != com.powercity.power_city_platform.enums.Currency.USD) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Payment not available",
                        "Payment processing is currently only available for USD. Please switch currency to proceed."));
        }

        // Create order from all cart items
        OrderResponse order = orderService.createOrderFromCart(request.getCurrency(), currentUser);

        // Create Stripe checkout session (USD only)
        Map<String, Object> checkoutSession;
        try {
            checkoutSession = stripeService.createCheckoutSession(
                    order.getId(),
                    order.getTotalAmount(),
                    request.getCurrency().name(),
                    currentUser.getEmail(),
                    "Cart Purchase - Order #" + order.getOrderNumber()
            );
        } catch (Exception stripeException) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Payment processing unavailable",
                            "Unable to create checkout session. Please try again later."));
        }

        // Include both order and checkout session info in response
        Map<String, Object> response = new HashMap<>();
        response.put("order", order);
        response.put("checkoutSession", checkoutSession);

        return ResponseEntity.ok(ApiResponse.success("Checkout session created successfully", response));
    }

    @PostMapping("/admin/{orderId}/confirm-payment")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Confirm payment and send download email", description = "Manually confirm payment and send download email to customer (Admin/Super Admin only)")
    public ResponseEntity<ApiResponse<OrderResponse>> confirmPaymentAndSendDownload(
            @Parameter(description = "Order ID", required = true) @PathVariable Long orderId) {
        User currentUser = userService.getCurrentUser();

        // Complete the order
        OrderResponse order = orderService.completeOrder(orderId, currentUser);

        // Send download email with actual book PDF URLs
        orderService.sendBookDownloadEmail(orderId, currentUser);

        return ResponseEntity.ok(ApiResponse.success("Payment confirmed and download email sent", order));
    }

    @PostMapping("/admin/{orderId}/send-download")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Send download email", description = "Send download email for completed order (Admin/Super Admin only)")
    public ResponseEntity<ApiResponse<String>> sendDownloadEmail(
            @Parameter(description = "Order ID", required = true) @PathVariable Long orderId) {
        User currentUser = userService.getCurrentUser();
        orderService.sendBookDownloadEmail(orderId, currentUser);

        return ResponseEntity.ok(ApiResponse.success("Download email sent successfully", "Email sent to customer"));
    }

    @GetMapping("/admin/analytics")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get order analytics", description = "Get comprehensive order analytics (Super Admin only)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOrderAnalytics() {
        Map<String, Object> analytics = orderService.getOrderAnalytics();
        return ResponseEntity.ok(ApiResponse.success("Order analytics retrieved successfully", analytics));
    }

    @GetMapping("/payment/success")
    @Operation(summary = "Payment success", description = "Handle successful payment redirect from Stripe")
    public ResponseEntity<ApiResponse<Map<String, Object>>> paymentSuccess(
            @Parameter(description = "Stripe session ID") @RequestParam String session_id) {
        // Get session details from Stripe
        Map<String, Object> sessionDetails = stripeService.getSessionDetails(session_id);

        // Extract order ID from metadata
        Map<String, Object> metadata = (Map<String, Object>) sessionDetails.get("metadata");
        String orderIdStr = (String) metadata.get("orderId");
        Long orderId = Long.parseLong(orderIdStr);

        // Get order details
        User currentUser = userService.getCurrentUser();
        OrderResponse order = orderService.getOrderById(orderId, currentUser);

        // Process the payment - this will create a payment record
        OrderResponse processedOrder = orderService.processOrderPayment(orderId, session_id, currentUser);

        // Complete the order - this will send email and mark as completed
        OrderResponse completedOrder = orderService.completeOrder(orderId, currentUser);

        Map<String, Object> response = new HashMap<>();
        response.put("orderId", orderId);
        response.put("sessionId", session_id);
        response.put("status", "success");
        response.put("message", "Payment completed successfully");

        return ResponseEntity.ok(ApiResponse.success("Payment successful", response));
    }

    @GetMapping("/payment/cancel")
    @Operation(summary = "Payment cancel", description = "Handle cancelled payment redirect from Stripe")
    public ResponseEntity<ApiResponse<Map<String, Object>>> paymentCancel(
            @Parameter(description = "Stripe session ID") @RequestParam(required = false) String session_id) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "cancelled");
        response.put("message", "Payment was cancelled by the user");

        if (session_id != null) {
            response.put("sessionId", session_id);
        }

        return ResponseEntity.ok(ApiResponse.success("Payment cancelled", response));
    }
}
