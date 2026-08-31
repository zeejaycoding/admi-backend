package com.powercity.power_city_platform.entity;

import com.powercity.power_city_platform.enums.Currency;
import com.powercity.power_city_platform.enums.OfferingType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "donations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Donation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "donor_email", nullable = false)
    private String donorEmail;

    @Column(name = "donor_name")
    private String donorName;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false)
    private Currency currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "offering_type", nullable = false)
    private OfferingType offeringType;

    @Column(name = "payment_gateway", nullable = false)
    private String paymentGateway;

    @Column(name = "paypal_order_id")
    private String paypalOrderId;

    @Column(name = "paypal_capture_id")
    private String paypalCaptureId;

    @Column(name = "stripe_session_id")
    private String stripeSessionId;

    @Column(name = "stripe_payment_intent_id")
    private String stripePaymentIntentId;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "donation_date")
    private LocalDateTime donationDate;

    @Column(name = "message", length = 500)
    private String message;

    @Column(name = "is_anonymous")
    private Boolean isAnonymous = false;

    @Column(name = "receipt_sent")
    private Boolean receiptSent = false;

    @Column(name = "receipt_sent_at")
    private LocalDateTime receiptSentAt;

    public void markAsCompleted(String captureId) {
        this.status = "COMPLETED";
        this.paypalCaptureId = captureId;
        this.donationDate = LocalDateTime.now();
    }

    public void markAsFailed() {
        this.status = "FAILED";
    }

    public void markAsRefunded() {
        this.status = "REFUNDED";
    }

    public void markReceiptSent() {
        this.receiptSent = true;
        this.receiptSentAt = LocalDateTime.now();
    }
}
