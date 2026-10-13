package com.drone.platform.module.emergency.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.drone.platform.common.BusinessException;
import com.drone.platform.module.device.entity.UavDevice;
import com.drone.platform.module.device.mapper.UavDeviceMapper;
import com.drone.platform.module.emergency.entity.EmergencyEvent;
import com.drone.platform.module.emergency.entity.RescueTarget;
import com.drone.platform.module.emergency.mapper.EmergencyEventMapper;
import com.drone.platform.module.emergency.mapper.RescueTargetMapper;
import com.drone.platform.module.notification.service.NotificationService;
import com.drone.platform.module.system.mapper.SysUserMapper;
import com.drone.platform.websocket.service.WebSocketPushService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * EmergencyService 单元测试：应急事件校验、无人机调度、结束处置与搜救目标管理。
 */
@ExtendWith(MockitoExtension.class)
class EmergencyServiceTest {

    @Mock private EmergencyEventMapper eventMapper;
    @Mock private RescueTargetMapper targetMapper;
    @Mock private SysUserMapper userMapper;
    @Mock private NotificationService notificationService;
    @Mock private UavDeviceMapper deviceMapper;
    @Mock private WebSocketPushService webSocketPushService;

    private EmergencyService service;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), EmergencyEvent.class);
        service = new EmergencyService(targetMapper, userMapper, notificationService,
                deviceMapper, webSocketPushService);
        ReflectionTestUtils.setField(service, "baseMapper", eventMapper);
    }

    private EmergencyEvent validEvent() {
        EmergencyEvent event = new EmergencyEvent();
        event.setEventName("城区内涝");
        event.setEventType(1);
        event.setLevel(3);
        event.setLng(new BigDecimal("101.72"));
        event.setLat(new BigDecimal("26.58"));
        return event;
    }

    private EmergencyEvent persisted(int status) {
        EmergencyEvent event = new EmergencyEvent();
        event.setId(1L);
        event.setStatus(status);
        return event;
    }

    @Test
    @DisplayName("上报：名称/类型/等级/坐标校验")
    void create_field_validations() {
        EmergencyEvent noName = validEvent();
        noName.setEventName(" ");
        assertThatThrownBy(() -> service.create(1L, noName))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("名称");

        EmergencyEvent badType = validEvent();
        badType.setEventType(9);
        assertThatThrownBy(() -> service.create(1L, badType))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("类型不合法");

        EmergencyEvent badLevel = validEvent();
        badLevel.setLevel(5);
        assertThatThrownBy(() -> service.create(1L, badLevel))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("等级不合法");

        EmergencyEvent noCoord = validEvent();
        noCoord.setLng(null);
        assertThatThrownBy(() -> service.create(1L, noCoord))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("坐标不能为空");
    }

    @Test
    @DisplayName("上报成功：状态进行中、记录上报人并广播创建事件")
    @SuppressWarnings("unchecked")
    void create_success_broadcasts() {
        EmergencyEvent event = validEvent();
        org.mockito.Mockito.doAnswer(inv -> {
            EmergencyEvent e = inv.getArgument(0);
            e.setId(77L);
            return 1;
        }).when(eventMapper).insert(any(EmergencyEvent.class));

        service.create(10L, event);

        assertThat(event.getStatus()).isZero();
        assertThat(event.getReportUserId()).isEqualTo(10L);
        assertThat(event.getCreateTime()).isNotNull();
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(webSocketPushService).broadcastToAll(captor.capture());
        assertThat(captor.getValue().get("type")).isEqualTo("EMERGENCY_UPDATE");
    }

    @Test
    @DisplayName("调度：已结束事件/未选设备/设备不存在均被拒绝")
    void dispatch_guards() {
        when(eventMapper.selectById(1L)).thenReturn(persisted(1));
        assertThatThrownBy(() -> service.dispatch(1L, 2L, 9L, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已结束");

        when(eventMapper.selectById(1L)).thenReturn(persisted(0));
        assertThatThrownBy(() -> service.dispatch(1L, 2L, null, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("调度无人机");

        when(eventMapper.selectById(1L)).thenReturn(persisted(0));
        when(deviceMapper.selectById(9L)).thenReturn(null);
        assertThatThrownBy(() -> service.dispatch(1L, 2L, 9L, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("调度无人机不存在");
    }

    @Test
    @DisplayName("调度成功：绑定设备、调度人、时间与指令")
    void dispatch_success() {
        EmergencyEvent event = persisted(0);
        when(eventMapper.selectById(1L)).thenReturn(event);
        UavDevice device = new UavDevice();
        device.setId(9L);
        when(deviceMapper.selectById(9L)).thenReturn(device);

        service.dispatch(1L, 2L, 9L, "立即前往");

        assertThat(event.getAssignedDeviceId()).isEqualTo(9L);
        assertThat(event.getDispatchUserId()).isEqualTo(2L);
        assertThat(event.getDispatchTime()).isNotNull();
        assertThat(event.getDispatchRemark()).isEqualTo("立即前往");
        verify(eventMapper).updateById(event);
        verify(webSocketPushService).broadcastToAll(any());
    }

    @Test
    @DisplayName("结束处置：重复结束被拒绝，成功后记录结束时间与说明")
    void finish_workflow() {
        when(eventMapper.selectById(1L)).thenReturn(persisted(1));
        assertThatThrownBy(() -> service.finish(1L, 2L, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已结束");

        EmergencyEvent event = persisted(0);
        event.setDispatchUserId(2L);
        when(eventMapper.selectById(1L)).thenReturn(event);
        service.finish(1L, 9L, "险情解除");
        assertThat(event.getStatus()).isEqualTo(1);
        assertThat(event.getEndTime()).isNotNull();
        assertThat(event.getCloseRemark()).isEqualTo("险情解除");
        assertThat(event.getDispatchUserId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("搜救目标：名称必填、关联事件必须存在，成功时填充默认值")
    void createTarget_workflow() {
        RescueTarget noName = new RescueTarget();
        assertThatThrownBy(() -> service.createTarget(noName))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("名称不能为空");

        RescueTarget target = new RescueTarget();
        target.setTargetName("受困人员");
        target.setTargetType(1);
        target.setLng(new BigDecimal("101.72"));
        target.setLat(new BigDecimal("26.58"));
        target.setEmergencyEventId(1L);
        when(eventMapper.selectById(1L)).thenReturn(null);
        assertThatThrownBy(() -> service.createTarget(target))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("应急事件不存在");

        when(eventMapper.selectById(1L)).thenReturn(persisted(0));
        RescueTarget created = service.createTarget(target);
        assertThat(created.getStatus()).isEqualTo(1);
        assertThat(created.getTracking()).isEqualTo(1);
        assertThat(created.getShared()).isZero();
        assertThat(created.getFoundTime()).isNotNull();
        verify(targetMapper).insert(target);
        verify(webSocketPushService).broadcastToAll(any());
    }

    @Test
    @DisplayName("目标追踪开关：目标不存在抛异常，成功切换追踪状态")
    void setTracking_workflow() {
        when(targetMapper.selectById(5L)).thenReturn(null);
        assertThatThrownBy(() -> service.setTracking(5L, true))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("搜救目标不存在");

        RescueTarget target = new RescueTarget();
        target.setId(5L);
        target.setTracking(0);
        when(targetMapper.selectById(5L)).thenReturn(target);
        service.setTracking(5L, true);
        assertThat(target.getTracking()).isEqualTo(1);
        service.setTracking(5L, false);
        assertThat(target.getTracking()).isZero();
        verify(targetMapper, org.mockito.Mockito.times(2)).updateById(target);
    }

    @Test
    @DisplayName("更新目标：目标必须存在且继承原事件关联")
    void updateTarget_keeps_event_binding() {
        RescueTarget patch = new RescueTarget();
        patch.setId(5L);
        patch.setTargetName("火点");
        patch.setTargetType(2);
        patch.setLng(new BigDecimal("101.72"));
        patch.setLat(new BigDecimal("26.58"));
        when(targetMapper.selectById(5L)).thenReturn(null);
        assertThatThrownBy(() -> service.updateTarget(patch))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("搜救目标不存在");

        RescueTarget exists = new RescueTarget();
        exists.setId(5L);
        exists.setEmergencyEventId(3L);
        when(targetMapper.selectById(5L)).thenReturn(exists);
        service.updateTarget(patch);
        assertThat(patch.getEmergencyEventId()).isEqualTo(3L);
        verify(targetMapper).updateById(patch);
        verify(webSocketPushService).broadcastToAll(eq(Map.of("type", "EMERGENCY_UPDATE",
                "data", Map.of("eventId", 3L, "action", "targetUpdated"))));
    }
}
