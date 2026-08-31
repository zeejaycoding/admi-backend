package com.powercity.power_city_platform.repository;

import com.powercity.power_city_platform.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, Long> {

    List<Lesson> findByCourseId(Long courseId);

    List<Lesson> findByCourseIdOrderByOrderIndexAsc(Long courseId);

    Optional<Lesson> findByCourseIdAndOrderIndex(Long courseId, Integer orderIndex);

    // Find next lesson (by order index)
    @Query("SELECT l FROM Lesson l WHERE l.course.id = :courseId AND l.orderIndex > :currentIndex ORDER BY l.orderIndex ASC")
    Optional<Lesson> findNextLesson(@Param("courseId") Long courseId, @Param("currentIndex") Integer currentIndex);

    // Find previous lesson (by order index)
    @Query("SELECT l FROM Lesson l WHERE l.course.id = :courseId AND l.orderIndex < :currentIndex ORDER BY l.orderIndex DESC")
    Optional<Lesson> findPreviousLesson(@Param("courseId") Long courseId, @Param("currentIndex") Integer currentIndex);

    // Find first lesson of a course
    @Query("SELECT l FROM Lesson l WHERE l.course.id = :courseId ORDER BY l.orderIndex ASC")
    Optional<Lesson> findFirstLesson(@Param("courseId") Long courseId);

    // Find last lesson of a course
    @Query("SELECT l FROM Lesson l WHERE l.course.id = :courseId ORDER BY l.orderIndex DESC")
    Optional<Lesson> findLastLesson(@Param("courseId") Long courseId);

    List<Lesson> findByIsPreviewTrueAndCourseId(Long courseId);

    Long countByCourseId(Long courseId);

    List<Lesson> findByIdIn(List<Long> ids);

    boolean existsByIdAndCourseId(Long lessonId, Long courseId);

    // Total duration for a course
    @Query("SELECT SUM(l.durationSeconds) FROM Lesson l WHERE l.course.id = :courseId")
    Long sumDurationByCourseId(@Param("courseId") Long courseId);

    Optional<Lesson> findByAudioKey(String audioKey);
    List<Lesson> findByAudioKeyIn(List<String> audioKeys);

    void deleteByCourseId(Long courseId);
}
