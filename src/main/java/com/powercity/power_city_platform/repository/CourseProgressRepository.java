package com.powercity.power_city_platform.repository;

import com.powercity.power_city_platform.entity.CourseProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseProgressRepository extends JpaRepository<CourseProgress, Long> {

    // Clear any references to a lesson so it can be deleted (progress rows reference lessons via
    // current_lesson_id and the course_completed_lessons join table).
    @Modifying
    @Query(value = "UPDATE course_progress SET current_lesson_id = NULL WHERE current_lesson_id = :lessonId", nativeQuery = true)
    void clearCurrentLessonReferences(@Param("lessonId") Long lessonId);

    @Modifying
    @Query(value = "DELETE FROM course_completed_lessons WHERE lesson_id = :lessonId", nativeQuery = true)
    void removeLessonFromCompleted(@Param("lessonId") Long lessonId);

    // Remove all completed-lesson join rows for a course's progress (run before deleting the progress rows).
    @Modifying
    @Query(value = "DELETE FROM course_completed_lessons WHERE progress_id IN (SELECT id FROM course_progress WHERE course_id = :courseId)", nativeQuery = true)
    void deleteCompletedLessonJoinsByCourseId(@Param("courseId") Long courseId);

    Optional<CourseProgress> findByUserIdAndCourseId(Long userId, Long courseId);

    List<CourseProgress> findByUserId(Long userId);

    // Find progress with completed lessons loaded
    @Query("SELECT cp FROM CourseProgress cp LEFT JOIN FETCH cp.completedLessons WHERE cp.user.id = :userId AND cp.course.id = :courseId")
    Optional<CourseProgress> findByUserIdAndCourseIdWithCompletedLessons(@Param("userId") Long userId, @Param("courseId") Long courseId);

    // Find all progress for a user with courses and lessons loaded
    @Query("SELECT DISTINCT cp FROM CourseProgress cp " +
           "LEFT JOIN FETCH cp.course c " +
           "LEFT JOIN FETCH c.lessons " +
           "WHERE cp.user.id = :userId")
    List<CourseProgress> findByUserIdWithCourses(@Param("userId") Long userId);

    // Find incomplete courses for a user
    @Query("SELECT cp FROM CourseProgress cp WHERE cp.user.id = :userId AND cp.isCompleted = false")
    List<CourseProgress> findIncompleteCoursesByUserId(@Param("userId") Long userId);

    // Find completed courses for a user
    @Query("SELECT cp FROM CourseProgress cp WHERE cp.user.id = :userId AND cp.isCompleted = true")
    List<CourseProgress> findCompletedCoursesByUserId(@Param("userId") Long userId);

    // Find recently accessed courses
    @Query("SELECT cp FROM CourseProgress cp WHERE cp.user.id = :userId ORDER BY cp.lastAccessedAt DESC")
    List<CourseProgress> findRecentlyAccessedByUserId(@Param("userId") Long userId);

    boolean existsByUserIdAndCourseId(Long userId, Long courseId);

    // Verify user has access to a lesson
    @Query("SELECT CASE WHEN COUNT(cp) > 0 THEN true ELSE false END FROM CourseProgress cp " +
           "WHERE cp.user.id = :userId AND cp.course.id = (SELECT l.course.id FROM Lesson l WHERE l.id = :lessonId)")
    boolean hasUserPurchasedCourse(@Param("userId") Long userId, @Param("lessonId") Long lessonId);

    Long countByCourseId(Long courseId);

    List<CourseProgress> findByCourseId(Long courseId);

    // Analytics: Find users who completed a course
    @Query("SELECT cp FROM CourseProgress cp WHERE cp.course.id = :courseId AND cp.isCompleted = true")
    List<CourseProgress> findCompletedProgressByCourseId(@Param("courseId") Long courseId);

    // Analytics: Average completion percentage for a course
    @Query("SELECT AVG(cp.completionPercentage) FROM CourseProgress cp WHERE cp.course.id = :courseId")
    Double findAverageCompletionPercentageByCourseId(@Param("courseId") Long courseId);

    // Analytics: Count of completed vs incomplete for a course
    @Query("SELECT COUNT(cp) FROM CourseProgress cp WHERE cp.course.id = :courseId AND cp.isCompleted = true")
    Long countCompletedByCourseId(@Param("courseId") Long courseId);

    List<CourseProgress> findByUserIdIn(List<Long> userIds);

    List<CourseProgress> findByCourseIdIn(List<Long> courseIds);

    void deleteByCourseId(Long courseId);

    void deleteByUserId(Long userId);
}
