package com.powercity.power_city_platform.repository;

import com.powercity.power_city_platform.entity.MarriageCertificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MarriageCertificateRepository extends JpaRepository<MarriageCertificate, Long> {

    List<MarriageCertificate> findByUserIdOrderBySubmittedAtDesc(Long userId);

    List<MarriageCertificate> findAllByOrderBySubmittedAtDesc();

    Optional<MarriageCertificate> findByCertificateNumber(String certificateNumber);

    @Query("SELECT COUNT(m) FROM MarriageCertificate m")
    long countTotal();

    long countByStatus(String status);

    @Query("SELECT COUNT(m) FROM MarriageCertificate m WHERE LOWER(m.campus) = LOWER(:campus)")
    long countByCampus(@Param("campus") String campus);

    @Query("SELECT COUNT(m) FROM MarriageCertificate m WHERE LOWER(m.campus) = LOWER(:campus) AND m.status = :status")
    long countByCampusAndStatus(@Param("campus") String campus, @Param("status") String status);

    @Query("SELECT COUNT(m) FROM MarriageCertificate m WHERE m.user.id = :userId " +
           "OR (:campus IS NOT NULL AND LOWER(m.campus) = LOWER(:campus))")
    long countByUserOrCampus(@Param("userId") Long userId, @Param("campus") String campus);

    @Query("SELECT COUNT(m) FROM MarriageCertificate m WHERE m.status = :status AND (m.user.id = :userId " +
           "OR (:campus IS NOT NULL AND LOWER(m.campus) = LOWER(:campus)))")
    long countByUserOrCampusAndStatus(@Param("userId") Long userId, @Param("campus") String campus, @Param("status") String status);

    @Query("SELECT m FROM MarriageCertificate m WHERE LOWER(m.campus) = LOWER(:campus) ORDER BY m.submittedAt DESC")
    List<MarriageCertificate> findRecentByCampus(@Param("campus") String campus);

    @Query("SELECT m FROM MarriageCertificate m WHERE LOWER(m.campus) = LOWER(:campus) AND m.status = 'Pending' ORDER BY m.submittedAt DESC")
    List<MarriageCertificate> findPendingApprovalsByCampus(@Param("campus") String campus);

    // Coordinator: own submissions or submissions by any coordinator assigned to the
    // same campus. Excludes NL/ADMIN/SUPER_ADMIN creators (only COORDINATOR roles qualify).
    @Query("SELECT m FROM MarriageCertificate m WHERE m.user.id = :userId " +
           "OR m.user.id IN (SELECT u.id FROM User u JOIN u.userRoles ur JOIN ur.role rl " +
           "    WHERE rl.name = 'COORDINATOR' AND ur.isActive = true " +
           "    AND LOWER(u.email) IN (SELECT LOWER(cp.coordinatorEmail) FROM Campus cp " +
           "        WHERE LOWER(cp.name) = LOWER(:campus))) " +
           "ORDER BY m.submittedAt DESC")
    List<MarriageCertificate> findByUserIdOrSameCampusCoordinators(
            @Param("userId") Long userId, @Param("campus") String campus);

    // Coordinator dashboard counts (excludes NL/ADMIN/SUPER_ADMIN).
    @Query("SELECT COUNT(m) FROM MarriageCertificate m WHERE m.user.id = :userId " +
           "OR m.user.id IN (SELECT u.id FROM User u JOIN u.userRoles ur JOIN ur.role rl " +
           "    WHERE rl.name = 'COORDINATOR' AND ur.isActive = true " +
           "    AND LOWER(u.email) IN (SELECT LOWER(cp.coordinatorEmail) FROM Campus cp " +
           "        WHERE LOWER(cp.name) = LOWER(:campus)))")
    long countByUserIdOrSameCampusCoordinators(@Param("userId") Long userId, @Param("campus") String campus);

    @Query("SELECT COUNT(m) FROM MarriageCertificate m WHERE m.status = :status AND (m.user.id = :userId " +
           "OR m.user.id IN (SELECT u.id FROM User u JOIN u.userRoles ur JOIN ur.role rl " +
           "    WHERE rl.name = 'COORDINATOR' AND ur.isActive = true " +
           "    AND LOWER(u.email) IN (SELECT LOWER(cp.coordinatorEmail) FROM Campus cp " +
           "        WHERE LOWER(cp.name) = LOWER(:campus))))")
    long countByUserIdOrSameCampusCoordinatorsAndStatus(@Param("userId") Long userId, @Param("campus") String campus, @Param("status") String status);

    @Query("SELECT m FROM MarriageCertificate m " +
           "WHERE m.user.region = :region " +
           "AND m.user.id NOT IN (SELECT ur.user.id FROM UserRole ur " +
           "    WHERE ur.isActive = true AND ur.role.name IN ('ADMIN','SUPER_ADMIN')) " +
           "ORDER BY m.submittedAt DESC")
    List<MarriageCertificate> findByRegionForNationalLeader(@Param("region") String region);

    @Query("SELECT m FROM MarriageCertificate m ORDER BY m.submittedAt DESC")
    List<MarriageCertificate> findRecent();

    @Query("SELECT COUNT(m) > 0 FROM MarriageCertificate m WHERE LOWER(m.groomName) = LOWER(:groomName) " +
           "AND LOWER(m.brideName) = LOWER(:brideName) AND m.marriageDate = :marriageDate")
    boolean existsDuplicate(@Param("groomName") String groomName,
                            @Param("brideName") String brideName,
                            @Param("marriageDate") LocalDate marriageDate);

    @Query("SELECT COUNT(m) FROM MarriageCertificate m WHERE LOWER(m.campus) = LOWER(:campus) " +
           "AND m.submittedAt >= :start AND m.submittedAt < :end")
    long countByCampusAndSubmittedAtBetween(@Param("campus") String campus,
                                            @Param("start") LocalDateTime start,
                                            @Param("end") LocalDateTime end);
}
