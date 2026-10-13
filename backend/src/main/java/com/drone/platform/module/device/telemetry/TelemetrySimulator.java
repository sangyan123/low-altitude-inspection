package com.drone.platform.module.device.telemetry;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.drone.platform.module.device.entity.UavDevice;
import com.drone.platform.module.device.mapper.UavDeviceMapper;
import com.drone.platform.module.task.entity.RouteTemplate;
import com.drone.platform.module.task.mapper.RouteTemplateMapper;
import com.drone.platform.websocket.service.WebSocketPushService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 无人机遥测模拟服务。
 *
 * <p>定时（2s）驱动所有在线/飞行/充电设备的状态演算，并通过既有的
 * WebSocket 通道（DEVICE_POSITION）广播位置，前端飞行态势与大屏无需改动即可看到
 * 无人机沿航线实时移动。
 *
 * <p>真实设备接入时：新增 MQTT/MAVLink 数据源实现同一推送出口
 * （{@link WebSocketPushService#broadcastToAll}），并将
 * {@code telemetry.simulation.enabled} 置为 false 即可无缝切换。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TelemetrySimulator {

    private static final long TICK_MS = 2000;
    /** 每个 tick 沿整条航线前进的比例：约 25 tick（50 秒）飞完一个单程，往返一个周期约 100 秒 */
    private static final double ROUTE_STEP = 1.0 / 25.0;
    private static final double BATTERY_LOW = 10.0;
    private static final double BATTERY_FULL = 100.0;

    private final UavDeviceMapper deviceMapper;
    private final RouteTemplateMapper routeTemplateMapper;
    private final WebSocketPushService webSocketPushService;
    private final ObjectMapper objectMapper;

    @Value("${telemetry.simulation.enabled:true}")
    private boolean enabled;

    /** 设备飞行模拟内存态：航点折线 + 折线参数化进度 */
    private final Map<Long, SimFlight> flights = new ConcurrentHashMap<>();

    @Scheduled(fixedRate = TICK_MS)
    public void tick() {
        if (!enabled) return;
        try {
            List<UavDevice> devices = deviceMapper.selectList(
                    new LambdaQueryWrapper<UavDevice>().in(UavDevice::getStatus, 1, 2, 3));
            if (devices.isEmpty()) return;

            // 飞行状态被人工改变的设备，清理其模拟航线
            Set<Long> aliveIds = devices.stream().map(UavDevice::getId).collect(Collectors.toSet());
            flights.keySet().removeIf(id -> !aliveIds.contains(id));

            List<RouteTemplate> routes = routeTemplateMapper.selectList(
                    new LambdaQueryWrapper<RouteTemplate>().eq(RouteTemplate::getStatus, 1));

            for (UavDevice device : devices) {
                int status = device.getStatus() == null ? 0 : device.getStatus();
                switch (status) {
                    case 2 -> simulateFlight(device, routes);
                    case 3 -> simulateCharging(device);
                    default -> simulateStandby(device);
                }
                persistAndPush(device);
            }
        } catch (Exception e) {
            log.warn("Telemetry simulation tick failed", e);
        }
    }

    /** 飞行中：沿分配的航线折线往返飞行，电量持续消耗 */
    private void simulateFlight(UavDevice device, List<RouteTemplate> routes) {
        SimFlight flight = flights.computeIfAbsent(device.getId(), id -> newFlight(id, device, routes));
        if (flight.waypoints.size() < 2) {
            // 无可用航线：悬停 + 轻微漂移
            hover(device);
        } else {
            flight.progress += ROUTE_STEP;
            if (flight.progress >= 1.0) {
                flight.progress = 0.0;
                flight.reverse = !flight.reverse; // 到终点后往返
            }
            double t = flight.reverse ? 1.0 - flight.progress : flight.progress;
            double[] p = pointAlong(flight, t);
            device.setLng(BigDecimal.valueOf(p[0]).setScale(6, RoundingMode.HALF_UP));
            device.setLat(BigDecimal.valueOf(p[1]).setScale(6, RoundingMode.HALF_UP));
            double alt = p[2] + Math.sin(System.currentTimeMillis() / 4000.0) * 2.0;
            device.setAltitude(BigDecimal.valueOf(alt).setScale(1, RoundingMode.HALF_UP));
        }
        consumeBattery(device, 0.04 + Math.random() * 0.03);
    }

    /** 充电中：电量上升，充满自动转为在线 */
    private void simulateCharging(UavDevice device) {
        double battery = batteryOf(device) + 0.4 + Math.random() * 0.2;
        if (battery >= BATTERY_FULL) {
            device.setBattery(BigDecimal.valueOf(BATTERY_FULL));
            device.setStatus(1);
        } else {
            device.setBattery(BigDecimal.valueOf(battery).setScale(1, RoundingMode.HALF_UP));
        }
    }

    /** 在线待机：坐标不动，电量缓慢自恢复（模拟待机功耗补偿） */
    private void simulateStandby(UavDevice device) {
        double battery = batteryOf(device);
        if (battery < BATTERY_FULL) {
            device.setBattery(BigDecimal.valueOf(Math.min(BATTERY_FULL, battery + 0.02))
                    .setScale(1, RoundingMode.HALF_UP));
        }
    }

    private SimFlight newFlight(Long deviceId, UavDevice device, List<RouteTemplate> routes) {
        SimFlight flight = new SimFlight();
        if (routes.isEmpty()) {
            flight.waypoints.add(pos(device.getLng(), device.getLat(), 100));
            return flight;
        }
        // 解析所有启用航线，优先选真实弧长最长的一条（部分存量航线航点过密、弧长过短）
        RouteTemplate best = null;
        double bestLen = -1;
        List<Map<String, Object>> bestPoints = null;
        for (RouteTemplate r : routes) {
            try {
                List<Map<String, Object>> points = objectMapper.readValue(
                        r.getRouteData(), new TypeReference<List<Map<String, Object>>>() {});
                double len = 0;
                double[] prev = null;
                for (Map<String, Object> p : points) {
                    double[] cur = {toDouble(p.get("lng")), toDouble(p.get("lat"))};
                    if (prev != null) len += distanceMeters(prev[0], prev[1], cur[0], cur[1]);
                    prev = cur;
                }
                if (len > bestLen) {
                    bestLen = len;
                    best = r;
                    bestPoints = points;
                }
            } catch (Exception e) {
                log.warn("Parse routeData failed, routeId={}", r.getId(), e);
            }
        }
        if (best == null || bestPoints == null || bestPoints.isEmpty()) {
            flight.waypoints.add(pos(device.getLng(), device.getLat(), 100));
            return flight;
        }
        RouteTemplate route = best;
        for (Map<String, Object> p : bestPoints) {
            double lng = toDouble(p.get("lng"));
            double lat = toDouble(p.get("lat"));
            double alt = toDouble(p.get("alt"));
            if (alt <= 0) alt = 100;
            flight.waypoints.add(new double[]{lng, lat, alt});
        }
        log.info("[telemetry] dev={} bound route={} arcLen={}m", deviceId, route.getRouteName(), Math.round(bestLen));
        if (flight.waypoints.isEmpty()) {
            flight.waypoints.add(pos(device.getLng(), device.getLat(), 100));
        }
        // 弧长参数化：按真实距离累计，保证任意航点分布下匀速前进
        flight.cum.add(0.0);
        for (int i = 1; i < flight.waypoints.size(); i++) {
            double[] a = flight.waypoints.get(i - 1);
            double[] b = flight.waypoints.get(i);
            double d = distanceMeters(a[0], a[1], b[0], b[1]);
            flight.totalMeters += d;
            flight.cum.add(flight.totalMeters);
        }
        if (flight.totalMeters <= 0) {
            flight.waypoints.clear();
            flight.waypoints.add(pos(device.getLng(), device.getLat(), 100));
        }
        return flight;
    }

    private void hover(UavDevice device) {
        double lng = toDouble(device.getLng()) + (Math.random() - 0.5) * 0.0004;
        double lat = toDouble(device.getLat()) + (Math.random() - 0.5) * 0.0004;
        device.setLng(BigDecimal.valueOf(lng).setScale(6, RoundingMode.HALF_UP));
        device.setLat(BigDecimal.valueOf(lat).setScale(6, RoundingMode.HALF_UP));
        device.setAltitude(BigDecimal.valueOf(100 + Math.random() * 4).setScale(1, RoundingMode.HALF_UP));
    }

    /** 弧长参数化：t ∈ [0,1] 按真实距离比例定位折线上的点 */
    private double[] pointAlong(SimFlight flight, double t) {
        double target = Math.min(0.999999, Math.max(0, t)) * flight.totalMeters;
        List<Double> cum = flight.cum;
        int seg = 1;
        while (seg < cum.size() - 1 && cum.get(seg) < target) seg++;
        double segStart = cum.get(seg - 1);
        double segLen = cum.get(seg) - segStart;
        double frac = segLen <= 0 ? 0 : (target - segStart) / segLen;
        double[] a = flight.waypoints.get(seg - 1);
        double[] b = flight.waypoints.get(seg);
        return new double[]{
                a[0] + (b[0] - a[0]) * frac,
                a[1] + (b[1] - a[1]) * frac,
                a[2] + (b[2] - a[2]) * frac,
        };
    }

    /** Haversine 两点球面距离（米） */
    private double distanceMeters(double lng1, double lat1, double lng2, double lat2) {
        double rad = Math.PI / 180;
        double dLat = (lat2 - lat1) * rad;
        double dLng = (lng2 - lng1) * rad;
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1 * rad) * Math.cos(lat2 * rad) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * 6371000 * Math.asin(Math.min(1, Math.sqrt(a)));
    }

    private void consumeBattery(UavDevice device, double amount) {
        double battery = batteryOf(device) - amount;
        if (battery <= BATTERY_LOW) {
            // 低电量自动“换电”恢复满电，保持演示连续性
            device.setBattery(BigDecimal.valueOf(BATTERY_FULL));
        } else {
            device.setBattery(BigDecimal.valueOf(battery).setScale(1, RoundingMode.HALF_UP));
        }
    }

    private void persistAndPush(UavDevice device) {

        UavDevice update = new UavDevice();
        update.setId(device.getId());
        update.setLng(device.getLng());
        update.setLat(device.getLat());
        update.setAltitude(device.getAltitude());
        update.setBattery(device.getBattery());
        if (device.getStatus() != null) {
            update.setStatus(device.getStatus());
        }
        update.setUpdateTime(LocalDateTime.now());
        deviceMapper.updateById(update);
        webSocketPushService.broadcastToAll(Map.of("type", "DEVICE_POSITION", "data", device));
    }

    private double batteryOf(UavDevice device) {
        return device.getBattery() == null ? BATTERY_FULL : device.getBattery().doubleValue();
    }

    private double toDouble(Object value) {
        if (value instanceof Number number) return number.doubleValue();
        try {
            return value == null ? 0 : Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private double[] pos(BigDecimal lng, BigDecimal lat, double alt) {
        return new double[]{
                lng == null ? 101.72 : lng.doubleValue(),
                lat == null ? 26.58 : lat.doubleValue(),
                alt,
        };
    }

    /** 飞行模拟内存态 */
    private static class SimFlight {
        final List<double[]> waypoints = new ArrayList<>();
        /** 到第 i 个航点的累计距离（米），cum[0]=0 */
        final List<Double> cum = new ArrayList<>();
        double totalMeters = 0.0;
        double progress = 0.0;
        boolean reverse = false;
    }
}





