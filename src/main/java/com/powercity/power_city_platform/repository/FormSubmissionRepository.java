package com.powercity.power_city_platform.repository;

import com.powercity.power_city_platform.entity.Form;
import com.powercity.power_city_platform.entity.FormSubmission;
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
public interface FormSubmissionRepository extends JpaRepository<FormSubmission, Long> {

    List<FormSubmission> findByFormOrderBySubmittedAtDesc(Form form);
    Page<FormSubmission> findByFormOrderBySubmittedAtDesc(Form form, Pageable pageable);
    Page<FormSubmission> findByForm(Form form, Pageable pageable);

    List<FormSubmission> findByUserOrderBySubmittedAtDesc(User user);
    Page<FormSubmission> findByUser(User user, Pageable pageable);

    List<FormSubmission> findByFormAndUserOrderBySubmittedAtDesc(Form form, User user);

    boolean existsByFormAndUser(Form form, User user);

    Long countByForm(Form form);

    // Date range queries for analytics
    @Query("SELECT fs FROM FormSubmission fs WHERE fs.form = :form AND fs.submittedAt BETWEEN :startDate AND :endDate ORDER BY fs.submittedAt DESC")
    List<FormSubmission> findByFormAndDateRange(
        @Param("form") Form form,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT COUNT(fs) FROM FormSubmission fs WHERE fs.form = :form AND fs.submittedAt BETWEEN :startDate AND :endDate")
    Long countByFormAndDateRange(
        @Param("form") Form form,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    Page<FormSubmission> findAllByOrderBySubmittedAtDesc(Pageable pageable);

    Page<FormSubmission> findAllByArchivedFalseOrderBySubmittedAtDesc(Pageable pageable);

    Page<FormSubmission> findByFormAndArchivedFalseOrderBySubmittedAtDesc(Form form, Pageable pageable);

    List<FormSubmission> findByFormAndArchivedFalseOrderBySubmittedAtDesc(Form form);

    List<FormSubmission> findTop10ByOrderBySubmittedAtDesc();

    Optional<FormSubmission> findByStripeSessionId(String stripeSessionId);

    // National leader scoping: submissions whose submitting user belongs to the
    // region, excluding submissions made by ADMIN / SUPER_ADMIN users.
    @Query("SELECT fs FROM FormSubmission fs " +
           "WHERE fs.form = :form AND fs.archived = false " +
           "AND fs.user.region = :region " +
           "AND fs.user.id NOT IN (SELECT ur.user.id FROM UserRole ur " +
           "    WHERE ur.isActive = true AND ur.role.name IN ('ADMIN','SUPER_ADMIN')) " +
           "ORDER BY fs.submittedAt DESC")
    Page<FormSubmission> findByFormAndArchivedFalseAndUserRegionOrderBySubmittedAtDesc(
            @Param("form") Form form, @Param("region") String region, Pageable pageable);

    @Query("SELECT fs FROM FormSubmission fs " +
           "WHERE fs.archived = false " +
           "AND fs.user.region = :region " +
           "AND fs.user.id NOT IN (SELECT ur.user.id FROM UserRole ur " +
           "    WHERE ur.isActive = true AND ur.role.name IN ('ADMIN','SUPER_ADMIN')) " +
           "ORDER BY fs.submittedAt DESC")
    Page<FormSubmission> findByArchivedFalseAndUserRegionOrderBySubmittedAtDesc(
            @Param("region") String region, Pageable pageable);
}
