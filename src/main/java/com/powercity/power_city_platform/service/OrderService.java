package com.powercity.power_city_platform.service;

import com.powercity.power_city_platform.dto.request.order.CreateOrderRequest;
import com.powercity.power_city_platform.dto.request.payment.CreatePaymentRequest;
import com.powercity.power_city_platform.dto.response.order.OrderResponse;
import com.powercity.power_city_platform.dto.response.payment.PaymentResponse;
import com.powercity.power_city_platform.entity.Book;
import com.powercity.power_city_platform.entity.Course;
import com.powercity.power_city_platform.entity.CourseProgress;
import com.powercity.power_city_platform.entity.Order;
import com.powercity.power_city_platform.entity.OrderItem;
import com.powercity.power_city_platform.entity.User;
import com.powercity.power_city_platform.enums.Currency;
import com.powercity.power_city_platform.enums.OrderStatus;
import com.powercity.power_city_platform.enums.ProductType;
import com.powercity.power_city_platform.exception.ResourceNotFoundException;
import com.powercity.power_city_platform.repository.BookRepository;
import com.powercity.power_city_platform.repository.CourseRepository;
import com.powercity.power_city_platform.repository.CourseProgressRepository;
import com.powercity.power_city_platform.repository.OrderRepository;
import com.powercity.power_city_platform.service.email.BookPurchaseEmailService;
import com.powercity.power_city_platform.service.email.CourseEnrollmentEmailService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final BookRepository bookRepository;
    private final PaymentService paymentService;
    private final BookPurchaseEmailService bookPurchaseEmailService;
    private final CourseEnrollmentEmailService courseEnrollmentEmailService;
    private final S3FileService s3FileService;
    private final CartService cartService;
    private final CourseRepository courseRepository;
    private final CourseProgressRepository courseProgressRepository;
    private final com.powercity.power_city_platform.repository.CartItemRepository cartItemRepository;

    @Value("${app.email.frontend-url}")
    private String frontendUrl;

    // Create a new order
    public OrderResponse createOrder(CreateOrderRequest request, User currentUser) {
        // Generate unique order number
        String orderNumber = generateOrderNumber();

        // Calculate totals
        BigDecimal subtotal = request.calculateSubtotal();
        BigDecimal totalAmount = request.calculateTotal();

        // Create order
        Order order = new Order(orderNumber, currentUser, totalAmount, request.getCurrency());
        order.setSubtotal(subtotal);
        order.setTaxAmount(request.getTaxAmount());
        order.setDiscountAmount(request.getDiscountAmount());
        order.setNotes(request.getNotes());
        order.setCustomerEmail(request.getCustomerEmail() != null ? request.getCustomerEmail() : currentUser.getEmail());
        order.setCustomerName(request.getCustomerName() != null ? request.getCustomerName() : currentUser.getFullName());
        order.setCustomerPhone(request.getCustomerPhone());
        order.setBillingAddress(request.getBillingAddress());
        order.setShippingAddress(request.getShippingAddress());
        order.setRegion(request.getRegion() != null ? request.getRegion() : currentUser.getRegion());
        order.setPaymentMethod(request.getPaymentMethod());
        order.setPaymentGateway(request.getPaymentGateway());
        order.setCreatedBy(currentUser.getId());
        order.setUpdatedBy(currentUser.getId());

        // Add order items
        for (CreateOrderRequest.OrderItemRequest itemRequest : request.getOrderItems()) {
            Book book = bookRepository.findById(itemRequest.getBookId())
                    .orElseThrow(() -> new ResourceNotFoundException("Book not found: " + itemRequest.getBookId()));

            OrderItem orderItem = new OrderItem(order, book, itemRequest.getQuantity(),
                    itemRequest.getUnitPrice(), request.getCurrency());
            orderItem.setDiscountAmount(itemRequest.getDiscountAmount());
            orderItem.setNotes(itemRequest.getNotes());
            orderItem.setCreatedBy(currentUser.getId());
            orderItem.setUpdatedBy(currentUser.getId());

            order.addOrderItem(orderItem);
        }

        Order savedOrder = orderRepository.save(order);
        return convertToOrderResponse(savedOrder, currentUser);
    }

    // Create order from cart (checkout all cart items)
    public OrderResponse createOrderFromCart(Currency currency, User currentUser) {
        // Get user's cart
        com.powercity.power_city_platform.entity.Cart cart = cartService.getOrCreateCart(currentUser);

        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        // Generate unique order number
        String orderNumber = generateOrderNumber();

        // Calculate totals from cart
        BigDecimal subtotal = BigDecimal.ZERO;
        for (com.powercity.power_city_platform.entity.CartItem cartItem : cart.getItems()) {
            subtotal = subtotal.add(cartItem.getSubtotal());
        }
        BigDecimal totalAmount = subtotal; // No tax or discount for now

        // Create order
        Order order = new Order(orderNumber, currentUser, totalAmount, currency);
        order.setSubtotal(subtotal);
        order.setTaxAmount(BigDecimal.ZERO);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setCustomerEmail(currentUser.getEmail());
        order.setCustomerName(currentUser.getFullName());
        order.setRegion(currentUser.getRegion());
        order.setPaymentMethod("STRIPE");
        order.setPaymentGateway("STRIPE");
        order.setCreatedBy(currentUser.getId());
        order.setUpdatedBy(currentUser.getId());

        // Add order items from cart items
        for (com.powercity.power_city_platform.entity.CartItem cartItem : cart.getItems()) {
            if (cartItem.getProductType() == ProductType.BOOK) {
                Book book = bookRepository.findById(cartItem.getProductId())
                        .orElseThrow(() -> new ResourceNotFoundException("Book not found: " + cartItem.getProductId()));

                OrderItem orderItem = new OrderItem(order, book, cartItem.getQuantity(),
                        cartItem.getPrice(), currency);
                orderItem.setCreatedBy(currentUser.getId());
                orderItem.setUpdatedBy(currentUser.getId());

                order.addOrderItem(orderItem);
            } else if (cartItem.getProductType() == ProductType.COURSE) {
                Course course = courseRepository.findById(cartItem.getProductId())
                        .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + cartItem.getProductId()));

                OrderItem orderItem = new OrderItem(order, course, cartItem.getQuantity(),
                        cartItem.getPrice(), currency);
                orderItem.setCreatedBy(currentUser.getId());
                orderItem.setUpdatedBy(currentUser.getId());

                order.addOrderItem(orderItem);
            }
        }

        Order savedOrder = orderRepository.save(order);
        return convertToOrderResponse(savedOrder, currentUser);
    }

    // Create order from a single cart item (for "Buy Now" functionality)
    public OrderResponse createOrderFromCartItem(Long cartItemId, Currency currency, User currentUser) {
        // Fetch the cart item
        com.powercity.power_city_platform.entity.CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

        // Verify the cart item belongs to the current user's cart
        com.powercity.power_city_platform.entity.Cart userCart = cartService.getOrCreateCart(currentUser);
        if (!cartItem.getCart().getId().equals(userCart.getId())) {
            throw new RuntimeException("Cart item does not belong to current user");
        }

        // Generate unique order number
        String orderNumber = generateOrderNumber();

        // Calculate totals from single cart item
        BigDecimal subtotal = cartItem.getSubtotal();
        BigDecimal totalAmount = subtotal; // No tax or discount for now

        // Create order
        Order order = new Order(orderNumber, currentUser, totalAmount, currency);
        order.setSubtotal(subtotal);
        order.setTaxAmount(BigDecimal.ZERO);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setCustomerEmail(currentUser.getEmail());
        order.setCustomerName(currentUser.getFullName());
        order.setRegion(currentUser.getRegion());
        order.setPaymentMethod("STRIPE");
        order.setPaymentGateway("STRIPE");
        order.setCreatedBy(currentUser.getId());
        order.setUpdatedBy(currentUser.getId());

        // Add single order item from cart item
        if (cartItem.getProductType() == ProductType.BOOK) {
            Book book = bookRepository.findById(cartItem.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Book not found: " + cartItem.getProductId()));

            OrderItem orderItem = new OrderItem(order, book, cartItem.getQuantity(),
                    cartItem.getPrice(), currency);
            orderItem.setCreatedBy(currentUser.getId());
            orderItem.setUpdatedBy(currentUser.getId());

            order.addOrderItem(orderItem);
        } else if (cartItem.getProductType() == ProductType.COURSE) {
            Course course = courseRepository.findById(cartItem.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + cartItem.getProductId()));

            OrderItem orderItem = new OrderItem(order, course, cartItem.getQuantity(),
                    cartItem.getPrice(), currency);
            orderItem.setCreatedBy(currentUser.getId());
            orderItem.setUpdatedBy(currentUser.getId());

            order.addOrderItem(orderItem);
        }

        Order savedOrder = orderRepository.save(order);
        return convertToOrderResponse(savedOrder, currentUser);
    }

    // Get order by ID
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId, User currentUser) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        // Check if user has access to this order
        if (!order.getUser().getId().equals(currentUser.getId()) &&
            !currentUser.getRoles().stream().anyMatch(role ->
                role.getRole().getName().equals("ADMIN") ||
                role.getRole().getName().equals("SUPER_ADMIN"))) {
            throw new RuntimeException("Access denied");
        }

        return convertToOrderResponse(order, currentUser);
    }

    // Get order by ID (public - for payment success page via Stripe session)
    @Transactional(readOnly = true)
    public OrderResponse getOrderByIdPublic(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        return convertToOrderResponse(order, order.getUser());
    }

    // Get order by order number
    @Transactional(readOnly = true)
    public OrderResponse getOrderByOrderNumber(String orderNumber, User currentUser) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        // Check if user has access to this order
        if (!order.getUser().getId().equals(currentUser.getId()) &&
            !currentUser.getRoles().stream().anyMatch(role ->
                role.getRole().getName().equals("ADMIN") ||
                role.getRole().getName().equals("SUPER_ADMIN"))) {
            throw new RuntimeException("Access denied");
        }

        return convertToOrderResponse(order, currentUser);
    }

    // Get user orders
    @Transactional(readOnly = true)
    public Page<OrderResponse> getUserOrders(User currentUser, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Order> ordersPage = orderRepository.findByUserOrderByCreatedAtDesc(currentUser, pageable);
        return ordersPage.map(order -> convertToOrderResponse(order, currentUser));
    }

    // Get user orders by status
    @Transactional(readOnly = true)
    public Page<OrderResponse> getUserOrdersByStatus(User currentUser, OrderStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Order> ordersPage = orderRepository.findByUserAndStatus(currentUser, status, pageable);
        return ordersPage.map(order -> convertToOrderResponse(order, currentUser));
    }

    // Get all orders (admin only)
    @Transactional(readOnly = true)
    public Page<OrderResponse> getAllOrders(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Order> ordersPage = orderRepository.findAll(pageable);
        return ordersPage.map(order -> convertToOrderResponse(order, null));
    }

    // Get orders by status (admin only)
    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrdersByStatus(OrderStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Order> ordersPage = orderRepository.findByStatus(status, pageable);
        return ordersPage.map(order -> convertToOrderResponse(order, null));
    }

    // Search orders (admin only)
    @Transactional(readOnly = true)
    public Page<OrderResponse> searchOrders(String searchTerm, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Order> ordersPage = orderRepository.searchOrders(searchTerm, pageable);
        return ordersPage.map(order -> convertToOrderResponse(order, null));
    }

    // Cancel order
    public OrderResponse cancelOrder(Long orderId, String reason, User currentUser) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        // Check if user has access to this order
        if (!order.getUser().getId().equals(currentUser.getId()) &&
            !currentUser.getRoles().stream().anyMatch(role ->
                role.getRole().getName().equals("ADMIN") ||
                role.getRole().getName().equals("SUPER_ADMIN"))) {
            throw new RuntimeException("Access denied");
        }

        if (!order.canBeCancelled()) {
            throw new RuntimeException("Order cannot be cancelled");
        }

        order.markAsCancelled(reason);
        order.setUpdatedBy(currentUser.getId());
        order.setUpdatedAt(LocalDateTime.now());

        Order savedOrder = orderRepository.save(order);
        return convertToOrderResponse(savedOrder, currentUser);
    }

    // Process order payment
    public OrderResponse processOrderPayment(Long orderId, String paymentGatewayTransactionId, User currentUser) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Order is not in pending status");
        }

        // Create payment record
        CreatePaymentRequest paymentRequest = new CreatePaymentRequest(
            orderId,
            order.getTotalAmount(),
            order.getCurrency(),
            "STRIPE", // payment method
            "STRIPE"  // payment gateway
        );
        paymentRequest.setNotes("Payment processed via Stripe");
        paymentRequest.setMetadata("{\"session_id\":\"" + paymentGatewayTransactionId + "\"}");

        PaymentResponse payment = paymentService.createPayment(paymentRequest, currentUser);

        // Mark payment as completed
        paymentService.confirmPayment(payment.getId(), currentUser);

        order.markAsProcessed();
        order.setPaymentGatewayTransactionId(paymentGatewayTransactionId);
        order.setUpdatedBy(currentUser.getId());
        order.setUpdatedAt(LocalDateTime.now());

        Order savedOrder = orderRepository.save(order);
        return convertToOrderResponse(savedOrder, currentUser);
    }

    // Complete order
    public OrderResponse completeOrder(Long orderId, User currentUser) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getStatus() != OrderStatus.PROCESSING) {
            throw new RuntimeException("Order is not in processing status");
        }

        order.markAsCompleted();
        order.setUpdatedBy(currentUser.getId());
        order.setUpdatedAt(LocalDateTime.now());

        // Increment sales count for books and courses
        for (OrderItem item : order.getOrderItems()) {
            if (item.getProductType() == ProductType.BOOK && item.getBook() != null) {
                Book book = item.getBook();
                book.incrementSalesCount();
                bookRepository.save(book);
            } else if (item.getProductType() == ProductType.COURSE && item.getCourse() != null) {
                Course course = item.getCourse();
                course.incrementEnrollmentCount();
                courseRepository.save(course);
            }
        }

        // Remove purchased items from cart
        try {
            User orderUser = order.getUser();
            cartService.removePurchasedItemsFromCart(orderUser, order.getOrderItems());
        } catch (Exception e) {
            // Log error but don't fail the order completion
            logger.error("Failed to remove items from cart: {}", e.getMessage(), e);
        }

        // Note: Purchase confirmation email removed - download email contains all order details
        // The download email (sent separately) includes order info + working download link

        Order savedOrder = orderRepository.save(order);
        return convertToOrderResponse(savedOrder, currentUser);
    }

    // Refund order
    public OrderResponse refundOrder(Long orderId, BigDecimal refundAmount, String reason, User currentUser) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        // Check if user has access to this order
        if (!order.getUser().getId().equals(currentUser.getId()) &&
            !currentUser.getRoles().stream().anyMatch(role ->
                role.getRole().getName().equals("ADMIN") ||
                role.getRole().getName().equals("SUPER_ADMIN"))) {
            throw new RuntimeException("Access denied");
        }

        if (!order.canBeRefunded()) {
            throw new RuntimeException("Order cannot be refunded");
        }

        if (refundAmount.compareTo(order.getTotalAmount()) > 0) {
            throw new RuntimeException("Refund amount cannot exceed order total");
        }

        order.markAsRefunded(refundAmount, reason);
        order.setUpdatedBy(currentUser.getId());
        order.setUpdatedAt(LocalDateTime.now());

        Order savedOrder = orderRepository.save(order);
        return convertToOrderResponse(savedOrder, currentUser);
    }

    // Get order analytics
    @Transactional(readOnly = true)
    public Map<String, Object> getOrderAnalytics() {
        Map<String, Object> analytics = new HashMap<>();

        analytics.put("totalOrders", orderRepository.count());
        analytics.put("totalRevenue", orderRepository.getTotalRevenue());
        analytics.put("averageOrderValue", orderRepository.getAverageOrderValue());
        analytics.put("ordersByStatus", orderRepository.getOrderCountByStatus());
        analytics.put("ordersByRegion", orderRepository.getOrderCountByRegion());
        analytics.put("ordersByCurrency", orderRepository.getOrderCountByCurrency());
        analytics.put("ordersByPaymentGateway", orderRepository.getOrderCountByPaymentGateway());
        analytics.put("monthlyRevenue", orderRepository.getMonthlyRevenue());
        analytics.put("dailyRevenue", orderRepository.getDailyRevenue());

        return analytics;
    }

    // Get user order analytics
    @Transactional(readOnly = true)
    public Map<String, Object> getUserOrderAnalytics(User currentUser) {
        Map<String, Object> analytics = new HashMap<>();

        analytics.put("totalOrders", orderRepository.countByUser(currentUser));
        analytics.put("totalRevenue", orderRepository.getTotalRevenueByUser(currentUser));
        analytics.put("averageOrderValue", orderRepository.getAverageOrderValueByUser(currentUser));

        return analytics;
    }

    // Send book download email with actual PDF URLs
    public void sendBookDownloadEmail(Long orderId, User adminUser) {
        // Use findByIdWithItems to eagerly fetch orderItems and books
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.isCompleted()) {
            throw new RuntimeException("Order is not completed");
        }

        // Check if admin has permission
        if (adminUser.getRoles() == null || !adminUser.getRoles().stream().anyMatch(role ->
            role.getRole().getName().equals("ADMIN") ||
            role.getRole().getName().equals("SUPER_ADMIN"))) {
            throw new RuntimeException("Access denied - Admin privileges required");
        }

        // Collect all books and their download URLs, then send ONE email with everything
        logger.info("Preparing to send consolidated download email for order: {}", order.getOrderNumber());

        java.util.List<java.util.Map<String, Object>> booksData = new java.util.ArrayList<>();

        for (OrderItem item : order.getOrderItems()) {
            try {
                Book book = item.getBook();
                if (book == null) {
                    logger.error("Skipping OrderItem with null Book reference (item ID: {})", item.getId());
                    continue;
                }

                Integer quantity = item.getQuantity();

                logger.info("Processing book: {} (quantity: {})", book.getTitle(), quantity);

                // Generate ONE download URL per book (regardless of quantity)
                String downloadUrl = generateSecureDownloadUrl(book, order.getUser());

                // Store book data
                java.util.Map<String, Object> bookData = new java.util.HashMap<>();
                bookData.put("title", book.getTitle());
                bookData.put("downloadUrl", downloadUrl);  // Single download URL
                bookData.put("quantity", quantity);
                bookData.put("price", item.getSubtotal());
                bookData.put("fileSizeBytes", book.getFileSizeBytes());

                booksData.add(bookData);
                logger.info("Added book to email: {}", book.getTitle());
            } catch (Exception e) {
                logger.error("Failed to process book for item ID: {}", item.getId(), e);
            }
        }

        if (booksData.isEmpty()) {
            throw new RuntimeException("No books were successfully processed for download email");
        }

        // Eagerly initialize User entity to avoid LazyInitializationException in async method
        User user = order.getUser();
        user.getFirstName();  // Trigger lazy loading
        user.getLastName();   // Trigger lazy loading
        user.getEmail();      // Trigger lazy loading
        user.getRegion();     // Trigger lazy loading

        // Send ONE email with ALL books
        try {
            bookPurchaseEmailService.sendOrderDownloadConfirmation(
                user,
                order.getOrderNumber(),
                booksData,
                order.getTotalAmount(),
                order.getCurrency().getCode()
            );
            logger.info("Successfully sent consolidated email with {} book(s)", booksData.size());
        } catch (Exception e) {
            logger.error("Failed to send consolidated download email: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send download email: " + e.getMessage());
        }
    }

    /**
     * Grant course access after successful purchase
     * Creates CourseProgress entry for each purchased course
     */
    public void grantCourseAccess(Long orderId, User systemUser) {
        logger.info("Granting course access for order: {}", orderId);

        // Use findByIdWithItems to eagerly fetch orderItems and courses
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new RuntimeException("Order must be COMPLETED to grant course access");
        }

        int coursesGranted = 0;
        for (OrderItem item : order.getOrderItems()) {
            // Only process course items
            if (item.getProductType() == ProductType.COURSE && item.getCourse() != null) {
                Course course = item.getCourse();
                grantUserCourseAccess(order.getUser(), course);
                coursesGranted++;
                logger.info("Granted access to course: {}", course.getTitle());
            }
        }

        logger.info("Granted access to {} course(s)", coursesGranted);
    }

    /**
     * Grant a specific user access to a course by creating CourseProgress
     */
    private void grantUserCourseAccess(User user, Course course) {
        // Check if user already has access
        Optional<CourseProgress> existing = courseProgressRepository
                .findByUserIdAndCourseId(user.getId(), course.getId());

        if (existing.isPresent()) {
            logger.info("User already has access to course: {}", course.getTitle());
            return;
        }

        // Create new course progress entry
        CourseProgress progress = new CourseProgress();
        progress.setUser(user);
        progress.setCourse(course);
        progress.setCompletionPercentage(BigDecimal.ZERO);
        progress.setIsCompleted(false);
        progress.setLastAccessedAt(LocalDateTime.now());
        progress.setCreatedBy(user.getId());
        progress.setUpdatedBy(user.getId());

        courseProgressRepository.save(progress);
        logger.info("Granted access to course: {} for user: {}", course.getTitle(), user.getEmail());

        // Eagerly initialize User entity to avoid LazyInitializationException in async method
        user.getFirstName();
        user.getLastName();
        user.getRegion();

        // Send enrollment confirmation email
        try {
            courseEnrollmentEmailService.sendCourseEnrollmentConfirmation(
                user,
                course.getTitle(),
                course.getId().toString()
            );
            logger.info("Sent enrollment confirmation email to: {}", user.getEmail());
        } catch (Exception e) {
            logger.error("Failed to send enrollment confirmation email: {}", e.getMessage(), e);
            // Continue - don't fail enrollment just because email failed
        }
    }

    /**
     * Send combined email with both books and courses, and grant course access
     * Used when order contains both product types
     */
    public void sendCombinedOrderEmailAndGrantAccess(Long orderId, User systemUser) {
        logger.info("Processing combined order (books + courses) for order: {}", orderId);

        // Fetch order with items
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new RuntimeException("Order must be COMPLETED to process");
        }

        // Collect books data (same logic as sendBookDownloadEmail)
        java.util.List<java.util.Map<String, Object>> booksData = new java.util.ArrayList<>();
        for (OrderItem item : order.getOrderItems()) {
            if (item.getProductType() == ProductType.BOOK && item.getBook() != null) {
                try {
                    Book book = item.getBook();
                    String downloadUrl = generateSecureDownloadUrl(book, order.getUser());

                    java.util.Map<String, Object> bookData = new java.util.HashMap<>();
                    bookData.put("title", book.getTitle());
                    bookData.put("downloadUrl", downloadUrl);
                    bookData.put("quantity", item.getQuantity());
                    bookData.put("price", item.getSubtotal());
                    bookData.put("fileSizeBytes", book.getFileSizeBytes());

                    booksData.add(bookData);
                    logger.info("Added book to combined email: {}", book.getTitle());
                } catch (Exception e) {
                    logger.error("Failed to process book for item ID: {}", item.getId(), e);
                }
            }
        }

        // Collect courses data AND grant access (without sending individual emails)
        java.util.List<java.util.Map<String, Object>> coursesData = new java.util.ArrayList<>();
        for (OrderItem item : order.getOrderItems()) {
            if (item.getProductType() == ProductType.COURSE && item.getCourse() != null) {
                try {
                    Course course = item.getCourse();

                    // Grant access (create CourseProgress) without sending email
                    grantUserCourseAccessSilent(order.getUser(), course);

                    // Collect course data for email
                    String courseLink = frontendUrl + "/courses/" + course.getId();

                    java.util.Map<String, Object> courseData = new java.util.HashMap<>();
                    courseData.put("title", course.getTitle());
                    courseData.put("courseLink", courseLink);
                    courseData.put("courseId", course.getId().toString());
                    courseData.put("price", item.getSubtotal());

                    coursesData.add(courseData);
                    logger.info("Added course to combined email: {}", course.getTitle());
                } catch (Exception e) {
                    logger.error("Failed to process course for item ID: {}", item.getId(), e);
                }
            }
        }

        if (booksData.isEmpty() && coursesData.isEmpty()) {
            throw new RuntimeException("No valid books or courses found in order");
        }

        // Eagerly initialize User entity to avoid LazyInitializationException
        User user = order.getUser();
        user.getFirstName();
        user.getLastName();
        user.getEmail();
        user.getRegion();

        // Send ONE combined email with everything
        try {
            bookPurchaseEmailService.sendCombinedOrderConfirmation(
                user,
                order.getOrderNumber(),
                booksData,
                coursesData,
                order.getTotalAmount(),
                order.getCurrency().getCode()
            );
            logger.info("Successfully sent combined email with {} book(s) and {} course(s)",
                    booksData.size(), coursesData.size());
        } catch (Exception e) {
            logger.error("Failed to send combined order email: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send combined email: " + e.getMessage());
        }
    }

    /**
     * Grant course access without sending individual enrollment email
     * Used when sending combined email
     */
    private void grantUserCourseAccessSilent(User user, Course course) {
        // Check if user already has access
        Optional<CourseProgress> existing = courseProgressRepository
                .findByUserIdAndCourseId(user.getId(), course.getId());

        if (existing.isPresent()) {
            logger.info("User already has access to course: {}", course.getTitle());
            return;
        }

        // Create new course progress entry
        CourseProgress progress = new CourseProgress();
        progress.setUser(user);
        progress.setCourse(course);
        progress.setCompletionPercentage(BigDecimal.ZERO);
        progress.setIsCompleted(false);
        progress.setLastAccessedAt(LocalDateTime.now());
        progress.setCreatedBy(user.getId());
        progress.setUpdatedBy(user.getId());

        courseProgressRepository.save(progress);
        logger.info("Granted access to course (silent): {}", course.getTitle());
    }

    // Generate unique order number
    private String generateOrderNumber() {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String random = String.valueOf((int) (Math.random() * 1000));
        return "ORD-" + timestamp.substring(timestamp.length() - 8) + "-" + random;
    }

    // Generate secure download URL for book PDF
    private String generateSecureDownloadUrl(Book book, User user) {
        // Check if book has PDF key for S3
        if (book.getPdfKey() == null || book.getPdfKey().isEmpty()) {
            throw new RuntimeException("Book PDF not available: " + book.getTitle());
        }

        // Generate presigned S3 URL that expires in 1 hour
        return s3FileService.generatePresignedUrl(book.getPdfKey(), java.time.Duration.ofHours(1));
    }

    // Convert Order entity to OrderResponse DTO
    private OrderResponse convertToOrderResponse(Order order, User currentUser) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setOrderNumber(order.getOrderNumber());
        response.setUserId(order.getUser().getId());
        response.setUserEmail(order.getUser().getEmail());
        response.setUserName(order.getUser().getFullName());
        response.setStatus(order.getStatus());
        response.setSubtotal(order.getSubtotal());
        response.setTaxAmount(order.getTaxAmount());
        response.setDiscountAmount(order.getDiscountAmount());
        response.setTotalAmount(order.getTotalAmount());
        response.setCurrency(order.getCurrency());
        response.setNotes(order.getNotes());
        response.setCustomerEmail(order.getCustomerEmail());
        response.setCustomerName(order.getCustomerName());
        response.setCustomerPhone(order.getCustomerPhone());
        response.setBillingAddress(order.getBillingAddress());
        response.setShippingAddress(order.getShippingAddress());
        response.setRegion(order.getRegion());
        response.setPaymentMethod(order.getPaymentMethod());
        response.setPaymentGateway(order.getPaymentGateway());
        response.setPaymentGatewayTransactionId(order.getPaymentGatewayTransactionId());
        response.setProcessedAt(order.getProcessedAt());
        response.setCompletedAt(order.getCompletedAt());
        response.setCancelledAt(order.getCancelledAt());
        response.setRefundedAt(order.getRefundedAt());
        response.setRefundAmount(order.getRefundAmount());
        response.setRefundReason(order.getRefundReason());
        response.setCreatedAt(order.getCreatedAt());
        response.setUpdatedAt(order.getUpdatedAt());

        // Add order items
        response.setOrderItems(order.getOrderItems().stream()
                .map(this::convertToOrderItemResponse)
                .collect(Collectors.toList()));

        // Add payments
        response.setPayments(order.getPayments().stream()
                .map(this::convertToPaymentResponse)
                .collect(Collectors.toList()));

        // Add user-specific information
        if (currentUser != null) {
            setUserSpecificOrderInfo(response, order, currentUser);
        }

        return response;
    }

    // Convert OrderItem entity to OrderItemResponse DTO
    private OrderResponse.OrderItemResponse convertToOrderItemResponse(OrderItem orderItem) {
        OrderResponse.OrderItemResponse response = new OrderResponse.OrderItemResponse();
        response.setId(orderItem.getId());
        response.setProductType(orderItem.getProductType().name());

        // Set product-specific fields based on type
        if (orderItem.getProductType() == ProductType.BOOK && orderItem.getBook() != null) {
            response.setBookId(orderItem.getBook().getId());
            response.setBookTitle(orderItem.getBook().getTitle());
            response.setBookCoverImageUrl(orderItem.getBook().getCoverImageUrl());
        } else if (orderItem.getProductType() == ProductType.COURSE && orderItem.getCourse() != null) {
            response.setCourseId(orderItem.getCourse().getId());
            response.setCourseTitle(orderItem.getCourse().getTitle());
            response.setCourseThumbnailUrl(orderItem.getCourse().getThumbnailUrl());
        }

        response.setQuantity(orderItem.getQuantity());
        response.setUnitPrice(orderItem.getUnitPrice());
        response.setSubtotal(orderItem.getSubtotal());
        response.setCurrency(orderItem.getCurrency());
        response.setDiscountAmount(orderItem.getDiscountAmount());
        response.setNotes(orderItem.getNotes());
        return response;
    }

    // Convert Payment entity to PaymentResponse DTO
    private OrderResponse.PaymentResponse convertToPaymentResponse(com.powercity.power_city_platform.entity.Payment payment) {
        OrderResponse.PaymentResponse response = new OrderResponse.PaymentResponse();
        response.setId(payment.getId());
        response.setAmount(payment.getAmount());
        response.setCurrency(payment.getCurrency());
        response.setStatus(payment.getStatus().getValue());
        response.setPaymentMethod(payment.getPaymentMethod());
        response.setPaymentGateway(payment.getPaymentGateway());
        response.setGatewayTransactionId(payment.getGatewayTransactionId());
        response.setGatewayResponseCode(payment.getGatewayResponseCode());
        response.setGatewayResponseMessage(payment.getGatewayResponseMessage());
        response.setGatewayFee(payment.getGatewayFee());
        response.setProcessingFee(payment.getProcessingFee());
        response.setRefundAmount(payment.getRefundAmount());
        response.setRefundReason(payment.getRefundReason());
        response.setProcessedAt(payment.getProcessedAt());
        response.setCompletedAt(payment.getCompletedAt());
        response.setFailedAt(payment.getFailedAt());
        response.setRefundedAt(payment.getRefundedAt());
        response.setNotes(payment.getNotes());
        return response;
    }

    // Set user-specific information for order
    private void setUserSpecificOrderInfo(OrderResponse response, Order order, User currentUser) {
        response.setCanBeCancelled(order.canBeCancelled());
        response.setCanBeRefunded(order.canBeRefunded());

        // Generate download URLs for completed book orders only
        if (order.isCompleted()) {
            List<String> downloadUrls = new ArrayList<>();
            for (OrderItem item : order.getOrderItems()) {
                if (item.getProductType() == ProductType.BOOK && item.getBook() != null && item.getBook().getPdfKey() != null) {
                    // TODO: Implement S3FileService.generatePresignedUrl call
                    downloadUrls.add("/api/v1/books/" + item.getBook().getId() + "/download");
                }
            }
            response.setDownloadUrls(downloadUrls);
        }
    }
}
