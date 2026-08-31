package com.powercity.power_city_platform.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "chat_conversations")
public class ChatConversation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "initiator_id", nullable = false)
    private User initiator;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participant_id", nullable = false)
    private User participant;

    @Column(name = "region", length = 50)
    private String region;

    @Column(name = "country", length = 100)
    private String country;

    @Column(name = "last_message", length = 2000)
    private String lastMessage;

    @Column(name = "last_message_at")
    private java.time.LocalDateTime lastMessageAt;

    @Column(name = "last_sender_id")
    private Long lastSenderId;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    public ChatConversation() {}

    public User getInitiator() {
        return initiator;
    }

    public void setInitiator(User initiator) {
        this.initiator = initiator;
    }

    public User getParticipant() {
        return participant;
    }

    public void setParticipant(User participant) {
        this.participant = participant;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public java.time.LocalDateTime getLastMessageAt() {
        return lastMessageAt;
    }

    public void setLastMessageAt(java.time.LocalDateTime lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
    }

    public Long getLastSenderId() {
        return lastSenderId;
    }

    public void setLastSenderId(Long lastSenderId) {
        this.lastSenderId = lastSenderId;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public User getOtherUser(Long userId) {
        if (initiator != null && initiator.getId().equals(userId)) {
            return participant;
        }
        return initiator;
    }
}
