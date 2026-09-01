package com.powercity.power_city_platform.repository;

import com.powercity.power_city_platform.entity.ChildDedication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChildDedicationRepository extends JpaRepository<ChildDedication, Long> {

    List<ChildDedication> findByUserIdOrderBySubmittedAtDesc(Long userId);

    List<ChildDedication> findAllByOrderBySubmittedAtDesc();

    @Query("SELECT c FROM ChildDedication c WHERE c.campus IN " +
           "(SELECT cp.name FROM Campus cp WHERE cp.region = :region) " +
           "ORDER BY c.submittedAt DESC")
    List<ChildDedication> findByCampusRegionOrderBySubmittedAtDesc(@Param("region") String region);

    @Query("SELECT c FROM ChildDedication c WHERE c.user.id = :userId " +
           "OR c.campus IN (SELECT cp.name FROM Campus cp WHERE cp.region = :region) " +
           "ORDER BY c.submittedAt DESC")
    List<ChildDedication> findByUserOrCampusRegionOrderBySubmittedAtDesc(
            @Param("userId") Long userId, @Param("region") String region);

    long countByStatus(String status);

    long countByUserIdAndStatus(Long userId, String status);

    @Query("SELECT COUNT(c) FROM ChildDedication c WHERE LOWER(c.campus) = LOWER(:campus)")
    long countByCampus(@Param("campus") String campus);

    @Query("SELECT COUNT(c) FROM ChildDedication c WHERE LOWER(c.campus) = LOWER(:campus) AND c.status = :status")
    long countByCampusAndStatus(@Param("campus") String campus, @Param("status") String status);

    @Query("SELECT COUNT(c) FROM ChildDedication c WHERE c.user.id = :userId " +
           "OR (:campus IS NOT NULL AND LOWER(c.campus) = LOWER(:campus))")
    long countByUserOrCampus(@Param("userId") Long userId, @Param("campus") String campus);

    @Query("SELECT COUNT(c) FROM ChildDedication c WHERE c.status = :status AND (c.user.id = :userId " +
           "OR (:campus IS NOT NULL AND LOWER(c.campus) = LOWER(:campus)))")
    long countByUserOrCampusAndStatus(@Param("userId") Long userId, @Param("campus") String campus, @Param("status") String status);

    @Query("SELECT c FROM ChildDedication c WHERE LOWER(c.campus) = LOWER(:campus) AND c.status = 'Pending' ORDER BY c.submittedAt DESC")
    List<ChildDedication> findPendingApprovalsByCampus(@Param("campus") String campus);

    @Query("SELECT c FROM ChildDedication c WHERE LOWER(c.campus) = LOWER(:campus) ORDER BY c.submittedAt DESC")
    List<ChildDedication> findRecentByCampus(@Param("campus") String campus);

    @Query("SELECT COUNT(c) FROM ChildDedication c")
    long countTotal();

    @Query("SELECT c FROM ChildDedication c WHERE c.status = 'Pending' ORDER BY c.submittedAt DESC")
    List<ChildDedication> findPendingApprovals();

    @Query("SELECT c FROM ChildDedication c ORDER BY c.submittedAt DESC")
    List<ChildDedication> findRecent();

    Optional<ChildDedication> findByCertificateNumber(String certificateNumber);

    @Query("SELECT COUNT(c) > 0 FROM ChildDedication c WHERE LOWER(c.childName) = LOWER(:childName) " +
           "AND c.dedicationDate = :dedicationDate AND c.parentName = :parentName")
    boolean existsDuplicate(@Param("childName") String childName,
                            @Param("dedicationDate") java.time.LocalDate dedicationDate,
                            @Param("parentName") String parentName);
}
