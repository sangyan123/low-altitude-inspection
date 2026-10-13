package com.drone.platform.module.dashboard.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.drone.platform.module.dashboard.vo.DashboardStatsVO;
import com.drone.platform.module.event.entity.EventInfo;
import com.drone.platform.module.event.mapper.EventInfoMapper;
import com.drone.platform.module.ai.entity.AiRecognitionResult;
import com.drone.platform.module.ai.mapper.AiRecognitionResultMapper;
import com.drone.platform.module.device.entity.UavDevice;
import com.drone.platform.module.device.mapper.UavDeviceMapper;
import com.drone.platform.module.task.entity.FlightPlan;
import com.drone.platform.module.task.entity.PatrolTask;
import com.drone.platform.module.task.entity.TransportTask;
import com.drone.platform.module.task.mapper.FlightPlanMapper;
import com.drone.platform.module.task.mapper.PatrolTaskMapper;
import com.drone.platform.module.task.mapper.TransportTaskMapper;
import lombok.RequiredArgsConstructor;
import com.drone.platform.websocket.service.WebSocketPushService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final int MAP_DEVICE_LIMIT = 1000;

    private final UavDeviceMapper deviceMapper;
    private final PatrolTaskMapper patrolTaskMapper;
    private final TransportTaskMapper transportTaskMapper;
    private final FlightPlanMapper flightPlanMapper;
    private final EventInfoMapper eventInfoMapper;
    private final AiRecognitionResultMapper aiRecognitionResultMapper;
    private final WebSocketPushService webSocketPushService;

    public DashboardStatsVO getStats() {
        DashboardStatsVO stats = new DashboardStatsVO();
        fillDeviceStats(stats);
        fillPatrolStats(stats);
        fillTransportStats(stats);
        fillPlanStats(stats);
        fillEventStats(stats);
        fillTodayStats(stats);
        fillTrendsAndCompare(stats);
        return stats;
    }


    @Scheduled(fixedRate = 30000)
    public void pushDashboardUpdate() {
        if (!webSocketPushService.hasConnectedSessions()) return;
        webSocketPushService.broadcastToAll(java.util.Map.of("type", "DASHBOARD_UPDATE", "data", getStats()));
    }

    /** 近30天趋势 + 今日/昨日环比 */
    private void fillTrendsAndCompare(DashboardStatsVO stats) {
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(29);
        LocalDateTime startTime = start.atStartOfDay();

        Map<LocalDate, Long> patrolByDay = countByDay(patrolTaskMapper.selectList(
                new LambdaQueryWrapper<PatrolTask>()
                        .select(PatrolTask::getCreateTime)
                        .ge(PatrolTask::getCreateTime, startTime)), PatrolTask::getCreateTime);
        Map<LocalDate, Long> transportByDay = countByDay(transportTaskMapper.selectList(
                new LambdaQueryWrapper<TransportTask>()
                        .select(TransportTask::getCreateTime)
                        .ge(TransportTask::getCreateTime, startTime)), TransportTask::getCreateTime);
        Map<LocalDate, Long> eventByDay = countByDay(eventInfoMapper.selectList(
                new LambdaQueryWrapper<EventInfo>()
                        .select(EventInfo::getCreateTime)
                        .ge(EventInfo::getCreateTime, startTime)), EventInfo::getCreateTime);

        List<DashboardStatsVO.TrendPoint> trends = new ArrayList<>(30);
        for (LocalDate d = start; !d.isAfter(today); d = d.plusDays(1)) {
            DashboardStatsVO.TrendPoint point = new DashboardStatsVO.TrendPoint();
            point.setDate(d.format(java.time.format.DateTimeFormatter.ofPattern("MM-dd")));
            point.setPatrolCount(patrolByDay.getOrDefault(d, 0L));
            point.setTransportCount(transportByDay.getOrDefault(d, 0L));
            point.setEventCount(eventByDay.getOrDefault(d, 0L));
            trends.add(point);
        }
        stats.setTrends(trends);

        // 环比：今日 vs 昨日
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime yesterdayStart = today.minusDays(1).atStartOfDay();

        long todayPatrol = patrolByDay.getOrDefault(today, 0L);
        long todayTransport = transportByDay.getOrDefault(today, 0L);
        long todayTask = todayPatrol + todayTransport;
        long yesterdayTask = patrolByDay.getOrDefault(today.minusDays(1), 0L)
                + transportByDay.getOrDefault(today.minusDays(1), 0L);

        long todayEvent = eventByDay.getOrDefault(today, 0L);
        long yesterdayEvent = eventByDay.getOrDefault(today.minusDays(1), 0L);

        long todayAi = countAi(todayStart, null);
        long yesterdayAi = countAi(yesterdayStart, todayStart);

        DashboardStatsVO.CompareBlock compare = new DashboardStatsVO.CompareBlock();
        compare.setYesterdayTaskCount(yesterdayTask);
        compare.setTaskChangePct(changePct(todayTask, yesterdayTask));
        compare.setYesterdayEventCount(yesterdayEvent);
        compare.setEventChangePct(changePct(todayEvent, yesterdayEvent));
        compare.setTodayAiCount(todayAi);
        compare.setYesterdayAiCount(yesterdayAi);
        compare.setAiChangePct(changePct(todayAi, yesterdayAi));
        stats.setCompare(compare);
    }

    /** 单表按日计数（只查 createTime 一列） */
    private <T> Map<LocalDate, Long> countByDay(List<T> rows, com.baomidou.mybatisplus.core.toolkit.support.SFunction<T, LocalDateTime> timeGetter) {
        Map<LocalDate, Long> result = new TreeMap<>();
        for (T row : rows) {
            LocalDateTime time = timeGetter.apply(row);
            if (time != null) {
                result.merge(time.toLocalDate(), 1L, Long::sum);
            }
        }
        return result;
    }

    private long countAi(LocalDateTime since, LocalDateTime until) {
        LambdaQueryWrapper<AiRecognitionResult> wrapper = new LambdaQueryWrapper<AiRecognitionResult>()
                .ge(AiRecognitionResult::getCreateTime, since);
        if (until != null) {
            wrapper.lt(AiRecognitionResult::getCreateTime, until);
        }
        Long count = aiRecognitionResultMapper.selectCount(wrapper);
        return count == null ? 0 : count;
    }

    private double changePct(long current, long previous) {
        if (previous == 0) {
            return current > 0 ? 100.0 : 0.0;
        }
        return BigDecimal.valueOf((current - previous) * 100.0 / previous)
                .setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private void fillDeviceStats(DashboardStatsVO stats) {
        List<UavDevice> devices = deviceMapper.selectList(
                new LambdaQueryWrapper<UavDevice>()
                        .orderByDesc(UavDevice::getUpdateTime)
                        .last("LIMIT " + MAP_DEVICE_LIMIT)
        );

        long online = 0;
        long flying = 0;
        long charging = 0;
        long offline = 0;
        long maintain = 0;
        long located = 0;
        BigDecimal batterySum = BigDecimal.ZERO;
        int batteryCount = 0;

        for (UavDevice device : devices) {
            Integer status = device.getStatus();
            if (status != null) {
                switch (status) {
                    case 1 -> online++;
                    case 2 -> flying++;
                    case 3 -> charging++;
                    case 4 -> maintain++;
                    default -> offline++;
                }
            } else {
                offline++;
            }

            if (device.getLng() != null && device.getLat() != null) {
                located++;
            }
            if (device.getBattery() != null) {
                batterySum = batterySum.add(device.getBattery());
                batteryCount++;
            }
        }

        stats.setDeviceTotal(devices.size());
        stats.setOnlineCount(online);
        stats.setFlyingCount(flying);
        stats.setChargingCount(charging);
        stats.setOfflineCount(offline);
        stats.setMaintainCount(maintain);
        stats.setLocatedCount(located);
        stats.setAvgBattery(batteryCount == 0 ? null
                : batterySum.divide(BigDecimal.valueOf(batteryCount), 1, RoundingMode.HALF_UP));
        stats.setDevices(devices.stream().map(this::toMarker).toList());
    }

    private DashboardStatsVO.DeviceMarker toMarker(UavDevice device) {
        DashboardStatsVO.DeviceMarker marker = new DashboardStatsVO.DeviceMarker();
        marker.setId(device.getId());
        marker.setDeviceName(device.getDeviceName());
        marker.setDeviceSn(device.getDeviceSn());
        marker.setModel(device.getModel());
        marker.setStatus(device.getStatus());
        marker.setBattery(device.getBattery());
        marker.setLng(device.getLng());
        marker.setLat(device.getLat());
        marker.setAltitude(device.getAltitude());
        marker.setDeptId(device.getDeptId());
        marker.setTotalFlightCount(device.getTotalFlightCount());
        return marker;
    }

    private void fillPatrolStats(DashboardStatsVO stats) {
        stats.setPatrolTotal(count(patrolTaskMapper, null));
        stats.setPatrolPending(count(patrolTaskMapper, PatrolTask::getStatus, 0));
        stats.setPatrolActive(count(patrolTaskMapper, PatrolTask::getStatus, 1));
        stats.setPatrolCompleted(count(patrolTaskMapper, PatrolTask::getStatus, 2));
        stats.setPatrolCancelled(count(patrolTaskMapper, PatrolTask::getStatus, 3));
    }

    private void fillTransportStats(DashboardStatsVO stats) {
        stats.setTransportTotal(count(transportTaskMapper, null));
        stats.setTransportPending(count(transportTaskMapper, TransportTask::getStatus, 0));
        stats.setTransportActive(count(transportTaskMapper, TransportTask::getStatus, 1));
        stats.setTransportArrived(count(transportTaskMapper, TransportTask::getStatus, 2));
        stats.setTransportSigned(count(transportTaskMapper, TransportTask::getStatus, 3));
        stats.setTransportCancelled(count(transportTaskMapper, TransportTask::getStatus, 4));
    }

    private void fillPlanStats(DashboardStatsVO stats) {
        stats.setPlanTotal(count(flightPlanMapper, null));
        stats.setPlanPending(count(flightPlanMapper, FlightPlan::getApprovalStatus, 0));
        stats.setPlanApproved(count(flightPlanMapper, FlightPlan::getApprovalStatus, 1));
        stats.setPlanRejected(count(flightPlanMapper, FlightPlan::getApprovalStatus, 2));
    }

    private void fillEventStats(DashboardStatsVO stats) {
        stats.setEventTotal(count(eventInfoMapper, null));
        stats.setEventPending(count(eventInfoMapper, EventInfo::getStatus, 0));
        stats.setEventDispatched(count(eventInfoMapper, EventInfo::getStatus, 1));
        stats.setEventHandled(count(eventInfoMapper, EventInfo::getStatus, 2));
        stats.setEventVerified(count(eventInfoMapper, EventInfo::getStatus, 3));
        stats.setEventClosed(count(eventInfoMapper, EventInfo::getStatus, 4));
        stats.setTodayEventCount(count(eventInfoMapper, EventInfo::getCreateTime, LocalDate.now().atStartOfDay()));
        stats.setActiveEventCount(stats.getEventPending() + stats.getEventDispatched());
    }

    private void fillTodayStats(DashboardStatsVO stats) {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        long todayPatrol = count(patrolTaskMapper, PatrolTask::getCreateTime, todayStart);
        long todayTransport = count(transportTaskMapper, TransportTask::getCreateTime, todayStart);
        stats.setTodayTaskCount(todayPatrol + todayTransport);
        stats.setActiveTaskCount(stats.getPatrolActive() + stats.getTransportActive());
    }

    private <T> long count(com.baomidou.mybatisplus.core.mapper.BaseMapper<T> mapper, LambdaQueryWrapper<T> wrapper) {
        Long result = mapper.selectCount(wrapper);
        return result == null ? 0 : result;
    }

    private <T> long count(com.baomidou.mybatisplus.core.mapper.BaseMapper<T> mapper,
                           com.baomidou.mybatisplus.core.toolkit.support.SFunction<T, ?> column,
                           Object value) {
        LambdaQueryWrapper<T> wrapper = new LambdaQueryWrapper<T>().eq(column, value);
        return count(mapper, wrapper);
    }

    private <T> long count(com.baomidou.mybatisplus.core.mapper.BaseMapper<T> mapper,
                           com.baomidou.mybatisplus.core.toolkit.support.SFunction<T, ?> column,
                           LocalDateTime value) {
        LambdaQueryWrapper<T> wrapper = new LambdaQueryWrapper<T>().ge(column, value);
        return count(mapper, wrapper);
    }
}


