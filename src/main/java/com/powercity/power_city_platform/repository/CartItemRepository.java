package com.powercity.power_city_platform.repository;

import com.powercity.power_city_platform.entity.Cart;
import com.powercity.power_city_platform.entity.CartItem;
import com.powercity.power_city_platform.enums.ProductType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    Optional<CartItem> findByCartAndProductIdAndProductType(Cart cart, Long productId, ProductType productType);

    void deleteByCart(Cart cart);

    // Remove a product from every cart (used when a course/book is permanently deleted).
    void deleteByProductIdAndProductType(Long productId, ProductType productType);
}
