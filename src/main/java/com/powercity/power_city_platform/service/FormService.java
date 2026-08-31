package com.powercity.power_city_platform.service;

import com.powercity.power_city_platform.dto.request.form.CreateFormRequest;
import com.powercity.power_city_platform.dto.request.form.UpdateFormRequest;
import com.powercity.power_city_platform.dto.response.form.FormAnalyticsResponse;
import com.powercity.power_city_platform.dto.response.form.FormResponse;
import com.powercity.power_city_platform.entity.Form;
import com.powercity.power_city_platform.entity.User;
import com.powercity.power_city_platform.exception.ResourceNotFoundException;
import com.powercity.power_city_platform.repository.FormRepository;
import com.powercity.power_city_platform.repository.FormSubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class FormService {

    private static final Logger logger = LoggerFactory.getLogger(FormService.class);

    private final FormRepository formRepository;

    private final FormSubmissionRepository submissionRepository;

    public FormResponse createForm(CreateFormRequest request, User currentUser) {
        logger.info("Creating new form: {}", request.getTitle());

        // Check if form with same title already exists
        if (formRepository.existsByTitleAndIsActiveTrue(request.getTitle())) {
            throw new RuntimeException("Form with title '" + request.getTitle() + "' already exists");
        }

        // Validate: formCode and eventCode are mutually exclusive
        if (request.getFormCode() != null && !request.getFormCode().trim().isEmpty() &&
            request.getEventCode() != null && !request.getEventCode().trim().isEmpty()) {
            throw new RuntimeException("Form cannot have both formCode and eventCode. Use formCode for page-based forms (e.g., PARTNERSHIP) or eventCode for program/event forms.");
        }

        // Create form entity
        Form form = new Form(
                request.getTitle(),
                request.getDescription(),
                request.getFormSchema(),
                request.getRequireAuthentication(),
                request.getIsPublished(),
                true, // isActive
                0, // submissionCount
                request.getSuccessMessage()
        );

        // Set new fields
        form.setFormCode(request.getFormCode());
        form.setEventCode(request.getEventCode());

        if (request.getRequiresPayment() != null) {
            form.setRequiresPayment(request.getRequiresPayment());
        }
        form.setPaymentAmount(request.getPaymentAmount());
        form.setPaymentCurrency(request.getPaymentCurrency());
        form.setPaymentGateway(request.getPaymentGateway());
        form.setPaymentDescription(request.getPaymentDescription());

        form.setCreatedBy(currentUser.getId());
        form.setUpdatedBy(currentUser.getId());

        Form savedForm = formRepository.save(form);
        logger.info("Form created successfully with ID: {}", savedForm.getId());

        return convertToFormResponse(savedForm);
    }

    public FormResponse updateForm(Long formId, UpdateFormRequest request, User currentUser) {
        logger.info("Updating form ID: {}", formId);

        Form form = formRepository.findByIdAndIsActiveTrue(formId)
                .orElseThrow(() -> new ResourceNotFoundException("Form not found with ID: " + formId));

        // Update fields (null-safe)
        if (request.getTitle() != null) {
            // Check if new title conflicts with existing form
            if (!form.getTitle().equals(request.getTitle()) &&
                formRepository.existsByTitleAndIsActiveTrue(request.getTitle())) {
                throw new RuntimeException("Form with title '" + request.getTitle() + "' already exists");
            }
            form.setTitle(request.getTitle());
        }

        if (request.getDescription() != null) {
            form.setDescription(request.getDescription());
        }

        if (request.getFormSchema() != null) {
            form.setFormSchema(request.getFormSchema());
        }

        if (request.getRequireAuthentication() != null) {
            form.setRequireAuthentication(request.getRequireAuthentication());
        }

        if (request.getIsPublished() != null) {
            form.setIsPublished(request.getIsPublished());
        }

        if (request.getSuccessMessage() != null) {
            form.setSuccessMessage(request.getSuccessMessage());
        }

        if (request.getFormCode() != null) {
            form.setFormCode(request.getFormCode());
        }

        if (request.getEventCode() != null) {
            form.setEventCode(request.getEventCode());
        }

        if (request.getRequiresPayment() != null) {
            form.setRequiresPayment(request.getRequiresPayment());
        }
        if (request.getPaymentAmount() != null) {
            form.setPaymentAmount(request.getPaymentAmount());
        }
        if (request.getPaymentCurrency() != null) {
            form.setPaymentCurrency(request.getPaymentCurrency());
        }
        if (request.getPaymentGateway() != null) {
            form.setPaymentGateway(request.getPaymentGateway());
        }
        if (request.getPaymentDescription() != null) {
            form.setPaymentDescription(request.getPaymentDescription());
        }

        // Validate: formCode and eventCode are mutually exclusive
        if (form.getFormCode() != null && !form.getFormCode().trim().isEmpty() &&
            form.getEventCode() != null && !form.getEventCode().trim().isEmpty()) {
            throw new RuntimeException("Form cannot have both formCode and eventCode. Use formCode for page-based forms (e.g., PARTNERSHIP) or eventCode for program/event forms.");
        }

        form.setUpdatedBy(currentUser.getId());

        Form updatedForm = formRepository.save(form);
        logger.info("Form updated successfully: {}", formId);

        return convertToFormResponse(updatedForm);
    }

    public void deleteForm(Long formId, User currentUser) {
        logger.info("Deleting form ID: {}", formId);

        Form form = formRepository.findByIdAndIsActiveTrue(formId)
                .orElseThrow(() -> new ResourceNotFoundException("Form not found with ID: " + formId));

        form.setIsActive(false);
        form.setUpdatedBy(currentUser.getId());

        formRepository.save(form);
        logger.info("Form soft deleted successfully: {}", formId);
    }

    public FormResponse publishForm(Long formId, boolean publish, User currentUser) {
        logger.info("Setting form ID {} publish status to: {}", formId, publish);

        Form form = formRepository.findByIdAndIsActiveTrue(formId)
                .orElseThrow(() -> new ResourceNotFoundException("Form not found with ID: " + formId));

        form.setIsPublished(publish);
        form.setUpdatedBy(currentUser.getId());

        Form updatedForm = formRepository.save(form);
        logger.info("Form publish status updated: {}", formId);

        return convertToFormResponse(updatedForm);
    }

    @Transactional(readOnly = true)
    public Page<FormResponse> getAllForms(Pageable pageable) {
        logger.info("Fetching all forms with pagination");

        Page<Form> forms = formRepository.findByIsActiveTrue(pageable);

        return forms.map(this::convertToFormResponse);
    }

    @Transactional(readOnly = true)
    public Page<FormResponse> searchForms(String query, Pageable pageable) {
        logger.info("Searching forms with query: {}", query);

        Page<Form> forms = formRepository.findByTitleContainingIgnoreCaseAndIsActiveTrue(query, pageable);

        return forms.map(this::convertToFormResponse);
    }

    @Transactional(readOnly = true)
    public FormResponse getFormById(Long formId) {
        logger.info("Fetching form by ID: {}", formId);

        Form form = formRepository.findByIdAndIsActiveTrue(formId)
                .orElseThrow(() -> new ResourceNotFoundException("Form not found with ID: " + formId));

        return convertToFormResponse(form);
    }

    @Transactional(readOnly = true)
    public List<FormResponse> getPublishedForms() {
        logger.info("Fetching all published forms");

        List<Form> forms = formRepository.findByIsPublishedTrueAndIsActiveTrue();

        return forms.stream()
                .map(this::convertToFormResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<FormResponse> getPublishedFormsPaginated(Pageable pageable) {
        logger.info("Fetching published forms with pagination");

        Page<Form> forms = formRepository.findByIsPublishedTrueAndIsActiveTrue(pageable);

        return forms.map(this::convertToFormResponse);
    }

    @Transactional(readOnly = true)
    public FormResponse getFormForSubmission(Long formId, User currentUser) {
        logger.info("Fetching form for submission: {}", formId);

        Form form = formRepository.findById(formId)
                .orElseThrow(() -> new ResourceNotFoundException("Form not found with ID: " + formId));

        // Check if form is published
        if (!form.getIsPublished()) {
            throw new RuntimeException("This form is not currently accepting submissions");
        }

        // Check if form is active
        if (!form.getIsActive()) {
            throw new RuntimeException("This form is no longer available");
        }

        // Check if authentication is required
        if (form.getRequireAuthentication() && currentUser == null) {
            throw new RuntimeException("Authentication required to access this form");
        }

        // Check if user can submit (if multiple submissions not allowed)
        if (currentUser != null && !form.canUserSubmit(currentUser.getId())) {
            throw new RuntimeException("You have already submitted this form");
        }

        return convertToFormResponse(form);
    }

    @Transactional(readOnly = true)
    public FormAnalyticsResponse getFormAnalytics(Long formId) {
        logger.info("Fetching analytics for form: {}", formId);

        Form form = formRepository.findByIdAndIsActiveTrue(formId)
                .orElseThrow(() -> new ResourceNotFoundException("Form not found with ID: " + formId));

        // Calculate analytics
        Long totalSubmissions = submissionRepository.countByForm(form);

        // Submissions today
        LocalDateTime startOfToday = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime endOfToday = LocalDateTime.now();
        Long submissionsToday = submissionRepository.countByFormAndDateRange(form, startOfToday, endOfToday);

        // Submissions this week
        LocalDateTime startOfWeek = LocalDateTime.now().minusWeeks(1);
        Long submissionsThisWeek = submissionRepository.countByFormAndDateRange(form, startOfWeek, endOfToday);

        // Submissions this month
        LocalDateTime startOfMonth = LocalDateTime.now().minusMonths(1);
        Long submissionsThisMonth = submissionRepository.countByFormAndDateRange(form, startOfMonth, endOfToday);

        // Additional metrics
        Map<String, Object> additionalMetrics = new HashMap<>();
        additionalMetrics.put("requiresAuthentication", form.getRequireAuthentication());
        additionalMetrics.put("isPublished", form.getIsPublished());
        additionalMetrics.put("createdAt", form.getCreatedAt());

        return new FormAnalyticsResponse(
                form.getId(),
                form.getTitle(),
                totalSubmissions,
                submissionsToday,
                submissionsThisWeek,
                submissionsThisMonth,
                additionalMetrics
        );
    }

    @Transactional(readOnly = true)
    public List<FormResponse> getTopFormsBySubmissions(int limit) {
        logger.info("Fetching top {} forms by submissions", limit);

        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "submissionCount"));
        List<Form> forms = formRepository.findTopFormsBySubmissions(pageable);

        return forms.stream()
                .map(this::convertToFormResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getGlobalAnalytics() {
        logger.info("Fetching global form analytics");

        Map<String, Object> analytics = new HashMap<>();

        analytics.put("totalForms", formRepository.countActiveForms());
        analytics.put("publishedForms", formRepository.countPublishedForms());
        analytics.put("totalSubmissions", formRepository.countTotalSubmissions());

        return analytics;
    }

    @Transactional(readOnly = true)
    public FormResponse getFormByFormCode(String formCode) {
        logger.info("Fetching form by form code: {}", formCode);

        Form form = formRepository.findByFormCodeAndIsActiveTrue(formCode)
                .orElseThrow(() -> new ResourceNotFoundException("Form not found with code: " + formCode));

        // Check if form is published
        if (!form.getIsPublished()) {
            throw new RuntimeException("This form is not currently accepting submissions");
        }

        return convertToFormResponse(form);
    }

    @Transactional(readOnly = true)
    public FormResponse getFormByEventCode(String eventCode) {
        logger.info("Fetching form by event code: {}", eventCode);

        Form form = formRepository.findByEventCodeAndIsActiveTrue(eventCode)
                .orElseThrow(() -> new ResourceNotFoundException("Form not found with event code: " + eventCode));

        // Check if form is published
        if (!form.getIsPublished()) {
            throw new RuntimeException("This form is not currently accepting submissions");
        }

        return convertToFormResponse(form);
    }

    private FormResponse convertToFormResponse(Form form) {
        FormResponse response = new FormResponse(
                form.getId(),
                form.getTitle(),
                form.getDescription(),
                form.getFormSchema(),
                form.getRequireAuthentication(),
                form.getIsPublished(),
                form.getIsActive(),
                form.getSubmissionCount(),
                form.getSuccessMessage(),
                form.getCreatedAt(),
                form.getUpdatedAt(),
                form.getCreatedBy(),
                form.getUpdatedBy()
        );

        // Set new fields
        response.setFormCode(form.getFormCode());
        response.setEventCode(form.getEventCode());
        response.setRequiresPayment(form.getRequiresPayment());
        response.setPaymentAmount(form.getPaymentAmount());
        response.setPaymentCurrency(form.getPaymentCurrency());
        response.setPaymentGateway(form.getPaymentGateway());
        response.setPaymentDescription(form.getPaymentDescription());

        return response;
    }
}
