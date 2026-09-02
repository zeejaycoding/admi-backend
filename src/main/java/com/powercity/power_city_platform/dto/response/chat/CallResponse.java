package com.powercity.power_city_platform.dto.response.chat;

import com.powercity.power_city_platform.enums.CallStatus;
import com.powercity.power_city_platform.enums.CallType;

import java.time.LocalDateTime;

public record CallResponse(
        Long id,
        Long conversationId,
        Long initiatorId,
        Long recipientId,
        CallType callType,
        CallStatus status,
        String startedAt,
        String endedAt,
        String createdAt
) {
}
