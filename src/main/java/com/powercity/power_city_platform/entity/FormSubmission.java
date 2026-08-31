package com.powercity.power_city_platform.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.powercity.power_city_platform.entity.converter.JsonNodeConverter;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "form_submissions")
public class FormSubmission extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "form_id", nullable = false)
    private Form form;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user; // Nullable for public submissions

    @Column(name = "submission_data", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    @Convert(converter = JsonNodeConverter.class)
    private JsonNode submissionData;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt = LocalDateTime.now();

    @Column(name = "payment_status", length = 30)
    private String paymentStatus;

    @Column(name = "stripe_session_id", length = 255)
    private String stripeSessionId;

    @Column(name = "paypal_order_id", length = 255)
    private String paypalOrderId;

    @Column(name = "payment_amount", precision = 10, scale = 2)
    private BigDecimal paymentAmount;

    @Column(name = "payment_currency", length = 10)
    private String paymentCurrency;

    @Column(name = "archived", nullable = false)
    private Boolean archived = false;

    public FormSubmission() {
        this.submittedAt = LocalDateTime.now();
    }

    public FormSubmission(Form form, User user, JsonNode submissionData, LocalDateTime submittedAt) {
        this.form = form;
        this.user = user;
        this.submissionData = submissionData;
        this.submittedAt = submittedAt != null ? submittedAt : LocalDateTime.now();
    }

    public Form getForm() {
        return form;
    }

    public void setForm(Form form) {
        this.form = form;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public JsonNode getSubmissionData() {
        return submissionData;
    }

    public void setSubmissionData(JsonNode submissionData) {
        this.submissionData = submissionData;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getStripeSessionId() {
        return stripeSessionId;
    }

    public void setStripeSessionId(String stripeSessionId) {
        this.stripeSessionId = stripeSessionId;
    }

    public String getPaypalOrderId() {
        return paypalOrderId;
    }

    public void setPaypalOrderId(String paypalOrderId) {
        this.paypalOrderId = paypalOrderId;
    }

    public BigDecimal getPaymentAmount() {
        return paymentAmount;
    }

    public void setPaymentAmount(BigDecimal paymentAmount) {
        this.paymentAmount = paymentAmount;
    }

    public String getPaymentCurrency() {
        return paymentCurrency;
    }

    public void setPaymentCurrency(String paymentCurrency) {
        this.paymentCurrency = paymentCurrency;
    }

    public Boolean getArchived() {
        return archived != null && archived;
    }

    public void setArchived(Boolean archived) {
        this.archived = archived != null && archived;
    }
}
