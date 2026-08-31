package com.powercity.power_city_platform.dto.response.chat;

import java.time.LocalDateTime;

public record MessageResponse(
        Long id,
        Long conversationId,
        Long senderId,
        String content,
        String createdAt,
        Boolean readByRecipient
) {
}
