package com.powercity.power_city_platform.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Charge;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;
import com.stripe.param.ChargeCreateParams;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StripeService {

    private static final Logger logger = LoggerFactory.getLogger(StripeService.class);

    @Value("${payment.stripe.secret-key}")
    private String stripeSecretKey;

    @Value("${payment.stripe.publishable-key}")
    private String stripePublishableKey;

    @Value("${payment.stripe.webhook-secret}")
    private String stripeWebhookSecret;

    @Value("${app.email.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    // Initialize Stripe
    public void initializeStripe() {
        Stripe.apiKey = stripeSecretKey;
    }

    // Process payment with Stripe
    public Map<String, Object> processPayment(BigDecimal amount, String currency, String token, String orderNumber) {
        try {
            initializeStripe();

            // Convert amount to cents (Stripe expects amounts in cents)
            long amountInCents = amount.multiply(new BigDecimal("100")).longValue();

            ChargeCreateParams params = ChargeCreateParams.builder()
                    .setAmount(amountInCents)
                    .setCurrency(currency.toLowerCase())
                    .setSource(token)
                    .setDescription("Payment for order: " + orderNumber)
                    .setMetadata(Map.of("orderNumber", orderNumber))
                    .build();

            Charge charge = Charge.create(params);

            Map<String, Object> response = new HashMap<>();
            response.put("transactionId", charge.getId());
            response.put("status", charge.getStatus());
            response.put("amount", amount);
            response.put("currency", currency);
            response.put("gatewayFee", calculateGatewayFee(amount));
            response.put("processingFee", calculateProcessingFee(amount));
            response.put("orderNumber", orderNumber);

            return response;

        } catch (StripeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("status", "failed");
            throw new RuntimeException("Stripe payment failed: " + e.getMessage());
        }
    }

    // Process refund with Stripe
    public Map<String, Object> processRefund(String chargeId, BigDecimal refundAmount, String reason) {
        try {
            initializeStripe();

            // Convert amount to cents
            long amountInCents = refundAmount.multiply(new BigDecimal("100")).longValue();

            RefundCreateParams params = RefundCreateParams.builder()
                    .setCharge(chargeId)
                    .setAmount(amountInCents)
                    .setReason(RefundCreateParams.Reason.REQUESTED_BY_CUSTOMER)
                    .setMetadata(Map.of("refundReason", reason))
                    .build();

            Refund refund = Refund.create(params);

            Map<String, Object> response = new HashMap<>();
            response.put("refundId", refund.getId());
            response.put("status", refund.getStatus());
            response.put("amount", refundAmount);
            response.put("reason", reason);

            return response;

        } catch (StripeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("status", "failed");
            throw new RuntimeException("Stripe refund failed: " + e.getMessage());
        }
    }

    // Get charge details
    @Transactional(readOnly = true)
    public Map<String, Object> getChargeDetails(String chargeId) {
        try {
            initializeStripe();

            Charge charge = Charge.retrieve(chargeId);

            Map<String, Object> response = new HashMap<>();
            response.put("transactionId", charge.getId());
            response.put("status", charge.getStatus());
            response.put("amount", new BigDecimal(charge.getAmount()).divide(new BigDecimal("100")));
            response.put("currency", charge.getCurrency());
            response.put("created", charge.getCreated());
            response.put("description", charge.getDescription());
            response.put("metadata", charge.getMetadata());

            return response;

        } catch (StripeException e) {
            throw new RuntimeException("Failed to retrieve charge details: " + e.getMessage());
        }
    }

    // Get refund details
    @Transactional(readOnly = true)
    public Map<String, Object> getRefundDetails(String refundId) {
        try {
            initializeStripe();

            Refund refund = Refund.retrieve(refundId);

            Map<String, Object> response = new HashMap<>();
            response.put("refundId", refund.getId());
            response.put("status", refund.getStatus());
            response.put("amount", new BigDecimal(refund.getAmount()).divide(new BigDecimal("100")));
            response.put("currency", refund.getCurrency());
            response.put("created", refund.getCreated());
            response.put("reason", refund.getReason());
            response.put("metadata", refund.getMetadata());

            return response;

        } catch (StripeException e) {
            throw new RuntimeException("Failed to retrieve refund details: " + e.getMessage());
        }
    }

    // Validate webhook signature - CRITICAL SECURITY
    public boolean validateWebhookSignature(String payload, String signature) {
        try {
            // Use Stripe's official cryptographic webhook signature validation
            // This prevents replay attacks and ensures webhooks are from Stripe
            com.stripe.model.Event event = com.stripe.net.Webhook.constructEvent(
                payload, signature, stripeWebhookSecret
            );

            return event != null;
        } catch (com.stripe.exception.SignatureVerificationException e) {
            // Signature verification failed - potential attack or misconfiguration
            logger.error("Stripe webhook signature verification failed: {}", e.getMessage(), e);
            return false;
        } catch (Exception e) {
            // Other errors (invalid payload, etc.)
            logger.error("Stripe webhook validation error: {}", e.getMessage(), e);
            return false;
        }
    }

    // Parse webhook event
    public Map<String, Object> parseWebhookEvent(String payload) {
        try {
            // Parse the JSON payload using Jackson
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> eventMap = mapper.readValue(payload, Map.class);

            return eventMap;

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse webhook event: " + e.getMessage());
        }
    }

    // Calculate gateway fee (Stripe's fee structure)
    private BigDecimal calculateGatewayFee(BigDecimal amount) {
        // Stripe charges 2.9% + 30 cents for successful charges
        BigDecimal percentageFee = amount.multiply(new BigDecimal("0.029"));
        BigDecimal flatFee = new BigDecimal("0.30");
        return percentageFee.add(flatFee);
    }

    // Calculate processing fee (additional fees)
    private BigDecimal calculateProcessingFee(BigDecimal amount) {
        // Additional processing fees (if any)
        return BigDecimal.ZERO;
    }

    // Get Stripe configuration
    @Transactional(readOnly = true)
    public Map<String, String> getStripeConfig() {
        Map<String, String> config = new HashMap<>();
        config.put("publishableKey", stripePublishableKey);
        config.put("secretKey", stripeSecretKey != null ? "***" : null);
        config.put("webhookSecret", stripeWebhookSecret != null ? "***" : null);
        return config;
    }

    // Test Stripe connection
    public boolean testConnection() {
        try {
            initializeStripe();
            // Try to retrieve account information to test connection
            com.stripe.model.Account account = com.stripe.model.Account.retrieve();
            return account != null;
        } catch (Exception e) {
            return false;
        }
    }

    // Create payment intent (for client-side integration)
    public Map<String, Object> createPaymentIntent(BigDecimal amount, String currency, String orderNumber) {
        try {
            initializeStripe();

            // Convert amount to cents
            long amountInCents = amount.multiply(new BigDecimal("100")).longValue();

            com.stripe.param.PaymentIntentCreateParams params = com.stripe.param.PaymentIntentCreateParams.builder()
                    .setAmount(amountInCents)
                    .setCurrency(currency.toLowerCase())
                    .setDescription("Payment for order: " + orderNumber)
                    .build();

            com.stripe.model.PaymentIntent paymentIntent = com.stripe.model.PaymentIntent.create(params);

            Map<String, Object> response = new HashMap<>();
            response.put("clientSecret", paymentIntent.getClientSecret());
            response.put("paymentIntentId", paymentIntent.getId());
            response.put("amount", amount);
            response.put("currency", currency);
            response.put("orderNumber", orderNumber);

            return response;

        } catch (StripeException e) {
            throw new RuntimeException("Failed to create payment intent: " + e.getMessage());
        }
    }

    // Confirm payment intent
    @Transactional(readOnly = true)
    public Map<String, Object> confirmPaymentIntent(String paymentIntentId) {
        try {
            initializeStripe();

            com.stripe.model.PaymentIntent paymentIntent = com.stripe.model.PaymentIntent.retrieve(paymentIntentId);

            Map<String, Object> response = new HashMap<>();
            response.put("transactionId", paymentIntent.getId());
            response.put("status", paymentIntent.getStatus());
            response.put("amount", new BigDecimal(paymentIntent.getAmount()).divide(new BigDecimal("100")));
            response.put("currency", paymentIntent.getCurrency());
            response.put("clientSecret", paymentIntent.getClientSecret());

            return response;

        } catch (StripeException e) {
            throw new RuntimeException("Failed to confirm payment intent: " + e.getMessage());
        }
    }

    // Create Stripe checkout session (for orders)
    public Map<String, Object> createCheckoutSession(Long orderId, BigDecimal amount, String currency, String customerEmail, String description) {
        try {
            initializeStripe();

            // Convert amount to cents
            long amountInCents = amount.multiply(new BigDecimal("100")).longValue();

            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(frontendUrl + "/payment/success?session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(frontendUrl + "/payment/cancel")
                    .addLineItem(SessionCreateParams.LineItem.builder()
                            .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                    .setCurrency(currency.toLowerCase())
                                    .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                            .setName(description)
                                            .build())
                                    .setUnitAmount(amountInCents)
                                    .build())
                            .setQuantity(1L)
                            .build())
                    .setCustomerEmail(customerEmail)
                    .putMetadata("orderId", orderId.toString())
                    .build();

            Session session = Session.create(params);

            Map<String, Object> response = new HashMap<>();
            response.put("sessionId", session.getId());
            response.put("checkoutUrl", session.getUrl());
            response.put("orderId", orderId);
            response.put("amount", amount);
            response.put("currency", currency);

            return response;

        } catch (StripeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("status", "failed");
            throw new RuntimeException("Failed to create Stripe checkout session: " + e.getMessage());
        }
    }

    // Create Stripe checkout session with custom metadata (for donations, etc.)
    public Map<String, Object> createCheckoutSession(
            BigDecimal amount,
            String currency,
            String description,
            String customerEmail,
            Map<String, String> customMetadata,
            String successUrl,
            String cancelUrl) {
        try {
            initializeStripe();

            // Convert amount to cents
            long amountInCents = amount.multiply(new BigDecimal("100")).longValue();

            // Build session params
            SessionCreateParams.Builder paramsBuilder = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(successUrl != null ? successUrl : frontendUrl + "/donate?payment=success")
                    .setCancelUrl(cancelUrl != null ? cancelUrl : frontendUrl + "/donate?payment=cancelled")
                    .addLineItem(SessionCreateParams.LineItem.builder()
                            .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                    .setCurrency(currency.toLowerCase())
                                    .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                            .setName(description)
                                            .build())
                                    .setUnitAmount(amountInCents)
                                    .build())
                            .setQuantity(1L)
                            .build())
                    .setCustomerEmail(customerEmail);

            // Add custom metadata
            if (customMetadata != null && !customMetadata.isEmpty()) {
                customMetadata.forEach(paramsBuilder::putMetadata);
            }

            Session session = Session.create(paramsBuilder.build());

            Map<String, Object> response = new HashMap<>();
            response.put("sessionId", session.getId());
            response.put("sessionUrl", session.getUrl());
            response.put("amount", amount);
            response.put("currency", currency);

            return response;

        } catch (StripeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("status", "failed");
            throw new RuntimeException("Failed to create Stripe checkout session: " + e.getMessage());
        }
    }

    // Get session details from Stripe
    @Transactional(readOnly = true)
    public Map<String, Object> getSessionDetails(String sessionId) {
        try {
            initializeStripe();

            Session session = Session.retrieve(sessionId);

            Map<String, Object> response = new HashMap<>();
            response.put("sessionId", session.getId());
            response.put("status", session.getStatus());
            response.put("amount", session.getAmountTotal());
            response.put("currency", session.getCurrency());
            response.put("metadata", session.getMetadata());

            return response;

        } catch (StripeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("status", "failed");
            throw new RuntimeException("Failed to get session details: " + e.getMessage());
        }
    }
}
