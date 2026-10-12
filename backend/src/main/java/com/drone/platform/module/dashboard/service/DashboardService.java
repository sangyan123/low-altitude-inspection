package com.drone.platform.module.dashboard.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.drone.platform.module.dashboard.vo.DashboardStatsVO;
import com.drone.platform.module.device.entity.UavDevice;
import com.drone.platform.module.device.mapper.UavDeviceMapper;
import com.drone.platform.module.task.entity.FlightPlan;
import com.drone.platform.module.task.entity.PatrolTask;
import com.drone.platform.module.task.entity.TransportTask;
import com.drone.platform.module.task.mapper.FlightPlanMapper;
import com.drone.platform.module.task.mapper.PatrolTaskMapper;
import com.drone.platform.module.task.mapper.TransportTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final int MAP_DEVICE_LIMIT = 1000;

    private final UavDeviceMapper deviceMapper;
    private final PatrolTaskMapper patrolTaskMapper;
    private final TransportTaskMapper transportTaskMapper;
    private final FlightPlanMapper flightPlanMapper;

    public DashboardStatsVO getStats() {
        DashboardStatsVO stats = new DashboardStatsVO();
        fillDeviceStats(stats);
        fillPatrolStats(stats);
        fillTransportStats(stats);
        fillPlanStats(stats);
        fillTodayStats(stats);
        return stats;
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