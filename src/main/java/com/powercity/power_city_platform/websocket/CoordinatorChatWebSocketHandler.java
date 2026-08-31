package com.powercity.power_city_platform.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.powercity.power_city_platform.dto.response.chat.CallResponse;
import com.powercity.power_city_platform.dto.response.chat.ConversationResponse;
import com.powercity.power_city_platform.dto.response.chat.MessageResponse;
import com.powercity.power_city_platform.entity.User;
import com.powercity.power_city_platform.enums.CallStatus;
import com.powercity.power_city_platform.enums.CallType;
import com.powercity.power_city_platform.repository.ChatConversationRepository;
import com.powercity.power_city_platform.service.CoordinatorChatService;
import com.powercity.power_city_platform.service.OnlineStatusService;
import com.powercity.power_city_platform.service.UserService;
import com.powercity.power_city_platform.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class CoordinatorChatWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(CoordinatorChatWebSocketHandler.class);

    private final Map<Long, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, Long> sessionUser = new ConcurrentHashMap<>();

    private final JwtUtil jwtUtil;
    private final UserService userService;
    private final CoordinatorChatService chatService;
    private final ChatConversationRepository conversationRepository;
    private final OnlineStatusService onlineStatusService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CoordinatorChatWebSocketHandler(JwtUtil jwtUtil,
                                           UserService userService,
                                           CoordinatorChatService chatService,
                                           ChatConversationRepository conversationRepository,
                                           OnlineStatusService onlineStatusService) {
        this.jwtUtil = jwtUtil;
        this.userService = userService;
        this.chatService = chatService;
        this.conversationRepository = conversationRepository;
        this.onlineStatusService = onlineStatusService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String token = extractToken(session.getUri().getQuery());
        if (token == null || !jwtUtil.validateToken(token)) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }
        String username = jwtUtil.extractUsername(token);
        User user = userService.getUserByEmailForWs(username);
        if (user == null || !user.hasRole("COORDINATOR")) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }
        session.getAttributes().put("userId", user.getId());
        sessions.put(user.getId(), session);
        sessionUser.put(session.getId(), user.getId());
        onlineStatusService.markOnline(user.getId());

        sendText(session, msg("connected", Map.of("userId", user.getId(), "online", true)));

        // Notify the coordinator's online peers
        notifyPresence(user.getId(), true);
        logger.info("Coordinator connected via websocket: {}", user.getEmail());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        Long userId = sessionUser.get(session.getId());
        if (userId == null) {
            return;
        }
        try {
            JsonNode payload = objectMapper.readTree(message.getPayload());
            String type = payload.path("type").asText();

            switch (type) {
                case "send_message": {
                    Long conversationId = payload.path("conversationId").asLong();
                    String content = payload.path("content").asText();
                    MessageResponse response = chatService.sendMessageFromSocket(conversationId, userId, content);
                    Long otherId = conversationRepository.findById(conversationId)
                            .map(c -> c.getInitiator().getId().equals(userId) ? c.getParticipant().getId() : c.getInitiator().getId())
                            .orElse(null);
                    sendToUser(userId, msg("message_delivered", response));
                    if (otherId != null) {
                        sendToUser(otherId, msg("new_message", response));
                    }
                    break;
                }
                case "mark_read": {
                    Long conversationId = payload.path("conversationId").asLong();
                    chatService.markAsReadFromSocket(conversationId, userId);
                    sendToUser(userId, msg("read_ack", Map.of("conversationId", conversationId)));
                    break;
                }
                case "typing": {
                    Long conversationId = payload.path("conversationId").asLong();
                    boolean typing = payload.path("typing").asBoolean(false);
                    Long otherId = conversationRepository.findById(conversationId)
                            .map(c -> c.getInitiator().getId().equals(userId) ? c.getParticipant().getId() : c.getInitiator().getId())
                            .orElse(null);
                    if (otherId != null) {
                        sendToUser(otherId, msg("typing", Map.of("conversationId", conversationId, "userId", userId, "typing", typing)));
                    }
                    break;
                }
                case "call": {
                    String action = payload.path("action").asText();
                    Long conversationId = payload.path("conversationId").asLong();
                    Long otherId = conversationRepository.findById(conversationId)
                            .map(c -> c.getInitiator().getId().equals(userId) ? c.getParticipant().getId() : c.getInitiator().getId())
                            .orElse(null);
                    CallResponse callResponse = null;
                    switch (action) {
                        case "offer": {
                            CallType callType = CallType.valueOf(payload.path("callType").asText("AUDIO"));
                            callResponse = chatService.initiateCallFromSocket(conversationId, userId, callType);
                            break;
                        }
                        case "accept": {
                            Long callId = payload.path("callId").asLong();
                            callResponse = chatService.updateCallStatus(callId, CallStatus.ACCEPTED, userId);
                            break;
                        }
                        case "reject": {
                            Long callId = payload.path("callId").asLong();
                            callResponse = chatService.updateCallStatus(callId, CallStatus.REJECTED, userId);
                            break;
                        }
                        case "cancel": {
                            Long callId = payload.path("callId").asLong();
                            callResponse = chatService.updateCallStatus(callId, CallStatus.CANCELLED, userId);
                            break;
                        }
                        case "end": {
                            Long callId = payload.path("callId").asLong();
                            callResponse = chatService.updateCallStatus(callId, CallStatus.ENDED, userId);
                            break;
                        }
                        default:
                            return;
                    }
                    if (otherId != null) {
                        sendToUser(otherId, msg("call", Map.of("action", action, "call", callResponse, "conversationId", conversationId)));
                    }
                    sendToUser(userId, msg("call_ack", Map.of("action", action, "call", callResponse)));
                    break;
                }
                default:
                    break;
            }
        } catch (Exception e) {
            logger.error("Error handling websocket message", e);
            sendText(session, msg("error", Map.of("message", e.getMessage())));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = sessionUser.remove(session.getId());
        if (userId != null) {
            sessions.remove(userId);
            onlineStatusService.markOffline(userId);
            notifyPresence(userId, false);
        }
    }

    private String extractToken(String query) {
        if (query == null) return null;
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2 && "token".equals(kv[0])) {
                return kv[1];
            }
        }
        return null;
    }

    private void notifyPresence(Long userId, boolean online) {
        List<Long> peers = conversationRepository.findAllForUser(userId).stream()
                .map(c -> c.getInitiator().getId().equals(userId) ? c.getParticipant().getId() : c.getInitiator().getId())
                .distinct()
                .collect(Collectors.toList());
        for (Long peerId : peers) {
            sendToUser(peerId, msg("presence", Map.of("userId", userId, "online", online)));
        }
    }

    private void sendToUser(Long userId, Map<String, Object> payload) {
        WebSocketSession session = sessions.get(userId);
        if (session != null && session.isOpen()) {
            sendText(session, payload);
        }
    }

    private void sendText(WebSocketSession session, Map<String, Object> payload) {
        try {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
        } catch (Exception e) {
            logger.error("Error sending websocket message", e);
        }
    }

    private Map<String, Object> msg(String type, Object data) {
        return Map.of("type", type, "data", data);
    }
}
