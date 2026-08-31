package com.powercity.power_city_platform.service;

import com.powercity.power_city_platform.dto.request.chat.SendMessageRequest;
import com.powercity.power_city_platform.dto.response.chat.*;
import com.powercity.power_city_platform.entity.*;
import com.powercity.power_city_platform.enums.CallStatus;
import com.powercity.power_city_platform.enums.CallType;
import com.powercity.power_city_platform.exception.ResourceNotFoundException;
import com.powercity.power_city_platform.repository.ChatCallRepository;
import com.powercity.power_city_platform.repository.ChatConversationRepository;
import com.powercity.power_city_platform.repository.ChatMessageRepository;
import com.powercity.power_city_platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class CoordinatorChatService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final ChatConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;
    private final ChatCallRepository callRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final OnlineStatusService onlineStatusService;

    // ---------------- Stats ----------------
    @Transactional(readOnly = true)
    public ChatStatsResponse getStats() {
        User current = currentUser();
        long activeChats = conversationRepository.countConversationsForUser(current.getId());
        List<Long> conversationIds = conversationRepository.findAllForUser(current.getId())
                .stream().map(ChatConversation::getId).collect(Collectors.toList());
        long unread = conversationIds.isEmpty()
                ? 0
                : messageRepository.countTotalUnread(conversationIds, current.getId());
        long online = onlineStatusService.countOnline();
        return new ChatStatsResponse(activeChats, unread, online);
    }

    // ---------------- Search coordinators ----------------
    @Transactional(readOnly = true)
    public List<CoordinatorResponse> searchCoordinators(String country, String searchTerm) {
        User current = currentUser();
        return userRepository.findCoordinators(blankToNull(country), blankToNull(searchTerm)).stream()
                .filter(u -> !u.getId().equals(current.getId()))
                .map(this::toCoordinatorResponse)
                .collect(Collectors.toList());
    }

    // ---------------- Conversations ----------------
    @Transactional(readOnly = true)
    public List<ConversationResponse> getConversations() {
        User current = currentUser();
        return conversationRepository.findAllForUser(current.getId()).stream()
                .map(c -> toConversationResponse(c, current))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ConversationResponse getConversation(Long conversationId) {
        User current = currentUser();
        ChatConversation conversation = getConversationForUser(conversationId, current.getId());
        return toConversationResponse(conversation, current);
    }

    public ConversationResponse startConversation(Long coordinatorId) {
        User current = currentUser();
        if (current.getId().equals(coordinatorId)) {
            throw new IllegalArgumentException("You cannot start a conversation with yourself");
        }
        User coordinator = userRepository.findById(coordinatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Coordinator", coordinatorId));
        requireCoordinator(coordinator);

        Optional<ChatConversation> existing = conversationRepository.findByUsers(current.getId(), coordinatorId);
        ChatConversation conversation = existing.orElseGet(() -> {
            ChatConversation created = new ChatConversation();
            created.setInitiator(current);
            created.setParticipant(coordinator);
            created.setRegion(coordinator.getRegion());
            created.setCountry(coordinator.getRegion());
            created.setLastMessageAt(LocalDateTime.now());
            created.setIsActive(true);
            return conversationRepository.save(created);
        });

        return toConversationResponse(conversation, current);
    }

    // ---------------- Messages ----------------
    @Transactional(readOnly = true)
    public List<MessageResponse> getMessages(Long conversationId) {
        User current = currentUser();
        ChatConversation conversation = getConversationForUser(conversationId, current.getId());
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId()).stream()
                .map(this::toMessageResponse)
                .collect(Collectors.toList());
    }

    public MessageResponse sendMessage(Long conversationId, String content) {
        User current = currentUser();
        ChatConversation conversation = getConversationForUser(conversationId, current.getId());

        ChatMessage message = new ChatMessage();
        message.setConversation(conversation);
        message.setSender(current);
        message.setContent(content);
        message.setReadByRecipient(false);
        message.setCreatedAt(LocalDateTime.now());
        message.setUpdatedAt(LocalDateTime.now());
        messageRepository.save(message);

        conversation.setLastMessage(content);
        conversation.setLastSenderId(current.getId());
        conversation.setLastMessageAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        return toMessageResponse(message);
    }

    @Transactional
    public MessageResponse sendMessageFromSocket(Long conversationId, Long senderId, String content) {
        ChatConversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", conversationId));
        if (!isParticipant(conversation, senderId)) {
            throw new IllegalArgumentException("User is not a participant of this conversation");
        }
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("User", senderId));

        ChatMessage message = new ChatMessage();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setContent(content);
        message.setReadByRecipient(false);
        message.setCreatedAt(LocalDateTime.now());
        message.setUpdatedAt(LocalDateTime.now());
        messageRepository.save(message);

        conversation.setLastMessage(content);
        conversation.setLastSenderId(senderId);
        conversation.setLastMessageAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        return toMessageResponse(message);
    }

    public void markAsRead(Long conversationId) {
        User current = currentUser();
        messageRepository.markConversationAsRead(conversationId, current.getId());
    }

    public void markAsReadFromSocket(Long conversationId, Long userId) {
        getConversationForUser(conversationId, userId);
        messageRepository.markConversationAsRead(conversationId, userId);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userSpecificConversationId) {
        User current = currentUser();
        return messageRepository.countUnread(userSpecificConversationId, current.getId());
    }

    // ---------------- Calls ----------------
    public CallResponse initiateCall(Long conversationId, CallType callType) {
        User current = currentUser();
        ChatConversation conversation = getConversationForUser(conversationId, current.getId());
        User recipient = conversation.getOtherUser(current.getId());
        requireCoordinator(recipient);

        ChatCall call = new ChatCall();
        call.setConversation(conversation);
        call.setInitiator(current);
        call.setRecipient(recipient);
        call.setCallType(callType);
        call.setStatus(CallStatus.REQUESTED);
        call.setCreatedAt(LocalDateTime.now());
        call.setUpdatedAt(LocalDateTime.now());
        callRepository.save(call);

        return toCallResponse(call);
    }

    public CallResponse initiateCallFromSocket(Long conversationId, Long initiatorId, CallType callType) {
        ChatConversation conversation = getConversationForUser(conversationId, initiatorId);
        User initiator = userRepository.findById(initiatorId)
                .orElseThrow(() -> new ResourceNotFoundException("User", initiatorId));
        User recipient = conversation.getOtherUser(initiatorId);
        requireCoordinator(recipient);

        ChatCall call = new ChatCall();
        call.setConversation(conversation);
        call.setInitiator(initiator);
        call.setRecipient(recipient);
        call.setCallType(callType);
        call.setStatus(CallStatus.REQUESTED);
        call.setCreatedAt(LocalDateTime.now());
        call.setUpdatedAt(LocalDateTime.now());
        callRepository.save(call);

        return toCallResponse(call);
    }

    public CallResponse updateCallStatus(Long callId, CallStatus status, Long actorId) {
        ChatCall call = callRepository.findById(callId)
                .orElseThrow(() -> new ResourceNotFoundException("Call", callId));
        if (actorId == null) {
            User current = currentUser();
            actorId = current.getId();
        }
        if (!call.getInitiator().getId().equals(actorId) && !call.getRecipient().getId().equals(actorId)) {
            throw new IllegalArgumentException("User is not a participant of this call");
        }

        switch (status) {
            case ACCEPTED:
                call.setStatus(CallStatus.ACCEPTED);
                call.setStartedAt(LocalDateTime.now());
                break;
            case ONGOING:
                if (call.getStartedAt() == null) call.setStartedAt(LocalDateTime.now());
                call.setStatus(CallStatus.ONGOING);
                break;
            case REJECTED:
            case CANCELLED:
            case ENDED:
            case MISSED:
                call.setStatus(status);
                call.setEndedAt(LocalDateTime.now());
                break;
            default:
                call.setStatus(status);
        }
        call.setUpdatedAt(LocalDateTime.now());
        return toCallResponse(call);
    }

    @Transactional(readOnly = true)
    public List<CallResponse> getCalls() {
        User current = currentUser();
        return callRepository.findAllForUser(current.getId()).stream()
                .map(this::toCallResponse)
                .collect(Collectors.toList());
    }

    // ---------------- Helpers ----------------
    private User currentUser() {
        return userService.getCurrentUser();
    }

    private ChatConversation getConversationForUser(Long conversationId, Long userId) {
        ChatConversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", conversationId));
        if (!isParticipant(conversation, userId)) {
            throw new IllegalArgumentException("User is not a participant of this conversation");
        }
        return conversation;
    }

    private boolean isParticipant(ChatConversation conversation, Long userId) {
        return conversation.getInitiator().getId().equals(userId)
                || conversation.getParticipant().getId().equals(userId);
    }

    private void requireCoordinator(User user) {
        if (!user.hasRole("COORDINATOR")) {
            throw new IllegalArgumentException("Only coordinators can participate in coordinator chat");
        }
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value;
    }

    private CoordinatorResponse toCoordinatorResponse(User user) {
        return new CoordinatorResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRegion(),
                user.getProfileImageUrl()
        );
    }

    private ConversationResponse toConversationResponse(ChatConversation conversation, User current) {
        User other = conversation.getOtherUser(current.getId());
        long unread = messageRepository.countUnread(conversation.getId(), current.getId());
        boolean online = onlineStatusService.isOnline(other.getId());
        return new ConversationResponse(
                conversation.getId(),
                toCoordinatorResponse(other),
                conversation.getLastMessage(),
                conversation.getLastSenderId(),
                conversation.getLastMessageAt() != null ? conversation.getLastMessageAt().format(FORMATTER) : null,
                unread,
                online
        );
    }

    private MessageResponse toMessageResponse(ChatMessage message) {
        return new MessageResponse(
                message.getId(),
                message.getConversation().getId(),
                message.getSender().getId(),
                message.getContent(),
                message.getCreatedAt() != null ? message.getCreatedAt().format(FORMATTER) : null,
                message.getReadByRecipient()
        );
    }

    private CallResponse toCallResponse(ChatCall call) {
        return new CallResponse(
                call.getId(),
                call.getConversation().getId(),
                call.getInitiator().getId(),
                call.getRecipient().getId(),
                call.getCallType(),
                call.getStatus(),
                call.getStartedAt() != null ? call.getStartedAt().format(FORMATTER) : null,
                call.getEndedAt() != null ? call.getEndedAt().format(FORMATTER) : null
        );
    }
}
