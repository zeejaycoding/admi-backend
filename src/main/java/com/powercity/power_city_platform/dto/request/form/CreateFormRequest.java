package com.powercity.power_city_platform.dto.request.form;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public class CreateFormRequest {

    @NotBlank(message = "Form title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    private String description;

    @NotNull(message = "Form schema is required")
    private JsonNode formSchema;

    private Boolean requireAuthentication = false;

    private Boolean isPublished = false;

    private String successMessage;

    @Size(max = 50, message = "Form code must not exceed 50 characters")
    private String formCode;

    @Size(max = 50, message = "Event code must not exceed 50 characters")
    private String eventCode;

    private Boolean requiresPayment = false;
    private BigDecimal paymentAmount;
    private String paymentCurrency;
    private String paymentGateway;
    private String paymentDescription;

    public CreateFormRequest() {
        this.requireAuthentication = false;
        this.isPublished = false;
    }

    public CreateFormRequest(String title, String description, JsonNode formSchema, Boolean requireAuthentication,
                            Boolean isPublished, String successMessage) {
        this.title = title;
        this.description = description;
        this.formSchema = formSchema;
        this.requireAuthentication = requireAuthentication != null ? requireAuthentication : false;
        this.isPublished = isPublished != null ? isPublished : false;
        this.successMessage = successMessage;
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

    public String getSuccessMessage() {
        return successMessage;
    }

    public void setSuccessMessage(String successMessage) {
        this.successMessage = successMessage;
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
}
