package com.drone.platform.module.notification.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.drone.platform.module.notification.entity.SysNotification;
import com.drone.platform.module.notification.mapper.SysNotificationMapper;
import com.drone.platform.websocket.service.WebSocketPushService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * NotificationService 单元测试：站内通知落库、实时推送与已读管理。
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private SysNotificationMapper notificationMapper;

    @Mock
    private WebSocketPushService webSocketPushService;

    @Captor
    private ArgumentCaptor<Map<String, Object>> payloadCaptor;

    private NotificationService service;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), SysNotification.class);
        service = new NotificationService(webSocketPushService);
        ReflectionTestUtils.setField(service, "baseMapper", notificationMapper);
    }

    @Test
    @DisplayName("push：接收人为空时直接返回，不落库不推送")
    void push_skips_null_receiver() {
        assertThat(service.push(null, "标题", "内容", 1, null)).isNull();
        verify(notificationMapper, never()).insert(any(SysNotification.class));
        verify(webSocketPushService, never()).pushToUser(any(), any());
    }

    @Test
    @DisplayName("push成功：落库默认未读、类型兜底为1并实时推送")
    void push_success_persists_and_pushes() {
        service.push(20L, "任务已派单", "河道巡查已派单", null, 9L);

        ArgumentCaptor<SysNotification> captor = ArgumentCaptor.forClass(SysNotification.class);
        verify(notificationMapper).insert(captor.capture());
        SysNotification saved = captor.getValue();
        assertThat(saved.getReceiverId()).isEqualTo(20L);
        assertThat(saved.getTitle()).isEqualTo("任务已派单");
        assertThat(saved.getNotifyType()).isEqualTo(1);
        assertThat(saved.getIsRead()).isZero();
        assertThat(saved.getRelatedId()).isEqualTo(9L);
        assertThat(saved.getCreateTime()).isNotNull();

        verify(webSocketPushService).pushToUser(eq(20L), any());
    }

    @Test
    @DisplayName("push推送的payload包含NOTIFICATION类型与业务字段")
    void push_payload_contains_type_and_fields() {
        service.push(7L, "告警", "设备离线", 3, null);

        verify(webSocketPushService).pushToUser(eq(7L), payloadCaptor.capture());
        Map<String, Object> payload = payloadCaptor.getValue();
        assertThat(payload.get("type")).isEqualTo("NOTIFICATION");
        assertThat(payload.get("title")).isEqualTo("告警");
        assertThat(payload.get("content")).isEqualTo("设备离线");
        assertThat(payload.get("notifyType")).isEqualTo(3);
    }

    @Test
    @DisplayName("unreadCount：返回未读数量")
    void unreadCount_returns_count() {
        when(notificationMapper.selectCount(any())).thenReturn(3L);

        assertThat(service.unreadCount(20L)).isEqualTo(3L);
    }

    @Test
    @DisplayName("markRead：不能标记他人的通知")
    void markRead_rejects_other_users_notification() {
        SysNotification other = new SysNotification();
        other.setId(1L);
        other.setReceiverId(99L);
        when(notificationMapper.selectById(1L)).thenReturn(other);

        assertThatThrownBy(() -> service.markRead(20L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("通知不存在");
    }

    @Test
    @DisplayName("markRead成功：未读转已读")
    void markRead_success() {
        SysNotification own = new SysNotification();
        own.setId(1L);
        own.setReceiverId(20L);
        own.setIsRead(0);
        when(notificationMapper.selectById(1L)).thenReturn(own);

        service.markRead(20L, 1L);

        assertThat(own.getIsRead()).isEqualTo(1);
        verify(notificationMapper).updateById(own);
    }

    @Test
    @DisplayName("markAllRead：使用已读实体批量更新未读记录")
    void markAllRead_updates_with_read_flag() {
        service.markAllRead(20L);

        ArgumentCaptor<SysNotification> captor = ArgumentCaptor.forClass(SysNotification.class);
        verify(notificationMapper).update(captor.capture(), any());
        assertThat(captor.getValue().getIsRead()).isEqualTo(1);
    }
}
