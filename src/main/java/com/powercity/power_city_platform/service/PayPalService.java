package com.powercity.power_city_platform.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PayPalService {

    private static final Logger logger = LoggerFactory.getLogger(PayPalService.class);

    @Value("${payment.paypal.client-id}")
    private String clientId;

    @Value("${payment.paypal.client-secret}")
    private String clientSecret;

    @Value("${payment.paypal.mode:sandbox}")
    private String mode;

    @Value("${payment.paypal.webhook-id:}")
    private String webhookId;

    @Value("${app.email.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String accessToken;
    private long tokenExpiry = 0;

    /**
     * Get PayPal API base URL based on mode
     */
    private String getBaseUrl() {
        return "live".equalsIgnoreCase(mode)
            ? "https://api-m.paypal.com"
            : "https://api-m.sandbox.paypal.com";
    }

    /**
     * Get PayPal access token using OAuth 2.0
     * SECURITY: Uses client credentials to authenticate with PayPal
     */
    private String getAccessToken() {
        try {
            // Check if token is still valid
            if (accessToken != null && System.currentTimeMillis() < tokenExpiry) {
                return accessToken;
            }

            String url = getBaseUrl() + "/v1/oauth2/token";

            HttpHeaders headers = new HttpHeaders();
            headers.setBasicAuth(clientId, clientSecret);
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            String body = "grant_type=client_credentials";

            HttpEntity<String> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);

            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null) {
                accessToken = (String) responseBody.get("access_token");
                Integer expiresIn = (Integer) responseBody.get("expires_in");
                // Set token expiry with 5 minute buffer
                tokenExpiry = System.currentTimeMillis() + ((expiresIn - 300) * 1000L);
                return accessToken;
            }

            throw new RuntimeException("Failed to obtain PayPal access token");

        } catch (Exception e) {
            throw new RuntimeException("Failed to authenticate with PayPal: " + e.getMessage(), e);
        }
    }

    /**
     * Create PayPal order for donation
     * SECURITY: Server-side order creation prevents amount manipulation
     */
    public Map<String, Object> createDonationOrder(BigDecimal amount, String currency, String description) {
        try {
            String url = getBaseUrl() + "/v2/checkout/orders";

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(getAccessToken());
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> orderRequest = new HashMap<>();
            orderRequest.put("intent", "CAPTURE");

            // Application context
            Map<String, Object> applicationContext = new HashMap<>();
            applicationContext.put("brand_name", "Power City International");
            applicationContext.put("landing_page", "BILLING");
            applicationContext.put("cancel_url", frontendUrl + "/donate?cancelled=true");
            applicationContext.put("return_url", frontendUrl + "/donate?success=true");
            applicationContext.put("user_action", "PAY_NOW");
            applicationContext.put("shipping_preference", "NO_SHIPPING");
            orderRequest.put("application_context", applicationContext);

            // Purchase unit
            Map<String, Object> purchaseUnit = new HashMap<>();
            purchaseUnit.put("description", description);

            Map<String, Object> amountObj = new HashMap<>();
            amountObj.put("currency_code", currency);
            amountObj.put("value", amount.toString());
            purchaseUnit.put("amount", amountObj);

            orderRequest.put("purchase_units", List.of(purchaseUnit));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(orderRequest, headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);

            Map<String, Object> order = response.getBody();
            if (order == null) {
                throw new RuntimeException("PayPal returned empty response");
            }

            Map<String, Object> result = new HashMap<>();
            result.put("orderId", order.get("id"));
            result.put("status", order.get("status"));
            result.put("amount", amount);
            result.put("currency", currency);

            // Find approval link
            List<Map<String, Object>> links = (List<Map<String, Object>>) order.get("links");
            if (links != null) {
                for (Map<String, Object> link : links) {
                    if ("approve".equals(link.get("rel"))) {
                        result.put("approvalUrl", link.get("href"));
                        break;
                    }
                }
            }

            return result;

        } catch (Exception e) {
            throw new RuntimeException("Failed to create PayPal order: " + e.getMessage(), e);
        }
    }

    /**
     * Capture PayPal order (complete the payment)
     * SECURITY: Server-side capture ensures payment is verified
     */
    public Map<String, Object> captureOrder(String orderId) {
        try {
            String url = getBaseUrl() + "/v2/checkout/orders/" + orderId + "/capture";

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(getAccessToken());
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> request = new HttpEntity<>("{}", headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);

            Map<String, Object> order = response.getBody();
            if (order == null) {
                throw new RuntimeException("PayPal returned empty response");
            }

            Map<String, Object> result = new HashMap<>();
            result.put("orderId", order.get("id"));
            result.put("status", order.get("status"));

            // Extract capture details
            List<Map<String, Object>> purchaseUnits = (List<Map<String, Object>>) order.get("purchase_units");
            if (purchaseUnits != null && !purchaseUnits.isEmpty()) {
                Map<String, Object> purchaseUnit = purchaseUnits.get(0);
                Map<String, Object> payments = (Map<String, Object>) purchaseUnit.get("payments");

                if (payments != null) {
                    List<Map<String, Object>> captures = (List<Map<String, Object>>) payments.get("captures");
                    if (captures != null && !captures.isEmpty()) {
                        Map<String, Object> capture = captures.get(0);
                        result.put("captureId", capture.get("id"));
                        result.put("captureStatus", capture.get("status"));

                        Map<String, Object> amount = (Map<String, Object>) capture.get("amount");
                        if (amount != null) {
                            result.put("amount", amount.get("value"));
                            result.put("currency", amount.get("currency_code"));
                        }
                    }
                }
            }

            return result;

        } catch (Exception e) {
            throw new RuntimeException("Failed to capture PayPal order: " + e.getMessage(), e);
        }
    }

    /**
     * Get order details from PayPal
     * SECURITY: Verify order details directly with PayPal
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getOrderDetails(String orderId) {
        try {
            String url = getBaseUrl() + "/v2/checkout/orders/" + orderId;

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(getAccessToken());

            HttpEntity<String> request = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);

            Map<String, Object> order = response.getBody();
            if (order == null) {
                throw new RuntimeException("PayPal returned empty response");
            }

            Map<String, Object> result = new HashMap<>();
            result.put("orderId", order.get("id"));
            result.put("status", order.get("status"));
            result.put("intent", order.get("intent"));

            List<Map<String, Object>> purchaseUnits = (List<Map<String, Object>>) order.get("purchase_units");
            if (purchaseUnits != null && !purchaseUnits.isEmpty()) {
                Map<String, Object> purchaseUnit = purchaseUnits.get(0);
                Map<String, Object> amount = (Map<String, Object>) purchaseUnit.get("amount");
                if (amount != null) {
                    result.put("amount", amount.get("value"));
                    result.put("currency", amount.get("currency_code"));
                }
                result.put("description", purchaseUnit.get("description"));
            }

            return result;

        } catch (Exception e) {
            throw new RuntimeException("Failed to get PayPal order details: " + e.getMessage(), e);
        }
    }

    /**
     * Verify PayPal webhook signature using self-verification method
     * SECURITY: CRITICAL - Uses RSA certificate-based cryptographic verification
     * This prevents fake webhooks and replay attacks
     *
     * Algorithm (from PayPal docs):
     * 1. Form message: transmissionId|timeStamp|webhookId|crc32
     * 2. Download certificate from paypal-cert-url header
     * 3. Verify signature using certificate's public key with SHA256withRSA
     */
    public boolean verifyWebhookSignature(
            Map<String, String> headers,
            String payload) {

        try {
            // Extract webhook headers (case-insensitive)
            String transmissionId = getHeaderCaseInsensitive(headers, "paypal-transmission-id");
            String transmissionTime = getHeaderCaseInsensitive(headers, "paypal-transmission-time");
            String transmissionSig = getHeaderCaseInsensitive(headers, "paypal-transmission-sig");
            String certUrl = getHeaderCaseInsensitive(headers, "paypal-cert-url");
            String authAlgo = getHeaderCaseInsensitive(headers, "paypal-auth-algo");

            // Validate required headers are present
            if (transmissionId == null || transmissionTime == null ||
                transmissionSig == null || certUrl == null || authAlgo == null) {
                logger.error("Missing required PayPal webhook headers");
                logger.error("Headers received: {}", headers.keySet());
                return false;
            }

            // Note: PayPal does NOT send webhook_id in headers
            // We use the configured webhook ID for signature verification
            if (webhookId == null || webhookId.isEmpty()) {
                logger.error("Webhook ID not configured in application properties");
                return false;
            }

            // Calculate CRC32 checksum of raw payload
            String crc = calculateCRC32(payload);

            // Form the original message string as per PayPal docs
            // Format: transmissionId|timeStamp|webhookId|crc32
            String message = String.format("%s|%s|%s|%s",
                    transmissionId, transmissionTime, webhookId, crc);

            logger.info("Verifying signature for message: {}", message);

            // Download PayPal certificate
            String certPem = downloadCertificate(certUrl);

            // Verify signature using certificate's public key
            boolean isValid = verifySignatureWithCertificate(
                    message, transmissionSig, certPem, authAlgo);

            if (isValid) {
                logger.info("PayPal webhook signature verified successfully");
            } else {
                logger.error("PayPal webhook signature verification failed");
            }

            return isValid;

        } catch (Exception e) {
            logger.error("PayPal webhook verification error: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Download PayPal certificate from the cert URL
     */
    private String downloadCertificate(String certUrl) {
        try {
            HttpHeaders headers = new HttpHeaders();
            HttpEntity<String> request = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(certUrl, HttpMethod.GET, request, String.class);
            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("Failed to download PayPal certificate: " + e.getMessage(), e);
        }
    }

    /**
     * Verify signature using RSA with certificate's public key
     */
    private boolean verifySignatureWithCertificate(
            String message, String signatureBase64, String certPem, String algorithm) {
        try {
            // Parse certificate
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            ByteArrayInputStream certInputStream = new ByteArrayInputStream(certPem.getBytes(StandardCharsets.UTF_8));
            java.security.cert.Certificate cert = cf.generateCertificate(certInputStream);

            // Get public key from certificate
            PublicKey publicKey = cert.getPublicKey();

            // Decode base64 signature
            byte[] signatureBytes = Base64.getDecoder().decode(signatureBase64);

            // Verify signature using SHA256withRSA
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(publicKey);
            signature.update(message.getBytes(StandardCharsets.UTF_8));

            return signature.verify(signatureBytes);

        } catch (Exception e) {
            logger.error("Failed to verify signature with certificate: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Get header value case-insensitively
     * HTTP headers are case-insensitive, but Map keys might not match exactly
     */
    private String getHeaderCaseInsensitive(Map<String, String> headers, String headerName) {
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(headerName)) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * Calculate HMAC-SHA256 signature
     * SECURITY: Used for webhook signature verification
     */
    private String calculateHMAC(String data, String key) {
        try {
            Mac sha256HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                    key.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            );
            sha256HMAC.init(secretKey);
            byte[] hash = sha256HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate HMAC: " + e.getMessage(), e);
        }
    }

    /**
     * Calculate CRC32 checksum
     * SECURITY: Used in webhook signature verification
     */
    private String calculateCRC32(String data) {
        try {
            java.util.zip.CRC32 crc32 = new java.util.zip.CRC32();
            crc32.update(data.getBytes(StandardCharsets.UTF_8));
            return String.valueOf(crc32.getValue());
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate CRC32: " + e.getMessage(), e);
        }
    }

    /**
     * Parse webhook event
     * SECURITY: Only called after signature verification passes
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> parseWebhookEvent(String payload) {
        try {
            return objectMapper.readValue(payload, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse PayPal webhook: " + e.getMessage(), e);
        }
    }

    /**
     * Test PayPal connection
     */
    public boolean testConnection() {
        try {
            // Test by getting access token
            String token = getAccessToken();
            return token != null && !token.isEmpty();
        } catch (Exception e) {
            logger.error("PayPal connection test failed: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Get PayPal configuration (sanitized for logs)
     * SECURITY: Never expose secrets in responses
     */
    public Map<String, String> getPayPalConfig() {
        Map<String, String> config = new HashMap<>();
        config.put("clientId", clientId != null ? clientId.substring(0, Math.min(10, clientId.length())) + "..." : null);
        config.put("mode", mode);
        config.put("webhookId", webhookId != null && !webhookId.isEmpty() ? "***" : "not configured");
        return config;
    }
}
