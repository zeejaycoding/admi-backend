package com.powercity.power_city_platform.repository;

import com.powercity.power_city_platform.entity.TravelForm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TravelFormRepository extends JpaRepository<TravelForm, Long> {

    List<TravelForm> findByUserIdOrderBySubmittedAtDesc(Long userId);

    List<TravelForm> findAllByOrderBySubmittedAtDesc();

    long countByStatus(String status);

    long countByUserIdAndStatus(Long userId, String status);

    @Query("SELECT COUNT(t) FROM TravelForm t")
    long countTotal();

    @Query("SELECT t FROM TravelForm t WHERE t.status = 'Pending' ORDER BY t.submittedAt DESC")
    List<TravelForm> findPendingApprovals();

    @Query("SELECT t FROM TravelForm t ORDER BY t.submittedAt DESC")
    List<TravelForm> findRecent();

    @Query("SELECT t FROM TravelForm t WHERE t.user.id = :userId ORDER BY t.submittedAt DESC")
    List<TravelForm> findByUserId(Long userId);
}
