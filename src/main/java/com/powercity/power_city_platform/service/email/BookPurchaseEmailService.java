package com.powercity.power_city_platform.service.email;

import com.powercity.power_city_platform.dto.request.email.SendEmailRequest;
import com.powercity.power_city_platform.entity.User;
import com.powercity.power_city_platform.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BookPurchaseEmailService {

    @Autowired
    private EmailService emailService;

    @Value("${app.email.frontend-url:http://localhost:8080}")
    private String frontendUrl;

    @Async("emailTaskExecutor")
    public void sendBookPurchaseConfirmation(User user, String bookTitle, Long bookId, String orderId,
                                             BigDecimal amount, String currency) {
        try {
            Thread.sleep(5000);  // Short delay for purchase confirmations
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String orderLink = frontendUrl + "/orders/" + orderId;

        // Generate simple download URL using order number
        String downloadLink = "http://localhost:8080/api/v1/books/download/order/" + orderId;

        Map<String, Object> templateData = new HashMap<>();
        templateData.put("firstName", user.getFirstName() != null ? user.getFirstName() : "Valued Customer");
        templateData.put("lastName", user.getLastName() != null ? user.getLastName() : "");
        templateData.put("bookTitle", bookTitle);
        templateData.put("orderId", orderId);
        templateData.put("amount", amount);
        templateData.put("currency", currency);
        templateData.put("orderLink", orderLink);
        templateData.put("downloadLink", downloadLink);
        templateData.put("region", user.getRegion() != null ? user.getRegion() : "Unknown");

        SendEmailRequest request = new SendEmailRequest(
                List.of(user.getEmail()),
                "Purchase Confirmation - " + bookTitle,
                "book-purchase-confirmation",
                templateData,
                null,
                null
        );

        emailService.sendEmail(request);
    }

    @Async("emailTaskExecutor")
    public void sendBookDownloadConfirmation(User user, String bookTitle,
                                           String orderNumber, BigDecimal amount, String currency,
                                           List<String> downloadUrls, Integer quantity, Long fileSizeBytes) {
        try {
            Thread.sleep(5000);  // Short delay for download confirmations
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String orderLink = frontendUrl + "/orders/" + orderNumber;
        String fileSizeMB = fileSizeBytes != null ? String.format("%.1f", fileSizeBytes / (1024.0 * 1024.0)) : "Unknown";

        Map<String, Object> templateData = new HashMap<>();
        templateData.put("firstName", user.getFirstName() != null ? user.getFirstName() : "Valued Customer");
        templateData.put("lastName", user.getLastName() != null ? user.getLastName() : "");
        templateData.put("email", user.getEmail() != null ? user.getEmail() : "");
        templateData.put("bookTitle", bookTitle);
        templateData.put("bookAuthor", "Abel Damina Ministries International");
        templateData.put("orderNumber", orderNumber);
        templateData.put("amount", amount);
        templateData.put("currency", currency);
        templateData.put("orderLink", orderLink);
        templateData.put("downloadUrls", downloadUrls);  // Multiple download URLs
        templateData.put("downloadUrl", downloadUrls.get(0));  // Keep first URL for backward compatibility
        templateData.put("quantity", quantity);  // Number of copies purchased
        templateData.put("fileSizeMB", fileSizeMB);
        templateData.put("createdAt", new java.util.Date());
        templateData.put("helpCenterUrl", frontendUrl + "/help");

        SendEmailRequest request = new SendEmailRequest(
                List.of(user.getEmail()),
                "Your Order - " + bookTitle + (quantity > 1 ? " (" + quantity + " copies)" : ""),
                "book-download-confirmation",
                templateData,
                null,
                null
        );

        emailService.sendEmail(request);
    }

    /**
     * Send ONE consolidated email with ALL books in an order
     * This avoids rate limit errors from sending multiple emails
     */
    @Async("emailTaskExecutor")
    public void sendOrderDownloadConfirmation(User user, String orderNumber,
                                             List<Map<String, Object>> booksData,
                                             BigDecimal totalAmount, String currency) {
        try {
            Thread.sleep(5000);  // Short delay for book emails
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String orderLink = frontendUrl + "/orders/" + orderNumber;

        // Process books data to add formatted file sizes
        List<Map<String, Object>> processedBooksData = new java.util.ArrayList<>();
        for (Map<String, Object> bookData : booksData) {
            Map<String, Object> processedBook = new HashMap<>(bookData);
            Long fileSizeBytes = (Long) bookData.get("fileSizeBytes");
            String fileSizeMB = fileSizeBytes != null ? String.format("%.1f", fileSizeBytes / (1024.0 * 1024.0)) : "Unknown";
            processedBook.put("fileSizeMB", fileSizeMB);
            processedBooksData.add(processedBook);
        }

        // Count total number of books
        int totalBooks = booksData.size();

        Map<String, Object> templateData = new HashMap<>();
        templateData.put("firstName", user.getFirstName() != null ? user.getFirstName() : "Valued Customer");
        templateData.put("lastName", user.getLastName() != null ? user.getLastName() : "");
        templateData.put("email", user.getEmail() != null ? user.getEmail() : "");
        templateData.put("orderNumber", orderNumber);
        templateData.put("totalAmount", totalAmount);
        templateData.put("currency", currency);
        templateData.put("orderLink", orderLink);
        templateData.put("booksData", processedBooksData);  // List of all books with download URLs
        templateData.put("totalBooks", totalBooks);
        templateData.put("createdAt", new java.util.Date());
        templateData.put("helpCenterUrl", frontendUrl + "/help");

        // Build subject line
        String subject = totalBooks == 1
            ? "Your Order - " + booksData.get(0).get("title")
            : "Your Order - " + totalBooks + " E-Books Ready";

        SendEmailRequest request = new SendEmailRequest(
                List.of(user.getEmail()),
                subject,
                "order-download-confirmation",  // New template for multiple books
                templateData,
                null,
                null
        );

        emailService.sendEmail(request);
    }

    /**
     * Send ONE combined email with BOTH books and courses in an order
     * Used when customer purchases both products together
     */
    @Async("emailTaskExecutor")
    public void sendCombinedOrderConfirmation(User user, String orderNumber,
                                             List<Map<String, Object>> booksData,
                                             List<Map<String, Object>> coursesData,
                                             BigDecimal totalAmount, String currency) {
        try {
            Thread.sleep(1000);  // Brief delay for combined emails
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String orderLink = frontendUrl + "/orders/" + orderNumber;
        String dashboardLink = frontendUrl + "/dashboard";

        // Process books data to add formatted file sizes
        List<Map<String, Object>> processedBooksData = new java.util.ArrayList<>();
        for (Map<String, Object> bookData : booksData) {
            Map<String, Object> processedBook = new HashMap<>(bookData);
            Long fileSizeBytes = (Long) bookData.get("fileSizeBytes");
            String fileSizeMB = fileSizeBytes != null ? String.format("%.1f", fileSizeBytes / (1024.0 * 1024.0)) : "Unknown";
            processedBook.put("fileSizeMB", fileSizeMB);
            processedBooksData.add(processedBook);
        }

        // Count totals
        int totalBooks = booksData.size();
        int totalCourses = coursesData.size();

        Map<String, Object> templateData = new HashMap<>();
        templateData.put("firstName", user.getFirstName() != null ? user.getFirstName() : "Valued Customer");
        templateData.put("lastName", user.getLastName() != null ? user.getLastName() : "");
        templateData.put("email", user.getEmail() != null ? user.getEmail() : "");
        templateData.put("orderNumber", orderNumber);
        templateData.put("totalAmount", totalAmount);
        templateData.put("currency", currency);
        templateData.put("orderLink", orderLink);
        templateData.put("dashboardLink", dashboardLink);
        templateData.put("booksData", processedBooksData);  // Books with download URLs
        templateData.put("coursesData", coursesData);  // Courses with access info
        templateData.put("totalBooks", totalBooks);
        templateData.put("totalCourses", totalCourses);
        templateData.put("createdAt", new java.util.Date());
        templateData.put("helpCenterUrl", frontendUrl + "/help");

        // Build subject line
        String subject = "Your Order - " + totalBooks + " E-Book(s) + " + totalCourses + " Course(s)";

        SendEmailRequest request = new SendEmailRequest(
                List.of(user.getEmail()),
                subject,
                "combined-order-confirmation",  // New template for books + courses
                templateData,
                null,
                null
        );

        emailService.sendEmail(request);
    }
}