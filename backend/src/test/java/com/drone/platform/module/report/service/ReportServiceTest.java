package com.drone.platform.module.report.service;

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
import com.drone.platform.module.report.vo.ReportVO;
import com.drone.platform.module.system.entity.SysDept;
import com.drone.platform.module.system.mapper.SysDeptMapper;
import com.drone.platform.module.task.entity.FlightPlan;
import com.drone.platform.module.task.entity.PatrolTask;
import com.drone.platform.module.task.entity.TransportTask;
import com.drone.platform.module.task.mapper.FlightPlanMapper;
import com.drone.platform.module.task.mapper.PatrolTaskMapper;
import com.drone.platform.module.task.mapper.TransportTaskMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

/**
 * ReportService 单元测试：报表日期与粒度规整、总览计数、趋势分桶与部门维度。
 */
@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock private UavDeviceMapper deviceMapper;
    @Mock private PatrolTaskMapper patrolMapper;
    @Mock private TransportTaskMapper transportMapper;
    @Mock private FlightPlanMapper planMapper;
    @Mock private AiRecognitionResultMapper aiMapper;
    @Mock private EventInfoMapper eventMapper;
    @Mock private EmergencyEventMapper emergencyMapper;
    @Mock private DataResultMapper dataMapper;
    @Mock private SysDeptMapper deptMapper;

    private ReportService service;

    @BeforeEach
    void setUp() {
        service = new ReportService(deviceMapper, patrolMapper, transportMapper, planMapper,
                aiMapper, eventMapper, emergencyMapper, dataMapper, deptMapper);
    }

    private void stubAllEmpty() {
        when(deviceMapper.selectList(isNull())).thenReturn(List.of());
        when(patrolMapper.selectList(any())).thenReturn(List.of());
        when(transportMapper.selectList(any())).thenReturn(List.of());
        when(planMapper.selectList(any())).thenReturn(List.of());
        when(aiMapper.selectList(any())).thenReturn(List.of());
        when(eventMapper.selectList(any())).thenReturn(List.of());
        when(emergencyMapper.selectList(any())).thenReturn(List.of());
        when(dataMapper.selectList(any())).thenReturn(List.of());
        when(deptMapper.selectList(any())).thenReturn(List.of());
    }

    @Test
    @DisplayName("日期反转自动交换，非法粒度回退day")
    void buildReport_normalizes_inputs() {
        stubAllEmpty();

        ReportVO report = service.buildReport(LocalDate.of(2026, 10, 3),
                LocalDate.of(2026, 10, 1), "quarter", null);

        assertThat(report.getStartDate()).isEqualTo("2026-10-01");
        assertThat(report.getEndDate()).isEqualTo("2026-10-03");
        assertThat(report.getGranularity()).isEqualTo("day");
        assertThat(report.getTrends()).hasSize(3);
    }

    @Test
    @DisplayName("总览计数：设备/巡检/运输/计划/事件/应急/成果各维度统计正确")
    void buildReport_overview_counts() {
        UavDevice online = new UavDevice();
        online.setStatus(1);
        UavDevice flying = new UavDevice();
        flying.setStatus(2);
        UavDevice offline = new UavDevice();
        offline.setStatus(0);
        when(deviceMapper.selectList(isNull())).thenReturn(List.of(online, flying, offline));

        when(patrolMapper.selectList(any())).thenReturn(List.of(patrol(2), patrol(2), patrol(0)));
        when(transportMapper.selectList(any())).thenReturn(List.of(transport(3)));
        when(planMapper.selectList(any())).thenReturn(List.of(plan(1), plan(0)));
        when(aiMapper.selectList(any())).thenReturn(List.of(new AiRecognitionResult(), new AiRecognitionResult()));
        when(eventMapper.selectList(any())).thenReturn(List.of(event(4), event(0)));

        EmergencyEvent ongoing = new EmergencyEvent();
        ongoing.setStatus(0);
        EmergencyEvent ended = new EmergencyEvent();
        ended.setStatus(1);
        when(emergencyMapper.selectList(any())).thenReturn(List.of(ongoing, ended));

        DataResult media = new DataResult();
        media.setDataType(1);
        DataResult media2 = new DataResult();
        media2.setDataType(2);
        DataResult model = new DataResult();
        model.setDataType(3);
        when(dataMapper.selectList(any())).thenReturn(List.of(media, media2, model));
        when(deptMapper.selectList(any())).thenReturn(List.of());

        ReportVO report = service.buildReport(LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 7), "day", null);

        var overview = report.getOverview();
        assertThat(overview.getDeviceTotal()).isEqualTo(3);
        assertThat(overview.getDeviceOnline()).isEqualTo(1);
        assertThat(overview.getDeviceFlying()).isEqualTo(1);
        assertThat(overview.getPatrolTotal()).isEqualTo(3);
        assertThat(overview.getPatrolCompleted()).isEqualTo(2);
        assertThat(overview.getPatrolCompletionRate()).isEqualByComparingTo("66.7");
        assertThat(overview.getTransportSigned()).isEqualTo(1);
        assertThat(overview.getTransportSignedRate()).isEqualByComparingTo("100.0");
        assertThat(overview.getPlanApprovalRate()).isEqualByComparingTo("50.0");
        assertThat(overview.getAiRecognitionTotal()).isEqualTo(2);
        assertThat(overview.getEventClosureRate()).isEqualByComparingTo("50.0");
        assertThat(overview.getEmergencyTotal()).isEqualTo(2);
        assertThat(overview.getEmergencyOngoing()).isEqualTo(1);
        assertThat(overview.getDataMediaTotal()).isEqualTo(2);
        assertThat(overview.getDataModelTotal()).isEqualTo(1);
    }

    @Test
    @DisplayName("周粒度趋势：同周一整周合并为一个点")
    void buildReport_weekly_buckets() {
        stubAllEmpty();

        ReportVO report = service.buildReport(LocalDate.of(2026, 10, 5),
                LocalDate.of(2026, 10, 11), "week", null);

        assertThat(report.getGranularity()).isEqualTo("week");
        assertThat(report.getTrends()).hasSize(2);
        assertThat(report.getTrends().get(0).getTimeLabel()).startsWith("周始于");
    }

    @Test
    @DisplayName("部门维度：按部门统计设备数量")
    void buildReport_department_breakdown() {
        UavDevice dept1Device = new UavDevice();
        dept1Device.setStatus(1);
        dept1Device.setDeptId(1L);
        UavDevice dept2Device = new UavDevice();
        dept2Device.setStatus(0);
        dept2Device.setDeptId(2L);
        when(deviceMapper.selectList(isNull())).thenReturn(List.of(dept1Device, dept2Device));
        when(patrolMapper.selectList(any())).thenReturn(List.of());
        when(transportMapper.selectList(any())).thenReturn(List.of());
        when(planMapper.selectList(any())).thenReturn(List.of());
        when(aiMapper.selectList(any())).thenReturn(List.of());
        when(eventMapper.selectList(any())).thenReturn(List.of());
        when(emergencyMapper.selectList(any())).thenReturn(List.of());
        when(dataMapper.selectList(any())).thenReturn(List.of());

        SysDept dept1 = new SysDept();
        dept1.setId(1L);
        dept1.setDeptName("城管一队");
        SysDept dept2 = new SysDept();
        dept2.setId(2L);
        dept2.setDeptName("城管二队");
        when(deptMapper.selectList(any())).thenReturn(List.of(dept1, dept2));

        ReportVO report = service.buildReport(LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 7), "day", null);

        assertThat(report.getDepartments()).hasSize(2);
        assertThat(report.getDepartments().get(0).getDeptName()).isEqualTo("城管一队");
        assertThat(report.getDepartments().get(0).getDeviceCount()).isEqualTo(1);
        assertThat(report.getDepartments().get(1).getDeviceCount()).isEqualTo(1);
    }

    private PatrolTask patrol(int status) {
        PatrolTask task = new PatrolTask();
        task.setStatus(status);
        task.setCreateTime(LocalDateTime.of(2026, 10, 2, 10, 0));
        return task;
    }

    private TransportTask transport(int status) {
        TransportTask task = new TransportTask();
        task.setStatus(status);
        task.setCreateTime(LocalDateTime.of(2026, 10, 2, 10, 0));
        return task;
    }

    private FlightPlan plan(int approvalStatus) {
        FlightPlan plan = new FlightPlan();
        plan.setApprovalStatus(approvalStatus);
        plan.setCreateTime(LocalDateTime.of(2026, 10, 2, 10, 0));
        return plan;
    }

    private EventInfo event(int status) {
        EventInfo event = new EventInfo();
        event.setStatus(status);
        event.setCreateTime(LocalDateTime.of(2026, 10, 2, 10, 0));
        return event;
    }
}
