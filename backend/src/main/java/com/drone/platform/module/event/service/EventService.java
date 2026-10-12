package com.drone.platform.module.event.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.drone.platform.common.BusinessException;
import com.drone.platform.module.ai.entity.AiAlgorithm;
import com.drone.platform.module.ai.entity.AiRecognitionResult;
import com.drone.platform.module.ai.mapper.AiAlgorithmMapper;
import com.drone.platform.module.ai.mapper.AiRecognitionResultMapper;
import com.drone.platform.module.event.entity.EventHandleLog;
import com.drone.platform.module.event.entity.EventInfo;
import com.drone.platform.module.event.mapper.EventHandleLogMapper;
import com.drone.platform.module.event.mapper.EventInfoMapper;
import com.drone.platform.module.system.entity.SysDept;
import com.drone.platform.module.system.entity.SysUser;
import com.drone.platform.module.system.mapper.SysDeptMapper;
import com.drone.platform.module.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventService extends ServiceImpl<EventInfoMapper, EventInfo> {

    private final EventHandleLogMapper handleLogMapper;
    private final AiRecognitionResultMapper recognitionMapper;
    private final AiAlgorithmMapper algorithmMapper;
    private final SysDeptMapper deptMapper;
    private final SysUserMapper userMapper;

    public Page<EventInfo> page(int current, int size, String eventType, Integer eventSource,
                                Long deptId, Integer status, String keyword,
                                LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<EventInfo> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(eventType)) wrapper.eq(EventInfo::getEventType, eventType);
        if (eventSource != null) wrapper.eq(EventInfo::getEventSource, eventSource);
        if (deptId != null) wrapper.eq(EventInfo::getDeptId, deptId);
        if (status != null) wrapper.eq(EventInfo::getStatus, status);
        if (startDate != null) wrapper.ge(EventInfo::getCreateTime, startDate.atStartOfDay());
        if (endDate != null) wrapper.lt(EventInfo::getCreateTime, endDate.plusDays(1).atStartOfDay());
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(EventInfo::getEventNo, keyword)
                    .or().like(EventInfo::getEventType, keyword)
                    .or().like(EventInfo::getDescription, keyword));
        }
        wrapper.orderByDesc(EventInfo::getId);

        Page<EventInfo> result = page(new Page<>(current, size), wrapper);
        fillRelations(result.getRecords());
        return result;
    }

    public EventInfo getDetail(Long id) {
        EventInfo event = getById(id);
        if (event == null) throw new BusinessException("事件不存在");
        fillRelations(List.of(event));
        List<EventHandleLog> logs = handleLogMapper.selectList(
                new LambdaQueryWrapper<EventHandleLog>()
                        .eq(EventHandleLog::getEventId, id)
                        .orderByAsc(EventHandleLog::getId)
        );
        fillLogOperators(logs);
        event.setHandleLogs(logs);
        return event;
    }

    @Transactional
    public Long createManual(Long userId, String eventType, String description,
                             BigDecimal lng, BigDecimal lat, Long deptId, String remark) {
        if (!StringUtils.hasText(eventType)) throw new BusinessException("事件类型不能为空");
        if (!StringUtils.hasText(description)) throw new BusinessException("事件描述不能为空");

        LocalDateTime now = LocalDateTime.now();
        EventInfo event = new EventInfo();
        event.setEventNo(generateEventNo());
        event.setEventType(eventType);
        event.setEventSource(2);
        event.setDeptId(deptId);
        event.setStatus(0);
        event.setLng(lng);
        event.setLat(lat);
        event.setDescription(description);
        event.setSubmitUserId(userId);
        event.setPushTime(now);
        event.setCreateTime(now);
        save(event);

        insertLog(event.getId(), "创建事件", userId, remark);
        return event.getId();
    }

    @Transactional
    public Long pushRecognitionEvent(Long recognitionId, Long operatorId, Long deptId, String remark) {
        AiRecognitionResult recognition = recognitionMapper.selectById(recognitionId);
        if (recognition == null) throw new BusinessException("AI识别结果不存在");
        if (recognition.getStatus() == null || recognition.getStatus() != 1) {
            throw new BusinessException("只有已确认的AI识别结果才能推送事件");
        }

        Long exists = count(new LambdaQueryWrapper<EventInfo>()
                .eq(EventInfo::getRecognitionId, recognitionId)
                .eq(EventInfo::getEventSource, 1));
        if (exists > 0) throw new BusinessException("该AI识别结果已推送事件");

        String resultType = StringUtils.hasText(recognition.getResultType())
                ? recognition.getResultType() : "AI识别事件";
        String description = StringUtils.hasText(recognition.getErrorMessage())
                ? recognition.getErrorMessage() : "AI识别发现疑似" + resultType + "目标";

        LocalDateTime now = LocalDateTime.now();
        EventInfo event = new EventInfo();
        event.setEventNo(generateEventNo());
        event.setEventType(resultType);
        event.setEventSource(1);
        event.setRecognitionId(recognitionId);
        event.setDeptId(deptId);
        event.setStatus(0);
        event.setPushSystem("AI识别中心");
        event.setPushTime(now);
        event.setLng(recognition.getLng());
        event.setLat(recognition.getLat());
        event.setDescription(description);
        event.setSubmitUserId(recognition.getConfirmUserId() == null ? operatorId : recognition.getConfirmUserId());
        event.setCreateTime(now);
        save(event);

        insertLog(event.getId(), "AI识别推送事件", operatorId, remark);
        recognition.setStatus(2);
        recognition.setConfirmTime(now);
        recognitionMapper.updateById(recognition);
        return event.getId();
    }

    @Transactional
    public void dispatch(Long id, Long operatorId, Long deptId, String remark) {
        EventInfo event = getById(id);
        if (event == null) throw new BusinessException("事件不存在");
        if (event.getStatus() == null || event.getStatus() != 0) {
            throw new BusinessException("只有待处置事件可以派单");
        }
        if (deptId == null) throw new BusinessException("处置部门不能为空");
        SysDept dept = deptMapper.selectById(deptId);
        if (dept == null) throw new BusinessException("处置部门不存在");

        event.setDeptId(deptId);
        event.setStatus(1);
        event.setDispatchTime(LocalDateTime.now());
        updateById(event);
        insertLog(id, "派单", operatorId, remark);
    }

    @Transactional
    public void handle(Long id, Long operatorId, String remark) {
        EventInfo event = getById(id);
        if (event == null) throw new BusinessException("事件不存在");
        if (event.getStatus() == null || event.getStatus() != 1) {
            throw new BusinessException("只有已派单事件可以处置");
        }
        if (!StringUtils.hasText(remark)) throw new BusinessException("处置说明不能为空");

        event.setStatus(2);
        event.setHandleTime(LocalDateTime.now());
        event.setHandleRemark(remark);
        event.setHandleUserId(operatorId);
        updateById(event);
        insertLog(id, "处置完成", operatorId, remark);
    }

    @Transactional
    public void verify(Long id, Long operatorId, Integer result, String remark) {
        EventInfo event = getById(id);
        if (event == null) throw new BusinessException("事件不存在");
        if (event.getStatus() == null || event.getStatus() != 2) {
            throw new BusinessException("只有已处置事件可以核验");
        }
        if (result == null || (result != 1 && result != 2)) {
            throw new BusinessException("核验结果不合法");
        }

        event.setVerifyUserId(operatorId);
        event.setVerifyTime(LocalDateTime.now());
        event.setVerifyResult(result);
        event.setVerifyRemark(remark);
        if (result == 1) {
            event.setStatus(3);
            updateById(event);
            insertLog(id, "核验通过", operatorId, remark);
        } else {
            event.setStatus(1);
            updateById(event);
            insertLog(id, "核验不通过，退回派单", operatorId, remark);
        }
    }

    @Transactional
    public void close(Long id, Long operatorId, String remark) {
        EventInfo event = getById(id);
        if (event == null) throw new BusinessException("事件不存在");
        if (event.getStatus() == null || event.getStatus() != 3) {
            throw new BusinessException("只有核验通过的事件可以销号");
        }

        event.setStatus(4);
        event.setCloseTime(LocalDateTime.now());
        updateById(event);
        insertLog(id, "销号", operatorId, remark);
    }

    @Transactional
    public void deleteEvent(Long id) {
        EventInfo event = getById(id);
        if (event == null) throw new BusinessException("事件不存在");
        if (event.getStatus() != null && event.getStatus() != 0) {
            throw new BusinessException("只有待处置事件可以删除");
        }

        removeById(id);
        if (event.getRecognitionId() != null && event.getEventSource() != null && event.getEventSource() == 1) {
            AiRecognitionResult recognition = recognitionMapper.selectById(event.getRecognitionId());
            if (recognition != null && recognition.getStatus() != null && recognition.getStatus() == 2) {
                recognition.setStatus(1);
                recognitionMapper.updateById(recognition);
            }
        }
    }

    private void fillRelations(List<EventInfo> events) {
        if (events == null || events.isEmpty()) return;

        Set<Long> deptIds = collectIds(events, EventInfo::getDeptId);
        Set<Long> userIds = new HashSet<>();
        for (EventInfo event : events) {
            addId(userIds, event.getSubmitUserId());
            addId(userIds, event.getHandleUserId());
            addId(userIds, event.getVerifyUserId());
        }
        Set<Long> recognitionIds = collectIds(events, EventInfo::getRecognitionId);

        Map<Long, SysDept> deptMap = deptIds.isEmpty() ? Map.of()
                : deptMapper.selectBatchIds(deptIds).stream()
                        .collect(Collectors.toMap(SysDept::getId, Function.identity()));
        Map<Long, SysUser> userMap = userIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(SysUser::getId, Function.identity()));

        Map<Long, AiRecognitionResult> recognitionMap = recognitionIds.isEmpty() ? Map.of()
                : recognitionMapper.selectBatchIds(recognitionIds).stream()
                        .collect(Collectors.toMap(AiRecognitionResult::getId, Function.identity()));
        Set<Long> algoIds = recognitionMap.values().stream()
                .map(AiRecognitionResult::getAlgoId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, AiAlgorithm> algorithmMap = algoIds.isEmpty() ? Map.of()
                : algorithmMapper.selectBatchIds(algoIds).stream()
                        .collect(Collectors.toMap(AiAlgorithm::getId, Function.identity()));

        for (EventInfo event : events) {
            SysDept dept = deptMap.get(event.getDeptId());
            event.setDeptName(dept == null ? null : dept.getDeptName());
            event.setSubmitUserName(getUserName(userMap, event.getSubmitUserId()));
            event.setHandleUserName(getUserName(userMap, event.getHandleUserId()));
            event.setVerifyUserName(getUserName(userMap, event.getVerifyUserId()));

            AiRecognitionResult recognition = recognitionMap.get(event.getRecognitionId());
            if (recognition != null) {
                event.setImageUrl(recognition.getImageUrl());
                event.setResultType(recognition.getResultType());
                event.setConfidence(recognition.getConfidence());
                event.setProvider(recognition.getProvider());
                event.setModelName(recognition.getModelName());
                event.setRawResponse(recognition.getRawResponse());
            }
        }
    }

    private void fillLogOperators(List<EventHandleLog> logs) {
        if (logs == null || logs.isEmpty()) return;
        Set<Long> userIds = collectIds(logs, EventHandleLog::getOperatorId);
        Map<Long, SysUser> userMap = userIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(SysUser::getId, Function.identity()));
        logs.forEach(log -> log.setOperatorName(getUserName(userMap, log.getOperatorId())));
    }

    private void insertLog(Long eventId, String action, Long operatorId, String remark) {
        EventHandleLog log = new EventHandleLog();
        log.setEventId(eventId);
        log.setAction(action);
        log.setOperatorId(operatorId);
        log.setRemark(remark);
        log.setCreateTime(LocalDateTime.now());
        handleLogMapper.insert(log);
    }

    private String generateEventNo() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int rand = new Random().nextInt(900) + 100;
        return "EV" + date + rand;
    }

    private <T> Set<Long> collectIds(List<T> records, Function<T, Long> getter) {
        return records.stream()
                .map(getter)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private void addId(Set<Long> ids, Long id) {
        if (id != null) ids.add(id);
    }

    private String getUserName(Map<Long, SysUser> userMap, Long userId) {
        SysUser user = userMap.get(userId);
        return user == null ? null : (StringUtils.hasText(user.getRealName()) ? user.getRealName() : user.getUsername());
    }
}
