package com.powercity.power_city_platform.controller;

import com.powercity.power_city_platform.dto.request.chat.InitiateCallRequest;
import com.powercity.power_city_platform.dto.request.chat.SendMessageRequest;
import com.powercity.power_city_platform.dto.request.chat.StartConversationRequest;
import com.powercity.power_city_platform.dto.response.chat.*;
import com.powercity.power_city_platform.dto.response.common.ApiResponse;
import com.powercity.power_city_platform.enums.CallStatus;
import com.powercity.power_city_platform.service.CoordinatorChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/coordinator-chat")
@Tag(name = "Coordinator Chat", description = "Communication between coordinators across regions")
@RequiredArgsConstructor
public class CoordinatorChatController {

    private final CoordinatorChatService chatService;

    @GetMapping("/stats")
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "Get chat statistics", description = "Active chats, unread messages and online coordinators")
    public ResponseEntity<ApiResponse<ChatStatsResponse>> getStats() {
        return ResponseEntity.ok(ApiResponse.success("Chat stats retrieved", chatService.getStats()));
    }

    @GetMapping("/coordinators")
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "Search coordinators", description = "Search coordinators by country or name")
    public ResponseEntity<ApiResponse<List<CoordinatorResponse>>> searchCoordinators(
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(ApiResponse.success("Coordinators retrieved", chatService.searchCoordinators(country, search)));
    }

    @GetMapping("/conversations")
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "List conversations", description = "List conversations for the current coordinator")
    public ResponseEntity<ApiResponse<List<ConversationResponse>>> getConversations() {
        return ResponseEntity.ok(ApiResponse.success("Conversations retrieved", chatService.getConversations()));
    }

    @GetMapping("/conversations/{conversationId}")
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "Get conversation", description = "Get a single conversation detail")
    public ResponseEntity<ApiResponse<ConversationResponse>> getConversation(
            @PathVariable Long conversationId) {
        return ResponseEntity.ok(ApiResponse.success("Conversation retrieved", chatService.getConversation(conversationId)));
    }

    @PostMapping("/conversations")
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "Start conversation", description = "Start (or fetch) a conversation with another coordinator")
    public ResponseEntity<ApiResponse<ConversationResponse>> startConversation(
            @Valid @RequestBody StartConversationRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Conversation started", chatService.startConversation(request.getCoordinatorId())));
    }

    @GetMapping("/conversations/{conversationId}/messages")
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "Get messages", description = "Get all messages in a conversation")
    public ResponseEntity<ApiResponse<List<MessageResponse>>> getMessages(
            @PathVariable Long conversationId) {
        return ResponseEntity.ok(ApiResponse.success("Messages retrieved", chatService.getMessages(conversationId)));
    }

    @PostMapping("/messages")
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "Send message", description = "Send a message in a conversation")
    public ResponseEntity<ApiResponse<MessageResponse>> sendMessage(
            @Valid @RequestBody SendMessageRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Message sent", chatService.sendMessage(request.getConversationId(), request.getContent())));
    }

    @PostMapping("/conversations/{conversationId}/read")
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "Mark conversation as read", description = "Mark all messages as read in a conversation")
    public ResponseEntity<ApiResponse<String>> markAsRead(
            @PathVariable Long conversationId) {
        chatService.markAsRead(conversationId);
        return ResponseEntity.ok(ApiResponse.success("Conversation marked as read"));
    }

    @PostMapping("/calls")
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "Initiate call", description = "Initiate an audio/video call with a coordinator")
    public ResponseEntity<ApiResponse<CallResponse>> initiateCall(
            @Valid @RequestBody InitiateCallRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Call initiated", chatService.initiateCall(request.getConversationId(), request.getCallType())));
    }

    @PostMapping("/calls/{callId}/status")
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "Update call status", description = "Accept, reject, end or cancel a call")
    public ResponseEntity<ApiResponse<CallResponse>> updateCallStatus(
            @PathVariable Long callId,
            @RequestParam CallStatus status) {
        return ResponseEntity.ok(ApiResponse.success("Call status updated", chatService.updateCallStatus(callId, status, null)));
    }

    @GetMapping("/calls")
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "List calls", description = "List calls for the current coordinator")
    public ResponseEntity<ApiResponse<List<CallResponse>>> getCalls() {
        return ResponseEntity.ok(ApiResponse.success("Calls retrieved", chatService.getCalls()));
    }
}
