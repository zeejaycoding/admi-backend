package com.powercity.power_city_platform.config;

import com.powercity.power_city_platform.websocket.CoordinatorChatWebSocketHandler;
import com.powercity.power_city_platform.websocket.NotificationWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final CoordinatorChatWebSocketHandler coordinatorChatWebSocketHandler;
    private final NotificationWebSocketHandler notificationWebSocketHandler;

    public WebSocketConfig(CoordinatorChatWebSocketHandler coordinatorChatWebSocketHandler,
                           NotificationWebSocketHandler notificationWebSocketHandler) {
        this.coordinatorChatWebSocketHandler = coordinatorChatWebSocketHandler;
        this.notificationWebSocketHandler = notificationWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(coordinatorChatWebSocketHandler, "/ws/coordinator-chat")
                .setAllowedOrigins("*")
                .addInterceptors(new HttpSessionHandshakeInterceptor());
        registry.addHandler(notificationWebSocketHandler, "/ws/notifications")
                .setAllowedOrigins("*")
                .addInterceptors(new HttpSessionHandshakeInterceptor());
    }
}
