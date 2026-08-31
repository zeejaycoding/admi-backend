package com.powercity.power_city_platform.config;

import com.powercity.power_city_platform.websocket.CoordinatorChatWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final CoordinatorChatWebSocketHandler coordinatorChatWebSocketHandler;

    public WebSocketConfig(CoordinatorChatWebSocketHandler coordinatorChatWebSocketHandler) {
        this.coordinatorChatWebSocketHandler = coordinatorChatWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(coordinatorChatWebSocketHandler, "/ws/coordinator-chat")
                .setAllowedOrigins("*")
                .addInterceptors(new HttpSessionHandshakeInterceptor());
    }
}
