package com.powercity.power_city_platform.controller;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import com.powercity.power_city_platform.dto.request.form.CreateFormRequest;
import com.powercity.power_city_platform.dto.request.form.UpdateFormRequest;
import com.powercity.power_city_platform.dto.response.form.FormAnalyticsResponse;
import com.powercity.power_city_platform.dto.response.form.FormResponse;
import com.powercity.power_city_platform.dto.response.form.FormSubmissionResponse;
import com.powercity.power_city_platform.entity.User;
import com.powercity.power_city_platform.service.FormService;
import com.powercity.power_city_platform.service.FormSubmissionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/forms")
@RequiredArgsConstructor
public class FormController {

    private final FormService formService;

    private final FormSubmissionService formSubmissionService;

    private static final Logger logger = LoggerFactory.getLogger(FormController.class);


    /**
     * Get all published forms (for public /programs page)
     */
    @GetMapping
    public ResponseEntity<List<FormResponse>> getPublishedForms() {
        List<FormResponse> forms = formService.getPublishedForms();
        return ResponseEntity.ok(forms);
    }

    /**
     * Get all published forms (paginated)
     */
    @GetMapping("/paginated")
    public ResponseEntity<Page<FormResponse>> getPublishedFormsPaginated(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir
    ) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<FormResponse> forms = formService.getPublishedFormsPaginated(pageable);
        return ResponseEntity.ok(forms);
    }

    /**
     * Get form for submission (validates auth and submission eligibility)
     */
    @GetMapping("/{formId}/submit")
    public ResponseEntity<FormResponse> getFormForSubmission(
            @PathVariable Long formId,
            @AuthenticationPrincipal User currentUser
    ) {
        FormResponse form = formService.getFormForSubmission(formId, currentUser);
        return ResponseEntity.ok(form);
    }

    /**
     * Get form by form code (for page-based forms like PARTNERSHIP)
     */
    @GetMapping("/code/{formCode}")
    public ResponseEntity<FormResponse> getFormByFormCode(@PathVariable String formCode) {
        FormResponse form = formService.getFormByFormCode(formCode);
        return ResponseEntity.ok(form);
    }

    /**
     * Get form by event code (for program/event-based forms)
     */
    @GetMapping("/event/{eventCode}")
    public ResponseEntity<FormResponse> getFormByEventCode(@PathVariable String eventCode) {
        FormResponse form = formService.getFormByEventCode(eventCode);
        return ResponseEntity.ok(form);
    }


    /**
     * Create new form (SUPER_ADMIN only)
     */
    @PostMapping("/admin")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<FormResponse> createForm(
            @Valid @RequestBody CreateFormRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        FormResponse form = formService.createForm(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(form);
    }

    /**
     * Update form (SUPER_ADMIN only)
     */
    @PutMapping("/admin/{formId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<FormResponse> updateForm(
            @PathVariable Long formId,
            @Valid @RequestBody UpdateFormRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        FormResponse form = formService.updateForm(formId, request, currentUser);
        return ResponseEntity.ok(form);
    }

    /**
     * Delete form (SUPER_ADMIN only)
     */
    @DeleteMapping("/admin/{formId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, String>> deleteForm(
            @PathVariable Long formId,
            @AuthenticationPrincipal User currentUser
    ) {
        formService.deleteForm(formId, currentUser);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Form deleted successfully");
        return ResponseEntity.ok(response);
    }

    /**
     * Publish or unpublish form (SUPER_ADMIN only)
     */
    @PostMapping("/admin/{formId}/publish")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<FormResponse> publishForm(
            @PathVariable Long formId,
            @RequestParam boolean publish,
            @AuthenticationPrincipal User currentUser
    ) {
        FormResponse form = formService.publishForm(formId, publish, currentUser);
        return ResponseEntity.ok(form);
    }

    /**
     * Get all forms (including unpublished) - SUPER_ADMIN only
     */
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Page<FormResponse>> getAllForms(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir
    ) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<FormResponse> forms = formService.getAllForms(pageable);
        return ResponseEntity.ok(forms);
    }

    /**
     * Search forms (SUPER_ADMIN only)
     */
    @GetMapping("/admin/search")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Page<FormResponse>> searchForms(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<FormResponse> forms = formService.searchForms(query, pageable);
        return ResponseEntity.ok(forms);
    }

    /**
     * Get form by ID (SUPER_ADMIN only)
     */
    @GetMapping("/admin/{formId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<FormResponse> getFormById(@PathVariable Long formId) {
        FormResponse form = formService.getFormById(formId);
        return ResponseEntity.ok(form);
    }

    /**
     * Get form analytics (SUPER_ADMIN only)
     */
    @GetMapping("/admin/{formId}/analytics")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<FormAnalyticsResponse> getFormAnalytics(@PathVariable Long formId) {
        FormAnalyticsResponse analytics = formService.getFormAnalytics(formId);
        return ResponseEntity.ok(analytics);
    }

    /**
     * Get top forms by submissions (SUPER_ADMIN only)
     */
    @GetMapping("/admin/top")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<FormResponse>> getTopForms(
            @RequestParam(defaultValue = "5") int limit
    ) {
        List<FormResponse> forms = formService.getTopFormsBySubmissions(limit);
        return ResponseEntity.ok(forms);
    }

    /**
     * Get global form analytics (SUPER_ADMIN only)
     */
    @GetMapping("/admin/analytics/global")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getGlobalAnalytics() {
        Map<String, Object> analytics = formService.getGlobalAnalytics();
        return ResponseEntity.ok(analytics);
    }

    @PostMapping("/{formId}/submit/bank-transfer")
    public ResponseEntity<Map<String, Object>> submitBankTransfer(
            @PathVariable Long formId,
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal User currentUser
    ) {
        JsonNode submissionData = null;
        if (request.containsKey("submissionData")) {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            submissionData = mapper.valueToTree(request.get("submissionData"));
        }
        BigDecimal overrideAmount = request.containsKey("overrideAmount")
                ? new BigDecimal(request.get("overrideAmount").toString()) : null;
        String overrideCurrency = (String) request.get("overrideCurrency");
        Map<String, Object> result = formSubmissionService.submitBankTransferForm(
                formId, submissionData, currentUser, overrideAmount, overrideCurrency);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/{formId}/checkout/stripe")
    public ResponseEntity<Map<String, Object>> createStripeCheckout(
            @PathVariable Long formId,
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal User currentUser
    ) {
        JsonNode submissionData = null;
        if (request.containsKey("submissionData")) {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            submissionData = mapper.valueToTree(request.get("submissionData"));
        }
        String customerEmail = (String) request.get("email");
        BigDecimal overrideAmount = request.containsKey("overrideAmount")
                ? new BigDecimal(request.get("overrideAmount").toString()) : null;
        String overrideCurrency = (String) request.get("overrideCurrency");
        Map<String, Object> result = formSubmissionService.createStripeCheckoutForForm(
                formId, submissionData, customerEmail, currentUser, overrideAmount, overrideCurrency);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{formId}/checkout/paypal")
    public ResponseEntity<Map<String, Object>> createPayPalOrder(
            @PathVariable Long formId,
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal User currentUser
    ) {
        JsonNode submissionData = null;
        if (request.containsKey("submissionData")) {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            submissionData = mapper.valueToTree(request.get("submissionData"));
        }
        BigDecimal overrideAmount = request.containsKey("overrideAmount")
                ? new BigDecimal(request.get("overrideAmount").toString()) : null;
        String overrideCurrency = (String) request.get("overrideCurrency");
        Map<String, Object> result = formSubmissionService.createPayPalOrderForForm(
                formId, submissionData, currentUser, overrideAmount, overrideCurrency);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/submission/{submissionId}/paypal/capture")
    public ResponseEntity<FormSubmissionResponse> capturePayPalFormPayment(
            @PathVariable Long submissionId,
            @RequestBody Map<String, String> request
    ) {
        String paypalOrderId = request.get("paypalOrderId");
        FormSubmissionResponse response = formSubmissionService.capturePayPalFormPayment(submissionId, paypalOrderId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/submission/by-session/{sessionId}")
    public ResponseEntity<FormSubmissionResponse> getSubmissionByStripeSession(
            @PathVariable String sessionId
    ) {
        FormSubmissionResponse response = formSubmissionService.getSubmissionByStripeSession(sessionId);
        return ResponseEntity.ok(response);
    }
}
