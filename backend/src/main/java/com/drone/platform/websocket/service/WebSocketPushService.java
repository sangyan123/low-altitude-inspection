package com.drone.platform.websocket.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketPushService {

    private final ObjectMapper objectMapper;
    private final Map<Long, CopyOnWriteArraySet<WebSocketSession>> userSessions = new ConcurrentHashMap<>();

    public void register(Long userId, WebSocketSession session) {
        userSessions.computeIfAbsent(userId, key -> new CopyOnWriteArraySet<>()).add(session);
    }

    public void remove(WebSocketSession session) {
        userSessions.values().forEach(sessions -> sessions.remove(session));
    }

    public boolean pushToUser(Long userId, Object payload) {
        CopyOnWriteArraySet<WebSocketSession> sessions = userSessions.get(userId);
        if (sessions == null || sessions.isEmpty()) return false;
        String text = toJson(payload);
        boolean delivered = false;
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    synchronized (session) {
                        session.sendMessage(new TextMessage(text));
                    }
                    delivered = true;
                } catch (IOException e) {
                    log.warn("WebSocket push failed: userId={}", userId, e);
                }
            }
        }
        return delivered;
    }

    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.warn("WebSocket payload serialize failed", e);
            return "{\"type\":\"ERROR\",\"title\":\"消息序列化失败\"}";
        }
    }
}