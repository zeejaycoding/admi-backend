package com.powercity.power_city_platform.repository;

import com.powercity.power_city_platform.entity.Course;
import com.powercity.power_city_platform.enums.Currency;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    Optional<Course> findByTitle(String title);
    boolean existsByTitle(String title);
    // Only active courses reserve a title — a soft-deleted course shouldn't block re-creating it.
    boolean existsByTitleAndIsActiveTrue(String title);

    List<Course> findByIsActiveTrue();
    Page<Course> findByIsActiveTrue(Pageable pageable);

    List<Course> findByIsFeaturedTrueAndIsActiveTrue();
    Page<Course> findByIsFeaturedTrueAndIsActiveTrue(Pageable pageable);

    List<Course> findByCategoryAndIsActiveTrue(String category);
    Page<Course> findByCategoryAndIsActiveTrue(String category, Pageable pageable);

    List<Course> findByTitleContainingIgnoreCaseAndIsActiveTrue(String title);
    Page<Course> findByTitleContainingIgnoreCaseAndIsActiveTrue(String title, Pageable pageable);

    // Search by multiple criteria (title, description, instructor)
    @Query("SELECT c FROM Course c WHERE c.isActive = true AND " +
           "(LOWER(c.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(c.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(c.instructor) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<Course> searchCourses(@Param("searchTerm") String searchTerm, Pageable pageable);

    // Search by category and search term
    @Query("SELECT c FROM Course c WHERE c.isActive = true AND c.category = :category AND " +
           "(LOWER(c.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(c.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(c.instructor) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<Course> searchCoursesByCategory(@Param("category") String category,
                                         @Param("searchTerm") String searchTerm,
                                         Pageable pageable);

    List<Course> findByBasePriceBetweenAndIsActiveTrue(BigDecimal minPrice, BigDecimal maxPrice);
    List<Course> findByBasePriceLessThanEqualAndIsActiveTrue(BigDecimal maxPrice);
    List<Course> findByBasePriceGreaterThanEqualAndIsActiveTrue(BigDecimal minPrice);

    List<Course> findByBaseCurrencyAndIsActiveTrue(Currency currency);

    List<Course> findByInstructorAndIsActiveTrue(String instructor);
    Page<Course> findByInstructorAndIsActiveTrue(String instructor, Pageable pageable);

    // Analytics queries
    @Query("SELECT c FROM Course c WHERE c.isActive = true ORDER BY c.enrollmentCount DESC")
    List<Course> findTopEnrolledCourses(Pageable pageable);

    @Query("SELECT c FROM Course c WHERE c.isActive = true ORDER BY c.lessonCount DESC")
    List<Course> findCoursesWithMostLessons(Pageable pageable);

    @Query("SELECT c FROM Course c WHERE c.isActive = true ORDER BY c.totalDurationSeconds DESC")
    List<Course> findLongestCourses(Pageable pageable);

    @Query("SELECT c FROM Course c WHERE c.isActive = true ORDER BY c.createdAt DESC")
    List<Course> findRecentCourses(Pageable pageable);

    @Query("SELECT COUNT(c) FROM Course c WHERE c.isActive = true")
    Long countActiveCourses();

    @Query("SELECT COUNT(c) FROM Course c WHERE c.isActive = true AND c.isFeatured = true")
    Long countFeaturedCourses();

    @Query("SELECT COUNT(c) FROM Course c WHERE c.isActive = true AND c.category = :category")
    Long countCoursesByCategory(@Param("category") String category);

    @Query("SELECT c.category, COUNT(c) FROM Course c WHERE c.category IS NOT NULL GROUP BY c.category")
    List<Object[]> countCoursesGroupedByCategory();

    // Price statistics
    @Query("SELECT MIN(c.basePrice) FROM Course c WHERE c.isActive = true")
    BigDecimal findMinPrice();

    @Query("SELECT MAX(c.basePrice) FROM Course c WHERE c.isActive = true")
    BigDecimal findMaxPrice();

    @Query("SELECT AVG(c.basePrice) FROM Course c WHERE c.isActive = true")
    BigDecimal findAveragePrice();

    @Query("SELECT SUM(c.enrollmentCount) FROM Course c WHERE c.isActive = true")
    Long countTotalEnrollments();

    @Query("SELECT AVG(c.enrollmentCount) FROM Course c WHERE c.isActive = true")
    Double findAverageEnrollmentCount();

    List<Course> findByIdIn(List<Long> ids);

    // Find course with lessons (fetch join to avoid N+1 problem)
    @Query("SELECT c FROM Course c LEFT JOIN FETCH c.lessons WHERE c.id = :id AND c.isActive = true")
    Optional<Course> findByIdWithLessons(@Param("id") Long id);
}
