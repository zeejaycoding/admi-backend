package com.powercity.power_city_platform.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.powercity.power_city_platform.entity.converter.JsonNodeConverter;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "forms")
public class Form extends BaseEntity {

    @Column(name = "title", nullable = false)
    @NotBlank(message = "Form title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "form_schema", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    @Convert(converter = JsonNodeConverter.class)
    private JsonNode formSchema;

    @Column(name = "require_authentication")
    private Boolean requireAuthentication = false;

    @Column(name = "is_published")
    private Boolean isPublished = false;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "submission_count")
    private Integer submissionCount = 0;

    @Column(name = "success_message", columnDefinition = "TEXT")
    private String successMessage;

    @Column(name = "form_code", unique = true, length = 50)
    private String formCode; // For page-based forms (e.g., PARTNERSHIP)

    @Column(name = "event_code", unique = true, length = 50)
    private String eventCode;

    @Column(name = "requires_payment")
    private Boolean requiresPayment = false;

    @Column(name = "payment_amount", precision = 10, scale = 2)
    private BigDecimal paymentAmount;

    @Column(name = "payment_currency", length = 10)
    private String paymentCurrency = "USD";

    @Column(name = "payment_gateway", length = 20)
    private String paymentGateway;

    @Column(name = "payment_description", length = 500)
    private String paymentDescription;

    @OneToMany(mappedBy = "form", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<FormSubmission> submissions = new ArrayList<>();

    public Form() {
        this.requireAuthentication = false;
        this.isPublished = false;
        this.isActive = true;
        this.submissionCount = 0;
        this.submissions = new ArrayList<>();
    }

    public Form(String title, String description, JsonNode formSchema, Boolean requireAuthentication,
                Boolean isPublished, Boolean isActive, Integer submissionCount, String successMessage) {
        this.title = title;
        this.description = description;
        this.formSchema = formSchema;
        this.requireAuthentication = requireAuthentication != null ? requireAuthentication : false;
        this.isPublished = isPublished != null ? isPublished : false;
        this.isActive = isActive != null ? isActive : true;
        this.submissionCount = submissionCount != null ? submissionCount : 0;
        this.successMessage = successMessage;
        this.submissions = new ArrayList<>();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public JsonNode getFormSchema() {
        return formSchema;
    }

    public void setFormSchema(JsonNode formSchema) {
        this.formSchema = formSchema;
    }

    public Boolean getRequireAuthentication() {
        return requireAuthentication;
    }

    public void setRequireAuthentication(Boolean requireAuthentication) {
        this.requireAuthentication = requireAuthentication;
    }

    public Boolean getIsPublished() {
        return isPublished;
    }

    public void setIsPublished(Boolean isPublished) {
        this.isPublished = isPublished;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Integer getSubmissionCount() {
        return submissionCount;
    }

    public void setSubmissionCount(Integer submissionCount) {
        this.submissionCount = submissionCount;
    }

    public String getSuccessMessage() {
        return successMessage;
    }

    public void setSuccessMessage(String successMessage) {
        this.successMessage = successMessage;
    }

    public List<FormSubmission> getSubmissions() {
        return submissions;
    }

    public void setSubmissions(List<FormSubmission> submissions) {
        this.submissions = submissions;
    }

    public String getFormCode() {
        return formCode;
    }

    public void setFormCode(String formCode) {
        this.formCode = formCode;
    }

    public String getEventCode() {
        return eventCode;
    }

    public void setEventCode(String eventCode) {
        this.eventCode = eventCode;
    }

    public Boolean getRequiresPayment() {
        return requiresPayment;
    }

    public void setRequiresPayment(Boolean requiresPayment) {
        this.requiresPayment = requiresPayment;
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

    public String getPaymentGateway() {
        return paymentGateway;
    }

    public void setPaymentGateway(String paymentGateway) {
        this.paymentGateway = paymentGateway;
    }

    public String getPaymentDescription() {
        return paymentDescription;
    }

    public void setPaymentDescription(String paymentDescription) {
        this.paymentDescription = paymentDescription;
    }

    public void incrementSubmissionCount() {
        this.submissionCount = (this.submissionCount == null ? 0 : this.submissionCount) + 1;
    }

    public boolean canUserSubmit(Long userId) {
        // Multiple submissions are not allowed - check if user already submitted
        return userId == null || this.submissions.stream()
                .noneMatch(submission -> submission.getUser() != null &&
                          submission.getUser().getId().equals(userId));
    }
}
