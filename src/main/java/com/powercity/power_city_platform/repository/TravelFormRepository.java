package com.powercity.power_city_platform.repository;

import com.powercity.power_city_platform.entity.TravelForm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TravelFormRepository extends JpaRepository<TravelForm, Long> {

    List<TravelForm> findByUserIdOrderBySubmittedAtDesc(Long userId);

    List<TravelForm> findAllByOrderBySubmittedAtDesc();

    @Query("SELECT t FROM TravelForm t WHERE LOWER(t.campus) = LOWER(:campus) ORDER BY t.submittedAt DESC")
    List<TravelForm> findByCampusIgnoreCaseOrderBySubmittedAtDesc(String campus);

    @Query("SELECT COUNT(t) FROM TravelForm t WHERE LOWER(t.campus) = LOWER(:campus)")
    long countByCampus(@Param("campus") String campus);

    @Query("SELECT COUNT(t) FROM TravelForm t WHERE LOWER(t.campus) = LOWER(:campus) AND t.status = :status")
    long countByCampusAndStatus(@Param("campus") String campus, @Param("status") String status);

    @Query("SELECT COUNT(t) FROM TravelForm t WHERE t.user.id = :userId " +
           "OR (:campus IS NOT NULL AND LOWER(t.campus) = LOWER(:campus))")
    long countByUserOrCampus(@Param("userId") Long userId, @Param("campus") String campus);

    @Query("SELECT COUNT(t) FROM TravelForm t WHERE t.status = :status AND (t.user.id = :userId " +
           "OR (:campus IS NOT NULL AND LOWER(t.campus) = LOWER(:campus)))")
    long countByUserOrCampusAndStatus(@Param("userId") Long userId, @Param("campus") String campus, @Param("status") String status);

    @Query("SELECT t FROM TravelForm t WHERE LOWER(t.campus) = LOWER(:campus) AND t.status = 'Pending' ORDER BY t.submittedAt DESC")
    List<TravelForm> findPendingApprovalsByCampus(@Param("campus") String campus);

    @Query("SELECT t FROM TravelForm t WHERE LOWER(t.campus) = LOWER(:campus) ORDER BY t.submittedAt DESC")
    List<TravelForm> findRecentByCampus(@Param("campus") String campus);

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

    @Query("SELECT COUNT(t) FROM TravelForm t WHERE LOWER(t.campus) = LOWER(:campus) " +
           "AND t.submittedAt >= :start AND t.submittedAt < :end")
    long countByCampusAndSubmittedAtBetween(@Param("campus") String campus,
                                            @Param("start") LocalDateTime start,
                                            @Param("end") LocalDateTime end);
}
