package com.powercity.power_city_platform.repository;

import com.powercity.power_city_platform.entity.Donation;
import com.powercity.power_city_platform.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DonationRepository extends JpaRepository<Donation, Long> {

    Optional<Donation> findByPaypalOrderId(String paypalOrderId);

    Optional<Donation> findByPaypalCaptureId(String paypalCaptureId);

    Optional<Donation> findByStripeSessionId(String stripeSessionId);

    Optional<Donation> findByStripePaymentIntentId(String stripePaymentIntentId);

    Page<Donation> findByUserOrderByDonationDateDesc(User user, Pageable pageable);

    Page<Donation> findByDonorEmailOrderByDonationDateDesc(String email, Pageable pageable);
    List<Donation> findByDonorEmailOrderByCreatedAtDesc(String email);

    List<Donation> findAllByOrderByCreatedAtDesc();

    Page<Donation> findByStatusOrderByDonationDateDesc(String status, Pageable pageable);

    // Find donations by date range
    @Query("SELECT d FROM Donation d WHERE d.donationDate BETWEEN :startDate AND :endDate ORDER BY d.donationDate DESC")
    List<Donation> findByDonationDateBetween(@Param("startDate") LocalDateTime startDate,
                                               @Param("endDate") LocalDateTime endDate);

    // Find donations by offering type
    @Query("SELECT d FROM Donation d WHERE d.offeringType = :offeringType ORDER BY d.donationDate DESC")
    Page<Donation> findByOfferingType(@Param("offeringType") com.powercity.power_city_platform.enums.OfferingType offeringType,
                                       Pageable pageable);

    @Query("SELECT SUM(d.amount) FROM Donation d WHERE d.status = 'COMPLETED'")
    Double getTotalDonations();

    @Query("SELECT SUM(d.amount) FROM Donation d WHERE d.status = 'COMPLETED' AND d.offeringType = :offeringType")
    Double getTotalDonationsByOfferingType(@Param("offeringType") com.powercity.power_city_platform.enums.OfferingType offeringType);

    @Query("SELECT COUNT(d) FROM Donation d WHERE d.status = 'COMPLETED'")
    Long getCompletedDonationsCount();

    // Get donations that need receipts sent
    @Query("SELECT d FROM Donation d WHERE d.status = 'COMPLETED' AND d.receiptSent = false")
    List<Donation> findDonationsNeedingReceipts();
}
