package com.powercity.power_city_platform.entity;

import com.powercity.power_city_platform.enums.Currency;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "courses")
public class Course extends BaseEntity {

    @Column(name = "title", nullable = false)
    @NotBlank(message = "Course title is required")
    @Size(max = 255, message = "Course title must be less than 255 characters")
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    @Size(max = 2000, message = "Course description must be less than 2000 characters")
    private String description;

    @Column(name = "instructor")
    @Size(max = 255, message = "Instructor name must be less than 255 characters")
    private String instructor;

    @Column(name = "category")
    private String category;

    @Column(name = "base_price", nullable = false)
    @NotNull(message = "Base price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Price cannot be negative")
    private BigDecimal basePrice;

    @Column(name = "ngn_price")
    @DecimalMin(value = "0.0", inclusive = true, message = "NGN price cannot be negative")
    private BigDecimal ngnPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "base_currency", nullable = false)
    private Currency baseCurrency = Currency.USD;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "thumbnail_key")
    private String thumbnailKey;

    @Column(name = "total_duration_seconds")
    private Long totalDurationSeconds;

    @Column(name = "lesson_count", nullable = false)
    private Integer lessonCount = 0;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "is_featured")
    private Boolean isFeatured = false;

    @Column(name = "enrollment_count")
    private Integer enrollmentCount = 0;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("orderIndex ASC")
    private List<Lesson> lessons = new ArrayList<>();

    public Course() {}

    public Course(String title, String category, BigDecimal basePrice, Currency baseCurrency) {
        this.title = title;
        this.category = category;
        this.basePrice = basePrice;
        this.baseCurrency = baseCurrency;
        this.isActive = true;
        this.lessonCount = 0;
        this.enrollmentCount = 0;
    }

    public void incrementEnrollmentCount() {
        this.enrollmentCount++;
    }

    public void updateLessonCount() {
        this.lessonCount = this.lessons.size();
    }

    public void updateTotalDuration() {
        this.totalDurationSeconds = this.lessons.stream()
                .mapToLong(Lesson::getDurationSeconds)
                .sum();
    }

    public void addLesson(Lesson lesson) {
        lessons.add(lesson);
        lesson.setCourse(this);
        updateLessonCount();
        updateTotalDuration();
    }

    public void removeLesson(Lesson lesson) {
        lessons.remove(lesson);
        lesson.setCourse(null);
        updateLessonCount();
        updateTotalDuration();
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

    public String getInstructor() {
        return instructor;
    }

    public void setInstructor(String instructor) {
        this.instructor = instructor;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    /** A course is free when its base price is zero (or unset). Free courses skip payment. */
    public boolean isFree() {
        return basePrice == null || basePrice.compareTo(BigDecimal.ZERO) == 0;
    }

    public BigDecimal getNgnPrice() {
        return ngnPrice;
    }

    public void setNgnPrice(BigDecimal ngnPrice) {
        this.ngnPrice = ngnPrice;
    }

    public Currency getBaseCurrency() {
        return baseCurrency;
    }

    public void setBaseCurrency(Currency baseCurrency) {
        this.baseCurrency = baseCurrency;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getThumbnailKey() {
        return thumbnailKey;
    }

    public void setThumbnailKey(String thumbnailKey) {
        this.thumbnailKey = thumbnailKey;
    }

    public Long getTotalDurationSeconds() {
        return totalDurationSeconds;
    }

    public void setTotalDurationSeconds(Long totalDurationSeconds) {
        this.totalDurationSeconds = totalDurationSeconds;
    }

    public Integer getLessonCount() {
        return lessonCount;
    }

    public void setLessonCount(Integer lessonCount) {
        this.lessonCount = lessonCount;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Boolean getIsFeatured() {
        return isFeatured;
    }

    public void setIsFeatured(Boolean isFeatured) {
        this.isFeatured = isFeatured;
    }

    public Integer getEnrollmentCount() {
        return enrollmentCount;
    }

    public void setEnrollmentCount(Integer enrollmentCount) {
        this.enrollmentCount = enrollmentCount;
    }

    public List<Lesson> getLessons() {
        return lessons;
    }

    public void setLessons(List<Lesson> lessons) {
        this.lessons = lessons;
    }
}
