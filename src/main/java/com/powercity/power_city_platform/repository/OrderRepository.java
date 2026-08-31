package com.powercity.power_city_platform.repository;

import com.powercity.power_city_platform.entity.Order;
import com.powercity.power_city_platform.entity.User;
import com.powercity.power_city_platform.enums.Currency;
import com.powercity.power_city_platform.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderNumber(String orderNumber);

    // Find order with items and products eagerly loaded (for webhook processing)
    @Query("SELECT DISTINCT o FROM Order o " +
           "LEFT JOIN FETCH o.orderItems oi " +
           "LEFT JOIN FETCH oi.course " +
           "LEFT JOIN FETCH oi.book " +
           "WHERE o.id = :id")
    Optional<Order> findByIdWithItems(@Param("id") Long id);
    
    List<Order> findByUserOrderByCreatedAtDesc(User user);
    Page<Order> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    List<Order> findByStatus(OrderStatus status);
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    List<Order> findByUserAndStatus(User user, OrderStatus status);
    Page<Order> findByUserAndStatus(User user, OrderStatus status, Pageable pageable);

    List<Order> findByCustomerEmailOrderByCreatedAtDesc(String customerEmail);
    Page<Order> findByCustomerEmailOrderByCreatedAtDesc(String customerEmail, Pageable pageable);

    List<Order> findByRegionOrderByCreatedAtDesc(String region);
    Page<Order> findByRegionOrderByCreatedAtDesc(String region, Pageable pageable);

    List<Order> findByCurrencyOrderByCreatedAtDesc(Currency currency);
    Page<Order> findByCurrencyOrderByCreatedAtDesc(Currency currency, Pageable pageable);

    List<Order> findByPaymentGatewayOrderByCreatedAtDesc(String paymentGateway);
    Page<Order> findByPaymentGatewayOrderByCreatedAtDesc(String paymentGateway, Pageable pageable);
    
    @Query("SELECT o FROM Order o WHERE o.createdAt BETWEEN :startDate AND :endDate")
    List<Order> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT o FROM Order o WHERE o.createdAt BETWEEN :startDate AND :endDate")
    Page<Order> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.totalAmount BETWEEN :minAmount AND :maxAmount")
    List<Order> findByTotalAmountBetween(@Param("minAmount") BigDecimal minAmount, @Param("maxAmount") BigDecimal maxAmount);

    @Query("SELECT o FROM Order o WHERE o.totalAmount BETWEEN :minAmount AND :maxAmount")
    Page<Order> findByTotalAmountBetween(@Param("minAmount") BigDecimal minAmount, @Param("maxAmount") BigDecimal maxAmount, Pageable pageable);
    
    // Search orders by multiple criteria
    @Query("SELECT o FROM Order o WHERE " +
           "(LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(o.customerName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(o.customerEmail) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<Order> searchOrders(@Param("searchTerm") String searchTerm, Pageable pageable);
    
    // Analytics queries
    @Query("SELECT o FROM Order o WHERE o.status = 'COMPLETED' ORDER BY o.totalAmount DESC")
    List<Order> findTopValueOrders(Pageable pageable);
    
    @Query("SELECT o FROM Order o WHERE o.status = 'COMPLETED' ORDER BY o.createdAt DESC")
    List<Order> findRecentCompletedOrders(Pageable pageable);
    
    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = :status")
    Long countByStatus(@Param("status") OrderStatus status);
    
    @Query("SELECT COUNT(o) FROM Order o WHERE o.user = :user")
    Long countByUser(@Param("user") User user);
    
    @Query("SELECT COUNT(o) FROM Order o WHERE o.user = :user AND o.status = :status")
    Long countByUserAndStatus(@Param("user") User user, @Param("status") OrderStatus status);
    
    @Query("SELECT COUNT(o) FROM Order o WHERE o.region = :region")
    Long countByRegion(@Param("region") String region);
    
    @Query("SELECT COUNT(o) FROM Order o WHERE o.currency = :currency")
    Long countByCurrency(@Param("currency") Currency currency);
    
    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.status = 'COMPLETED'")
    BigDecimal getTotalRevenue();
    
    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.status = 'COMPLETED' AND o.user = :user")
    BigDecimal getTotalRevenueByUser(@Param("user") User user);
    
    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.status = 'COMPLETED' AND o.region = :region")
    BigDecimal getTotalRevenueByRegion(@Param("region") String region);
    
    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.status = 'COMPLETED' AND o.currency = :currency")
    BigDecimal getTotalRevenueByCurrency(@Param("currency") Currency currency);
    
    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.status = 'COMPLETED' AND o.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal getTotalRevenueByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT AVG(o.totalAmount) FROM Order o WHERE o.status = 'COMPLETED'")
    BigDecimal getAverageOrderValue();
    
    @Query("SELECT AVG(o.totalAmount) FROM Order o WHERE o.status = 'COMPLETED' AND o.user = :user")
    BigDecimal getAverageOrderValueByUser(@Param("user") User user);
    
    @Query("SELECT AVG(o.totalAmount) FROM Order o WHERE o.status = 'COMPLETED' AND o.region = :region")
    BigDecimal getAverageOrderValueByRegion(@Param("region") String region);
    
    @Query("SELECT o.status, COUNT(o) FROM Order o GROUP BY o.status")
    List<Object[]> getOrderCountByStatus();

    @Query("SELECT o.region, COUNT(o) FROM Order o GROUP BY o.region")
    List<Object[]> getOrderCountByRegion();

    @Query("SELECT o.currency, COUNT(o) FROM Order o GROUP BY o.currency")
    List<Object[]> getOrderCountByCurrency();

    @Query("SELECT o.paymentGateway, COUNT(o) FROM Order o GROUP BY o.paymentGateway")
    List<Object[]> getOrderCountByPaymentGateway();

    // Monthly revenue
    @Query("SELECT YEAR(o.createdAt), MONTH(o.createdAt), SUM(o.totalAmount) FROM Order o " +
           "WHERE o.status = 'COMPLETED' GROUP BY YEAR(o.createdAt), MONTH(o.createdAt) " +
           "ORDER BY YEAR(o.createdAt) DESC, MONTH(o.createdAt) DESC")
    List<Object[]> getMonthlyRevenue();
    
    @Query("SELECT DATE(o.createdAt), SUM(o.totalAmount) FROM Order o " +
           "WHERE o.status = 'COMPLETED' GROUP BY DATE(o.createdAt) " +
           "ORDER BY DATE(o.createdAt) DESC")
    List<Object[]> getDailyRevenue();
    
    boolean existsByOrderNumber(String orderNumber);

    List<Order> findByIdIn(List<Long> ids);

    List<Order> findByStatusIn(List<OrderStatus> statuses);

    List<Order> findByRegionIn(List<String> regions);

    List<Order> findByCurrencyIn(List<Currency> currencies);

    Optional<Order> findByPaymentGatewayTransactionId(String transactionId);

    @Query("SELECT o FROM Order o WHERE o.status = 'PENDING' AND o.createdAt < :date")
    List<Order> findPendingOrdersOlderThan(@Param("date") LocalDateTime date);

    @Query("SELECT o FROM Order o WHERE o.status IN ('PENDING', 'PROCESSING')")
    List<Order> findCancellableOrders();

    @Query("SELECT o FROM Order o WHERE o.status = 'COMPLETED'")
    List<Order> findRefundableOrders();
}
