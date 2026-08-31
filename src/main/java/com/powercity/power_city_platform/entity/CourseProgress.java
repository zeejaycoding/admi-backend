package com.powercity.power_city_platform.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "course_progress",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "course_id"}))
public class CourseProgress extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "User is required")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    @NotNull(message = "Course is required")
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_lesson_id")
    private Lesson currentLesson;

    @Column(name = "current_position_seconds")
    private Long currentPositionSeconds = 0L;

    @Column(name = "completion_percentage", precision = 5, scale = 2)
    @DecimalMin(value = "0.0", message = "Completion percentage must be at least 0")
    @DecimalMax(value = "100.0", message = "Completion percentage must not exceed 100")
    private BigDecimal completionPercentage = BigDecimal.ZERO;

    @Column(name = "is_completed")
    private Boolean isCompleted = false;

    @Column(name = "last_accessed_at")
    private LocalDateTime lastAccessedAt;

    @ManyToMany
    @JoinTable(
            name = "course_completed_lessons",
            joinColumns = @JoinColumn(name = "progress_id"),
            inverseJoinColumns = @JoinColumn(name = "lesson_id")
    )
    private Set<Lesson> completedLessons = new HashSet<>();

    public CourseProgress() {}

    public CourseProgress(User user, Course course) {
        this.user = user;
        this.course = course;
        this.currentPositionSeconds = 0L;
        this.completionPercentage = BigDecimal.ZERO;
        this.isCompleted = false;
        this.lastAccessedAt = LocalDateTime.now();
    }

    public void markLessonComplete(Lesson lesson) {
        completedLessons.add(lesson);
        updateCompletionPercentage();
    }

    public void updateCompletionPercentage() {
        if (course != null && course.getLessonCount() > 0) {
            double percentage = (completedLessons.size() * 100.0) / course.getLessonCount();
            this.completionPercentage = BigDecimal.valueOf(percentage).setScale(2, RoundingMode.HALF_UP);

            // Auto-complete course if all lessons are complete
            if (percentage >= 100.0) {
                this.isCompleted = true;
            }
        }
    }

    public boolean isLessonCompleted(Lesson lesson) {
        return completedLessons.contains(lesson);
    }

    public void updateLastAccessed() {
        this.lastAccessedAt = LocalDateTime.now();
    }

    @JsonIgnore
    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    @JsonIgnore
    public Lesson getCurrentLesson() {
        return currentLesson;
    }

    public void setCurrentLesson(Lesson currentLesson) {
        this.currentLesson = currentLesson;
    }

    public Long getCurrentPositionSeconds() {
        return currentPositionSeconds;
    }

    public void setCurrentPositionSeconds(Long currentPositionSeconds) {
        this.currentPositionSeconds = currentPositionSeconds;
    }

    public BigDecimal getCompletionPercentage() {
        return completionPercentage;
    }

    public void setCompletionPercentage(BigDecimal completionPercentage) {
        this.completionPercentage = completionPercentage;
    }

    public Boolean getIsCompleted() {
        return isCompleted;
    }

    public void setIsCompleted(Boolean isCompleted) {
        this.isCompleted = isCompleted;
    }

    public LocalDateTime getLastAccessedAt() {
        return lastAccessedAt;
    }

    public void setLastAccessedAt(LocalDateTime lastAccessedAt) {
        this.lastAccessedAt = lastAccessedAt;
    }

    @JsonIgnore
    public Set<Lesson> getCompletedLessons() {
        return completedLessons;
    }

    public void setCompletedLessons(Set<Lesson> completedLessons) {
        this.completedLessons = completedLessons;
    }
}
