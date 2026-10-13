package com.drone.platform.websocket.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * WebSocketPushService 单元测试：会话注册、定向推送、全员广播与容错。
 */
@ExtendWith(MockitoExtension.class)
class WebSocketPushServiceTest {

    @Mock
    private WebSocketSession sessionA;

    @Mock
    private WebSocketSession sessionB;

    private WebSocketPushService service;

    @BeforeEach
    void setUp() {
        service = new WebSocketPushService(new ObjectMapper());
    }

    @Test
    @DisplayName("pushToUser：无会话时返回false")
    void pushToUser_returns_false_without_session() {
        assertThat(service.pushToUser(1L, Map.of("type", "NOTIFICATION"))).isFalse();
    }

    @Test
    @DisplayName("pushToUser：开放会话收到JSON消息并返回true")
    void pushToUser_delivers_to_open_session() throws Exception {
        when(sessionA.isOpen()).thenReturn(true);
        service.register(1L, sessionA);

        boolean delivered = service.pushToUser(1L, Map.of("type", "NOTIFICATION", "title", "派单"));

        assertThat(delivered).isTrue();
        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(sessionA).sendMessage(captor.capture());
        assertThat(captor.getValue().getPayload()).contains("NOTIFICATION").contains("派单");
    }

    @Test
    @DisplayName("pushToUser：关闭的会话不投递")
    void pushToUser_skips_closed_session() throws Exception {
        when(sessionA.isOpen()).thenReturn(false);
        service.register(1L, sessionA);

        assertThat(service.pushToUser(1L, Map.of("type", "NOTIFICATION"))).isFalse();
        verify(sessionA, never()).sendMessage(any());
    }

    @Test
    @DisplayName("pushToUser：会话发送IO异常时安全返回false")
    void pushToUser_swallows_io_exception() throws Exception {
        when(sessionA.isOpen()).thenReturn(true);
        doThrow(new java.io.IOException("broken pipe")).when(sessionA).sendMessage(any());
        service.register(1L, sessionA);

        assertThat(service.pushToUser(1L, Map.of("type", "NOTIFICATION"))).isFalse();
    }

    @Test
    @DisplayName("broadcastToAll：多用户会话全部收到消息")
    void broadcast_delivers_to_all_sessions() throws Exception {
        when(sessionA.isOpen()).thenReturn(true);
        when(sessionB.isOpen()).thenReturn(true);
        service.register(1L, sessionA);
        service.register(2L, sessionB);

        service.broadcastToAll(Map.of("type", "DEVICE_POSITION"));

        verify(sessionA).sendMessage(any(TextMessage.class));
        verify(sessionB).sendMessage(any(TextMessage.class));
    }

    @Test
    @DisplayName("hasConnectedSessions：仅统计开放中的会话")
    void hasConnectedSessions_checks_open_state() {
        when(sessionA.isOpen()).thenReturn(true);
        assertThat(service.hasConnectedSessions()).isFalse();

        service.register(1L, sessionA);
        assertThat(service.hasConnectedSessions()).isTrue();

        when(sessionA.isOpen()).thenReturn(false);
        assertThat(service.hasConnectedSessions()).isFalse();
    }
}

