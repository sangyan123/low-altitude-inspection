package com.drone.platform.module.dashboard.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.drone.platform.module.ai.mapper.AiRecognitionResultMapper;
import com.drone.platform.module.dashboard.vo.DashboardStatsVO;
import com.drone.platform.module.device.entity.UavDevice;
import com.drone.platform.module.device.mapper.UavDeviceMapper;
import com.drone.platform.module.event.entity.EventInfo;
import com.drone.platform.module.event.mapper.EventInfoMapper;
import com.drone.platform.module.task.entity.FlightPlan;
import com.drone.platform.module.task.mapper.FlightPlanMapper;
import com.drone.platform.module.task.entity.PatrolTask;
import com.drone.platform.module.task.mapper.PatrolTaskMapper;
import com.drone.platform.module.task.entity.TransportTask;
import com.drone.platform.module.task.mapper.TransportTaskMapper;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * DashboardService 单元测试：设备统计聚合、趋势长度与定时推送行为。
 */
@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock private UavDeviceMapper deviceMapper;
    @Mock private PatrolTaskMapper patrolTaskMapper;
    @Mock private TransportTaskMapper transportTaskMapper;
    @Mock private FlightPlanMapper flightPlanMapper;
    @Mock private EventInfoMapper eventInfoMapper;
    @Mock private AiRecognitionResultMapper aiRecognitionResultMapper;
    @Mock private WebSocketPushService webSocketPushService;

    @Captor
    private ArgumentCaptor<Map<String, Object>> payloadCaptor;

    private DashboardService service;

    @BeforeEach
    void initTableMeta() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, UavDevice.class);
        TableInfoHelper.initTableInfo(assistant, PatrolTask.class);
        TableInfoHelper.initTableInfo(assistant, TransportTask.class);
        TableInfoHelper.initTableInfo(assistant, FlightPlan.class);
        TableInfoHelper.initTableInfo(assistant, EventInfo.class);
    }

    @BeforeEach
    void setUp() {
        service = new DashboardService(deviceMapper, patrolTaskMapper, transportTaskMapper,
                flightPlanMapper, eventInfoMapper, aiRecognitionResultMapper, webSocketPushService);
    }

    private UavDevice device(Integer status, String battery, boolean located) {
        UavDevice device = new UavDevice();
        device.setId(1L);
        device.setDeviceName("巡检无人机");
        device.setStatus(status);
        if (battery != null) device.setBattery(new BigDecimal(battery));
        if (located) {
            device.setLng(new BigDecimal("101.72"));
            device.setLat(new BigDecimal("26.58"));
        }
        return device;
    }

    @Test
    @DisplayName("getStats：设备状态聚合、平均电量与地图标记")
    void getStats_aggregates_devices() {
        when(deviceMapper.selectList(any())).thenReturn(List.of(
                device(1, "80.0", true),
                device(2, "60.0", false),
                device(null, null, true)
        ));

        DashboardStatsVO stats = service.getStats();

        assertThat(stats.getDeviceTotal()).isEqualTo(3);
        assertThat(stats.getOnlineCount()).isEqualTo(1);
        assertThat(stats.getFlyingCount()).isEqualTo(1);
        assertThat(stats.getOfflineCount()).isEqualTo(1);
        assertThat(stats.getLocatedCount()).isEqualTo(2);
        assertThat(stats.getAvgBattery()).isEqualByComparingTo("70.0");
        assertThat(stats.getDevices()).hasSize(3);
        assertThat(stats.getDevices().get(0).getDeviceName()).isEqualTo("巡检无人机");
    }

    @Test
    @DisplayName("getStats：空数据时趋势固定30天且计数为0")
    void getStats_empty_data_yields_30_day_trend() {
        when(deviceMapper.selectList(any())).thenReturn(List.of());

        DashboardStatsVO stats = service.getStats();

        assertThat(stats.getTrends()).hasSize(30);
        assertThat(stats.getPatrolTotal()).isZero();
        assertThat(stats.getTodayTaskCount()).isZero();
        assertThat(stats.getActiveEventCount()).isZero();
    }

    @Test
    @DisplayName("定时推送：无连接会话时不广播")
    void pushDashboardUpdate_skips_when_no_session() {
        when(webSocketPushService.hasConnectedSessions()).thenReturn(false);

        service.pushDashboardUpdate();

        verify(webSocketPushService, never()).broadcastToAll(any());
    }

    @Test
    @DisplayName("定时推送：有连接会话时广播DASHBOARD_UPDATE消息")
    void pushDashboardUpdate_broadcasts_when_sessions_exist() {
        when(webSocketPushService.hasConnectedSessions()).thenReturn(true);
        when(deviceMapper.selectList(any())).thenReturn(List.of());

        service.pushDashboardUpdate();

        verify(webSocketPushService).broadcastToAll(payloadCaptor.capture());
        assertThat(payloadCaptor.getValue().get("type")).isEqualTo("DASHBOARD_UPDATE");
    }
}
