package com.powercity.power_city_platform.entity;

import com.powercity.power_city_platform.enums.CourseCategory;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

@Entity
@Table(name = "events")
public class Event extends BaseEntity {

    @Column(name = "title", nullable = false, unique = true)
    @NotBlank(message = "Event title is required")
    @Size(max = 255, message = "Event title must be less than 255 characters")
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    @Size(max = 2000, message = "Event description must be less than 2000 characters")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "module", nullable = false)
    @NotNull(message = "Event module is required")
    private CourseCategory module;

    @Column(name = "event_date")
    private LocalDate eventDate;

    @Column(name = "time_estimate")
    @Size(max = 100, message = "Time estimate must be less than 100 characters")
    private String timeEstimate;

    @Column(name = "registration_deadline")
    private LocalDate registrationDeadline;

    @Column(name = "ticket_type")
    private String ticketType = "FREE";

    @Column(name = "ticket_price", precision = 10, scale = 2)
    private java.math.BigDecimal ticketPrice;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "thumbnail_key")
    private String thumbnailKey;

    @Column(name = "location")
    private String location = "Online";

    public Event() {}

    public Event(String title, CourseCategory module, LocalDate eventDate) {
        this.title = title;
        this.module = module;
        this.eventDate = eventDate;
        this.isActive = true;
        this.location = "Online";
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

    public CourseCategory getModule() {
        return module;
    }

    public void setModule(CourseCategory module) {
        this.module = module;
    }

    public LocalDate getEventDate() {
        return eventDate;
    }

    public void setEventDate(LocalDate eventDate) {
        this.eventDate = eventDate;
    }

    public String getTimeEstimate() {
        return timeEstimate;
    }

    public void setTimeEstimate(String timeEstimate) {
        this.timeEstimate = timeEstimate;
    }

    public LocalDate getRegistrationDeadline() {
        return registrationDeadline;
    }

    public void setRegistrationDeadline(LocalDate registrationDeadline) {
        this.registrationDeadline = registrationDeadline;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
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

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getTicketType() {
        return ticketType;
    }

    public void setTicketType(String ticketType) {
        this.ticketType = ticketType;
    }

    public java.math.BigDecimal getTicketPrice() {
        return ticketPrice;
    }

    public void setTicketPrice(java.math.BigDecimal ticketPrice) {
        this.ticketPrice = ticketPrice;
    }
}
