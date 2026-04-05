package com.piloto.cdi.gateway.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket + STOMP configuration for real-time deliberation streaming.
 *
 * <p>
 * <b>Protocol:</b> STOMP over WebSocket
 * </p>
 *
 * <p>
 * <b>Topic layout:</b>
 * </p>
 * <ul>
 * <li>{@code /topic/deliberation/{sessionId}} — deliberation stage events</li>
 * <li>{@code /topic/system} — system-wide events (health, errors)</li>
 * </ul>
 *
 * <p>
 * <b>Connection endpoint:</b> {@code ws://localhost:8080/ws}
 * </p>
 *
 * @since Phase 4 WebSocket
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // In-memory broker for deliberation topics
        registry.enableSimpleBroker("/topic");
        // Prefix for messages sent FROM client (app → server)
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(
                        "http://localhost:5173",
                        "http://localhost:3000",
                        "http://127.0.0.1:5173",
                        "http://127.0.0.1:3000")
                .withSockJS(); // SockJS fallback for browsers that lack native WS
    }
}
