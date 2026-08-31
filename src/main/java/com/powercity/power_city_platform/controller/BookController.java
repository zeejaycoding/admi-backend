package com.powercity.power_city_platform.controller;

import com.powercity.power_city_platform.dto.request.book.BookSearchRequest;
import com.powercity.power_city_platform.dto.request.book.CreateBookWithFileRequest;
import com.powercity.power_city_platform.dto.request.book.UpdateBookRequest;
import com.powercity.power_city_platform.dto.response.book.BookResponse;
import com.powercity.power_city_platform.dto.response.common.ApiResponse;
import com.powercity.power_city_platform.entity.Book;
import com.powercity.power_city_platform.entity.Order;
import com.powercity.power_city_platform.entity.OrderItem;
import com.powercity.power_city_platform.entity.User;
import com.powercity.power_city_platform.service.BookService;
import com.powercity.power_city_platform.service.OrderService;
import com.powercity.power_city_platform.service.S3FileService;
import com.powercity.power_city_platform.service.UserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import java.time.Duration;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/books")
@Tag(name = "Book Management", description = "APIs for managing digital books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;
    private final UserService userService;
    private final OrderService orderService;
    private final S3FileService s3FileService;

    private static final Logger logger = LoggerFactory.getLogger(BookController.class);


    @GetMapping
    @Operation(summary = "Search books", description = "Search and filter books with pagination")
    public ResponseEntity<ApiResponse<Page<BookResponse>>> searchBooks(
            @Parameter(description = "Search request parameters") @ModelAttribute BookSearchRequest request) {
        User currentUser = null;
        try {
            currentUser = userService.getCurrentUser();
        } catch (Exception e) {
            // Allow anonymous browsing
        }
        Page<BookResponse> books = bookService.searchBooks(request, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Books retrieved successfully", books));
    }

    @GetMapping("/{bookId}")
    @Operation(summary = "Get book by ID", description = "Get detailed information about a specific book")
    public ResponseEntity<ApiResponse<BookResponse>> getBookById(
            @Parameter(description = "Book ID", required = true) @PathVariable Long bookId) {
        User currentUser = null;
        try {
            currentUser = userService.getCurrentUser();
        } catch (Exception e) {
            // Allow anonymous browsing
        }
        BookResponse book = bookService.getBookById(bookId, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Book retrieved successfully", book));
    }

    @GetMapping("/featured")
    @Operation(summary = "Get featured books", description = "Get list of featured books")
    public ResponseEntity<ApiResponse<List<BookResponse>>> getFeaturedBooks() {
        User currentUser = userService.getCurrentUser();
        List<BookResponse> books = bookService.getFeaturedBooks(currentUser);
        return ResponseEntity.ok(ApiResponse.success("Featured books retrieved successfully", books));
    }

    @GetMapping("/recent")
    @Operation(summary = "Get recent books", description = "Get list of recently added books")
    public ResponseEntity<ApiResponse<List<BookResponse>>> getRecentBooks(
            @Parameter(description = "Number of books to retrieve") @RequestParam(defaultValue = "10") int limit) {
        User currentUser = userService.getCurrentUser();
        List<BookResponse> books = bookService.getRecentBooks(limit, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Recent books retrieved successfully", books));
    }

    @GetMapping("/top-selling")
    @Operation(summary = "Get top selling books", description = "Get list of top selling books")
    public ResponseEntity<ApiResponse<List<BookResponse>>> getTopSellingBooks(
            @Parameter(description = "Number of books to retrieve") @RequestParam(defaultValue = "10") int limit) {
        User currentUser = userService.getCurrentUser();
        List<BookResponse> books = bookService.getTopSellingBooks(limit, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Top selling books retrieved successfully", books));
    }

    @GetMapping("/most-viewed")
    @Operation(summary = "Get most viewed books", description = "Get list of most viewed books")
    public ResponseEntity<ApiResponse<List<BookResponse>>> getMostViewedBooks(
            @Parameter(description = "Number of books to retrieve") @RequestParam(defaultValue = "10") int limit) {
        User currentUser = userService.getCurrentUser();
        List<BookResponse> books = bookService.getMostViewedBooks(limit, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Most viewed books retrieved successfully", books));
    }

    @GetMapping("/{bookId}/download")
    @Operation(summary = "Download book", description = "Get download link for purchased book (authentication required)")
    public ResponseEntity<ApiResponse<String>> downloadBook(
            @Parameter(description = "Book ID", required = true) @PathVariable Long bookId) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required", "Please login to download books"));
        }
        String downloadUrl = bookService.getBookDownloadUrl(bookId, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Download URL generated successfully", downloadUrl));
    }

    @GetMapping("/download/{bookId}")
    @Operation(summary = "Serve book PDF", description = "Serve the actual PDF file for download")
    public ResponseEntity<Resource> serveBookPdf(
            @Parameter(description = "Book ID", required = true) @PathVariable Long bookId,
            @RequestParam(required = false) String user,
            @RequestParam(required = false) String timestamp,
            @RequestParam(required = false) String expires) {
        try {
            // Basic validation of download parameters
            if (user == null || timestamp == null || expires == null) {
                return ResponseEntity.badRequest().build();
            }

            // Check if download link has expired
            long expirationTime = Long.parseLong(expires);
            if (System.currentTimeMillis() > expirationTime) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
            }

            // Get the book and serve the PDF
            Book book = bookService.getBookEntityById(bookId);
            if (book == null || book.getPdfUrl() == null) {
                return ResponseEntity.notFound().build();
            }

            // For now, redirect to S3 URL. In production, you'd stream the file
            String s3Url = "https://powercity-intl-platform-files.s3.amazonaws.com/books/pdf/" + book.getPdfUrl();

            return ResponseEntity.status(302)
                    .header("Location", s3Url)
                    .build();

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/download/order/{orderNumber}")
    @Operation(summary = "Download book by order number", description = "Download book using order number (simpler format)")
    public ResponseEntity<Resource> downloadBookByOrderNumber(
            @Parameter(description = "Order number", required = true) @PathVariable String orderNumber) {
        try {
            // Get the order by order number
            User currentUser = userService.getCurrentUser();
            var orderResponse = orderService.getOrderByOrderNumber(orderNumber, currentUser);

            if (orderResponse == null) {
                return ResponseEntity.notFound().build();
            }

            // Check if order is completed
            if (!orderResponse.isCompleted()) {
                return ResponseEntity.status(400)
                        .body(new ByteArrayResource("Order not completed".getBytes()));
            }

            // Get the first book from the order
            var orderItems = orderResponse.getOrderItems();
            if (orderItems == null || orderItems.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            var orderItem = orderItems.get(0);
            Long bookId = orderItem.getBookId();

            // Get the book details
            Book book = bookService.getBookEntityById(bookId);
            if (book == null || book.getPdfUrl() == null) {
                return ResponseEntity.notFound().build();
            }

            // Generate pre-signed URL for secure S3 access
            String presignedUrl = s3FileService.generatePresignedUrl(book.getPdfKey(), Duration.ofHours(1));

            // Redirect to the pre-signed URL
            return ResponseEntity.status(302)
                    .header("Location", presignedUrl)
                    .build();

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ByteArrayResource("Internal server error".getBytes()));
        }
    }


    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Create book with files", description = "Create a new digital book with PDF, cover image, and back cover image uploads (Super Admin only)")
    public ResponseEntity<ApiResponse<BookResponse>> createBook(
            @Parameter(description = "Book creation request with files", required = true)
            @Valid @ModelAttribute CreateBookWithFileRequest request) {
        User currentUser = userService.getCurrentUser();
        BookResponse book = bookService.createBookWithFileUpload(request, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Book created successfully with files", book));
    }

    @PutMapping("/{bookId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update book", description = "Update an existing book (Super Admin only)")
    public ResponseEntity<ApiResponse<BookResponse>> updateBook(
            @Parameter(description = "Book ID", required = true) @PathVariable Long bookId,
            @Parameter(description = "Book update request", required = true)
            @Valid @RequestBody UpdateBookRequest request) {
        User currentUser = userService.getCurrentUser();
        BookResponse book = bookService.updateBook(bookId, request, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Book updated successfully", book));
    }

    @DeleteMapping("/{bookId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Delete book", description = "Soft delete a book (Super Admin only)")
    public ResponseEntity<ApiResponse<Void>> deleteBook(
            @Parameter(description = "Book ID", required = true) @PathVariable Long bookId) {
        User currentUser = userService.getCurrentUser();
        bookService.deleteBook(bookId, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Book deleted successfully", null));
    }

    @GetMapping("/admin/{bookId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get book by ID (Admin)", description = "Get book details without incrementing view count (Super Admin only)")
    public ResponseEntity<ApiResponse<BookResponse>> getBookByIdAdmin(
            @Parameter(description = "Book ID", required = true) @PathVariable Long bookId) {
        BookResponse book = bookService.getBookByIdAdmin(bookId);
        return ResponseEntity.ok(ApiResponse.success("Book retrieved successfully", book));
    }

    @GetMapping("/admin/analytics")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get book analytics", description = "Get comprehensive book analytics (Super Admin only)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getBookAnalytics() {
        Map<String, Object> analytics = bookService.getBookAnalytics();
        return ResponseEntity.ok(ApiResponse.success("Book analytics retrieved successfully", analytics));
    }
}
