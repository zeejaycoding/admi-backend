package com.powercity.power_city_platform.service;

import com.powercity.power_city_platform.dto.request.cart.AddToCartRequest;
import com.powercity.power_city_platform.dto.response.cart.CartItemResponse;
import com.powercity.power_city_platform.dto.response.cart.CartResponse;
import com.powercity.power_city_platform.entity.Book;
import com.powercity.power_city_platform.entity.Cart;
import com.powercity.power_city_platform.entity.CartItem;
import com.powercity.power_city_platform.entity.Course;
import com.powercity.power_city_platform.entity.User;
import com.powercity.power_city_platform.enums.Currency;
import com.powercity.power_city_platform.enums.ProductType;
import com.powercity.power_city_platform.exception.ResourceNotFoundException;
import com.powercity.power_city_platform.repository.BookRepository;
import com.powercity.power_city_platform.repository.CartItemRepository;
import com.powercity.power_city_platform.repository.CartRepository;
import com.powercity.power_city_platform.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private static final Logger logger = LoggerFactory.getLogger(CartService.class);

    private final CartRepository cartRepository;

    private final CartItemRepository cartItemRepository;

    private final BookRepository bookRepository;

    private final CourseRepository courseRepository;

    /**
     * Get or create cart for user
     */
    @Transactional
    public Cart getOrCreateCart(User user) {
        return cartRepository.findByUser(user)
                .orElseGet(() -> {
                    Cart cart = new Cart(user);
                    return cartRepository.save(cart);
                });
    }

    /**
     * Add item to cart
     * Supports multiple quantities for all product types including digital products (BOOK)
     */
    @Transactional
    public CartResponse addToCart(User user, AddToCartRequest request) {
        Cart cart = getOrCreateCart(user);

        // VALIDATE: Cart can only contain items of a single currency
        if (!cart.getItems().isEmpty()) {
            Currency cartCurrency = cart.getItems().get(0).getCurrency();
            Currency newItemCurrency = request.getCurrency();

            if (!cartCurrency.equals(newItemCurrency)) {
                String currencySymbol = cartCurrency == Currency.NGN ? "₦" : "$";
                String newCurrencySymbol = newItemCurrency == Currency.NGN ? "₦" : "$";
                throw new RuntimeException(
                    String.format("Cannot mix currencies! Your cart contains %s%s items. " +
                                "Please checkout or clear your cart to add %s%s items.",
                                currencySymbol, cartCurrency.name(),
                                newCurrencySymbol, newItemCurrency.name())
                );
            }
        }

        // Check if item already exists in cart
        var existingItem = cartItemRepository.findByCartAndProductIdAndProductType(
                cart, request.getProductId(), request.getProductType()
        );

        if (existingItem.isPresent()) {
            // Item already in cart
            CartItem item = existingItem.get();

            // Courses are access-based, don't increase quantity
            if (request.getProductType() == ProductType.COURSE) {
                throw new RuntimeException("Course already in cart. You can only purchase one enrollment.");
            }

            // For books, increase quantity
            item.setQuantity(item.getQuantity() + request.getQuantity());
            cartItemRepository.save(item);
        } else {
            // Create new cart item
            String productTitle = getProductTitle(request.getProductId(), request.getProductType());
            String productImageUrl = getProductImageUrl(request.getProductId(), request.getProductType());

            CartItem newItem = new CartItem(
                    request.getProductId(),
                    request.getProductType(),
                    productTitle,
                    productImageUrl,
                    request.getPrice(),
                    request.getCurrency()
            );

            // Courses are access-based, always quantity 1
            int quantity = request.getProductType() == ProductType.COURSE ? 1 : request.getQuantity();
            newItem.setQuantity(quantity);

            cart.addItem(newItem);
            cartRepository.save(cart);
        }

        return convertToCartResponse(cart);
    }

    /**
     * Remove item from cart
     */
    @Transactional
    public CartResponse removeFromCart(User user, Long cartItemId) {
        Cart cart = getOrCreateCart(user);

        CartItem itemToRemove = cart.getItems().stream()
                .filter(item -> item.getId().equals(cartItemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

        cart.removeItem(itemToRemove);
        cartItemRepository.delete(itemToRemove);

        return convertToCartResponse(cart);
    }

    /**
     * Update item quantity
     */
    @Transactional
    public CartResponse updateCartItemQuantity(User user, Long cartItemId, Integer quantity) {
        Cart cart = getOrCreateCart(user);

        CartItem item = cart.getItems().stream()
                .filter(i -> i.getId().equals(cartItemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

        if (quantity <= 0) {
            // Remove item if quantity is 0 or negative
            cart.removeItem(item);
            cartItemRepository.delete(item);
        } else {
            // Update quantity
            item.setQuantity(quantity);
            cartItemRepository.save(item);
        }

        return convertToCartResponse(cart);
    }

    /**
     * Get user's cart
     */
    @Transactional(readOnly = true)
    public CartResponse getCart(User user) {
        Cart cart = getOrCreateCart(user);
        return convertToCartResponse(cart);
    }

    /**
     * Clear cart
     */
    @Transactional
    public void clearCart(User user) {
        Cart cart = getOrCreateCart(user);
        cart.clearItems();
        cartRepository.save(cart);
    }

    /**
     * Update cart currency (convert all items to new currency)
     */
    @Transactional
    public CartResponse updateCartCurrency(User user, Currency newCurrency) {
        Cart cart = getOrCreateCart(user);

        for (CartItem item : cart.getItems()) {
            // Fetch fresh price in new currency from product
            BigDecimal newPrice = getProductPrice(item.getProductId(), item.getProductType(), newCurrency);
            item.setPrice(newPrice);
            item.setCurrency(newCurrency);
        }

        cartRepository.save(cart);
        return convertToCartResponse(cart);
    }

    /**
     * Convert Cart entity to CartResponse DTO
     */
    private CartResponse convertToCartResponse(Cart cart) {
        List<CartItemResponse> itemResponses = cart.getItems().stream()
                .map(this::convertToCartItemResponse)
                .collect(Collectors.toList());

        BigDecimal totalAmount = cart.getItems().stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String currency = cart.getItems().isEmpty() ? "USD" :
                cart.getItems().get(0).getCurrency().name();

        return new CartResponse(
                cart.getId(),
                itemResponses,
                cart.getTotalItems(),
                totalAmount,
                currency
        );
    }

    /**
     * Convert CartItem entity to CartItemResponse DTO
     */
    private CartItemResponse convertToCartItemResponse(CartItem item) {
        return new CartItemResponse(
                item.getId(),
                item.getProductId(),
                item.getProductType().name(),
                item.getProductTitle(),
                item.getProductImageUrl(),
                item.getPrice(),
                item.getCurrency().name(),
                item.getQuantity(),
                item.getSubtotal()
        );
    }

    /**
     * Get product title by ID and type
     */
    private String getProductTitle(Long productId, ProductType productType) {
        if (productType == ProductType.BOOK) {
            return bookRepository.findById(productId)
                    .map(Book::getTitle)
                    .orElse("Unknown Book");
        } else if (productType == ProductType.COURSE) {
            return courseRepository.findById(productId)
                    .map(Course::getTitle)
                    .orElse("Unknown Course");
        }
        return "Unknown Product";
    }

    /**
     * Get product image URL by ID and type
     */
    private String getProductImageUrl(Long productId, ProductType productType) {
        if (productType == ProductType.BOOK) {
            return bookRepository.findById(productId)
                    .map(Book::getCoverImageUrl)
                    .orElse(null);
        } else if (productType == ProductType.COURSE) {
            return courseRepository.findById(productId)
                    .map(Course::getThumbnailUrl)
                    .orElse(null);
        }
        return null;
    }

    /**
     * Get product price in specified currency
     */
    private BigDecimal getProductPrice(Long productId, ProductType productType, Currency currency) {
        if (productType == ProductType.BOOK) {
            Book book = bookRepository.findById(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Book not found"));

            // Return appropriate price based on currency
            if (currency == Currency.NGN && book.getNgnPrice() != null) {
                return book.getNgnPrice();
            }
            return book.getBasePrice();
        } else if (productType == ProductType.COURSE) {
            Course course = courseRepository.findById(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Course not found"));

            // Return appropriate price based on currency
            if (currency == Currency.NGN && course.getNgnPrice() != null) {
                return course.getNgnPrice();
            }
            return course.getBasePrice();
        }
        return BigDecimal.ZERO;
    }

    /**
     * Remove purchased items from cart (called after order completion)
     */
    @Transactional
    public void removePurchasedItemsFromCart(User user, java.util.List<com.powercity.power_city_platform.entity.OrderItem> orderItems) {
        Cart cart = cartRepository.findByUser(user).orElse(null);
        if (cart == null) {
            return; // No cart to clean up
        }

        // Remove items from cart that match the purchased items
        for (com.powercity.power_city_platform.entity.OrderItem orderItem : orderItems) {
            if (orderItem.getProductType() == ProductType.BOOK && orderItem.getBook() != null) {
                cart.getItems().removeIf(cartItem ->
                    cartItem.getProductId().equals(orderItem.getBook().getId()) &&
                    cartItem.getProductType() == ProductType.BOOK
                );
            } else if (orderItem.getProductType() == ProductType.COURSE && orderItem.getCourse() != null) {
                cart.getItems().removeIf(cartItem ->
                    cartItem.getProductId().equals(orderItem.getCourse().getId()) &&
                    cartItem.getProductType() == ProductType.COURSE
                );
            }
        }

        cartRepository.save(cart);
    }
}
