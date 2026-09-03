package com.powercity.power_city_platform.service;


import com.powercity.power_city_platform.dto.request.email.SendEmailRequest;
import com.powercity.power_city_platform.entity.EmailLog;
import com.powercity.power_city_platform.entity.User;
import com.powercity.power_city_platform.repository.EmailLogRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private TemplateEngine templateEngine;

    @Autowired
    private EmailLogRepository emailLogRepository;

    @Value("${app.email.from}")
    private String fromEmail;

    @Value("${app.email.base-url}")
    private String baseUrl;

    @Value("${app.email.frontend-url:http://localhost:8080}")
    private String frontendUrl;

    public boolean sendEmail(SendEmailRequest request) {
        String messageId = UUID.randomUUID().toString();

        try {
            MimeMessage message = createMimeMessage(request, messageId);
            mailSender.send(message);

            // Log successful email
            logEmail(messageId, request.to().get(0), request.subject(),
                    request.templateName(), "SENT", null);

            System.out.println("Email sent successfully to: " + request.to().get(0) + " (Message ID: " + messageId + ")");
            return true;

        } catch (Exception e) {
            // Log failed email
            logEmail(messageId, request.to().get(0), request.subject(),
                    request.templateName(), "FAILED", e.getMessage());

            System.err.println("Failed to send email to: " + request.to().get(0) + " - " + e.getMessage());
            return false;
        }
    }

    @Async("emailTaskExecutor")
    public void sendWelcomeEmail(User user, String verificationToken) {
        try {
            Thread.sleep(10000);  // Medium delay for welcome emails
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String verificationLink = frontendUrl + "/verify-email?token=" + verificationToken;

        Map<String, Object> templateData = Map.of(
                "firstName", user.getFirstName(),
                "lastName", user.getLastName(),
                "email", user.getEmail(),
                "region", user.getRegion() != null ? user.getRegion() : "Not specified",
                "verificationLink", verificationLink
        );

        SendEmailRequest request = new SendEmailRequest(
                List.of(user.getEmail()),
                "Welcome to Power City International!",
                "welcome-email",
                templateData,
                null,
                null
        );

        sendEmail(request);
    }

    @Async("emailTaskExecutor")
    public void sendEmailVerificationEmail(User user, String verificationToken) {
        try {
            Thread.sleep(10000);  // Medium delay for verification emails
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String verificationLink = frontendUrl + "/verify-email?token=" + verificationToken;

        Map<String, Object> templateData = Map.of(
                "firstName", user.getFirstName(),
                "email", user.getEmail(),
                "region", user.getRegion() != null ? user.getRegion() : "Not specified",
                "verificationLink", verificationLink
        );

        SendEmailRequest request = new SendEmailRequest(
                List.of(user.getEmail()),
                "Verify Your Email - Power City International",
                "email-verification",
                templateData,
                null,
                null
        );

        sendEmail(request);
    }

    private MimeMessage createMimeMessage(SendEmailRequest request, String messageId) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        // Set basic email properties
        helper.setFrom(fromEmail);
        helper.setTo(request.to().toArray(new String[0]));
        helper.setSubject(request.subject());

        // Add CC and BCC if provided
        if (request.cc() != null && !request.cc().isEmpty()) {
            helper.setCc(request.cc().toArray(new String[0]));
        }
        if (request.bcc() != null && !request.bcc().isEmpty()) {
            helper.setBcc(request.bcc().toArray(new String[0]));
        }

        // Process template and set HTML content
        String htmlContent = processTemplate(request.templateName(), request.templateData());
        helper.setText(htmlContent, true);

        // Add embedded images
        addEmbeddedImages(helper);

        // Set custom headers
        message.setHeader("Message-ID", messageId);
        message.setHeader("X-Mailer", "Power City Platform");

        return message;
    }

    private String processTemplate(String templateName, Map<String, Object> templateData) {
        Context context = new Context();

        // Add template data
        if (templateData != null) {
            templateData.forEach(context::setVariable);
        }

        // Add common variables
        context.setVariable("baseUrl", baseUrl);
        context.setVariable("frontendUrl", frontendUrl);
        context.setVariable("currentYear", LocalDateTime.now().getYear());

        return templateEngine.process("email/" + templateName, context);
    }

    private void addEmbeddedImages(MimeMessageHelper helper) throws MessagingException {
        // Add logo
        ClassPathResource logoResource = new ClassPathResource("static/email-assets/logo.png");
        if (logoResource.exists()) {
            helper.addInline("logo", logoResource);
        }
    }

    private void logEmail(String messageId, String recipient, String subject,
                          String template, String status, String errorMessage) {
        try {
            EmailLog emailLog = new EmailLog();
            emailLog.setMessageId(messageId);
            emailLog.setRecipient(recipient);
            emailLog.setSubject(subject);
            emailLog.setTemplate(template);
            emailLog.setStatus(status);
            emailLog.setErrorMessage(errorMessage);
            emailLog.setSentAt(LocalDateTime.now());

            emailLogRepository.save(emailLog);
        } catch (Exception e) {
            // Log the error but don't fail the email sending process
            System.err.println("Failed to log email: " + e.getMessage());
        }
    }


    @Async("emailTaskExecutor")
    public void sendFormSubmissionConfirmation(
            String recipientEmail,
            String firstName,
            String formTitle,
            String successMessage,
            boolean requiresPayment,
            BigDecimal paymentAmount,
            String paymentCurrency,
            String contactEmail) {

        Map<String, Object> templateData = new HashMap<>();
        templateData.put("firstName", firstName != null ? firstName : "Friend");
        templateData.put("formTitle", formTitle);
        templateData.put("successMessage", successMessage);
        templateData.put("requiresPayment", requiresPayment);
        templateData.put("email", recipientEmail);
        templateData.put("contactEmail", contactEmail != null ? contactEmail : "it@drabeldamina.org");

        if (requiresPayment && paymentAmount != null) {
            templateData.put("paymentAmount", paymentAmount.toPlainString());
            templateData.put("paymentCurrency", paymentCurrency != null ? paymentCurrency : "USD");
            templateData.put("currencySymbol", resolveCurrencySymbol(paymentCurrency));
        }

        String subject = requiresPayment
                ? "Payment Confirmed - " + formTitle
                : "Registration Confirmed - " + formTitle;

        SendEmailRequest request = new SendEmailRequest(
                List.of(recipientEmail),
                subject,
                "form-submission-confirmation",
                templateData,
                null,
                null
        );

        sendEmail(request);
    }

    @Async("emailTaskExecutor")
    public void sendContactFormEmail(String name, String email, String subject, String message) {
        String resolvedSubject = (subject != null && !subject.isBlank()) ? subject : "Website Enquiry";

        Map<String, Object> templateData = new HashMap<>();
        templateData.put("name", name);
        templateData.put("senderEmail", email);
        templateData.put("subject", resolvedSubject);
        templateData.put("message", message);

        SendEmailRequest request = new SendEmailRequest(
                List.of("it@drabeldamina.org"),
                resolvedSubject + " — from " + name,
                "contact-form",
                templateData,
                null,
                null
        );

        sendEmail(request);
    }

    private String resolveCurrencySymbol(String currency) {
        if (currency == null) return "$";
        return switch (currency.toUpperCase()) {
            case "GBP" -> "£";
            case "NGN" -> "₦";
            case "GHS" -> "₵";
            case "ZAR" -> "R";
            default -> "$";
        };
    }

    @Async("emailTaskExecutor")
    public void sendPBSStripePaymentNotification(
            String firstName,
            String lastName,
            String registrantEmail,
            String phone,
            String attendanceLabel,
            BigDecimal amount,
            String currency) {

        Map<String, Object> templateData = new HashMap<>();
        templateData.put("firstName", firstName != null ? firstName : "");
        templateData.put("lastName", lastName != null ? lastName : "");
        templateData.put("registrantEmail", registrantEmail);
        templateData.put("phone", phone != null ? phone : "Not provided");
        templateData.put("planLabel", attendanceLabel != null ? attendanceLabel : "N/A");
        templateData.put("amount", amount != null ? amount : BigDecimal.ZERO);
        templateData.put("currencySymbol", resolveCurrencySymbol(currency));
        templateData.put("currency", currency != null ? currency : "USD");
        templateData.put("currentYear", java.time.Year.now().getValue());

        String displayName = ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();

        SendEmailRequest request = new SendEmailRequest(
                List.of("powerbibleschool@drabeldamina.org"),
                "New PBS Stripe Payment Confirmed: " + displayName,
                "pbs-stripe-payment-notification",
                templateData,
                null,
                null
        );

        sendEmail(request);
    }

    @Async("emailTaskExecutor")
    public void sendStripePaymentNotification(
            String firstName,
            String lastName,
            String registrantEmail,
            String phone,
            String planLabel,
            BigDecimal amount,
            String currency) {

        Map<String, Object> templateData = new HashMap<>();
        templateData.put("firstName", firstName != null ? firstName : "");
        templateData.put("lastName", lastName != null ? lastName : "");
        templateData.put("registrantEmail", registrantEmail);
        templateData.put("phone", phone != null ? phone : "Not provided");
        templateData.put("planLabel", planLabel != null ? planLabel : "N/A");
        templateData.put("amount", amount != null ? amount : BigDecimal.ZERO);
        templateData.put("currencySymbol", resolveCurrencySymbol(currency));
        templateData.put("currency", currency != null ? currency : "USD");
        templateData.put("currentYear", java.time.Year.now().getValue());

        String displayName = ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();

        SendEmailRequest request = new SendEmailRequest(
                List.of("adoma@drabeldamina.org"),
                "New ADOMA Stripe Payment Confirmed: " + displayName,
                "adoma-stripe-payment-notification",
                templateData,
                null,
                null
        );

        sendEmail(request);
    }

    @Async("emailTaskExecutor")
    public void sendBankTransferNotification(
            String firstName,
            String lastName,
            String registrantEmail,
            String phone,
            String planLabel,
            BigDecimal amount,
            String currency) {

        Map<String, Object> templateData = new HashMap<>();
        templateData.put("firstName", firstName != null ? firstName : "");
        templateData.put("lastName", lastName != null ? lastName : "");
        templateData.put("registrantEmail", registrantEmail);
        templateData.put("phone", phone != null ? phone : "Not provided");
        templateData.put("planLabel", planLabel != null ? planLabel : "N/A");
        templateData.put("amount", amount != null ? amount : BigDecimal.ZERO);
        templateData.put("currencySymbol", resolveCurrencySymbol(currency));
        templateData.put("currency", currency != null ? currency : "NGN");
        templateData.put("currentYear", java.time.Year.now().getValue());

        String displayName = ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();

        SendEmailRequest request = new SendEmailRequest(
                List.of("adoma@drabeldamina.org"),
                "New ADOMA Registration — Bank Transfer Pending: " + displayName,
                "adoma-bank-transfer-notification",
                templateData,
                null,
                null
        );

        sendEmail(request);
    }

    @Async("emailTaskExecutor")
    public void sendBankTransferUserConfirmation(
            String recipientEmail,
            String firstName,
            String planLabel,
            BigDecimal amount,
            String currency) {

        Map<String, Object> templateData = new HashMap<>();
        templateData.put("firstName", firstName != null ? firstName : "Friend");
        templateData.put("planLabel", planLabel != null ? planLabel : "Registration Fee");
        templateData.put("amount", amount != null ? amount : BigDecimal.ZERO);
        templateData.put("currencySymbol", resolveCurrencySymbol(currency));
        templateData.put("currency", currency != null ? currency : "NGN");
        templateData.put("email", recipientEmail);
        templateData.put("currentYear", java.time.Year.now().getValue());

        SendEmailRequest request = new SendEmailRequest(
                List.of(recipientEmail),
                "ADOMA Registration Received — Complete Your Payment",
                "adoma-bank-transfer-confirmation",
                templateData,
                null,
                null
        );

        sendEmail(request);
    }

    @Async("emailTaskExecutor")
    public void sendPBSBankTransferNotification(
            String firstName,
            String lastName,
            String registrantEmail,
            String phone,
            String attendanceLabel,
            BigDecimal amount,
            String currency) {

        Map<String, Object> templateData = new HashMap<>();
        templateData.put("firstName", firstName != null ? firstName : "");
        templateData.put("lastName", lastName != null ? lastName : "");
        templateData.put("registrantEmail", registrantEmail);
        templateData.put("phone", phone != null ? phone : "Not provided");
        templateData.put("planLabel", attendanceLabel != null ? attendanceLabel : "N/A");
        templateData.put("amount", amount != null ? amount : BigDecimal.ZERO);
        templateData.put("currencySymbol", resolveCurrencySymbol(currency));
        templateData.put("currency", currency != null ? currency : "NGN");
        templateData.put("currentYear", java.time.Year.now().getValue());

        String displayName = ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();

        SendEmailRequest request = new SendEmailRequest(
                List.of("powerbibleschool@drabeldamina.org"),
                "New PBS Registration — Bank Transfer Pending: " + displayName,
                "pbs-bank-transfer-notification",
                templateData,
                null,
                null
        );

        sendEmail(request);
    }

    @Async("emailTaskExecutor")
    public void sendPBSBankTransferUserConfirmation(
            String recipientEmail,
            String firstName,
            String attendanceLabel,
            BigDecimal amount,
            String currency) {

        Map<String, Object> templateData = new HashMap<>();
        templateData.put("firstName", firstName != null ? firstName : "Friend");
        templateData.put("planLabel", attendanceLabel != null ? attendanceLabel : "Registration Fee");
        templateData.put("amount", amount != null ? amount : BigDecimal.ZERO);
        templateData.put("currencySymbol", resolveCurrencySymbol(currency));
        templateData.put("currency", currency != null ? currency : "NGN");
        templateData.put("email", recipientEmail);
        templateData.put("currentYear", java.time.Year.now().getValue());

        SendEmailRequest request = new SendEmailRequest(
                List.of(recipientEmail),
                "Power Bible School Registration Received — Complete Your Payment",
                "pbs-bank-transfer-confirmation",
                templateData,
                null,
                null
        );

        sendEmail(request);
    }

    @Async("emailTaskExecutor")
    public void sendPasswordResetEmail(String email, String firstName, String resetToken) {
        try {
            Thread.sleep(5000);  // Short delay for urgent password resets
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String resetLink = frontendUrl + "/reset-password?token=" + resetToken;

        Map<String, Object> templateData = Map.of(
                "firstName", firstName,
                "email", email,
                "resetLink", resetLink
        );

        SendEmailRequest request = new SendEmailRequest(
                List.of(email),
                "Reset Your Password - Power City International",
                "password-reset",
                templateData,
                null,
                null
        );

        sendEmail(request);
    }

    @Async("emailTaskExecutor")
    public void sendAnnouncementEmail(String email, String firstName, String announcementTitle,
                                      String announcementBody, String senderName) {
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("firstName", firstName);
        templateData.put("announcementTitle", announcementTitle);
        templateData.put("announcementBody", announcementBody);
        templateData.put("senderName", senderName);
        templateData.put("portalLink", frontendUrl + "/admin/announcements");

        SendEmailRequest request = new SendEmailRequest(
                List.of(email),
                "New Announcement from Your National Leader",
                "announcement-notification",
                templateData,
                null,
                null
        );

        sendEmail(request);
    }
}