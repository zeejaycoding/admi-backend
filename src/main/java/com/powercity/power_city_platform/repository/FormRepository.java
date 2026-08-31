package com.powercity.power_city_platform.repository;

import com.powercity.power_city_platform.entity.Form;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FormRepository extends JpaRepository<Form, Long> {

    List<Form> findByIsPublishedTrueAndIsActiveTrue();
    Page<Form> findByIsPublishedTrueAndIsActiveTrue(Pageable pageable);

    Page<Form> findByTitleContainingIgnoreCaseAndIsActiveTrue(String title, Pageable pageable);

    Page<Form> findByIsActiveTrue(Pageable pageable);

    boolean existsByTitleAndIsActiveTrue(String title);

    Optional<Form> findByIdAndIsActiveTrue(Long id);

    // Find by form code (for page-based forms like PARTNERSHIP)
    Optional<Form> findByFormCodeAndIsActiveTrue(String formCode);

    // Find by event code (for program/event-based forms)
    Optional<Form> findByEventCodeAndIsActiveTrue(String eventCode);

    // Analytics
    @Query("SELECT COUNT(f) FROM Form f WHERE f.isActive = true AND f.isPublished = true")
    Long countPublishedForms();

    @Query("SELECT f FROM Form f WHERE f.isActive = true ORDER BY f.submissionCount DESC")
    List<Form> findTopFormsBySubmissions(Pageable pageable);

    @Query("SELECT COUNT(f) FROM Form f WHERE f.isActive = true")
    Long countActiveForms();

    @Query("SELECT SUM(f.submissionCount) FROM Form f WHERE f.isActive = true")
    Long countTotalSubmissions();
}
