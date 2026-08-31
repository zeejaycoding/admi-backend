package com.powercity.power_city_platform.service;

import com.powercity.power_city_platform.dto.request.book.BookSearchRequest;
import com.powercity.power_city_platform.dto.request.book.CreateBookWithFileRequest;
import com.powercity.power_city_platform.dto.request.book.UpdateBookRequest;
import com.powercity.power_city_platform.dto.response.book.BookResponse;
import com.powercity.power_city_platform.entity.Book;
import com.powercity.power_city_platform.entity.Order;
import com.powercity.power_city_platform.entity.OrderItem;
import com.powercity.power_city_platform.entity.User;
import com.powercity.power_city_platform.enums.Currency;
import com.powercity.power_city_platform.enums.OrderStatus;
import com.powercity.power_city_platform.exception.ResourceNotFoundException;
import com.powercity.power_city_platform.repository.BookRepository;
import com.powercity.power_city_platform.repository.OrderRepository;
import com.powercity.power_city_platform.service.email.BookPurchaseEmailService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class BookService {

    private static final Logger logger = LoggerFactory.getLogger(BookService.class);

    private final BookRepository bookRepository;
    private final OrderRepository orderRepository;
    private final CurrencyService currencyService;
    private final S3FileService s3FileService;
    private final CloudinaryService cloudinaryService;
    private final BookPurchaseEmailService bookPurchaseEmailService;

    // Create a new book with file uploads (PDF, cover images)
    public BookResponse createBookWithFileUpload(CreateBookWithFileRequest request, User currentUser) {
        // Only an active book reserves a title; a soft-deleted one shouldn't block re-creation.
        if (bookRepository.existsByTitleAndIsActiveTrue(request.getTitle())) {
            throw new RuntimeException("Book with this title already exists");
        }

        // Validate PDF file
        if (request.getPdfFile() == null || request.getPdfFile().isEmpty()) {
            throw new RuntimeException("PDF file is required");
        }

        try {
            // First create the book to get an ID for the S3 uploads
            Book book = new Book();
            book.setTitle(request.getTitle());
            book.setDescription(request.getDescription());
            book.setBasePrice(request.getBasePrice());
            book.setNgnPrice(request.getNgnPrice());
            book.setBaseCurrency(request.getBaseCurrency());
            book.setLanguage(request.getLanguage());
            book.setPublisher(request.getPublisher());
            book.setIsActive(request.getIsActive());
            book.setIsFeatured(request.getIsFeatured());
            book.setCreatedBy(currentUser.getId());
            book.setUpdatedBy(currentUser.getId());

            // Save book first to get the ID
            Book savedBook = bookRepository.save(book);

            // Upload PDF to S3 using the book ID (PDFs stay in S3)
            logger.info("Starting PDF upload to S3 for book ID: {}, file size: {} bytes",
                    savedBook.getId(), request.getPdfFile().getSize());
            long startTime = System.currentTimeMillis();
            String pdfS3Key = s3FileService.uploadBookPdf(savedBook.getId(), request.getPdfFile());
            long uploadTime = System.currentTimeMillis() - startTime;
            logger.info("PDF upload completed in {}ms, S3 key: {}", uploadTime, pdfS3Key);

            savedBook.setPdfKey(pdfS3Key);
            savedBook.setPdfUrl(pdfS3Key); // Store the S3 key as URL for now
            savedBook.setFileSizeBytes(request.getPdfFile().getSize());

            // Upload cover image to Cloudinary if provided (NEW: Using Cloudinary for images)
            if (request.getCoverImageFile() != null && !request.getCoverImageFile().isEmpty()) {
                Map<String, String> coverResult = cloudinaryService.uploadBookCover(
                    request.getCoverImageFile(),
                    savedBook.getId()
                );
                savedBook.setCoverImageUrl(coverResult.get("url"));
                savedBook.setCoverImageKey(coverResult.get("publicId"));
            }

            // Upload back cover to Cloudinary if provided (NEW: Using Cloudinary for images)
            if (request.getBackCoverImageFile() != null && !request.getBackCoverImageFile().isEmpty()) {
                Map<String, String> backCoverResult = cloudinaryService.uploadBookBackCover(
                    request.getBackCoverImageFile(),
                    savedBook.getId()
                );
                savedBook.setBackCoverImageUrl(backCoverResult.get("url"));
                savedBook.setBackCoverImageKey(backCoverResult.get("publicId"));
            }

            // Save updated book with all file URLs
            Book finalBook = bookRepository.save(savedBook);
            return convertToBookResponse(finalBook, currentUser);

        } catch (Exception e) {
            // Log the full error for debugging (only visible in server logs)
            logger.error("Failed to create book with file uploads. Error: {}", e.getMessage(), e);

            // Return a sanitized error message to the client (no internal details)
            throw new RuntimeException("Failed to create book. Please check the file formats and try again.");
        }
    }

    // Update an existing book
    public BookResponse updateBook(Long bookId, UpdateBookRequest request, User currentUser) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found"));

        if (request.getTitle() != null) book.setTitle(request.getTitle());
        if (request.getDescription() != null) book.setDescription(request.getDescription());
        if (request.getBasePrice() != null) book.setBasePrice(request.getBasePrice());
        if (request.getBaseCurrency() != null) book.setBaseCurrency(request.getBaseCurrency());
        if (request.getLanguage() != null) book.setLanguage(request.getLanguage());
        if (request.getPublisher() != null) book.setPublisher(request.getPublisher());
        if (request.getIsActive() != null) book.setIsActive(request.getIsActive());
        if (request.getIsFeatured() != null) book.setIsFeatured(request.getIsFeatured());
        if (request.getCoverImageUrl() != null) book.setCoverImageUrl(request.getCoverImageUrl());
        if (request.getBackCoverImageUrl() != null) book.setBackCoverImageUrl(request.getBackCoverImageUrl());
        if (request.getPdfUrl() != null) book.setPdfUrl(request.getPdfUrl());
        if (request.getFileSizeBytes() != null) book.setFileSizeBytes(request.getFileSizeBytes());

        book.setUpdatedBy(currentUser.getId());
        book.setUpdatedAt(LocalDateTime.now());

        Book updatedBook = bookRepository.save(book);
        return convertToBookResponse(updatedBook, currentUser);
    }

    // Get book by ID
    @Transactional(readOnly = true)
    public BookResponse getBookById(Long bookId, User currentUser) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found"));

        // Increment view count
        book.incrementViewCount();
        bookRepository.save(book);

        return convertToBookResponse(book, currentUser);
    }

    // Get book by ID (admin version - no view count increment)
    @Transactional(readOnly = true)
    public BookResponse getBookByIdAdmin(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found"));

        return convertToBookResponse(book, null);
    }

    // Get book entity by ID (for internal use)
    @Transactional(readOnly = true)
    public Book getBookEntityById(Long bookId) {
        return bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found"));
    }

    // Search books
    @Transactional(readOnly = true)
    public Page<BookResponse> searchBooks(BookSearchRequest request, User currentUser) {
        Pageable pageable = createPageable(request);
        Page<Book> booksPage;

        if (request.hasSearchCriteria()) {
            if (request.getSearchTerm() != null) {
                booksPage = bookRepository.searchBooks(request.getSearchTerm(), pageable);
            } else if (request.hasPriceRange()) {
                BigDecimal minPrice = request.getMinPrice() != null ? request.getMinPrice() : BigDecimal.ZERO;
                BigDecimal maxPrice = request.getMaxPrice() != null ? request.getMaxPrice() : BigDecimal.valueOf(999999);
                List<Book> books = bookRepository.findByBasePriceBetweenAndIsActiveTrue(minPrice, maxPrice);
                booksPage = createPageFromList(books, pageable);
            } else {
                booksPage = bookRepository.findByIsActiveTrue(pageable);
            }
        } else {
            booksPage = bookRepository.findByIsActiveTrue(pageable);
        }

        return booksPage.map(book -> convertToBookResponse(book, currentUser));
    }

    // Get featured books
    @Transactional(readOnly = true)
    public List<BookResponse> getFeaturedBooks(User currentUser) {
        List<Book> books = bookRepository.findByIsFeaturedTrueAndIsActiveTrue();
        return books.stream()
                .map(book -> convertToBookResponse(book, currentUser))
                .collect(Collectors.toList());
    }

    // Get recent books
    @Transactional(readOnly = true)
    public List<BookResponse> getRecentBooks(int limit, User currentUser) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<Book> books = bookRepository.findRecentBooks(pageable);
        return books.stream()
                .map(book -> convertToBookResponse(book, currentUser))
                .collect(Collectors.toList());
    }

    // Get top selling books
    @Transactional(readOnly = true)
    public List<BookResponse> getTopSellingBooks(int limit, User currentUser) {
        Pageable pageable = PageRequest.of(0, limit);
        List<Book> books = bookRepository.findTopSellingBooks(pageable);
        return books.stream()
                .map(book -> convertToBookResponse(book, currentUser))
                .collect(Collectors.toList());
    }

    // Get most viewed books
    @Transactional(readOnly = true)
    public List<BookResponse> getMostViewedBooks(int limit, User currentUser) {
        Pageable pageable = PageRequest.of(0, limit);
        List<Book> books = bookRepository.findMostViewedBooks(pageable);
        return books.stream()
                .map(book -> convertToBookResponse(book, currentUser))
                .collect(Collectors.toList());
    }

    // Delete book (soft delete)
    public void deleteBook(Long bookId, User currentUser) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found"));

        book.setIsActive(false);
        book.setUpdatedBy(currentUser.getId());
        book.setUpdatedAt(LocalDateTime.now());

        bookRepository.save(book);
    }

    // Get book analytics
    @Transactional(readOnly = true)
    public Map<String, Object> getBookAnalytics() {
        Map<String, Object> analytics = new HashMap<>();

        analytics.put("totalBooks", bookRepository.countActiveBooks());
        analytics.put("featuredBooks", bookRepository.countFeaturedBooks());
        analytics.put("minPrice", bookRepository.findMinPrice());
        analytics.put("maxPrice", bookRepository.findMaxPrice());
        analytics.put("averagePrice", bookRepository.findAveragePrice());

        return analytics;
    }

    // Get download URL for purchased book
    @Transactional(readOnly = true)
    public String getBookDownloadUrl(Long bookId, User currentUser) {
        // Check if user has purchased this book
        List<Order> userOrders = orderRepository.findByUserOrderByCreatedAtDesc(currentUser);

        boolean hasPurchased = userOrders.stream()
                .anyMatch(order -> order.getStatus() == OrderStatus.COMPLETED &&
                        order.getOrderItems().stream()
                                .anyMatch(item -> item.getBook().getId().equals(bookId)));

        if (!hasPurchased) {
            throw new RuntimeException("You have not purchased this book");
        }

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found"));

        if (book.getPdfKey() == null) {
            throw new RuntimeException("Book file not available");
        }

        // Generate presigned URL for download
        return s3FileService.generatePresignedUrl(book.getPdfKey(), Duration.ofHours(1)); // 1 hour expiry
    }

    // Convert Book entity to BookResponse DTO
    private BookResponse convertToBookResponse(Book book, User currentUser) {
        BookResponse response = new BookResponse();
        response.setId(book.getId());
        response.setTitle(book.getTitle());
        response.setDescription(book.getDescription());
        response.setBasePrice(book.getBasePrice());
        response.setNgnPrice(book.getNgnPrice());
        response.setBaseCurrency(book.getBaseCurrency());
        response.setLanguage(book.getLanguage());
        response.setPublisher(book.getPublisher());
        response.setIsActive(book.getIsActive());
        response.setIsFeatured(book.getIsFeatured());
        response.setCoverImageUrl(book.getCoverImageUrl());
        response.setBackCoverImageUrl(book.getBackCoverImageUrl());
        response.setPdfUrl(book.getPdfUrl());
        response.setFileSizeBytes(book.getFileSizeBytes());
        response.setDownloadCount(book.getDownloadCount());
        response.setViewCount(book.getViewCount());
        response.setSalesCount(book.getSalesCount());
        response.setCreatedAt(book.getCreatedAt());
        response.setUpdatedAt(book.getUpdatedAt());

        return response;
    }

    // Create pageable from search request
    private Pageable createPageable(BookSearchRequest request) {
        Sort sort = Sort.by(Sort.Direction.fromString(request.getSortDirection()), request.getSortBy());
        return PageRequest.of(request.getPage(), request.getSize(), sort);
    }

    // Create page from list (helper method)
    private Page<Book> createPageFromList(List<Book> books, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), books.size());

        if (start > books.size()) {
            return Page.empty(pageable);
        }

        return new org.springframework.data.domain.PageImpl<>(
            books.subList(start, end), pageable, books.size());
    }
}
