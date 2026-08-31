package com.powercity.power_city_platform.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "child_dedications")
public class ChildDedication extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "child_name", nullable = false, length = 255)
    private String childName;

    @Column(name = "dedication_date", nullable = false)
    private LocalDate dedicationDate;

    @Column(name = "parent_name", nullable = false, length = 255)
    private String parentName;

    @Column(name = "campus", nullable = false, length = 150)
    private String campus;

    @Column(name = "minister", nullable = false, length = 255)
    private String minister;

    @Column(name = "certificate_number", nullable = false, unique = true, length = 50)
    private String certificateNumber;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "Pending";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    public ChildDedication() {
        this.submittedAt = LocalDateTime.now();
        this.status = "Pending";
    }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getChildName() { return childName; }
    public void setChildName(String childName) { this.childName = childName; }

    public LocalDate getDedicationDate() { return dedicationDate; }
    public void setDedicationDate(LocalDate dedicationDate) { this.dedicationDate = dedicationDate; }

    public String getParentName() { return parentName; }
    public void setParentName(String parentName) { this.parentName = parentName; }

    public String getCampus() { return campus; }
    public void setCampus(String campus) { this.campus = campus; }

    public String getMinister() { return minister; }
    public void setMinister(String minister) { this.minister = minister; }

    public String getCertificateNumber() { return certificateNumber; }
    public void setCertificateNumber(String certificateNumber) { this.certificateNumber = certificateNumber; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public User getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(User reviewedBy) { this.reviewedBy = reviewedBy; }

    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
}
