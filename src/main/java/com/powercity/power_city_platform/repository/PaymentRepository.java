package com.powercity.power_city_platform.repository;

import com.powercity.power_city_platform.entity.Order;
import com.powercity.power_city_platform.entity.Payment;
import com.powercity.power_city_platform.entity.User;
import com.powercity.power_city_platform.enums.Currency;
import com.powercity.power_city_platform.enums.PaymentStatus;
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
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByGatewayTransactionId(String gatewayTransactionId);

    List<Payment> findByOrderOrderByCreatedAtDesc(Order order);
    Page<Payment> findByOrderOrderByCreatedAtDesc(Order order, Pageable pageable);

    List<Payment> findByUserOrderByCreatedAtDesc(User user);
    Page<Payment> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    List<Payment> findByStatus(PaymentStatus status);
    Page<Payment> findByStatus(PaymentStatus status, Pageable pageable);

    List<Payment> findByUserAndStatus(User user, PaymentStatus status);
    Page<Payment> findByUserAndStatus(User user, PaymentStatus status, Pageable pageable);

    List<Payment> findByPaymentMethodOrderByCreatedAtDesc(String paymentMethod);
    Page<Payment> findByPaymentMethodOrderByCreatedAtDesc(String paymentMethod, Pageable pageable);

    List<Payment> findByPaymentGatewayOrderByCreatedAtDesc(String paymentGateway);
    Page<Payment> findByPaymentGatewayOrderByCreatedAtDesc(String paymentGateway, Pageable pageable);

    List<Payment> findByCurrencyOrderByCreatedAtDesc(Currency currency);
    Page<Payment> findByCurrencyOrderByCreatedAtDesc(Currency currency, Pageable pageable);
    
    @Query("SELECT p FROM Payment p WHERE p.amount BETWEEN :minAmount AND :maxAmount")
    List<Payment> findByAmountBetween(@Param("minAmount") BigDecimal minAmount, @Param("maxAmount") BigDecimal maxAmount);

    @Query("SELECT p FROM Payment p WHERE p.amount BETWEEN :minAmount AND :maxAmount")
    Page<Payment> findByAmountBetween(@Param("minAmount") BigDecimal minAmount, @Param("maxAmount") BigDecimal maxAmount, Pageable pageable);

    @Query("SELECT p FROM Payment p WHERE p.createdAt BETWEEN :startDate AND :endDate")
    List<Payment> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT p FROM Payment p WHERE p.createdAt BETWEEN :startDate AND :endDate")
    Page<Payment> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate, Pageable pageable);
    
    // Search payments by multiple criteria
    @Query("SELECT p FROM Payment p WHERE " +
           "(LOWER(p.gatewayTransactionId) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(p.paymentMethod) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(p.paymentGateway) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<Payment> searchPayments(@Param("searchTerm") String searchTerm, Pageable pageable);
    
    // Analytics queries
    @Query("SELECT p FROM Payment p WHERE p.status = 'COMPLETED' ORDER BY p.amount DESC")
    List<Payment> findTopValuePayments(Pageable pageable);
    
    @Query("SELECT p FROM Payment p WHERE p.status = 'COMPLETED' ORDER BY p.createdAt DESC")
    List<Payment> findRecentCompletedPayments(Pageable pageable);
    
    @Query("SELECT p FROM Payment p WHERE p.status = 'FAILED' ORDER BY p.createdAt DESC")
    List<Payment> findRecentFailedPayments(Pageable pageable);
    
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.status = :status")
    Long countByStatus(@Param("status") PaymentStatus status);
    
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.user = :user")
    Long countByUser(@Param("user") User user);
    
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.user = :user AND p.status = :status")
    Long countByUserAndStatus(@Param("user") User user, @Param("status") PaymentStatus status);
    
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.order = :order")
    Long countByOrder(@Param("order") Order order);
    
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.paymentGateway = :paymentGateway")
    Long countByPaymentGateway(@Param("paymentGateway") String paymentGateway);
    
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.paymentMethod = :paymentMethod")
    Long countByPaymentMethod(@Param("paymentMethod") String paymentMethod);
    
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.currency = :currency")
    Long countByCurrency(@Param("currency") Currency currency);
    
    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = 'COMPLETED'")
    BigDecimal getTotalRevenue();
    
    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = 'COMPLETED' AND p.user = :user")
    BigDecimal getTotalRevenueByUser(@Param("user") User user);
    
    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = 'COMPLETED' AND p.order = :order")
    BigDecimal getTotalRevenueByOrder(@Param("order") Order order);
    
    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = 'COMPLETED' AND p.paymentGateway = :paymentGateway")
    BigDecimal getTotalRevenueByPaymentGateway(@Param("paymentGateway") String paymentGateway);
    
    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = 'COMPLETED' AND p.currency = :currency")
    BigDecimal getTotalRevenueByCurrency(@Param("currency") Currency currency);
    
    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = 'COMPLETED' AND p.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal getTotalRevenueByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT SUM(p.gatewayFee) FROM Payment p WHERE p.status = 'COMPLETED'")
    BigDecimal getTotalGatewayFees();
    
    @Query("SELECT SUM(p.processingFee) FROM Payment p WHERE p.status = 'COMPLETED'")
    BigDecimal getTotalProcessingFees();
    
    @Query("SELECT SUM(p.refundAmount) FROM Payment p WHERE p.status = 'REFUNDED'")
    BigDecimal getTotalRefunds();
    
    @Query("SELECT AVG(p.amount) FROM Payment p WHERE p.status = 'COMPLETED'")
    BigDecimal getAveragePaymentValue();
    
    @Query("SELECT AVG(p.amount) FROM Payment p WHERE p.status = 'COMPLETED' AND p.user = :user")
    BigDecimal getAveragePaymentValueByUser(@Param("user") User user);
    
    @Query("SELECT AVG(p.amount) FROM Payment p WHERE p.status = 'COMPLETED' AND p.paymentGateway = :paymentGateway")
    BigDecimal getAveragePaymentValueByPaymentGateway(@Param("paymentGateway") String paymentGateway);
    
    @Query("SELECT p.status, COUNT(p) FROM Payment p GROUP BY p.status")
    List<Object[]> getPaymentCountByStatus();

    @Query("SELECT p.paymentGateway, COUNT(p) FROM Payment p GROUP BY p.paymentGateway")
    List<Object[]> getPaymentCountByPaymentGateway();

    @Query("SELECT p.paymentMethod, COUNT(p) FROM Payment p GROUP BY p.paymentMethod")
    List<Object[]> getPaymentCountByPaymentMethod();

    @Query("SELECT p.currency, COUNT(p) FROM Payment p GROUP BY p.currency")
    List<Object[]> getPaymentCountByCurrency();

    // Monthly revenue
    @Query("SELECT YEAR(p.createdAt), MONTH(p.createdAt), SUM(p.amount) FROM Payment p " +
           "WHERE p.status = 'COMPLETED' GROUP BY YEAR(p.createdAt), MONTH(p.createdAt) " +
           "ORDER BY YEAR(p.createdAt) DESC, MONTH(p.createdAt) DESC")
    List<Object[]> getMonthlyRevenue();
    
    @Query("SELECT DATE(p.createdAt), SUM(p.amount) FROM Payment p " +
           "WHERE p.status = 'COMPLETED' GROUP BY DATE(p.createdAt) " +
           "ORDER BY DATE(p.createdAt) DESC")
    List<Object[]> getDailyRevenue();
    
    boolean existsByGatewayTransactionId(String gatewayTransactionId);

    List<Payment> findByIdIn(List<Long> ids);

    List<Payment> findByStatusIn(List<PaymentStatus> statuses);

    List<Payment> findByPaymentGatewayIn(List<String> paymentGateways);

    List<Payment> findByPaymentMethodIn(List<String> paymentMethods);

    List<Payment> findByCurrencyIn(List<Currency> currencies);

    @Query("SELECT p FROM Payment p WHERE p.status = 'FAILED'")
    List<Payment> findFailedPayments();

    @Query("SELECT p FROM Payment p WHERE p.status = 'PENDING' AND p.createdAt < :date")
    List<Payment> findPendingPaymentsOlderThan(@Param("date") LocalDateTime date);

    @Query("SELECT p FROM Payment p WHERE p.status = 'COMPLETED' AND p.refundAmount < p.amount")
    List<Payment> findRefundablePayments();

    List<Payment> findByGatewayResponseCode(String responseCode);

    @Query("SELECT p FROM Payment p WHERE LOWER(p.gatewayResponseMessage) LIKE LOWER(CONCAT('%', :message, '%'))")
    List<Payment> findByGatewayResponseMessageContaining(@Param("message") String message);

    @Query("SELECT p FROM Payment p WHERE p.order = :order AND p.status = 'COMPLETED'")
    List<Payment> findSuccessfulPaymentsByOrder(@Param("order") Order order);

    @Query("SELECT p FROM Payment p WHERE p.order = :order ORDER BY p.createdAt DESC")
    List<Payment> findAllPaymentsByOrder(@Param("order") Order order);
}
