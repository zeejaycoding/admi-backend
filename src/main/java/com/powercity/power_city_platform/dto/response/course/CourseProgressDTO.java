package com.powercity.power_city_platform.dto.response.course;

import com.powercity.power_city_platform.entity.CourseProgress;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CourseProgressDTO {
    private Long id;
    private CourseSummaryDTO course;
    private Long currentLessonId;
    private Long currentPositionSeconds;
    private BigDecimal completionPercentage;
    private Boolean isCompleted;
    private LocalDateTime lastAccessedAt;

    public CourseProgressDTO() {}

    public CourseProgressDTO(CourseProgress progress) {
        this.id = progress.getId();
        this.course = new CourseSummaryDTO(progress.getCourse());
        this.currentLessonId = progress.getCurrentLesson() != null ? progress.getCurrentLesson().getId() : null;
        this.currentPositionSeconds = progress.getCurrentPositionSeconds();
        this.completionPercentage = progress.getCompletionPercentage();
        this.isCompleted = progress.getIsCompleted();
        this.lastAccessedAt = progress.getLastAccessedAt();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CourseSummaryDTO getCourse() {
        return course;
    }

    public void setCourse(CourseSummaryDTO course) {
        this.course = course;
    }

    public Long getCurrentLessonId() {
        return currentLessonId;
    }

    public void setCurrentLessonId(Long currentLessonId) {
        this.currentLessonId = currentLessonId;
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
}
