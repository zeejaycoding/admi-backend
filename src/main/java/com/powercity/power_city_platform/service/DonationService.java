package com.powercity.power_city_platform.service;

import com.powercity.power_city_platform.dto.request.donation.CreateDonationRequest;
import com.powercity.power_city_platform.dto.response.donation.DonationResponse;
import com.powercity.power_city_platform.entity.Donation;
import com.powercity.power_city_platform.enums.Currency;
import com.powercity.power_city_platform.enums.OfferingType;
import com.powercity.power_city_platform.exception.ResourceNotFoundException;
import com.powercity.power_city_platform.repository.DonationRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DonationService {

    private static final Logger logger = LoggerFactory.getLogger(DonationService.class);

    private final DonationRepository donationRepository;

    private final PayPalService payPalService;

    private final EmailService emailService;

    private final StripeService stripeService;

    /**
     * Create PayPal order for donation
     * SECURITY: Server-side order creation prevents amount manipulation
     */
    @Transactional
    public Map<String, Object> createDonation(CreateDonationRequest request) {
        try {
            // Validate request
            validateDonationRequest(request);

            // Create PayPal order
            String description = formatOfferingDescription(request.getOfferingType());
            Map<String, Object> paypalOrder = payPalService.createDonationOrder(
                request.getAmount(),
                request.getCurrency().name(),
                description
            );

            // Create donation record in PENDING status
            Donation donation = new Donation();
            donation.setDonorEmail(request.getDonorEmail());
            donation.setAmount(request.getAmount());
            donation.setCurrency(request.getCurrency());
            donation.setOfferingType(request.getOfferingType());
            donation.setPaymentGateway("PAYPAL");
            donation.setPaypalOrderId((String) paypalOrder.get("orderId"));
            donation.setStatus("PENDING");
            donation.setReceiptSent(false);

            Donation savedDonation = donationRepository.save(donation);
            logger.info("Created donation record: {} for order: {}",
                savedDonation.getId(), savedDonation.getPaypalOrderId());

            // Return PayPal order details with donation ID
            paypalOrder.put("donationId", savedDonation.getId());
            return paypalOrder;

        } catch (Exception e) {
            logger.error("Failed to create donation: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create donation: " + e.getMessage());
        }
    }

    /**
     * Create Stripe donation record
     * SECURITY: Server-side donation creation prevents amount manipulation
     */
    @Transactional
    public Map<String, Object> createStripeDonation(CreateDonationRequest request) {
        try {
            // Validate request
            validateDonationRequest(request);

            // Create donation record in PENDING status
            Donation donation = new Donation();
            donation.setDonorEmail(request.getDonorEmail());
            donation.setAmount(request.getAmount());
            donation.setCurrency(request.getCurrency());
            donation.setOfferingType(request.getOfferingType());
            donation.setPaymentGateway("STRIPE");
            donation.setStatus("PENDING");
            donation.setReceiptSent(false);

            Donation savedDonation = donationRepository.save(donation);
            logger.info("Created Stripe donation record: {}", savedDonation.getId());

            // Return donation details
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("donationId", savedDonation.getId());
            result.put("amount", savedDonation.getAmount());
            result.put("currency", savedDonation.getCurrency());
            result.put("offeringType", savedDonation.getOfferingType());

            return result;

        } catch (Exception e) {
            logger.error("Failed to create Stripe donation: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create Stripe donation: " + e.getMessage());
        }
    }

    /**
     * Update donation with Stripe session ID
     */
    @Transactional
    public void updateDonationWithStripeSession(Long donationId, String sessionId) {
        Donation donation = donationRepository.findById(donationId)
            .orElseThrow(() -> new ResourceNotFoundException("Donation not found: " + donationId));

        donation.setStripeSessionId(sessionId);
        donationRepository.save(donation);
        logger.info("Updated donation {} with Stripe session ID", donationId);
    }

    /**
     * Handle Stripe checkout completion
     * SECURITY: Only called after webhook signature verification
     */
    @Transactional
    public void handleStripeCheckoutComplete(String sessionId, String paymentIntentId) {
        try {
            Optional<Donation> donationOpt = donationRepository.findByStripeSessionId(sessionId);

            if (donationOpt.isPresent()) {
                Donation donation = donationOpt.get();
                donation.setStatus("COMPLETED");
                donation.setStripePaymentIntentId(paymentIntentId);
                donation.setDonationDate(LocalDateTime.now());

                Donation savedDonation = donationRepository.save(donation);
                logger.info("Donation {} completed with Stripe payment intent: {}",
                    savedDonation.getId(), paymentIntentId);

                // Send receipt email asynchronously
                sendDonationReceipt(savedDonation);
            } else {
                logger.warn("No donation found for Stripe session ID: {}", sessionId);
            }

        } catch (Exception e) {
            logger.error("Failed to handle Stripe checkout completion: {}", e.getMessage(), e);
        }
    }

    /**
     * Process PayPal webhook event
     * SECURITY: Only called after webhook signature verification
     */
    @Transactional
    public void processPayPalWebhook(Map<String, Object> webhookEvent) {
        try {
            String eventType = (String) webhookEvent.get("event_type");
            logger.info("Processing PayPal webhook event: {}", eventType);

            switch (eventType) {
                case "CHECKOUT.ORDER.APPROVED":
                    handleOrderApproved(webhookEvent);
                    break;

                case "PAYMENT.CAPTURE.COMPLETED":
                    handlePaymentCaptureCompleted(webhookEvent);
                    break;

                case "PAYMENT.CAPTURE.DENIED":
                case "PAYMENT.CAPTURE.PENDING":
                case "PAYMENT.CAPTURE.REFUNDED":
                    handlePaymentStatusChange(webhookEvent, eventType);
                    break;

                default:
                    logger.info("Unhandled webhook event type: {}", eventType);
            }

        } catch (Exception e) {
            logger.error("Failed to process PayPal webhook: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process webhook: " + e.getMessage());
        }
    }

    /**
     * Handle ORDER.APPROVED webhook event
     * User has approved payment but not yet captured
     */
    private void handleOrderApproved(Map<String, Object> webhookEvent) {
        try {
            Map<String, Object> resource = (Map<String, Object>) webhookEvent.get("resource");
            String orderId = (String) resource.get("id");

            Optional<Donation> donationOpt = donationRepository.findByPaypalOrderId(orderId);
            if (donationOpt.isPresent()) {
                Donation donation = donationOpt.get();
                donation.setStatus("APPROVED");
                donationRepository.save(donation);
                logger.info("Donation {} approved, waiting for capture", donation.getId());
            }

        } catch (Exception e) {
            logger.error("Failed to handle order approved: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle PAYMENT.CAPTURE.COMPLETED webhook event
     * Payment has been successfully captured by PayPal
     */
    private void handlePaymentCaptureCompleted(Map<String, Object> webhookEvent) {
        try {
            Map<String, Object> resource = (Map<String, Object>) webhookEvent.get("resource");
            String captureId = (String) resource.get("id");

            // Get order ID from supplementary_data
            Map<String, Object> supplementaryData = (Map<String, Object>) resource.get("supplementary_data");
            Map<String, Object> relatedIds = (Map<String, Object>) supplementaryData.get("related_ids");
            String orderId = (String) relatedIds.get("order_id");

            Optional<Donation> donationOpt = donationRepository.findByPaypalOrderId(orderId);
            if (donationOpt.isPresent()) {
                Donation donation = donationOpt.get();
                donation.setStatus("COMPLETED");
                donation.setPaypalCaptureId(captureId);
                donation.setDonationDate(LocalDateTime.now());

                Donation savedDonation = donationRepository.save(donation);
                logger.info("Donation {} completed with capture ID: {}",
                    savedDonation.getId(), captureId);

                // Send receipt email asynchronously
                sendDonationReceipt(savedDonation);

            } else {
                logger.warn("No donation found for PayPal order ID: {}", orderId);
            }

        } catch (Exception e) {
            logger.error("Failed to handle payment capture: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle payment status changes
     */
    private void handlePaymentStatusChange(Map<String, Object> webhookEvent, String eventType) {
        try {
            Map<String, Object> resource = (Map<String, Object>) webhookEvent.get("resource");
            String captureId = (String) resource.get("id");

            // Get order ID from supplementary_data
            Map<String, Object> supplementaryData = (Map<String, Object>) resource.get("supplementary_data");
            Map<String, Object> relatedIds = (Map<String, Object>) supplementaryData.get("related_ids");
            String orderId = (String) relatedIds.get("order_id");

            Optional<Donation> donationOpt = donationRepository.findByPaypalOrderId(orderId);
            if (donationOpt.isPresent()) {
                Donation donation = donationOpt.get();

                String status = switch (eventType) {
                    case "PAYMENT.CAPTURE.DENIED" -> "DENIED";
                    case "PAYMENT.CAPTURE.PENDING" -> "PENDING";
                    case "PAYMENT.CAPTURE.REFUNDED" -> "REFUNDED";
                    default -> donation.getStatus();
                };

                donation.setStatus(status);
                donation.setPaypalCaptureId(captureId);
                donationRepository.save(donation);

                logger.info("Donation {} status updated to: {}", donation.getId(), status);
            }

        } catch (Exception e) {
            logger.error("Failed to handle payment status change: {}", e.getMessage(), e);
        }
    }

    /**
     * Get donation by ID
     */
    @Transactional(readOnly = true)
    public DonationResponse getDonationById(Long id) {
        Donation donation = donationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Donation not found with ID: " + id));
        return mapToResponse(donation);
    }

    /**
     * Get donation by PayPal order ID
     */
    @Transactional(readOnly = true)
    public DonationResponse getDonationByPaypalOrderId(String orderId) {
        Donation donation = donationRepository.findByPaypalOrderId(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Donation not found for order ID: " + orderId));
        return mapToResponse(donation);
    }

    /**
     * Get all donations by email
     */
    @Transactional(readOnly = true)
    public List<DonationResponse> getDonationsByEmail(String email) {
        List<Donation> donations = donationRepository.findByDonorEmailOrderByCreatedAtDesc(email);
        return donations.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    /**
     * Get all donations (admin only)
     */
    @Transactional(readOnly = true)
    public List<DonationResponse> getAllDonations() {
        List<Donation> donations = donationRepository.findAllByOrderByCreatedAtDesc();
        return donations.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    /**
     * Get donation analytics
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getDonationAnalytics() {
        Double totalDonationsDouble = donationRepository.getTotalDonations();
        BigDecimal totalDonations = totalDonationsDouble != null
            ? BigDecimal.valueOf(totalDonationsDouble)
            : BigDecimal.ZERO;
        Long donationCount = donationRepository.count();

        return Map.of(
            "totalDonations", totalDonations,
            "donationCount", donationCount,
            "averageDonation", donationCount > 0 && totalDonations.compareTo(BigDecimal.ZERO) > 0
                ? totalDonations.divide(BigDecimal.valueOf(donationCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO
        );
    }

    /**
     * Send donation receipt email
     * TODO: Implement email template for donation receipts
     */
    private void sendDonationReceipt(Donation donation) {
        try {
            if (donation.getDonorEmail() == null) {
                logger.warn("Cannot send receipt - no email for donation: {}", donation.getId());
                return;
            }

            // TODO: Create donation receipt email template and send using EmailService
            // For now, just mark as receipt sent and log
            logger.info("Donation receipt ready for: {} (Email: {})",
                donation.getId(), donation.getDonorEmail());

            donation.setReceiptSent(true);
            donation.setReceiptSentAt(LocalDateTime.now());
            donationRepository.save(donation);

            logger.info("Receipt marked as sent for donation: {}", donation.getId());

        } catch (Exception e) {
            logger.error("Failed to process donation receipt: {}", e.getMessage(), e);
        }
    }

    /**
     * Validate donation request
     */
    private void validateDonationRequest(CreateDonationRequest request) {
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Donation amount must be greater than zero");
        }

        if (request.getAmount().compareTo(new BigDecimal("1000000")) > 0) {
            throw new IllegalArgumentException("Donation amount exceeds maximum allowed");
        }
    }

    /**
     * Format offering type for display
     */
    public String formatOfferingDescription(OfferingType offeringType) {
        return switch (offeringType) {
            case WORSHIP_OFFERING -> "Worship Offering";
            case HONOUR_OFFERING -> "Honour Offering";
            case ADOMA_HONOUR_OFFERING -> "ADOMA Honour Offering";
            case DISCIPLESHIP_HONOUR_OFFERING -> "Discipleship Honour Offering";
            case PBS_HONOUR_OFFERING -> "Power Bible School Honour Offering";
            case PARTNERSHIP_OFFERING -> "Partnership Offering";
            case MISSION_OFFERING -> "Mission Offering";
            case MEDIA_OFFERING -> "Media Offering";
        };
    }

    /**
     * Map entity to response DTO
     */
    private DonationResponse mapToResponse(Donation donation) {
        return DonationResponse.builder()
            .id(donation.getId())
            .donorEmail(donation.getDonorEmail())
            .donorName(donation.getDonorName())
            .amount(donation.getAmount())
            .currency(donation.getCurrency())
            .offeringType(donation.getOfferingType())
            .paymentGateway(donation.getPaymentGateway())
            .paypalOrderId(donation.getPaypalOrderId())
            .paypalCaptureId(donation.getPaypalCaptureId())
            .status(donation.getStatus())
            .donationDate(donation.getDonationDate())
            .message(donation.getMessage())
            .isAnonymous(donation.getIsAnonymous())
            .receiptSent(donation.getReceiptSent())
            .receiptSentAt(donation.getReceiptSentAt())
            .createdAt(donation.getCreatedAt())
            .build();
    }
}
