package com.powercity.power_city_platform.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.powercity.power_city_platform.dto.request.form.SubmitFormRequest;
import com.powercity.power_city_platform.dto.response.form.FormSubmissionResponse;
import com.powercity.power_city_platform.entity.User;
import com.powercity.power_city_platform.service.FormSubmissionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/form-submissions")
@RequiredArgsConstructor
public class FormSubmissionController {

    private final FormSubmissionService submissionService;
    private final ObjectMapper objectMapper;
    private final com.powercity.power_city_platform.service.S3FileService s3FileService;

    private static final Logger logger = LoggerFactory.getLogger(FormSubmissionController.class);


    /**
     * Submit form (public or authenticated based on form settings)
     */
    @PostMapping("/{formId}")
    public ResponseEntity<Map<String, Object>> submitForm(
            @PathVariable Long formId,
            @RequestParam(value = "submissionData") String submissionDataJson,
            @RequestParam(required = false) Map<String, List<MultipartFile>> files,
            @AuthenticationPrincipal User currentUser,
            HttpServletRequest httpRequest
    ) {
        try {
            // Parse submission data JSON
            JsonNode submissionData = objectMapper.readTree(submissionDataJson);
            SubmitFormRequest request = new SubmitFormRequest();
            request.setSubmissionData(submissionData);

            // Process submission
            FormSubmissionResponse submission = submissionService.submitForm(
                    formId,
                    request,
                    files,
                    currentUser,
                    httpRequest
            );

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Form submitted successfully");
            response.put("submissionId", submission.getId());
            response.put("submittedAt", submission.getSubmittedAt());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse submission data: " + e.getMessage());
        }
    }

    /**
     * Get current user's submissions
     */
    @GetMapping("/my-submissions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<FormSubmissionResponse>> getMySubmissions(
            @AuthenticationPrincipal User currentUser
    ) {
        List<FormSubmissionResponse> submissions = submissionService.getUserSubmissions(currentUser);
        return ResponseEntity.ok(submissions);
    }

    /**
     * Get current user's submissions for a specific form
     */
    @GetMapping("/my-submissions/form/{formId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<FormSubmissionResponse>> getMyFormSubmissions(
            @PathVariable Long formId,
            @AuthenticationPrincipal User currentUser
    ) {
        List<FormSubmissionResponse> submissions = submissionService.getUserFormSubmissions(formId, currentUser);
        return ResponseEntity.ok(submissions);
    }


    /**
     * Get all submissions for a form (SUPER_ADMIN only)
     */
    @GetMapping("/admin/form/{formId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Page<FormSubmissionResponse>> getFormSubmissions(
            @PathVariable Long formId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(5000) int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "submittedAt"));
        Page<FormSubmissionResponse> submissions = submissionService.getFormSubmissions(formId, pageable);
        return ResponseEntity.ok(submissions);
    }

    /**
     * Get all submissions across all forms (SUPER_ADMIN only)
     */
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Page<FormSubmissionResponse>> getAllSubmissions(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(5000) int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "submittedAt"));
        Page<FormSubmissionResponse> submissions = submissionService.getAllSubmissions(pageable);
        return ResponseEntity.ok(submissions);
    }

    /**
     * Get submission by ID (SUPER_ADMIN only)
     */
    @GetMapping("/admin/{submissionId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<FormSubmissionResponse> getSubmissionById(@PathVariable Long submissionId) {
        FormSubmissionResponse submission = submissionService.getSubmissionById(submissionId);
        return ResponseEntity.ok(submission);
    }

    /**
     * Delete submission (SUPER_ADMIN only)
     */
    @DeleteMapping("/admin/{submissionId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, String>> deleteSubmission(@PathVariable Long submissionId) {
        submissionService.deleteSubmission(submissionId);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Submission deleted successfully");
        return ResponseEntity.ok(response);
    }

    /**
     * Export form submissions to CSV or Excel with optional date filtering (SUPER_ADMIN only)
     */
    @GetMapping("/admin/form/{formId}/export")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<byte[]> exportSubmissions(
            @PathVariable Long formId,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(defaultValue = "csv") String format
    ) {
        try {
            byte[] exportData = submissionService.exportSubmissions(
                    formId,
                    dateFrom,
                    dateTo,
                    format
            );

            HttpHeaders headers = new HttpHeaders();

            if ("excel".equalsIgnoreCase(format)) {
                headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
                headers.setContentDispositionFormData("attachment", "form-submissions-" + formId + ".xlsx");
            } else {
                headers.setContentType(MediaType.parseMediaType("text/csv"));
                headers.setContentDispositionFormData("attachment", "form-submissions-" + formId + ".csv");
            }

            headers.setContentLength(exportData.length);

            return new ResponseEntity<>(exportData, headers, HttpStatus.OK);
        } catch (IOException e) {
            throw new RuntimeException("Failed to export submissions: " + e.getMessage());
        }
    }

    /**
     * Export payment submissions to CSV or Excel (SUPER_ADMIN only)
     */
    @GetMapping("/admin/form/{formId}/export-payments")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<byte[]> exportPayments(
            @PathVariable Long formId,
            @RequestParam(defaultValue = "ALL") String paymentMethod,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(defaultValue = "csv") String format
    ) {
        try {
            byte[] exportData = submissionService.exportPayments(formId, paymentMethod, dateFrom, dateTo, format);

            HttpHeaders headers = new HttpHeaders();
            if ("excel".equalsIgnoreCase(format)) {
                headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
                headers.setContentDispositionFormData("attachment", "payments-" + formId + ".xlsx");
            } else {
                headers.setContentType(MediaType.parseMediaType("text/csv"));
                headers.setContentDispositionFormData("attachment", "payments-" + formId + ".csv");
            }
            headers.setContentLength(exportData.length);
            return new ResponseEntity<>(exportData, headers, HttpStatus.OK);
        } catch (IOException e) {
            throw new RuntimeException("Failed to export payments: " + e.getMessage());
        }
    }

    /**
     * Generate presigned URL for form submission file download (ADMIN/SUPER_ADMIN only)
     * This is a secure endpoint that validates file access before generating download URLs
     */
    @GetMapping("/admin/file/download")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getFormSubmissionFileUrl(
            @RequestParam String s3Key,
            @RequestParam(defaultValue = "1") int expirationHours
    ) {
        try {
            // Validate that the S3 key is for a form submission
            if (!s3Key.startsWith("prod/form-submissions/") && !s3Key.startsWith("dev/form-submissions/")) {
                throw new RuntimeException("Invalid file path");
            }

            // Validate expiration hours (max 24 hours for security)
            if (expirationHours < 1 || expirationHours > 24) {
                expirationHours = 1;
            }

            // Generate presigned URL
            java.time.Duration expiration = java.time.Duration.ofHours(expirationHours);
            String presignedUrl = s3FileService.generatePresignedUrl(s3Key, expiration);

            Map<String, Object> response = new HashMap<>();
            response.put("presignedUrl", presignedUrl);
            response.put("expiresIn", expirationHours + " hours");
            response.put("fileName", s3Key.substring(s3Key.lastIndexOf('/') + 1));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate download URL: " + e.getMessage());
        }
    }
}
