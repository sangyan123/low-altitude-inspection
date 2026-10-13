package com.drone.platform.module.emergency.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.drone.platform.common.BusinessException;
import com.drone.platform.module.device.entity.UavDevice;
import com.drone.platform.module.device.mapper.UavDeviceMapper;
import com.drone.platform.module.emergency.entity.EmergencyEvent;
import com.drone.platform.module.emergency.entity.RescueTarget;
import com.drone.platform.module.emergency.mapper.EmergencyEventMapper;
import com.drone.platform.module.emergency.mapper.RescueTargetMapper;
import com.drone.platform.module.system.entity.SysUser;
import com.drone.platform.module.notification.service.NotificationService;
import com.drone.platform.module.system.mapper.SysUserMapper;
import com.drone.platform.websocket.service.WebSocketPushService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmergencyService extends ServiceImpl<EmergencyEventMapper, EmergencyEvent> {

    private final RescueTargetMapper targetMapper;
    private final SysUserMapper userMapper;
    private final NotificationService notificationService;
    private final UavDeviceMapper deviceMapper;
    private final WebSocketPushService webSocketPushService;

    public Page<EmergencyEvent> page(int current, int size, Integer eventType, Integer level,
                                     Integer status, String keyword, LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<EmergencyEvent> wrapper = new LambdaQueryWrapper<>();
        if (eventType != null) wrapper.eq(EmergencyEvent::getEventType, eventType);
        if (level != null) wrapper.eq(EmergencyEvent::getLevel, level);
        if (status != null) wrapper.eq(EmergencyEvent::getStatus, status);
        if (startDate != null) wrapper.ge(EmergencyEvent::getCreateTime, startDate.atStartOfDay());
        if (endDate != null) wrapper.lt(EmergencyEvent::getCreateTime, endDate.plusDays(1).atStartOfDay());
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(EmergencyEvent::getEventName, keyword)
                    .or().like(EmergencyEvent::getLocationDesc, keyword)
                    .or().like(EmergencyEvent::getDescription, keyword));
        }
        wrapper.orderByAsc(EmergencyEvent::getStatus)
                .orderByDesc(EmergencyEvent::getId);

        Page<EmergencyEvent> result = page(new Page<>(current, size), wrapper);
        fillEventRelations(result.getRecords());
        return result;
    }

    public EmergencyEvent getDetail(Long id) {
        EmergencyEvent event = getById(id);
        if (event == null) throw new BusinessException("应急事件不存在");
        fillEventRelations(List.of(event));
        List<RescueTarget> targets = targetMapper.selectList(
                new LambdaQueryWrapper<RescueTarget>()
                        .eq(RescueTarget::getEmergencyEventId, id)
                        .orderByAsc(RescueTarget::getStatus)
                        .orderByDesc(RescueTarget::getId)
        );
        fillTargetRelations(targets);
        event.setTargets(targets);
        return event;
    }

    @Transactional
    public Long create(Long userId, EmergencyEvent event) {
        validate(event);
        event.setId(null);
        event.setStatus(0);
        event.setReportUserId(userId);
        event.setCreateTime(LocalDateTime.now());
        save(event);
        webSocketPushService.broadcastToAll(java.util.Map.of("type", "EMERGENCY_UPDATE", "data", java.util.Map.of("eventId", event.getId(), "action", "created")));
        return event.getId();
    }

    @Transactional
    public void update(EmergencyEvent event) {
        if (event.getId() == null) throw new BusinessException("应急事件ID不能为空");
        EmergencyEvent exists = getById(event.getId());
        if (exists == null) throw new BusinessException("应急事件不存在");
        if (exists.getStatus() != null && exists.getStatus() == 1) {
            throw new BusinessException("已结束事件不能编辑");
        }
        validate(event);
        event.setUpdateTime(LocalDateTime.now());
        updateById(event);
        webSocketPushService.broadcastToAll(java.util.Map.of("type", "EMERGENCY_UPDATE", "data", java.util.Map.of("eventId", event.getId(), "action", "updated")));
    }

    @Transactional
    public void dispatch(Long id, Long operatorId, Long deviceId, String remark) {
        EmergencyEvent event = getById(id);
        if (event == null) throw new BusinessException("应急事件不存在");
        if (event.getStatus() != null && event.getStatus() == 1) {
            throw new BusinessException("应急事件已结束，不能调度");
        }
        if (deviceId == null) throw new BusinessException("请选择调度无人机");
        UavDevice device = deviceMapper.selectById(deviceId);
        if (device == null) throw new BusinessException("调度无人机不存在");

        event.setAssignedDeviceId(deviceId);
        event.setDispatchUserId(operatorId);
        event.setDispatchTime(LocalDateTime.now());
        event.setDispatchRemark(remark);
        event.setUpdateTime(LocalDateTime.now());
        updateById(event);
        webSocketPushService.broadcastToAll(java.util.Map.of("type", "EMERGENCY_UPDATE", "data", java.util.Map.of("eventId", event.getId(), "action", "dispatched")));
    }

    @Transactional
    public void finish(Long id, Long operatorId, String remark) {
        EmergencyEvent event = getById(id);
        if (event == null) throw new BusinessException("应急事件不存在");
        if (event.getStatus() != null && event.getStatus() == 1) {
            throw new BusinessException("应急事件已结束");
        }
        event.setStatus(1);
        event.setEndTime(LocalDateTime.now());
        event.setCloseRemark(remark);
        event.setDispatchUserId(event.getDispatchUserId() == null ? operatorId : event.getDispatchUserId());
        event.setUpdateTime(LocalDateTime.now());
        updateById(event);
        webSocketPushService.broadcastToAll(java.util.Map.of("type", "EMERGENCY_UPDATE", "data", java.util.Map.of("eventId", event.getId(), "action", "finished")));
    }

    @Transactional
    public void deleteEvent(Long id) {
        EmergencyEvent event = getById(id);
        if (event == null) throw new BusinessException("应急事件不存在");
        removeById(id);
        webSocketPushService.broadcastToAll(java.util.Map.of("type", "EMERGENCY_UPDATE", "data", java.util.Map.of("eventId", id, "action", "deleted")));
    }

    public List<RescueTarget> listTargets(Long eventId) {
        List<RescueTarget> targets = targetMapper.selectList(
                new LambdaQueryWrapper<RescueTarget>()
                        .eq(RescueTarget::getEmergencyEventId, eventId)
                        .orderByAsc(RescueTarget::getStatus)
                        .orderByDesc(RescueTarget::getId)
        );
        fillTargetRelations(targets);
        return targets;
    }

    @Transactional
    public RescueTarget createTarget(RescueTarget target) {
        validateTarget(target);
        if (getById(target.getEmergencyEventId()) == null) {
            throw new BusinessException("关联应急事件不存在");
        }
        target.setId(null);
        if (target.getStatus() == null) target.setStatus(1);
        if (target.getTracking() == null) target.setTracking(1);
        if (target.getShared() == null) target.setShared(0);
        if (target.getFoundTime() == null) target.setFoundTime(LocalDateTime.now());
        target.setCreateTime(LocalDateTime.now());
        targetMapper.insert(target);
        broadcastEventUpdate(target.getEmergencyEventId(), "targetCreated");
        return target;
    }

    @Transactional
    public void updateTarget(RescueTarget target) {
        if (target.getId() == null) throw new BusinessException("搜救目标ID不能为空");
        RescueTarget exists = targetMapper.selectById(target.getId());
        if (exists == null) throw new BusinessException("搜救目标不存在");
        target.setEmergencyEventId(exists.getEmergencyEventId());
        validateTarget(target);
        target.setUpdateTime(LocalDateTime.now());
        targetMapper.updateById(target);
        broadcastEventUpdate(exists.getEmergencyEventId(), "targetUpdated");
    }

    @Transactional
    public void setTracking(Long id, boolean tracking) {
        RescueTarget target = targetMapper.selectById(id);
        if (target == null) throw new BusinessException("搜救目标不存在");
        target.setTracking(tracking ? 1 : 0);
        target.setUpdateTime(LocalDateTime.now());
        targetMapper.updateById(target);
    }

    @Transactional
    public void deleteTarget(Long id) {
        if (targetMapper.selectById(id) == null) throw new BusinessException("搜救目标不存在");
        targetMapper.deleteById(id);
        broadcastEventUpdate(null, "targetDeleted");
    }

    private void broadcastEventUpdate(Long eventId, String action) {
        webSocketPushService.broadcastToAll(java.util.Map.of("type", "EMERGENCY_UPDATE",
                "data", java.util.Map.of("eventId", eventId == null ? 0 : eventId, "action", action)));
    }

    private void validate(EmergencyEvent event) {
        if (!StringUtils.hasText(event.getEventName())) throw new BusinessException("应急事件名称不能为空");
        if (event.getEventType() == null || event.getEventType() < 1 || event.getEventType() > 5) {
            throw new BusinessException("应急事件类型不合法");
        }
        if (event.getLevel() == null || event.getLevel() < 1 || event.getLevel() > 4) {
            throw new BusinessException("应急事件等级不合法");
        }
        if (event.getLng() == null || event.getLat() == null) {
            throw new BusinessException("应急事件坐标不能为空");
        }
    }

    private void validateTarget(RescueTarget target) {
        if (!StringUtils.hasText(target.getTargetName())) throw new BusinessException("搜救目标名称不能为空");
        if (target.getTargetType() == null || target.getTargetType() < 1 || target.getTargetType() > 3) {
            throw new BusinessException("搜救目标类型不合法");
        }
        if (target.getLng() == null || target.getLat() == null) {
            throw new BusinessException("搜救目标坐标不能为空");
        }
        if (target.getStatus() == null || target.getStatus() < 1 || target.getStatus() > 3) {
            target.setStatus(1);
        }
    }

    private void fillEventRelations(List<EmergencyEvent> events) {
        if (events == null || events.isEmpty()) return;

        Set<Long> userIds = new HashSet<>();
        Set<Long> deviceIds = new HashSet<>();
        for (EmergencyEvent event : events) {
            addId(userIds, event.getReportUserId());
            addId(userIds, event.getDispatchUserId());
            addId(deviceIds, event.getAssignedDeviceId());
        }

        Map<Long, SysUser> userMap = userIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(SysUser::getId, Function.identity()));
        Map<Long, UavDevice> deviceMap = deviceIds.isEmpty() ? Map.of()
                : deviceMapper.selectBatchIds(deviceIds).stream()
                        .collect(Collectors.toMap(UavDevice::getId, Function.identity()));

        Set<Long> eventIds = events.stream().map(EmergencyEvent::getId).collect(Collectors.toSet());
        List<RescueTarget> allTargets = targetMapper.selectList(
                new LambdaQueryWrapper<RescueTarget>().in(RescueTarget::getEmergencyEventId, eventIds)
        );
        Map<Long, List<RescueTarget>> targetMap = allTargets.stream()
                .collect(Collectors.groupingBy(RescueTarget::getEmergencyEventId));

        for (EmergencyEvent event : events) {
            SysUser reporter = userMap.get(event.getReportUserId());
            event.setReporterName(displayName(reporter));
            SysUser dispatcher = userMap.get(event.getDispatchUserId());
            event.setDispatchUserName(displayName(dispatcher));
            UavDevice device = deviceMap.get(event.getAssignedDeviceId());
            event.setAssignedDeviceName(device == null ? null : device.getDeviceName());

            List<RescueTarget> targets = targetMap.getOrDefault(event.getId(), List.of());
            event.setTargets(targets);
            event.setTargetCount((long) targets.size());
            event.setTrackingCount(targets.stream().filter(item -> item.getTracking() != null && item.getTracking() == 1).count());
        }
    }

    private void fillTargetRelations(List<RescueTarget> targets) {
        if (targets == null || targets.isEmpty()) return;
        Set<Long> deviceIds = targets.stream()
                .map(RescueTarget::getFoundByDevice)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, UavDevice> deviceMap = deviceIds.isEmpty() ? Map.of()
                : deviceMapper.selectBatchIds(deviceIds).stream()
                        .collect(Collectors.toMap(UavDevice::getId, Function.identity()));

        for (RescueTarget target : targets) {
            target.setTargetTypeName(switch (target.getTargetType() == null ? 0 : target.getTargetType()) {
                case 1 -> "受困人员";
                case 2 -> "火点";
                case 3 -> "险情点";
                default -> "未知";
            });
            target.setStatusName(switch (target.getStatus() == null ? 0 : target.getStatus()) {
                case 1 -> "待核实";
                case 2 -> "已确认";
                case 3 -> "已处置";
                default -> "未知";
            });
            UavDevice device = deviceMap.get(target.getFoundByDevice());
            target.setFoundByDeviceName(device == null ? null : device.getDeviceName());
        }
    }

    private String displayName(SysUser user) {
        if (user == null) return null;
        return StringUtils.hasText(user.getRealName()) ? user.getRealName() : user.getUsername();
    }

    private void addId(Set<Long> ids, Long id) {
        if (id != null) ids.add(id);
    }
}

