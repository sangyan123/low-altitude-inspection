package com.drone.platform.module.report.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.drone.platform.module.ai.entity.AiRecognitionResult;
import com.drone.platform.module.ai.mapper.AiRecognitionResultMapper;
import com.drone.platform.module.data.entity.DataResult;
import com.drone.platform.module.data.mapper.DataResultMapper;
import com.drone.platform.module.device.entity.UavDevice;
import com.drone.platform.module.device.mapper.UavDeviceMapper;
import com.drone.platform.module.emergency.entity.EmergencyEvent;
import com.drone.platform.module.emergency.mapper.EmergencyEventMapper;
import com.drone.platform.module.event.entity.EventInfo;
import com.drone.platform.module.event.mapper.EventInfoMapper;
import com.drone.platform.module.report.vo.DepartmentReportVO;
import com.drone.platform.module.report.vo.NameValueVO;
import com.drone.platform.module.report.vo.ReportOverviewVO;
import com.drone.platform.module.report.vo.ReportTrendPointVO;
import com.drone.platform.module.report.vo.ReportVO;
import com.drone.platform.module.system.entity.SysDept;
import com.drone.platform.module.system.mapper.SysDeptMapper;
import com.drone.platform.module.task.entity.FlightPlan;
import com.drone.platform.module.task.entity.PatrolTask;
import com.drone.platform.module.task.entity.TransportTask;
import com.drone.platform.module.task.mapper.FlightPlanMapper;
import com.drone.platform.module.task.mapper.PatrolTaskMapper;
import com.drone.platform.module.task.mapper.TransportTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final UavDeviceMapper deviceMapper;
    private final PatrolTaskMapper patrolMapper;
    private final TransportTaskMapper transportMapper;
    private final FlightPlanMapper planMapper;
    private final AiRecognitionResultMapper aiMapper;
    private final EventInfoMapper eventMapper;
    private final EmergencyEventMapper emergencyMapper;
    private final DataResultMapper dataMapper;
    private final SysDeptMapper deptMapper;

    public ReportVO buildReport(LocalDate startDate, LocalDate endDate, String granularity, Long deptId) {
        LocalDate start = startDate == null ? LocalDate.now().minusDays(29) : startDate;
        LocalDate end = endDate == null ? LocalDate.now() : endDate;
        if (end.isBefore(start)) {
            LocalDate temp = start;
            start = end;
            end = temp;
        }
        String grain = StringUtils.hasText(granularity) ? granularity.toLowerCase(Locale.ROOT) : "day";
        if (!List.of("day", "week", "month").contains(grain)) grain = "day";

        LocalDateTime startTime = start.atStartOfDay();
        LocalDateTime endTime = end.plusDays(1).atStartOfDay();

        List<UavDevice> devices = deviceMapper.selectList(null);
        List<PatrolTask> patrols = patrolMapper.selectList(timeWrapper(PatrolTask::getCreateTime, startTime, endTime));
        List<TransportTask> transports = transportMapper.selectList(timeWrapper(TransportTask::getCreateTime, startTime, endTime));
        List<FlightPlan> plans = planMapper.selectList(timeWrapper(FlightPlan::getCreateTime, startTime, endTime));
        List<AiRecognitionResult> recognitions = aiMapper.selectList(timeWrapper(AiRecognitionResult::getCreateTime, startTime, endTime));
        List<EventInfo> events = eventMapper.selectList(timeWrapper(EventInfo::getCreateTime, startTime, endTime));
        List<EmergencyEvent> emergencies = emergencyMapper.selectList(timeWrapper(EmergencyEvent::getCreateTime, startTime, endTime));
        List<DataResult> dataResults = dataMapper.selectList(timeWrapper(DataResult::getCreateTime, startTime, endTime));

        if (deptId != null) {
            devices = devices.stream().filter(item -> deptId.equals(item.getDeptId())).toList();
            patrols = patrols.stream().filter(item -> deptId.equals(item.getDeptId())).toList();
            transports = transports.stream().filter(item -> deptId.equals(item.getDeptId())).toList();
            plans = plans.stream().filter(item -> deptId.equals(item.getDeptId())).toList();
            events = events.stream().filter(item -> deptId.equals(item.getDeptId())).toList();
            dataResults = dataResults.stream().filter(item -> deptId.equals(item.getDeptId())).toList();
        }

        ReportVO report = new ReportVO();
        report.setStartDate(start.format(DAY_FORMAT));
        report.setEndDate(end.format(DAY_FORMAT));
        report.setGranularity(grain);
        report.setOverview(buildOverview(devices, patrols, transports, plans, recognitions, events, emergencies, dataResults));
        report.setTrends(buildTrends(start, end, grain, devices, patrols, transports, plans,
                recognitions, events, emergencies, dataResults));
        report.setDepartments(buildDepartmentReport(devices, patrols, transports, plans, events, dataResults));
        report.setPatrolStatus(statusDistribution(patrols.stream().map(PatrolTask::getStatus).toList(),
                List.of("待派单", "执行中", "已完成", "已取消")));
        report.setTransportStatus(statusDistribution(transports.stream().map(TransportTask::getStatus).toList(),
                List.of("待起飞", "运输中", "已到达", "已签收", "已取消")));
        report.setEventStatus(statusDistribution(events.stream().map(EventInfo::getStatus).toList(),
                List.of("待处置", "已派单", "已处置", "已核验", "已销号")));
        report.setEmergencyType(statusDistribution(emergencies.stream().map(EmergencyEvent::getEventType).toList(),
                List.of("洪涝", "山火", "地质灾害", "事故", "搜救")));
        report.setDataType(statusDistribution(dataResults.stream().map(DataResult::getDataType).toList(),
                List.of("视频", "照片", "DOM", "DSM", "点云", "三维模型")));
        return report;
    }

    private ReportOverviewVO buildOverview(List<UavDevice> devices,
                                           List<PatrolTask> patrols,
                                           List<TransportTask> transports,
                                           List<FlightPlan> plans,
                                           List<AiRecognitionResult> recognitions,
                                           List<EventInfo> events,
                                           List<EmergencyEvent> emergencies,
                                           List<DataResult> dataResults) {
        ReportOverviewVO overview = new ReportOverviewVO();
        overview.setDeviceTotal(devices.size());
        overview.setDeviceOnline(devices.stream().filter(item -> item.getStatus() != null && item.getStatus() == 1).count());
        overview.setDeviceFlying(devices.stream().filter(item -> item.getStatus() != null && item.getStatus() == 2).count());

        long patrolCompleted = patrols.stream().filter(item -> item.getStatus() != null && item.getStatus() == 2).count();
        overview.setPatrolTotal(patrols.size());
        overview.setPatrolCompleted(patrolCompleted);
        overview.setPatrolCompletionRate(rate(patrolCompleted, patrols.size()));

        long transportSigned = transports.stream().filter(item -> item.getStatus() != null && item.getStatus() == 3).count();
        overview.setTransportTotal(transports.size());
        overview.setTransportSigned(transportSigned);
        overview.setTransportSignedRate(rate(transportSigned, transports.size()));

        long planApproved = plans.stream().filter(item -> item.getApprovalStatus() != null && item.getApprovalStatus() == 1).count();
        overview.setPlanTotal(plans.size());
        overview.setPlanApproved(planApproved);
        overview.setPlanApprovalRate(rate(planApproved, plans.size()));

        overview.setAiRecognitionTotal(recognitions.size());

        long eventClosed = events.stream().filter(item -> item.getStatus() != null && item.getStatus() == 4).count();
        overview.setEventTotal(events.size());
        overview.setEventClosed(eventClosed);
        overview.setEventClosureRate(rate(eventClosed, events.size()));

        overview.setEmergencyTotal(emergencies.size());
        overview.setEmergencyOngoing(emergencies.stream().filter(item -> item.getStatus() != null && item.getStatus() == 0).count());

        overview.setDataResultTotal(dataResults.size());
        overview.setDataMediaTotal(dataResults.stream().filter(item -> item.getDataType() != null && item.getDataType() <= 2).count());
        overview.setDataModelTotal(dataResults.stream().filter(item -> item.getDataType() != null && item.getDataType() >= 3).count());
        return overview;
    }

    private List<ReportTrendPointVO> buildTrends(LocalDate start, LocalDate end, String granularity,
                                                 List<UavDevice> devices,
                                                 List<PatrolTask> patrols,
                                                 List<TransportTask> transports,
                                                 List<FlightPlan> plans,
                                                 List<AiRecognitionResult> recognitions,
                                                 List<EventInfo> events,
                                                 List<EmergencyEvent> emergencies,
                                                 List<DataResult> dataResults) {
        Map<String, String> labels = new LinkedHashMap<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            String key = bucketKey(date, granularity);
            labels.putIfAbsent(key, bucketLabel(date, key, granularity));
        }

        Map<String, Long> deviceGroup = groupByTime(devices, UavDevice::getCreateTime, granularity, labels);
        Map<String, Long> patrolGroup = groupByTime(patrols, PatrolTask::getCreateTime, granularity, labels);
        Map<String, Long> transportGroup = groupByTime(transports, TransportTask::getCreateTime, granularity, labels);
        Map<String, Long> planGroup = groupByTime(plans, FlightPlan::getCreateTime, granularity, labels);
        Map<String, Long> aiGroup = groupByTime(recognitions, AiRecognitionResult::getCreateTime, granularity, labels);
        Map<String, Long> eventGroup = groupByTime(events, EventInfo::getCreateTime, granularity, labels);
        Map<String, Long> emergencyGroup = groupByTime(emergencies, EmergencyEvent::getCreateTime, granularity, labels);
        Map<String, Long> dataGroup = groupByTime(dataResults, DataResult::getCreateTime, granularity, labels);

        return labels.entrySet().stream().map(entry -> {
            ReportTrendPointVO point = new ReportTrendPointVO();
            point.setTimeLabel(entry.getValue());
            point.setDeviceCount(deviceGroup.getOrDefault(entry.getKey(), 0L));
            point.setPatrolCount(patrolGroup.getOrDefault(entry.getKey(), 0L));
            point.setTransportCount(transportGroup.getOrDefault(entry.getKey(), 0L));
            point.setPlanCount(planGroup.getOrDefault(entry.getKey(), 0L));
            point.setAiCount(aiGroup.getOrDefault(entry.getKey(), 0L));
            point.setEventCount(eventGroup.getOrDefault(entry.getKey(), 0L));
            point.setEmergencyCount(emergencyGroup.getOrDefault(entry.getKey(), 0L));
            point.setDataCount(dataGroup.getOrDefault(entry.getKey(), 0L));
            return point;
        }).toList();
    }

    private List<DepartmentReportVO> buildDepartmentReport(List<UavDevice> devices,
                                                           List<PatrolTask> patrols,
                                                           List<TransportTask> transports,
                                                           List<FlightPlan> plans,
                                                           List<EventInfo> events,
                                                           List<DataResult> dataResults) {
        List<SysDept> depts = deptMapper.selectList(new LambdaQueryWrapper<SysDept>().orderByAsc(SysDept::getSortOrder));
        List<DepartmentReportVO> result = new ArrayList<>();
        for (SysDept dept : depts) {
            DepartmentReportVO item = new DepartmentReportVO();
            item.setDeptId(dept.getId());
            item.setDeptName(dept.getDeptName());
            item.setDeviceCount(countDeptMatch(devices, dept.getId()));
            item.setPatrolCount(countDeptMatch(patrols, dept.getId()));
            item.setTransportCount(countDeptMatch(transports, dept.getId()));
            item.setPlanCount(countDeptMatch(plans, dept.getId()));
            item.setEventCount(countDeptMatch(events, dept.getId()));
            item.setDataCount(countDeptMatch(dataResults, dept.getId()));
            item.setTotalBusinessCount(item.getPatrolCount() + item.getTransportCount()
                    + item.getPlanCount() + item.getEventCount() + item.getDataCount());
            result.add(item);
        }
        return result;
    }

    private long countDeptMatch(List<?> records, Long deptId) {
        return records.stream().filter(item -> {
            if (item instanceof UavDevice device) return deptId.equals(device.getDeptId());
            if (item instanceof PatrolTask task) return deptId.equals(task.getDeptId());
            if (item instanceof TransportTask task) return deptId.equals(task.getDeptId());
            if (item instanceof FlightPlan plan) return deptId.equals(plan.getDeptId());
            if (item instanceof EventInfo event) return deptId.equals(event.getDeptId());
            if (item instanceof DataResult data) return deptId.equals(data.getDeptId());
            return false;
        }).count();
    }

    private List<NameValueVO> statusDistribution(List<Integer> values, List<String> names) {
        List<NameValueVO> result = new ArrayList<>();
        for (int i = 0; i < names.size(); i++) {
            int status = i;
            result.add(new NameValueVO(names.get(i), values.stream().filter(value -> value != null && value == status).count()));
        }
        return result;
    }

    private <T> Map<String, Long> groupByTime(List<T> records,
                                              Function<T, LocalDateTime> timeGetter,
                                              String granularity,
                                              Map<String, String> labels) {
        return records.stream()
                .filter(item -> timeGetter.apply(item) != null)
                .collect(Collectors.groupingBy(item -> bucketKey(timeGetter.apply(item).toLocalDate(), granularity),
                        Collectors.counting()));
    }

    private String bucketKey(LocalDate date, String granularity) {
        if ("month".equals(granularity)) return date.format(MONTH_FORMAT);
        if ("week".equals(granularity)) {
            LocalDate monday = date.with(WeekFields.of(Locale.CHINA).dayOfWeek(), 1);
            return "W" + monday.format(DAY_FORMAT);
        }
        return date.format(DAY_FORMAT);
    }

    private String bucketLabel(LocalDate date, String key, String granularity) {
        if ("month".equals(granularity)) return key;
        if ("week".equals(granularity)) return key.replace("W", "周始于 ");
        return key;
    }

    private <T> LambdaQueryWrapper<T> timeWrapper(com.baomidou.mybatisplus.core.toolkit.support.SFunction<T, ?> getter,
                                                  LocalDateTime start,
                                                  LocalDateTime end) {
        LambdaQueryWrapper<T> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(getter, start).lt(getter, end);
        return wrapper;
    }


    private BigDecimal rate(long numerator, long denominator) {
        if (denominator == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(numerator * 100L)
                .divide(BigDecimal.valueOf(denominator), 1, RoundingMode.HALF_UP);
    }
}
