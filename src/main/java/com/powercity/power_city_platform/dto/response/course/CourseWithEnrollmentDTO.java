package com.powercity.power_city_platform.dto.response.course;

import com.powercity.power_city_platform.entity.Course;
import com.powercity.power_city_platform.entity.Lesson;
import com.powercity.power_city_platform.enums.Currency;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class CourseWithEnrollmentDTO {
    private Long id;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
    private Long updatedBy;
    private String title;
    private String description;
    private String instructor;
    private String category;
    private BigDecimal basePrice;
    private BigDecimal ngnPrice;
    private Currency baseCurrency;
    private String thumbnailUrl;
    private String thumbnailKey;
    private Long totalDurationSeconds;
    private Integer lessonCount;
    private Boolean isActive;
    private Boolean isFeatured;
    private Integer enrollmentCount;
    private List<Lesson> lessons;

    // Additional field for enrollment status
    private Boolean isEnrolled;

    // Constructor from Course entity
    public CourseWithEnrollmentDTO(Course course, Boolean isEnrolled) {
        this.id = course.getId();
        this.createdAt = course.getCreatedAt();
        this.updatedAt = course.getUpdatedAt();
        this.createdBy = course.getCreatedBy();
        this.updatedBy = course.getUpdatedBy();
        this.title = course.getTitle();
        this.description = course.getDescription();
        this.instructor = course.getInstructor();
        this.category = course.getCategory();
        this.basePrice = course.getBasePrice();
        this.ngnPrice = course.getNgnPrice();
        this.baseCurrency = course.getBaseCurrency();
        this.thumbnailUrl = course.getThumbnailUrl();
        this.thumbnailKey = course.getThumbnailKey();
        this.totalDurationSeconds = course.getTotalDurationSeconds();
        this.lessonCount = course.getLessonCount();
        this.isActive = course.getIsActive();
        this.isFeatured = course.getIsFeatured();
        this.enrollmentCount = course.getEnrollmentCount();
        this.lessons = course.getLessons();
        this.isEnrolled = isEnrolled;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Long updatedBy) {
        this.updatedBy = updatedBy;
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

    public Boolean getIsEnrolled() {
        return isEnrolled;
    }

    public void setIsEnrolled(Boolean isEnrolled) {
        this.isEnrolled = isEnrolled;
    }
}
