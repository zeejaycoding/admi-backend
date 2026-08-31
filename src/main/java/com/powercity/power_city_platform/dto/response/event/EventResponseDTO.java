package com.powercity.power_city_platform.dto.response.event;

import com.powercity.power_city_platform.entity.Event;
import com.powercity.power_city_platform.entity.User;
import com.powercity.power_city_platform.enums.CourseCategory;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class EventResponseDTO {
    private Long id;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private Long updatedBy;
    private String title;
    private String description;
    private CourseCategory module;
    private LocalDate eventDate;
    private LocalDate date; // Alias for eventDate to match frontend field "date"
    private String timeEstimate;
    private LocalDate registrationDeadline;
    private Boolean isActive;
    private String thumbnailUrl;
    private String thumbnailKey;
    private String location;
    private String status; // "Active" or "Inactive" to match frontend field "status"
    private Integer users; // Registration/Enrollment count to match frontend field "users"
    private String ticketType;
    private String ticketPrice;
    private String organizerName;
    private String organizerEmail;
    private String organizerPhone;

    public EventResponseDTO() {}

    public EventResponseDTO(Event event, User creator) {
        this.id = event.getId();
        this.createdAt = event.getCreatedAt();
        this.updatedAt = event.getUpdatedAt();
        this.createdBy = creator != null ? creator.getFullName() : "—";
        this.updatedBy = event.getUpdatedBy();
        this.title = event.getTitle();
        this.description = event.getDescription();
        this.module = event.getModule();
        this.eventDate = event.getEventDate();
        this.date = event.getEventDate();
        this.timeEstimate = event.getTimeEstimate();
        this.registrationDeadline = event.getRegistrationDeadline();
        this.isActive = event.getIsActive();
        this.thumbnailUrl = event.getThumbnailUrl();
        this.thumbnailKey = event.getThumbnailKey();
        this.location = event.getLocation();
        this.status = Boolean.TRUE.equals(event.getIsActive()) ? "Active" : "Inactive";
        this.users = 0; // Default count
        this.ticketType = event.getTicketType();
        this.ticketPrice = event.getTicketPrice() != null ? event.getTicketPrice().toString() : null;
        this.organizerName = creator != null ? creator.getFullName() : "—";
        this.organizerEmail = creator != null ? creator.getEmail() : "—";
        this.organizerPhone = creator != null ? creator.getPhoneNumber() : "—";
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

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
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
        this.date = eventDate;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
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
        this.status = Boolean.TRUE.equals(isActive) ? "Active" : "Inactive";
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getUsers() {
        return users;
    }

    public void setUsers(Integer users) {
        this.users = users;
    }

    public String getTicketType() {
        return ticketType;
    }

    public void setTicketType(String ticketType) {
        this.ticketType = ticketType;
    }

    public String getTicketPrice() {
        return ticketPrice;
    }

    public void setTicketPrice(String ticketPrice) {
        this.ticketPrice = ticketPrice;
    }

    public String getOrganizerName() {
        return organizerName;
    }

    public void setOrganizerName(String organizerName) {
        this.organizerName = organizerName;
    }

    public String getOrganizerEmail() {
        return organizerEmail;
    }

    public void setOrganizerEmail(String organizerEmail) {
        this.organizerEmail = organizerEmail;
    }

    public String getOrganizerPhone() {
        return organizerPhone;
    }

    public void setOrganizerPhone(String organizerPhone) {
        this.organizerPhone = organizerPhone;
    }
}
