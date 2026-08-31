package com.powercity.power_city_platform.controller;

import com.powercity.power_city_platform.dto.request.cart.AddToCartRequest;
import com.powercity.power_city_platform.dto.response.cart.CartResponse;
import com.powercity.power_city_platform.dto.response.common.ApiResponse;
import com.powercity.power_city_platform.entity.User;
import com.powercity.power_city_platform.enums.Currency;
import com.powercity.power_city_platform.service.CartService;
import com.powercity.power_city_platform.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cart")
@Tag(name = "Shopping Cart", description = "APIs for managing shopping cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final UserService userService;

    private static final Logger logger = LoggerFactory.getLogger(CartController.class);

    @PostMapping("/add")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Add item to cart", description = "Add a book or course to the shopping cart")
    public ResponseEntity<ApiResponse<CartResponse>> addToCart(
            @Valid @RequestBody AddToCartRequest request) {
        User currentUser = userService.getCurrentUser();
        CartResponse cart = cartService.addToCart(currentUser, request);
        return ResponseEntity.ok(ApiResponse.success("Item added to cart successfully", cart));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get cart", description = "Get current user's shopping cart")
    public ResponseEntity<ApiResponse<CartResponse>> getCart() {
        User currentUser = userService.getCurrentUser();
        CartResponse cart = cartService.getCart(currentUser);
        return ResponseEntity.ok(ApiResponse.success("Cart retrieved successfully", cart));
    }

    @DeleteMapping("/items/{cartItemId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Remove item from cart", description = "Remove a specific item from the cart")
    public ResponseEntity<ApiResponse<CartResponse>> removeFromCart(
            @Parameter(description = "Cart item ID") @PathVariable Long cartItemId) {
        User currentUser = userService.getCurrentUser();
        CartResponse cart = cartService.removeFromCart(currentUser, cartItemId);
        return ResponseEntity.ok(ApiResponse.success("Item removed from cart successfully", cart));
    }

    @PutMapping("/items/{cartItemId}/quantity")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update item quantity", description = "Update the quantity of an item in the cart")
    public ResponseEntity<ApiResponse<CartResponse>> updateQuantity(
            @Parameter(description = "Cart item ID") @PathVariable Long cartItemId,
            @Parameter(description = "New quantity") @RequestParam Integer quantity) {
        User currentUser = userService.getCurrentUser();
        CartResponse cart = cartService.updateCartItemQuantity(currentUser, cartItemId, quantity);
        return ResponseEntity.ok(ApiResponse.success("Quantity updated successfully", cart));
    }

    @PutMapping("/currency")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update cart currency", description = "Update all cart items to a new currency")
    public ResponseEntity<ApiResponse<CartResponse>> updateCurrency(
            @Parameter(description = "New currency") @RequestParam Currency currency) {
        User currentUser = userService.getCurrentUser();
        CartResponse cart = cartService.updateCartCurrency(currentUser, currency);
        return ResponseEntity.ok(ApiResponse.success("Currency updated successfully", cart));
    }

    @DeleteMapping("/clear")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Clear cart", description = "Remove all items from the cart")
    public ResponseEntity<ApiResponse<Void>> clearCart() {
        User currentUser = userService.getCurrentUser();
        cartService.clearCart(currentUser);
        return ResponseEntity.ok(ApiResponse.success("Cart cleared successfully", null));
    }
}
