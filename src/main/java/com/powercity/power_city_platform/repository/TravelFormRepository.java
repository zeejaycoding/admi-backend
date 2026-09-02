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

    // Coordinator dashboard: count own + same-campus coordinator submissions (excludes NL/ADMIN/SUPER_ADMIN).
    @Query("SELECT COUNT(t) FROM TravelForm t WHERE t.user.id = :userId " +
           "OR t.user.id IN (SELECT u.id FROM User u JOIN u.userRoles ur JOIN ur.role rl " +
           "    WHERE rl.name = 'COORDINATOR' AND ur.isActive = true " +
           "    AND LOWER(u.email) IN (SELECT LOWER(cp.coordinatorEmail) FROM Campus cp " +
           "        WHERE LOWER(cp.name) = LOWER(:campus)))")
    long countByUserIdOrSameCampusCoordinators(@Param("userId") Long userId, @Param("campus") String campus);

    @Query("SELECT COUNT(t) FROM TravelForm t WHERE t.status = :status AND (t.user.id = :userId " +
           "OR t.user.id IN (SELECT u.id FROM User u JOIN u.userRoles ur JOIN ur.role rl " +
           "    WHERE rl.name = 'COORDINATOR' AND ur.isActive = true " +
           "    AND LOWER(u.email) IN (SELECT LOWER(cp.coordinatorEmail) FROM Campus cp " +
           "        WHERE LOWER(cp.name) = LOWER(:campus))))")
    long countByUserIdOrSameCampusCoordinatorsAndStatus(@Param("userId") Long userId, @Param("campus") String campus, @Param("status") String status);

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

    // Coordinator: own submissions or submissions by any coordinator assigned to the
    // same campus. Excludes NL/ADMIN/SUPER_ADMIN creators (only COORDINATOR roles qualify).
    @Query("SELECT t FROM TravelForm t WHERE t.user.id = :userId " +
           "OR t.user.id IN (SELECT u.id FROM User u JOIN u.userRoles ur JOIN ur.role rl " +
           "    WHERE rl.name = 'COORDINATOR' AND ur.isActive = true " +
           "    AND LOWER(u.email) IN (SELECT LOWER(cp.coordinatorEmail) FROM Campus cp " +
           "        WHERE LOWER(cp.name) = LOWER(:campus))) " +
           "ORDER BY t.submittedAt DESC")
    List<TravelForm> findByUserIdOrSameCampusCoordinators(
            @Param("userId") Long userId, @Param("campus") String campus);

    // National leader: see all travel forms submitted by users of the region,
    // excluding submissions made by ADMIN / SUPER_ADMIN.
    @Query("SELECT t FROM TravelForm t " +
           "WHERE t.user.region = :region " +
           "AND t.user.id NOT IN (SELECT ur.user.id FROM UserRole ur " +
           "    WHERE ur.isActive = true AND ur.role.name IN ('ADMIN','SUPER_ADMIN')) " +
           "ORDER BY t.submittedAt DESC")
    List<TravelForm> findByRegionForNationalLeader(
            @Param("region") String region);

    @Query("SELECT t FROM TravelForm t " +
           "WHERE t.status = 'Pending' AND t.user.region = :region " +
           "AND t.user.id NOT IN (SELECT ur.user.id FROM UserRole ur " +
           "    WHERE ur.isActive = true AND ur.role.name IN ('ADMIN','SUPER_ADMIN')) " +
           "ORDER BY t.submittedAt DESC")
    List<TravelForm> findPendingApprovalsByRegion(@Param("region") String region);

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
