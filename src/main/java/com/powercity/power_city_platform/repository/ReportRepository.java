package com.powercity.power_city_platform.repository;

import com.powercity.power_city_platform.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    List<Report> findByCoordinator(String coordinator);
    List<Report> findByCreatedBy(Long createdBy);

    @Query("SELECT COUNT(r) FROM Report r WHERE LOWER(r.campus) = LOWER(:campus)")
    long countByCampus(@Param("campus") String campus);

    @Query("SELECT COUNT(r) FROM Report r WHERE LOWER(r.campus) = LOWER(:campus) " +
           "AND r.createdAt >= :start AND r.createdAt < :end")
    long countByCampusAndCreatedAtBetween(@Param("campus") String campus,
                                          @Param("start") LocalDateTime start,
                                          @Param("end") LocalDateTime end);

    @Query("SELECT r FROM Report r WHERE LOWER(r.campus) = LOWER(:campus) ORDER BY r.createdAt DESC")
    List<Report> findRecentByCampus(@Param("campus") String campus);

    @Query("SELECT r FROM Report r WHERE r.createdBy = :userId " +
           "OR r.campus IN (SELECT cp.name FROM Campus cp WHERE cp.region = :region) " +
           "ORDER BY r.createdAt DESC")
    List<Report> findByUserOrCampusRegionOrderByCreatedAtDesc(
            @Param("userId") Long userId, @Param("region") String region);

    @Query("SELECT r FROM Report r " +
           "WHERE (r.createdBy = :userId " +
           "       OR r.createdBy IN (SELECT u.id FROM User u " +
           "           JOIN u.userRoles ur JOIN ur.role rl " +
           "           WHERE rl.name = 'COORDINATOR' AND ur.isActive = true " +
           "           AND LOWER(u.email) IN (SELECT LOWER(cp.coordinatorEmail) FROM Campus cp " +
           "               WHERE LOWER(cp.name) = LOWER(:campus)))) " +
           "ORDER BY r.createdAt DESC")
    List<Report> findByCreatedByOrSameCampusCoordinators(
            @Param("userId") Long userId, @Param("campus") String campus);

    // National leader: see reports of their region (country) by coordinators / NLs,
    // but never reports created by ADMIN or SUPER_ADMIN.
    @Query("SELECT r FROM Report r " +
           "WHERE (r.createdBy = :userId " +
           "       OR (LOWER(r.country) = LOWER(:region) " +
           "           AND r.createdBy NOT IN (SELECT ur.user.id FROM UserRole ur " +
           "               WHERE ur.isActive = true AND ur.role.name IN ('ADMIN','SUPER_ADMIN')))) " +
           "ORDER BY r.createdAt DESC")
    List<Report> findByRegionForNationalLeader(
            @Param("userId") Long userId, @Param("region") String region);
}
