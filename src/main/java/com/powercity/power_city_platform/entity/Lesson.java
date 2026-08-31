package com.powercity.power_city_platform.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "course_lessons")
public class Lesson extends BaseEntity {

    @JsonBackReference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    @NotNull(message = "Course is required")
    private Course course;

    @Column(name = "title", nullable = false)
    @NotBlank(message = "Lesson title is required")
    @Size(max = 255, message = "Lesson title must be less than 255 characters")
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    @Size(max = 2000, message = "Lesson description must be less than 2000 characters")
    private String description;

    @Column(name = "audio_key", nullable = false)
    @NotBlank(message = "Audio key is required")
    @Size(max = 255, message = "Audio key must be less than 255 characters")
    private String audioKey;

    @Column(name = "duration_seconds", nullable = false)
    @NotNull(message = "Duration is required")
    @Min(value = 0, message = "Duration cannot be negative")
    private Long durationSeconds = 0L;

    @Column(name = "order_index", nullable = false)
    @NotNull(message = "Order index is required")
    @Min(value = 0, message = "Order index must be at least 0")
    private Integer orderIndex;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "is_preview")
    private Boolean isPreview = false;

    public Lesson() {}

    public Lesson(Course course, String title, String audioKey, Long durationSeconds, Integer orderIndex) {
        this.course = course;
        this.title = title;
        this.audioKey = audioKey;
        this.durationSeconds = durationSeconds;
        this.orderIndex = orderIndex;
        this.isPreview = false;
    }

    public String getFormattedDuration() {
        long hours = durationSeconds / 3600;
        long minutes = (durationSeconds % 3600) / 60;
        long seconds = durationSeconds % 60;

        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%d:%02d", minutes, seconds);
        }
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAudioKey() {
        return audioKey;
    }

    public void setAudioKey(String audioKey) {
        this.audioKey = audioKey;
    }

    public Long getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Long durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public Integer getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
    }

    public Long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public void setFileSizeBytes(Long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
    }

    public Boolean getIsPreview() {
        return isPreview;
    }

    public void setIsPreview(Boolean isPreview) {
        this.isPreview = isPreview;
    }
}
