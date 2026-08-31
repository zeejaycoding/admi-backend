package com.powercity.power_city_platform.controller;

import com.powercity.power_city_platform.dto.request.donation.CreateDonationRequest;
import com.powercity.power_city_platform.dto.response.common.ApiResponse;
import com.powercity.power_city_platform.dto.response.donation.DonationResponse;
import com.powercity.power_city_platform.service.DonationService;
import com.powercity.power_city_platform.service.PayPalService;
import com.powercity.power_city_platform.service.StripeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/donations")
@Tag(name = "Donations", description = "Church donation management endpoints")
public class DonationController {

    private static final Logger logger = LoggerFactory.getLogger(DonationController.class);

    private final DonationService donationService;
    private final PayPalService payPalService;
    private final StripeService stripeService;

    // Constructor injection (Spring auto-detects, no @Autowired needed)
    public DonationController(
            DonationService donationService,
            PayPalService payPalService,
            StripeService stripeService) {
        this.donationService = donationService;
        this.payPalService = payPalService;
        this.stripeService = stripeService;
    }

    /**
     * Create a new donation order
     * PUBLIC endpoint - no authentication required for donations
     * SECURITY: Server-side order creation prevents amount manipulation
     */
    @PostMapping("/create")
    @Operation(summary = "Create donation order", description = "Creates a PayPal donation order with server-side validation")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createDonation(
            @Valid @RequestBody CreateDonationRequest request) {

        try {
            logger.info("Creating donation order");

            Map<String, Object> result = donationService.createDonation(request);

            return ResponseEntity.ok(
                ApiResponse.success(
                    "Donation order created successfully. Please complete payment.",
                    result
                )
            );

        } catch (IllegalArgumentException e) {
            logger.warn("Invalid donation request: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Invalid request", e.getMessage()));

        } catch (Exception e) {
            logger.error("Failed to create donation: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to create donation",
                    "An error occurred while processing your donation. Please try again."));
        }
    }

    /**
     * Create Stripe checkout session for offering/donation
     * PUBLIC endpoint - no authentication required for donations
     * SECURITY: Server-side session creation prevents amount manipulation
     */
    @PostMapping("/create-stripe-session")
    @Operation(summary = "Create Stripe checkout session", description = "Creates a Stripe checkout session for donations")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createStripeCheckoutSession(
            @Valid @RequestBody CreateDonationRequest request) {

        try {
            logger.info("Creating Stripe checkout session for offering");

            // Create donation record with PENDING status
            Map<String, Object> donationResult = donationService.createStripeDonation(request);
            Long donationId = (Long) donationResult.get("donationId");

            // Prepare offering description
            String description = donationService.formatOfferingDescription(request.getOfferingType());

            // Create Stripe checkout session with donation metadata
            Map<String, String> metadata = new HashMap<>();
            metadata.put("donationId", donationId.toString());
            metadata.put("type", "offering");
            metadata.put("offeringType", request.getOfferingType().toString());

            Map<String, Object> sessionData = stripeService.createCheckoutSession(
                request.getAmount(),
                request.getCurrency().toString(),
                description,
                request.getDonorEmail(),
                metadata,
                request.getSuccessUrl(), // Use custom success URL if provided
                request.getCancelUrl()   // Use custom cancel URL if provided
            );

            // Update donation with Stripe session ID
            String sessionId = (String) sessionData.get("sessionId");
            donationService.updateDonationWithStripeSession(donationId, sessionId);

            Map<String, Object> response = new HashMap<>();
            response.put("sessionId", sessionId);
            response.put("sessionUrl", sessionData.get("sessionUrl"));
            response.put("donationId", donationId);

            return ResponseEntity.ok(
                ApiResponse.success(
                    "Stripe checkout session created successfully",
                    response
                )
            );

        } catch (IllegalArgumentException e) {
            logger.warn("Invalid offering request: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Invalid request", e.getMessage()));

        } catch (Exception e) {
            logger.error("Failed to create Stripe offering session: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to create offering session",
                    "An error occurred while processing your offering. Please try again."));
        }
    }

    /**
     * Capture donation after user approval
     * PUBLIC endpoint - called after user approves payment on PayPal
     */
    @PostMapping("/capture/{orderId}")
    @Operation(summary = "Capture donation", description = "Captures an approved PayPal donation")
    public ResponseEntity<ApiResponse<Map<String, Object>>> captureDonation(
            @PathVariable String orderId) {

        try {
            logger.info("Capturing donation for order: {}", orderId);

            // Capture payment via PayPal
            Map<String, Object> result = payPalService.captureOrder(orderId);

            // Webhook will update donation status when capture completes

            return ResponseEntity.ok(
                ApiResponse.success(
                    "Donation captured successfully. Thank you for your support!",
                    result
                )
            );

        } catch (Exception e) {
            logger.error("Failed to capture donation: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to capture donation",
                    "An error occurred while completing your donation. Please contact support."));
        }
    }

    /**
     * Get donation by ID
     * PUBLIC endpoint - allows donors to check their donation status
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get donation by ID", description = "Retrieves donation details by ID")
    public ResponseEntity<ApiResponse<DonationResponse>> getDonationById(@PathVariable Long id) {
        try {
            DonationResponse donation = donationService.getDonationById(id);
            return ResponseEntity.ok(
                ApiResponse.success("Donation retrieved successfully", donation)
            );
        } catch (Exception e) {
            logger.error("Failed to get donation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("Donation not found", e.getMessage()));
        }
    }

    /**
     * Get donation by PayPal order ID
     * PUBLIC endpoint - allows checking status after payment
     */
    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get donation by order ID", description = "Retrieves donation by PayPal order ID")
    public ResponseEntity<ApiResponse<DonationResponse>> getDonationByOrderId(@PathVariable String orderId) {
        try {
            DonationResponse donation = donationService.getDonationByPaypalOrderId(orderId);
            return ResponseEntity.ok(
                ApiResponse.success("Donation retrieved successfully", donation)
            );
        } catch (Exception e) {
            logger.error("Failed to get donation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("Donation not found", e.getMessage()));
        }
    }

    /**
     * Get donations by email
     * PUBLIC endpoint - allows donors to view their donation history
     */
    @GetMapping("/email/{email}")
    @Operation(summary = "Get donations by email", description = "Retrieves all donations for an email address")
    public ResponseEntity<ApiResponse<List<DonationResponse>>> getDonationsByEmail(@PathVariable String email) {
        try {
            List<DonationResponse> donations = donationService.getDonationsByEmail(email);
            return ResponseEntity.ok(
                ApiResponse.success("Donations retrieved successfully", donations)
            );
        } catch (Exception e) {
            logger.error("Failed to get donations: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to retrieve donations", e.getMessage()));
        }
    }

    /**
     * Get all donations (Admin only)
     * SECURITY: Requires ADMIN or SUPER_ADMIN role
     */
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Get all donations", description = "Admin: Retrieves all donations")
    public ResponseEntity<ApiResponse<List<DonationResponse>>> getAllDonations() {
        try {
            List<DonationResponse> donations = donationService.getAllDonations();
            return ResponseEntity.ok(
                ApiResponse.success("Donations retrieved successfully", donations)
            );
        } catch (Exception e) {
            logger.error("Failed to get all donations: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to retrieve donations", e.getMessage()));
        }
    }

    /**
     * Get donation analytics (Admin only)
     * SECURITY: Requires ADMIN or SUPER_ADMIN role
     */
    @GetMapping("/analytics")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Get donation analytics", description = "Admin: Retrieves donation statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDonationAnalytics() {
        try {
            Map<String, Object> analytics = donationService.getDonationAnalytics();
            return ResponseEntity.ok(
                ApiResponse.success("Analytics retrieved successfully", analytics)
            );
        } catch (Exception e) {
            logger.error("Failed to get analytics: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to retrieve analytics", e.getMessage()));
        }
    }

    /**
     * PayPal webhook endpoint
     * PUBLIC endpoint - called by PayPal servers
     * SECURITY: CRITICAL - Webhook signature verification is MANDATORY
     *
     * This endpoint receives real-time payment notifications from PayPal.
     * ALL webhooks MUST be cryptographically verified before processing.
     */
    @PostMapping("/webhook/paypal")
    @Operation(summary = "PayPal webhook", description = "Receives PayPal webhook notifications")
    public ResponseEntity<String> handlePayPalWebhook(
            HttpServletRequest request,
            @RequestBody String payload) {

        try {
            logger.info("Received PayPal webhook");

            // Extract all headers from request
            Map<String, String> headers = new HashMap<>();
            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                String headerValue = request.getHeader(headerName);
                headers.put(headerName, headerValue);
            }

            // SECURITY: Verify webhook signature using HMAC-SHA256
            // This prevents fake webhooks and replay attacks
            boolean isValid = payPalService.verifyWebhookSignature(headers, payload);

            if (!isValid) {
                logger.error("PayPal webhook signature verification FAILED - potential attack");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Webhook signature verification failed");
            }

            logger.info("PayPal webhook signature verified successfully");

            // Parse webhook event
            Map<String, Object> webhookEvent = payPalService.parseWebhookEvent(payload);

            // Process webhook event
            donationService.processPayPalWebhook(webhookEvent);

            return ResponseEntity.ok("Webhook processed successfully");

        } catch (Exception e) {
            logger.error("PayPal webhook processing error: {}", e.getMessage(), e);
            // Return 200 to PayPal even on error to prevent retries for invalid data
            // But log the error for investigation
            return ResponseEntity.ok("Webhook received");
        }
    }

    /**
     * Test PayPal connection (Admin only)
     * SECURITY: Requires ADMIN or SUPER_ADMIN role
     */
    @GetMapping("/test-paypal")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Test PayPal connection", description = "Admin: Tests PayPal API connection")
    public ResponseEntity<ApiResponse<Map<String, String>>> testPayPalConnection() {
        try {
            boolean isConnected = payPalService.testConnection();
            Map<String, String> config = payPalService.getPayPalConfig();

            Map<String, String> result = new HashMap<>(config);
            result.put("status", isConnected ? "connected" : "disconnected");

            return ResponseEntity.ok(
                ApiResponse.success(
                    isConnected ? "PayPal connection successful" : "PayPal connection failed",
                    result
                )
            );
        } catch (Exception e) {
            logger.error("PayPal connection test failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Connection test failed", e.getMessage()));
        }
    }
}
