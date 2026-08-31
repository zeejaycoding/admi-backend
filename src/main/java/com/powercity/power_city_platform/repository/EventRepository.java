package com.powercity.power_city_platform.repository;

import com.powercity.power_city_platform.entity.Event;
import com.powercity.power_city_platform.enums.CourseCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    Optional<Event> findByTitle(String title);
    boolean existsByTitle(String title);

    List<Event> findByIsActiveTrue();
    Page<Event> findByIsActiveTrue(Pageable pageable);

    List<Event> findByModuleAndIsActiveTrue(CourseCategory module);
    Page<Event> findByModuleAndIsActiveTrue(CourseCategory module, Pageable pageable);

    @Query("SELECT e FROM Event e WHERE e.isActive = true AND " +
           "(LOWER(e.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(e.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(e.location) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<Event> searchEvents(@Param("searchTerm") String searchTerm, Pageable pageable);

    @Query("SELECT e FROM Event e WHERE e.isActive = true AND e.module = :module AND " +
           "(LOWER(e.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(e.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(e.location) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<Event> searchEventsByModule(@Param("module") CourseCategory module,
                                     @Param("searchTerm") String searchTerm,
                                     Pageable pageable);

    @Query("SELECT COUNT(e) FROM Event e WHERE e.isActive = true")
    Long countActiveEvents();

    @Query("SELECT COUNT(e) FROM Event e WHERE e.isActive = true AND e.module = :module")
    Long countEventsByModule(@Param("module") CourseCategory module);

    @Query("SELECT e FROM Event e WHERE e.isActive = true ORDER BY e.createdAt DESC")
    List<Event> findRecentEvents(Pageable pageable);
}
