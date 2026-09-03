package com.powercity.power_city_platform.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.powercity.power_city_platform.entity.User;
import com.powercity.power_city_platform.service.UserService;
import com.powercity.power_city_platform.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(NotificationWebSocketHandler.class);

    private final Map<Long, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, Long> sessionUser = new ConcurrentHashMap<>();

    private final JwtUtil jwtUtil;
    private final UserService userService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public NotificationWebSocketHandler(JwtUtil jwtUtil, UserService userService) {
        this.jwtUtil = jwtUtil;
        this.userService = userService;
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
        if (user == null) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }
        session.getAttributes().put("userId", user.getId());
        sessions.put(user.getId(), session);
        sessionUser.put(session.getId(), user.getId());

        sendText(session, msg("connected", Map.of("userId", user.getId())));
        logger.info("Notification websocket connected: {}", user.getEmail());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = sessionUser.remove(session.getId());
        if (userId != null) {
            sessions.remove(userId);
        }
    }

    public void sendToUser(Long userId, Map<String, Object> payload) {
        WebSocketSession session = sessions.get(userId);
        if (session != null && session.isOpen()) {
            sendText(session, payload);
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

    private void sendText(WebSocketSession session, Map<String, Object> payload) {
        try {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
        } catch (Exception e) {
            logger.error("Error sending notification websocket message", e);
        }
    }

    private Map<String, Object> msg(String type, Object data) {
        return Map.of("type", type, "data", data);
    }
}
