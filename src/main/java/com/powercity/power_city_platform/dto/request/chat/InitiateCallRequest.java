package com.powercity.power_city_platform.dto.request.chat;

import com.powercity.power_city_platform.enums.CallType;
import jakarta.validation.constraints.NotNull;

public class InitiateCallRequest {

    @NotNull(message = "Conversation id is required")
    private Long conversationId;

    @NotNull(message = "Call type is required")
    private CallType callType;

    public Long getConversationId() {
        return conversationId;
    }

    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
    }

    public CallType getCallType() {
        return callType;
    }

    public void setCallType(CallType callType) {
        this.callType = callType;
    }
}
