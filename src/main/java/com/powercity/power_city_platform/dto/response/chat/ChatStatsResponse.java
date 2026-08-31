package com.powercity.power_city_platform.dto.response.chat;

public record ChatStatsResponse(
        long activeChats,
        long unreadMessages,
        long onlineNow
) {
}
