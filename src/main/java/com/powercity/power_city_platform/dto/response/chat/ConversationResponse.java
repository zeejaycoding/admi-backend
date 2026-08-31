package com.powercity.power_city_platform.dto.response.chat;

import java.time.LocalDateTime;

public record ConversationResponse(
        Long id,
        CoordinatorResponse otherUser,
        String lastMessage,
        Long lastSenderId,
        String lastMessageAt,
        long unreadCount,
        boolean online
) {
}
