package com.powercity.power_city_platform.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.powercity.power_city_platform.dto.request.form.SubmitFormRequest;
import com.powercity.power_city_platform.dto.response.form.FormSubmissionResponse;
import com.powercity.power_city_platform.entity.Form;
import com.powercity.power_city_platform.entity.FormSubmission;
import com.powercity.power_city_platform.entity.User;
import com.powercity.power_city_platform.exception.ResourceNotFoundException;
import com.powercity.power_city_platform.repository.FormRepository;
import com.powercity.power_city_platform.repository.FormSubmissionRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

@Service
@Transactional
@RequiredArgsConstructor
public class FormSubmissionService {

    private static final Logger logger = LoggerFactory.getLogger(FormSubmissionService.class);

    private final FormSubmissionRepository submissionRepository;

    private final FormRepository formRepository;

    private final S3FileService s3FileService;

    private final ObjectMapper objectMapper;

    private final StripeService stripeService;

    private final PayPalService payPalService;

    private final EmailService emailService;

    @Value("${app.email.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    public FormSubmissionResponse submitForm(
            Long formId,
            SubmitFormRequest request,
            Map<String, List<MultipartFile>> files,
            User currentUser,
            HttpServletRequest httpRequest
    ) {
        logger.info("Processing form submission for form ID: {}", formId);

        // Get form
        Form form = formRepository.findById(formId)
                .orElseThrow(() -> new ResourceNotFoundException("Form not found with ID: " + formId));

        // Validate form is published
        if (!form.getIsPublished()) {
            throw new RuntimeException("This form is not currently accepting submissions");
        }

        // Validate form is active
        if (!form.getIsActive()) {
            throw new RuntimeException("This form is no longer available");
        }

        // Check authentication requirement
        if (form.getRequireAuthentication() && currentUser == null) {
            throw new RuntimeException("Authentication required to submit this form");
        }

        // Check if user can submit (multiple submissions not allowed)
        if (currentUser != null) {
            if (submissionRepository.existsByFormAndUser(form, currentUser)) {
                throw new RuntimeException("You have already submitted this form");
            }
        }

        // Process file uploads if any
        JsonNode submissionData = request.getSubmissionData();
        if (files != null && !files.isEmpty()) {
            submissionData = processFileUploads(submissionData, files, formId);
        }

        // Create submission entity
        FormSubmission submission = new FormSubmission(
                form,
                currentUser,
                submissionData,
                LocalDateTime.now()
        );

        FormSubmission savedSubmission = submissionRepository.save(submission);

        // Increment form submission count
        form.incrementSubmissionCount();
        formRepository.save(form);

        logger.info("Form submission saved successfully with ID: {}", savedSubmission.getId());

        if (!Boolean.TRUE.equals(form.getRequiresPayment())) {
            sendSubmissionConfirmationEmail(savedSubmission, form, false);
        }

        return convertToSubmissionResponse(savedSubmission);
    }

    @Transactional(readOnly = true)
    public Page<FormSubmissionResponse> getFormSubmissions(Long formId, Pageable pageable, User currentUser) {
        logger.info("Fetching submissions for form ID: {}", formId);

        Form form = formRepository.findByIdAndIsActiveTrue(formId)
                .orElseThrow(() -> new ResourceNotFoundException("Form not found with ID: " + formId));

        Page<FormSubmission> submissions;
        if (currentUser != null && isNationalLeader(currentUser) && currentUser.getRegion() != null) {
            submissions = submissionRepository.findByFormAndArchivedFalseAndUserRegionOrderBySubmittedAtDesc(
                    form, currentUser.getRegion(), pageable);
        } else {
            submissions = submissionRepository.findByFormAndArchivedFalseOrderBySubmittedAtDesc(form, pageable);
        }

        return submissions.map(this::convertToSubmissionResponse);
    }

    @Transactional(readOnly = true)
    public Page<FormSubmissionResponse> getAllSubmissions(Pageable pageable, User currentUser) {
        logger.info("Fetching all form submissions");

        Page<FormSubmission> submissions;
        if (currentUser != null && isNationalLeader(currentUser) && currentUser.getRegion() != null) {
            submissions = submissionRepository.findByArchivedFalseAndUserRegionOrderBySubmittedAtDesc(
                    currentUser.getRegion(), pageable);
        } else {
            submissions = submissionRepository.findAllByArchivedFalseOrderBySubmittedAtDesc(pageable);
        }

        return submissions.map(this::convertToSubmissionResponse);
    }

    private boolean isNationalLeader(User user) {
        return user.getUserRoles() != null && user.getUserRoles().stream()
                .filter(ur -> Boolean.TRUE.equals(ur.getIsActive()))
                .anyMatch(ur -> ur.getRole() != null && "NATIONAL_LEADER".equals(ur.getRole().getName()));
    }

    @Transactional(readOnly = true)
    public FormSubmissionResponse getSubmissionById(Long submissionId) {
        logger.info("Fetching submission by ID: {}", submissionId);

        FormSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found with ID: " + submissionId));

        return convertToSubmissionResponse(submission);
    }

    @Transactional(readOnly = true)
    public List<FormSubmissionResponse> getUserSubmissions(User currentUser) {
        logger.info("Fetching submissions for user ID: {}", currentUser.getId());

        List<FormSubmission> submissions = submissionRepository.findByUserOrderBySubmittedAtDesc(currentUser);

        return submissions.stream()
                .map(this::convertToSubmissionResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FormSubmissionResponse> getUserFormSubmissions(Long formId, User currentUser) {
        logger.info("Fetching submissions for user ID: {} and form ID: {}", currentUser.getId(), formId);

        Form form = formRepository.findById(formId)
                .orElseThrow(() -> new ResourceNotFoundException("Form not found with ID: " + formId));

        List<FormSubmission> submissions = submissionRepository.findByFormAndUserOrderBySubmittedAtDesc(form, currentUser);

        return submissions.stream()
                .map(this::convertToSubmissionResponse)
                .collect(Collectors.toList());
    }

    public void deleteSubmission(Long submissionId) {
        logger.info("Deleting submission ID: {}", submissionId);

        FormSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found with ID: " + submissionId));

        // Delete associated files from S3 if any
        deleteSubmissionFiles(submission);

        submissionRepository.delete(submission);
        logger.info("Submission deleted successfully: {}", submissionId);
    }

    public Map<String, Object> submitBankTransferForm(
            Long formId,
            JsonNode submissionData,
            User currentUser,
            BigDecimal overrideAmount,
            String overrideCurrency) {

        Form form = formRepository.findById(formId)
                .orElseThrow(() -> new ResourceNotFoundException("Form not found with ID: " + formId));

        if (!form.getIsPublished()) {
            throw new RuntimeException("This form is not currently accepting submissions");
        }
        if (!form.getIsActive()) {
            throw new RuntimeException("This form is no longer available");
        }

        BigDecimal finalAmount = (overrideAmount != null && overrideAmount.compareTo(BigDecimal.ZERO) > 0)
                ? overrideAmount : (form.getPaymentAmount() != null ? form.getPaymentAmount() : BigDecimal.ZERO);
        String finalCurrency = (overrideCurrency != null && !overrideCurrency.isBlank())
                ? overrideCurrency : (form.getPaymentCurrency() != null ? form.getPaymentCurrency() : "NGN");

        FormSubmission submission = new FormSubmission(form, currentUser, submissionData, LocalDateTime.now());
        submission.setPaymentStatus("PENDING_TRANSFER");
        submission.setPaymentAmount(finalAmount);
        submission.setPaymentCurrency(finalCurrency);
        FormSubmission savedSubmission = submissionRepository.save(submission);

        String firstName = extractFieldFromData(submissionData, "firstName", "first_name", "First Name");
        String lastName  = extractFieldFromData(submissionData, "lastName",  "last_name",  "Last Name");
        String email     = extractFieldFromData(submissionData, "email", "Email");
        String phone     = extractFieldFromData(submissionData, "phone", "Phone");
        String planLabel = extractFieldFromData(submissionData, "paymentPlanLabel");

        if (firstName == null) firstName = "Friend";
        if (lastName  == null) lastName  = "";
        if (email == null && currentUser != null) email = currentUser.getEmail();

        if ("POWER_BIBLE_SCHOOL".equals(form.getEventCode())) {
            // Alert powerbibleschool@drabeldamina.org about new PBS registration
            emailService.sendPBSBankTransferNotification(firstName, lastName, email, phone, planLabel, finalAmount, finalCurrency);
            if (email != null && !email.isBlank()) {
                emailService.sendPBSBankTransferUserConfirmation(email, firstName, planLabel, finalAmount, finalCurrency);
            }
        } else {
            // Alert adoma@drabeldamina.org about new ADOMA registration
            emailService.sendBankTransferNotification(firstName, lastName, email, phone, planLabel, finalAmount, finalCurrency);
            if (email != null && !email.isBlank()) {
                emailService.sendBankTransferUserConfirmation(email, firstName, planLabel, finalAmount, finalCurrency);
            }
        }

        logger.info("Bank transfer submission saved ID: {} for form ID: {}", savedSubmission.getId(), formId);

        Map<String, Object> result = new HashMap<>();
        result.put("submissionId", savedSubmission.getId());
        result.put("message", "Registration received. Please complete your bank transfer.");
        return result;
    }

    public Map<String, Object> createStripeCheckoutForForm(Long formId, JsonNode submissionData, String customerEmail, User currentUser, BigDecimal overrideAmount, String overrideCurrency) {
        Form form = formRepository.findById(formId)
                .orElseThrow(() -> new ResourceNotFoundException("Form not found with ID: " + formId));

        if (!form.getIsPublished()) {
            throw new RuntimeException("This form is not currently accepting submissions");
        }
        if (!form.getIsActive()) {
            throw new RuntimeException("This form is no longer available");
        }
        if (!Boolean.TRUE.equals(form.getRequiresPayment())) {
            throw new RuntimeException("This form does not require payment");
        }
        if (form.getPaymentAmount() == null || form.getPaymentAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Invalid payment amount configured for this form");
        }
        if (currentUser != null && submissionRepository.existsByFormAndUser(form, currentUser)) {
            throw new RuntimeException("You have already submitted this form");
        }

        BigDecimal finalAmount = (overrideAmount != null && overrideAmount.compareTo(BigDecimal.ZERO) > 0)
                ? overrideAmount : form.getPaymentAmount();
        String finalCurrency = (overrideCurrency != null && !overrideCurrency.isBlank())
                ? overrideCurrency : (form.getPaymentCurrency() != null ? form.getPaymentCurrency() : "USD");

        FormSubmission submission = new FormSubmission(form, currentUser, submissionData, LocalDateTime.now());
        submission.setPaymentStatus("PENDING");
        submission.setPaymentAmount(finalAmount);
        submission.setPaymentCurrency(finalCurrency);
        FormSubmission savedSubmission = submissionRepository.save(submission);

        String successUrl = frontendUrl + "/form-payment-success?sessionId={CHECKOUT_SESSION_ID}";
        String cancelUrl = frontendUrl + "/form-payment-cancelled";

        Map<String, String> metadata = new HashMap<>();
        metadata.put("formSubmissionId", savedSubmission.getId().toString());
        metadata.put("formId", formId.toString());

        String description = (form.getPaymentDescription() != null && !form.getPaymentDescription().isBlank())
                ? form.getPaymentDescription()
                : "Registration for " + (form.getTitle() != null ? form.getTitle() : "Programme");

        Map<String, Object> session = stripeService.createCheckoutSession(
                finalAmount, finalCurrency, description, customerEmail, metadata, successUrl, cancelUrl);

        savedSubmission.setStripeSessionId((String) session.get("sessionId"));
        submissionRepository.save(savedSubmission);

        Map<String, Object> result = new HashMap<>();
        result.put("submissionId", savedSubmission.getId());
        result.put("checkoutUrl", session.get("sessionUrl"));
        result.put("sessionId", session.get("sessionId"));
        return result;
    }

    public Map<String, Object> createPayPalOrderForForm(Long formId, JsonNode submissionData, User currentUser, BigDecimal overrideAmount, String overrideCurrency) {
        Form form = formRepository.findById(formId)
                .orElseThrow(() -> new ResourceNotFoundException("Form not found with ID: " + formId));

        if (!form.getIsPublished()) {
            throw new RuntimeException("This form is not currently accepting submissions");
        }
        if (!form.getIsActive()) {
            throw new RuntimeException("This form is no longer available");
        }
        if (!Boolean.TRUE.equals(form.getRequiresPayment())) {
            throw new RuntimeException("This form does not require payment");
        }
        if (form.getPaymentAmount() == null || form.getPaymentAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Invalid payment amount configured for this form");
        }
        if (currentUser != null && submissionRepository.existsByFormAndUser(form, currentUser)) {
            throw new RuntimeException("You have already submitted this form");
        }

        BigDecimal finalAmount = (overrideAmount != null && overrideAmount.compareTo(BigDecimal.ZERO) > 0)
                ? overrideAmount : form.getPaymentAmount();
        String finalCurrency = (overrideCurrency != null && !overrideCurrency.isBlank())
                ? overrideCurrency : (form.getPaymentCurrency() != null ? form.getPaymentCurrency() : "USD");

        FormSubmission submission = new FormSubmission(form, currentUser, submissionData, LocalDateTime.now());
        submission.setPaymentStatus("PENDING");
        submission.setPaymentAmount(finalAmount);
        submission.setPaymentCurrency(finalCurrency);
        FormSubmission savedSubmission = submissionRepository.save(submission);

        String description = (form.getPaymentDescription() != null && !form.getPaymentDescription().isBlank())
                ? form.getPaymentDescription()
                : "Registration for " + (form.getTitle() != null ? form.getTitle() : "Programme");

        Map<String, Object> order = payPalService.createDonationOrder(finalAmount, finalCurrency, description);

        savedSubmission.setPaypalOrderId((String) order.get("orderId"));
        submissionRepository.save(savedSubmission);

        Map<String, Object> result = new HashMap<>();
        result.put("submissionId", savedSubmission.getId());
        result.put("approvalUrl", order.get("approvalUrl"));
        result.put("paypalOrderId", order.get("orderId"));
        return result;
    }

    public FormSubmissionResponse capturePayPalFormPayment(Long submissionId, String paypalOrderId) {
        FormSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found with ID: " + submissionId));

        Map<String, Object> captureResult = payPalService.captureOrder(paypalOrderId);
        String captureStatus = (String) captureResult.get("captureStatus");

        Form form = submission.getForm();
        if ("COMPLETED".equals(captureStatus)) {
            submission.setPaymentStatus("COMPLETED");
            submission.setPaypalOrderId(paypalOrderId);

            form.incrementSubmissionCount();
            formRepository.save(form);

            submissionRepository.save(submission);
            sendSubmissionConfirmationEmail(submission, form, true);
        } else {
            submission.setPaymentStatus("FAILED");
            submissionRepository.save(submission);
        }

        return convertToSubmissionResponse(submission);
    }

    @Transactional(readOnly = true)
    public FormSubmissionResponse getSubmissionByStripeSession(String sessionId) {
        FormSubmission submission = submissionRepository.findByStripeSessionId(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("No submission found for session: " + sessionId));
        return convertToSubmissionResponse(submission);
    }

    public void completeStripeFormPayment(String sessionId, String submissionIdStr) {
        Long submissionId = Long.parseLong(submissionIdStr);
        FormSubmission submission = submissionRepository.findById(submissionId).orElse(null);

        if (submission != null && "PENDING".equals(submission.getPaymentStatus())) {
            submission.setPaymentStatus("COMPLETED");
            submission.setStripeSessionId(sessionId);
            submissionRepository.save(submission);

            Form form = submission.getForm();
            form.incrementSubmissionCount();
            formRepository.save(form);
            logger.info("Form submission {} marked as COMPLETED via Stripe session {}", submissionId, sessionId);

            sendSubmissionConfirmationEmail(submission, form, true);

            // Send admin notification for paid form Stripe payments
            JsonNode data = submission.getSubmissionData();
            if ("ABEL_DAMINA_MENTORING_ACADEMY".equals(form.getEventCode())) {
                emailService.sendStripePaymentNotification(
                        extractFieldFromData(data, "firstName"),
                        extractFieldFromData(data, "lastName"),
                        extractFieldFromData(data, "email", "Email", "emailAddress"),
                        extractFieldFromData(data, "phone"),
                        extractFieldFromData(data, "paymentPlanLabel", "paymentPlan"),
                        submission.getPaymentAmount(),
                        submission.getPaymentCurrency()
                );
            } else if ("POWER_BIBLE_SCHOOL".equals(form.getEventCode())) {
                emailService.sendPBSStripePaymentNotification(
                        extractFieldFromData(data, "firstName"),
                        extractFieldFromData(data, "lastName"),
                        extractFieldFromData(data, "email", "Email", "emailAddress"),
                        extractFieldFromData(data, "phone"),
                        extractFieldFromData(data, "attendanceType", "attendanceLabel"),
                        submission.getPaymentAmount(),
                        submission.getPaymentCurrency()
                );
            }
        }
    }

    private void sendSubmissionConfirmationEmail(FormSubmission submission, Form form, boolean paymentMade) {
        String recipientEmail = null;
        String firstName = "Friend";

        if (submission.getUser() != null) {
            recipientEmail = submission.getUser().getEmail();
            firstName = submission.getUser().getFirstName();
            if (firstName == null || firstName.isBlank()) {
                firstName = submission.getUser().getFullName();
            }
        }

        if (recipientEmail == null || recipientEmail.isBlank()) {
            recipientEmail = extractFieldFromData(submission.getSubmissionData(), "email", "Email", "EMAIL", "emailAddress");
        }
        if ("Friend".equals(firstName)) {
            String extracted = extractFieldFromData(submission.getSubmissionData(), "firstName", "first_name", "First Name", "name", "Name", "fullName");
            if (extracted != null) firstName = extracted;
        }

        if (recipientEmail == null || recipientEmail.isBlank()) {
            logger.warn("No email address found for form submission {}, skipping confirmation email", submission.getId());
            return;
        }

        String contactEmail = "ABEL_DAMINA_MENTORING_ACADEMY".equals(form.getEventCode())
                ? "adoma@drabeldamina.org"
                : "POWER_BIBLE_SCHOOL".equals(form.getEventCode())
                ? "powerbibleschool@drabeldamina.org"
                : "DISCIPLESHIP_PROGRAM".equals(form.getEventCode())
                ? "discipleship@drabeldamina.org"
                : "it@drabeldamina.org";

        emailService.sendFormSubmissionConfirmation(
                recipientEmail,
                firstName,
                form.getTitle(),
                form.getSuccessMessage(),
                paymentMade,
                paymentMade ? submission.getPaymentAmount() : null,
                paymentMade ? submission.getPaymentCurrency() : null,
                contactEmail
        );
    }

    private String extractFieldFromData(JsonNode data, String... fieldNames) {
        if (data == null) return null;
        for (String field : fieldNames) {
            if (data.has(field) && !data.get(field).isNull()) {
                String val = data.get(field).asText("").trim();
                if (!val.isEmpty()) return val;
            }
        }
        return null;
    }

    public byte[] exportSubmissionsToCSV(Long formId) throws IOException {
        logger.info("Exporting submissions to CSV for form ID: {}", formId);

        Form form = formRepository.findByIdAndIsActiveTrue(formId)
                .orElseThrow(() -> new ResourceNotFoundException("Form not found with ID: " + formId));

        List<FormSubmission> submissions = submissionRepository.findByFormAndArchivedFalseOrderBySubmittedAtDesc(form);

        return generateCSV(form, submissions);
    }

    public byte[] exportPayments(Long formId, String paymentMethod, String dateFrom, String dateTo, String format) throws IOException {
        logger.info("Exporting payments for form ID: {} paymentMethod: {} from {} to {} in format: {}", formId, paymentMethod, dateFrom, dateTo, format);

        Form form = formRepository.findByIdAndIsActiveTrue(formId)
                .orElseThrow(() -> new ResourceNotFoundException("Form not found with ID: " + formId));

        List<FormSubmission> submissions = submissionRepository.findByFormAndArchivedFalseOrderBySubmittedAtDesc(form);

        // Filter by date range
        if (dateFrom != null || dateTo != null) {
            LocalDateTime fromDate = dateFrom != null ? LocalDateTime.parse(dateFrom + "T00:00:00") : null;
            LocalDateTime toDate   = dateTo   != null ? LocalDateTime.parse(dateTo   + "T23:59:59") : null;
            submissions = submissions.stream()
                    .filter(s -> {
                        LocalDateTime at = s.getSubmittedAt();
                        boolean after  = fromDate == null || !at.isBefore(fromDate);
                        boolean before = toDate   == null || !at.isAfter(toDate);
                        return after && before;
                    })
                    .collect(Collectors.toList());
        }

        // Filter by payment method
        if ("STRIPE".equalsIgnoreCase(paymentMethod)) {
            submissions = submissions.stream()
                    .filter(s -> s.getStripeSessionId() != null && !s.getStripeSessionId().isBlank())
                    .collect(Collectors.toList());
        } else if ("NGN_TRANSFER".equalsIgnoreCase(paymentMethod)) {
            submissions = submissions.stream()
                    .filter(s -> (s.getStripeSessionId() == null || s.getStripeSessionId().isBlank()))
                    .collect(Collectors.toList());
        }

        if ("excel".equalsIgnoreCase(format)) {
            return generatePaymentExcel(form, submissions);
        } else {
            return generatePaymentCSV(submissions);
        }
    }

    private byte[] generatePaymentCSV(List<FormSubmission> submissions) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(out);
        try {
            writer.println("First Name,Last Name,Email,Phone,Payment Plan,Plan Label,Payment Status,Amount,Currency,Payment Method,Submitted At");
            for (FormSubmission s : submissions) {
                JsonNode d = s.getSubmissionData();
                String firstName   = getField(d, "firstName");
                String lastName    = getField(d, "lastName");
                String email       = getField(d, "email");
                String phone       = getField(d, "phone");
                String plan        = getField(d, "paymentPlan");
                String planLabel   = getField(d, "paymentPlanLabel");
                String status      = s.getPaymentStatus() != null ? s.getPaymentStatus() : "";
                String amount      = s.getPaymentAmount() != null ? s.getPaymentAmount().toPlainString() : "";
                String currency    = s.getPaymentCurrency() != null ? s.getPaymentCurrency() : "";
                String method      = (s.getStripeSessionId() != null && !s.getStripeSessionId().isBlank()) ? "Stripe" : "Bank Transfer";
                String submittedAt = s.getSubmittedAt() != null ? s.getSubmittedAt().toString() : "";

                writer.println(String.join(",",
                        escapeCSV(firstName), escapeCSV(lastName), escapeCSV(email), escapeCSV(phone),
                        escapeCSV(plan), escapeCSV(planLabel), escapeCSV(status),
                        escapeCSV(amount), escapeCSV(currency), escapeCSV(method), escapeCSV(submittedAt)));
            }
            writer.flush();
            return out.toByteArray();
        } finally {
            writer.close();
        }
    }

    private byte[] generatePaymentExcel(Form form, List<FormSubmission> submissions) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Payments");
        try {
            // Header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            String[] headers = {"First Name", "Last Name", "Email", "Phone", "Payment Plan", "Plan Label", "Payment Status", "Amount", "Currency", "Payment Method", "Submitted At"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (FormSubmission s : submissions) {
                JsonNode d = s.getSubmissionData();
                String method = (s.getStripeSessionId() != null && !s.getStripeSessionId().isBlank()) ? "Stripe" : "Bank Transfer";
                String[] values = {
                        getField(d, "firstName"),
                        getField(d, "lastName"),
                        getField(d, "email"),
                        getField(d, "phone"),
                        getField(d, "paymentPlan"),
                        getField(d, "paymentPlanLabel"),
                        s.getPaymentStatus() != null ? s.getPaymentStatus() : "",
                        s.getPaymentAmount() != null ? s.getPaymentAmount().toPlainString() : "",
                        s.getPaymentCurrency() != null ? s.getPaymentCurrency() : "",
                        method,
                        s.getSubmittedAt() != null ? s.getSubmittedAt().toString() : "",
                };
                Row row = sheet.createRow(rowIdx++);
                for (int i = 0; i < values.length; i++) {
                    row.createCell(i).setCellValue(values[i]);
                }
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } finally {
            workbook.close();
        }
    }

    private String getField(JsonNode data, String key) {
        if (data == null) return "";
        JsonNode node = data.get(key);
        return node != null && !node.isNull() ? node.asText() : "";
    }

    public byte[] exportSubmissions(Long formId, String dateFrom, String dateTo, String format) throws IOException {
        logger.info("Exporting submissions for form ID: {} from {} to {} in format: {}", formId, dateFrom, dateTo, format);

        Form form = formRepository.findByIdAndIsActiveTrue(formId)
                .orElseThrow(() -> new ResourceNotFoundException("Form not found with ID: " + formId));

        // Get all non-archived submissions for the form
        List<FormSubmission> submissions = submissionRepository.findByFormAndArchivedFalseOrderBySubmittedAtDesc(form);

        // Filter by date range if provided
        if (dateFrom != null || dateTo != null) {
            LocalDateTime fromDate = dateFrom != null ? LocalDateTime.parse(dateFrom + "T00:00:00") : null;
            LocalDateTime toDate = dateTo != null ? LocalDateTime.parse(dateTo + "T23:59:59") : null;

            submissions = submissions.stream()
                    .filter(submission -> {
                        LocalDateTime submittedAt = submission.getSubmittedAt();
                        boolean afterFrom = fromDate == null || submittedAt.isAfter(fromDate) || submittedAt.isEqual(fromDate);
                        boolean beforeTo = toDate == null || submittedAt.isBefore(toDate) || submittedAt.isEqual(toDate);
                        return afterFrom && beforeTo;
                    })
                    .collect(Collectors.toList());
        }

        // Generate export based on format
        if ("excel".equalsIgnoreCase(format)) {
            return generateExcel(form, submissions);
        } else {
            return generateCSV(form, submissions);
        }
    }

    private JsonNode processFileUploads(JsonNode submissionData, Map<String, List<MultipartFile>> files, Long formId) {
        try {
            ObjectNode modifiedData = submissionData.deepCopy();

            for (Map.Entry<String, List<MultipartFile>> entry : files.entrySet()) {
                String fieldId = entry.getKey();
                // Spring binds non-file request params (e.g. submissionData) into this
                // map as String values, so a raw cast to List<MultipartFile> fails.
                // Skip any entry whose value is not actually a list of files.
                Object rawValue = entry.getValue();
                if (!(rawValue instanceof List) || rawValue instanceof String) {
                    continue;
                }
                List<MultipartFile> fieldFiles = (List<MultipartFile>) rawValue;
                if (fieldFiles == null) {
                    continue;
                }

                List<ObjectNode> uploaded = new ArrayList<>();
                for (MultipartFile file : fieldFiles) {
                    if (file == null || file.isEmpty()) {
                        continue;
                    }
                    String fileName = file.getOriginalFilename();
                    String s3Key = "form-submissions/" + formId + "/" + System.currentTimeMillis() + "_" + fileName;
                    String fileUrl = s3FileService.uploadFile(file, s3Key);

                    ObjectNode fileInfo = objectMapper.createObjectNode();
                    fileInfo.put("fileName", fileName);
                    fileInfo.put("fileUrl", fileUrl);
                    fileInfo.put("fileSize", file.getSize());
                    fileInfo.put("contentType", file.getContentType());
                    uploaded.add(fileInfo);
                }

                if (uploaded.size() == 1) {
                    // Single file: keep the flat object shape (back-compatible with existing viewers)
                    modifiedData.set(fieldId, uploaded.get(0));
                } else if (uploaded.size() > 1) {
                    // Multiple files (e.g. report receipts): store an array of file objects
                    ArrayNode arr = objectMapper.createArrayNode();
                    uploaded.forEach(arr::add);
                    modifiedData.set(fieldId, arr);
                }
            }

            return modifiedData;

        } catch (Exception e) {
            logger.error("Error processing file uploads: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process file uploads: " + e.getMessage());
        }
    }

    private void deleteSubmissionFiles(FormSubmission submission) {
        try {
            JsonNode submissionData = submission.getSubmissionData();

            if (submissionData != null && submissionData.isObject()) {
                Iterator<Map.Entry<String, JsonNode>> fields = submissionData.fields();

                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> field = fields.next();
                    JsonNode value = field.getValue();

                    // Check if this field contains a single file
                    if (value.isObject() && value.has("fileUrl")) {
                        s3FileService.deleteFileByUrl(value.get("fileUrl").asText());
                    } else if (value.isArray()) {
                        // Or a list of files (multi-file field, e.g. receipts)
                        for (JsonNode item : value) {
                            if (item.isObject() && item.has("fileUrl")) {
                                s3FileService.deleteFileByUrl(item.get("fileUrl").asText());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error deleting submission files: {}", e.getMessage(), e);
            // Don't throw exception, continue with submission deletion
        }
    }

    private List<String> buildColumnKeys(List<FormSubmission> submissions) {
        java.util.Set<String> excludedKeys = java.util.Set.of("regionCode", "regionName");
        java.util.List<String> locationSubKeys = java.util.List.of("country", "state", "city", "postalCode", "addressLine1", "addressLine2");
        boolean[] hasNestedAddress = {false};
        java.util.LinkedHashSet<String> keySet = new java.util.LinkedHashSet<>();
        for (FormSubmission submission : submissions) {
            JsonNode data = submission.getSubmissionData();
            if (data != null && data.isObject()) {
                data.fieldNames().forEachRemaining(k -> {
                    if (excludedKeys.contains(k)) return;
                    if ("location".equals(k) || "address".equals(k)) { hasNestedAddress[0] = true; }
                    else { keySet.add(k); }
                });
            }
        }
        // Expand nested location/address object into individual columns
        if (hasNestedAddress[0]) {
            locationSubKeys.forEach(keySet::add); // no-op if already present as flat field
        }
        return new java.util.ArrayList<>(keySet);
    }

    /** Resolves a cell value for a given key. fieldDef (when supplied) drives repeater column order. */
    private String resolveSubmissionValue(JsonNode data, String key, JsonNode fieldDef) {
        if (data == null) return "";
        JsonNode value = data.get(key);
        if (value != null && !value.isNull()) {
            if (value.isObject() && value.has("fileUrl")) {
                return value.has("fileName") ? value.get("fileName").asText() : value.get("fileUrl").asText();
            }
            if (value.isArray()) {
                // Repeater (e.g. expenses): export the total of its numeric column(s)
                if (fieldDef != null && "repeater".equals(fieldDef.path("type").asText()) && fieldDef.has("subFields")) {
                    java.util.List<String> numberLabels = new java.util.ArrayList<>();
                    for (JsonNode sub : fieldDef.get("subFields")) {
                        if ("number".equals(sub.path("type").asText())) {
                            numberLabels.add(sub.path("label").asText());
                        }
                    }
                    double total = 0;
                    for (JsonNode row : value) {
                        for (String lbl : numberLabels) {
                            JsonNode cell = row.get(lbl);
                            if (cell != null && !cell.isNull()) {
                                try { total += Double.parseDouble(cell.asText()); } catch (NumberFormatException ignored) { }
                            }
                        }
                    }
                    return (total == Math.floor(total)) ? String.valueOf((long) total) : String.format("%.2f", total);
                }
                // Generic arrays: files (by name), checkboxes, or unknown objects
                StringBuilder sb = new StringBuilder();
                for (JsonNode item : value) {
                    if (sb.length() > 0) sb.append("; ");
                    if (item.isObject()) {
                        if (item.has("fileUrl")) {
                            sb.append(item.has("fileName") ? item.get("fileName").asText() : item.get("fileUrl").asText());
                        } else {
                            StringBuilder rowSb = new StringBuilder();
                            item.fields().forEachRemaining(e -> {
                                if (rowSb.length() > 0) rowSb.append(", ");
                                rowSb.append(e.getKey()).append(": ").append(e.getValue().asText());
                            });
                            sb.append(rowSb);
                        }
                    } else {
                        sb.append(item.asText());
                    }
                }
                return sb.toString();
            }
            return value.asText();
        }
        // Fall back to nested location/address object (legacy submissions)
        JsonNode locationObj = data.get("location");
        if (locationObj == null || !locationObj.isObject()) locationObj = data.get("address");
        if (locationObj != null && locationObj.isObject()) {
            if ("country".equals(key)) {
                JsonNode code = locationObj.get("countryCode");
                if (code != null && !code.isNull() && !code.asText().isBlank()) {
                    String displayName = new java.util.Locale("", code.asText()).getDisplayCountry(java.util.Locale.ENGLISH);
                    return displayName.isBlank() ? code.asText() : displayName;
                }
            } else {
                String subKey = "state".equals(key) ? "stateCode" : key;
                JsonNode subValue = locationObj.get(subKey);
                if (subValue != null && !subValue.isNull()) return subValue.asText();
            }
        }
        return "";
    }

    private String resolveAction(FormSubmission s) {
        String status = s.getPaymentStatus();
        if (status == null) return "";
        switch (status) {
            case "COMPLETED":        return "PAID via Stripe";
            case "PENDING_TRANSFER": return "Bank Transfer - check FCMB";
            default:                 return "";
        }
    }

    private byte[] generateCSV(Form form, List<FormSubmission> submissions) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(outputStream);

        try {
            JsonNode formSchema = form.getFormSchema();
            ArrayNode fields = formSchema != null ? (ArrayNode) formSchema.get("fields") : null;

            List<String> columnKeys = new java.util.ArrayList<>();
            if (fields != null && fields.size() > 0) {
                for (JsonNode field : fields) {
                    columnKeys.add(field.get("label").asText());
                }
            } else {
                columnKeys = buildColumnKeys(submissions);
            }

            java.util.Map<String, JsonNode> fieldByLabel = new java.util.HashMap<>();
            if (fields != null) {
                for (JsonNode field : fields) fieldByLabel.put(field.path("label").asText(), field);
            }

            // Payment columns only apply to forms that collect payment
            boolean includePayment = Boolean.TRUE.equals(form.getRequiresPayment());

            // Write header row — form fields (+ payment columns when relevant)
            StringBuilder header = new StringBuilder();
            for (int i = 0; i < columnKeys.size(); i++) {
                if (i > 0) header.append(",");
                header.append(escapeCSV(columnKeys.get(i)));
            }
            if (includePayment) {
                header.append(",Payment Amount,Payment Currency,Payment Status,Action");
            }
            writer.println(header.toString());

            // Write data rows
            for (FormSubmission submission : submissions) {
                StringBuilder row = new StringBuilder();
                JsonNode submissionData = submission.getSubmissionData();
                for (int i = 0; i < columnKeys.size(); i++) {
                    if (i > 0) row.append(",");
                    String colKey = columnKeys.get(i);
                    row.append(escapeCSV(resolveSubmissionValue(submissionData, colKey, fieldByLabel.get(colKey))));
                }
                if (includePayment) {
                    row.append(",").append(escapeCSV(submission.getPaymentAmount() != null ? submission.getPaymentAmount().toPlainString() : ""));
                    row.append(",").append(escapeCSV(submission.getPaymentCurrency() != null ? submission.getPaymentCurrency() : ""));
                    row.append(",").append(escapeCSV(submission.getPaymentStatus() != null ? submission.getPaymentStatus() : ""));
                    row.append(",").append(escapeCSV(resolveAction(submission)));
                }
                writer.println(row.toString());
            }

            writer.flush();
            return outputStream.toByteArray();

        } finally {
            writer.close();
        }
    }

    private byte[] generateExcel(Form form, List<FormSubmission> submissions) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Form Submissions");

        try {
            JsonNode formSchema = form.getFormSchema();
            ArrayNode fields = formSchema != null ? (ArrayNode) formSchema.get("fields") : null;

            List<String> columnKeys = new java.util.ArrayList<>();
            if (fields != null && fields.size() > 0) {
                for (JsonNode field : fields) {
                    columnKeys.add(field.get("label").asText());
                }
            } else {
                columnKeys = buildColumnKeys(submissions);
            }

            java.util.Map<String, JsonNode> fieldByLabel = new java.util.HashMap<>();
            if (fields != null) {
                for (JsonNode field : fields) fieldByLabel.put(field.path("label").asText(), field);
            }

            // Create header row with styling
            Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            // Payment columns only apply to forms that collect payment
            boolean includePayment = Boolean.TRUE.equals(form.getRequiresPayment());

            int colIndex = 0;
            for (String key : columnKeys) {
                Cell cell = headerRow.createCell(colIndex++);
                cell.setCellValue(key);
                cell.setCellStyle(headerStyle);
            }
            if (includePayment) {
                for (String ph : new String[]{"Payment Amount", "Payment Currency", "Payment Status", "Action"}) {
                    Cell cell = headerRow.createCell(colIndex++);
                    cell.setCellValue(ph);
                    cell.setCellStyle(headerStyle);
                }
            }

            // Create data rows
            int rowIndex = 1;
            for (FormSubmission submission : submissions) {
                Row row = sheet.createRow(rowIndex++);
                int cellIdx = 0;
                JsonNode submissionData = submission.getSubmissionData();
                for (String key : columnKeys) {
                    row.createCell(cellIdx++).setCellValue(resolveSubmissionValue(submissionData, key, fieldByLabel.get(key)));
                }
                if (includePayment) {
                    row.createCell(cellIdx++).setCellValue(submission.getPaymentAmount() != null ? submission.getPaymentAmount().toPlainString() : "");
                    row.createCell(cellIdx++).setCellValue(submission.getPaymentCurrency() != null ? submission.getPaymentCurrency() : "");
                    row.createCell(cellIdx++).setCellValue(submission.getPaymentStatus() != null ? submission.getPaymentStatus() : "");
                    row.createCell(cellIdx).setCellValue(resolveAction(submission));
                }
            }

            // Auto-size columns
            for (int i = 0; i < colIndex; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();

        } finally {
            workbook.close();
        }
    }

    private String escapeCSV(String value) {
        if (value == null) {
            return "";
        }

        // Escape double quotes and wrap in quotes if contains comma, quote, or newline
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }

        return value;
    }

    private FormSubmissionResponse convertToSubmissionResponse(FormSubmission submission) {
        // Extract user info if present
        Long userId = null;
        String userEmail = null;
        String userName = null;
        if (submission.getUser() != null) {
            userId = submission.getUser().getId();
            userEmail = submission.getUser().getEmail();
            userName = submission.getUser().getFullName();
        }

        FormSubmissionResponse response = new FormSubmissionResponse(
                submission.getId(),
                submission.getForm().getId(),
                submission.getForm().getTitle(),
                userId,
                userEmail,
                userName,
                submission.getSubmissionData(),
                submission.getSubmittedAt(),
                submission.getCreatedAt(),
                submission.getUpdatedAt()
        );
        response.setPaymentStatus(submission.getPaymentStatus());
        response.setPaymentAmount(submission.getPaymentAmount());
        response.setPaymentCurrency(submission.getPaymentCurrency());
        return response;
    }
}
