package com.powercity.power_city_platform.service;

import com.powercity.power_city_platform.dto.response.course.CourseProgressDTO;
import com.powercity.power_city_platform.entity.Course;
import com.powercity.power_city_platform.entity.CourseProgress;
import com.powercity.power_city_platform.entity.Lesson;
import com.powercity.power_city_platform.entity.User;
import com.powercity.power_city_platform.exception.ResourceNotFoundException;
import com.powercity.power_city_platform.repository.CourseProgressRepository;
import com.powercity.power_city_platform.repository.CourseRepository;
import com.powercity.power_city_platform.repository.LessonRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class CourseProgressService {

    private static final Logger logger = LoggerFactory.getLogger(CourseProgressService.class);

    private final CourseProgressRepository courseProgressRepository;

    private final CourseRepository courseRepository;

    private final LessonRepository lessonRepository;

    // Get or create progress for a user's course
    public CourseProgress getOrCreateProgress(Long userId, Long courseId) {
        Optional<CourseProgress> existing = courseProgressRepository.findByUserIdAndCourseId(userId, courseId);

        if (existing.isPresent()) {
            return existing.get();
        }

        // Create new progress
        User user = new User();
        user.setId(userId);

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));

        CourseProgress progress = new CourseProgress(user, course);
        progress = courseProgressRepository.save(progress);

        logger.info("Created new progress for user {} in course {}", userId, courseId);
        return progress;
    }

    // Update playback position (auto-save every 10 seconds)
    public CourseProgress updatePlaybackPosition(Long userId, Long courseId, Long lessonId, Long positionSeconds) {
        CourseProgress progress = getOrCreateProgress(userId, courseId);

        // Update current lesson
        if (lessonId != null) {
            Lesson lesson = lessonRepository.findById(lessonId)
                    .orElseThrow(() -> new ResourceNotFoundException("Lesson not found"));

            if (!lesson.getCourse().getId().equals(courseId)) {
                throw new RuntimeException("Lesson does not belong to this course");
            }

            progress.setCurrentLesson(lesson);
        }

        // Update position
        progress.setCurrentPositionSeconds(positionSeconds);
        progress.updateLastAccessed();

        progress = courseProgressRepository.save(progress);
        logger.debug("Updated playback position for user {} in course {}, lesson {}, position {}s",
                userId, courseId, lessonId, positionSeconds);

        return progress;
    }

    // Mark lesson as complete
    public CourseProgress markLessonComplete(Long userId, Long courseId, Long lessonId) {
        CourseProgress progress = courseProgressRepository
                .findByUserIdAndCourseIdWithCompletedLessons(userId, courseId)
                .orElseGet(() -> getOrCreateProgress(userId, courseId));

        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson not found"));

        if (!lesson.getCourse().getId().equals(courseId)) {
            throw new RuntimeException("Lesson does not belong to this course");
        }

        // Mark lesson as complete
        progress.markLessonComplete(lesson);
        progress.updateLastAccessed();

        progress = courseProgressRepository.save(progress);
        logger.info("Marked lesson {} as complete for user {} in course {}", lessonId, userId, courseId);

        return progress;
    }

    // Get user's enrolled courses (all courses with progress)
    @Transactional(readOnly = true)
    public List<CourseProgress> getUserCourses(Long userId) {
        return courseProgressRepository.findByUserIdWithCourses(userId);
    }

    // Get user's enrolled courses as DTOs (no lazy loading issues)
    @Transactional(readOnly = true)
    public List<CourseProgressDTO> getUserCoursesDTO(Long userId) {
        List<CourseProgress> progressList = courseProgressRepository.findByUserIdWithCourses(userId);
        return progressList.stream()
                .map(CourseProgressDTO::new)
                .collect(Collectors.toList());
    }

    // Get user's incomplete courses
    @Transactional(readOnly = true)
    public List<CourseProgress> getIncompleteCourses(Long userId) {
        return courseProgressRepository.findIncompleteCoursesByUserId(userId);
    }

    // Get user's completed courses
    @Transactional(readOnly = true)
    public List<CourseProgress> getCompletedCourses(Long userId) {
        return courseProgressRepository.findCompletedCoursesByUserId(userId);
    }

    // Get recently accessed courses
    @Transactional(readOnly = true)
    public List<CourseProgress> getRecentlyAccessedCourses(Long userId) {
        return courseProgressRepository.findRecentlyAccessedByUserId(userId);
    }

    // Get progress for specific course
    @Transactional(readOnly = true)
    public Optional<CourseProgress> getProgress(Long userId, Long courseId) {
        return courseProgressRepository.findByUserIdAndCourseIdWithCompletedLessons(userId, courseId);
    }

    // Get progress data as Map (with completed lesson IDs extracted)
    @Transactional(readOnly = true)
    public Optional<Map<String, Object>> getProgressData(Long userId, Long courseId) {
        Optional<CourseProgress> progressOpt = courseProgressRepository
                .findByUserIdAndCourseIdWithCompletedLessons(userId, courseId);

        if (progressOpt.isEmpty()) {
            return Optional.empty();
        }

        CourseProgress cp = progressOpt.get();
        Map<String, Object> data = new HashMap<>();
        data.put("id", cp.getId());
        data.put("completionPercentage", cp.getCompletionPercentage());
        data.put("isCompleted", cp.getIsCompleted());
        data.put("currentPositionSeconds", cp.getCurrentPositionSeconds());
        data.put("currentLessonId", cp.getCurrentLesson() != null ? cp.getCurrentLesson().getId() : null);
        data.put("lastAccessedAt", cp.getLastAccessedAt());

        // Extract completed lesson IDs within the transaction
        List<Long> completedLessonIds = cp.getCompletedLessons().stream()
                .map(Lesson::getId)
                .collect(Collectors.toList());
        data.put("completedLessonIds", completedLessonIds);

        return Optional.of(data);
    }

    // Check if user has enrolled in course
    @Transactional(readOnly = true)
    public boolean hasUserEnrolled(Long userId, Long courseId) {
        return courseProgressRepository.existsByUserIdAndCourseId(userId, courseId);
    }

    // Get course analytics for admin
    @Transactional(readOnly = true)
    public CourseProgressAnalytics getCourseAnalytics(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));

        Long totalEnrolled = courseProgressRepository.countByCourseId(courseId);
        Long totalCompleted = courseProgressRepository.countCompletedByCourseId(courseId);
        Double avgCompletion = courseProgressRepository.findAverageCompletionPercentageByCourseId(courseId);

        CourseProgressAnalytics analytics = new CourseProgressAnalytics();
        analytics.setCourseId(courseId);
        analytics.setCourseTitle(course.getTitle());
        analytics.setTotalEnrolled(totalEnrolled);
        analytics.setTotalCompleted(totalCompleted);
        analytics.setAverageCompletionPercentage(avgCompletion != null ? avgCompletion : 0.0);
        analytics.setCompletionRate(totalEnrolled > 0 ? (totalCompleted * 100.0 / totalEnrolled) : 0.0);

        return analytics;
    }

    // Inner class for analytics
    public static class CourseProgressAnalytics {
        private Long courseId;
        private String courseTitle;
        private Long totalEnrolled;
        private Long totalCompleted;
        private Double averageCompletionPercentage;
        private Double completionRate;

        public Long getCourseId() {
            return courseId;
        }

        public void setCourseId(Long courseId) {
            this.courseId = courseId;
        }

        public String getCourseTitle() {
            return courseTitle;
        }

        public void setCourseTitle(String courseTitle) {
            this.courseTitle = courseTitle;
        }

        public Long getTotalEnrolled() {
            return totalEnrolled;
        }

        public void setTotalEnrolled(Long totalEnrolled) {
            this.totalEnrolled = totalEnrolled;
        }

        public Long getTotalCompleted() {
            return totalCompleted;
        }

        public void setTotalCompleted(Long totalCompleted) {
            this.totalCompleted = totalCompleted;
        }

        public Double getAverageCompletionPercentage() {
            return averageCompletionPercentage;
        }

        public void setAverageCompletionPercentage(Double averageCompletionPercentage) {
            this.averageCompletionPercentage = averageCompletionPercentage;
        }

        public Double getCompletionRate() {
            return completionRate;
        }

        public void setCompletionRate(Double completionRate) {
            this.completionRate = completionRate;
        }
    }
}
