package com.powercity.power_city_platform.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "marriage_certificates")
public class MarriageCertificate extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "groom_name", nullable = false, length = 255)
    private String groomName;

    @Column(name = "groom_email", nullable = false, length = 255)
    private String groomEmail;

    @Column(name = "bride_name", nullable = false, length = 255)
    private String brideName;

    @Column(name = "bride_email", nullable = false, length = 255)
    private String brideEmail;

    @Column(name = "marriage_date", nullable = false)
    private LocalDate marriageDate;

    @Column(name = "campus", nullable = false, length = 150)
    private String campus;

    @Column(name = "minister", nullable = false, length = 255)
    private String minister;

    @Column(name = "maid_of_honor", length = 255)
    private String maidOfHonor;

    @Column(name = "best_man", length = 255)
    private String bestMan;

    @Column(name = "subject", length = 500)
    private String subject;

    @Column(name = "additional_message", columnDefinition = "TEXT")
    private String additionalMessage;

    @Column(name = "certificate_number", nullable = false, unique = true, length = 50)
    private String certificateNumber;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "Sent";

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    public MarriageCertificate() {
        this.submittedAt = LocalDateTime.now();
        this.status = "Sent";
    }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getGroomName() { return groomName; }
    public void setGroomName(String groomName) { this.groomName = groomName; }

    public String getGroomEmail() { return groomEmail; }
    public void setGroomEmail(String groomEmail) { this.groomEmail = groomEmail; }

    public String getBrideName() { return brideName; }
    public void setBrideName(String brideName) { this.brideName = brideName; }

    public String getBrideEmail() { return brideEmail; }
    public void setBrideEmail(String brideEmail) { this.brideEmail = brideEmail; }

    public LocalDate getMarriageDate() { return marriageDate; }
    public void setMarriageDate(LocalDate marriageDate) { this.marriageDate = marriageDate; }

    public String getCampus() { return campus; }
    public void setCampus(String campus) { this.campus = campus; }

    public String getMinister() { return minister; }
    public void setMinister(String minister) { this.minister = minister; }

    public String getMaidOfHonor() { return maidOfHonor; }
    public void setMaidOfHonor(String maidOfHonor) { this.maidOfHonor = maidOfHonor; }

    public String getBestMan() { return bestMan; }
    public void setBestMan(String bestMan) { this.bestMan = bestMan; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getAdditionalMessage() { return additionalMessage; }
    public void setAdditionalMessage(String additionalMessage) { this.additionalMessage = additionalMessage; }

    public String getCertificateNumber() { return certificateNumber; }
    public void setCertificateNumber(String certificateNumber) { this.certificateNumber = certificateNumber; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
}
