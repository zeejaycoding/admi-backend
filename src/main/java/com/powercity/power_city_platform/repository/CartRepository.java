package com.powercity.power_city_platform.repository;

import com.powercity.power_city_platform.entity.Cart;
import com.powercity.power_city_platform.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findByUser(User user);

    Optional<Cart> findBySessionId(String sessionId);

    void deleteByUser(User user);
}
