package com.drone.platform.websocket.handler;

import com.drone.platform.security.JwtTokenProvider;
import com.drone.platform.websocket.service.WebSocketPushService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class NotificationWebSocketHandler extends TextWebSocketHandler implements HandshakeInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final WebSocketPushService webSocketPushService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) return false;
        String token = servletRequest.getServletRequest().getParameter("token");
        if (token == null || !jwtTokenProvider.validateToken(token)) return false;
        attributes.put("userId", jwtTokenProvider.getUserIdFromToken(token));
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId == null) {
            try { session.close(); } catch (Exception ignored) {}
            return;
        }
        session.getAttributes().put("userId", userId);
        webSocketPushService.register(userId, session);
        try {
            session.sendMessage(new TextMessage("{\"type\":\"CONNECTED\",\"title\":\"实时通知已连接\"}"));
        } catch (Exception ignored) {}
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) {
        webSocketPushService.remove(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            session.sendMessage(new TextMessage("{\"type\":\"PONG\"}"));
        } catch (Exception ignored) {}
    }
}