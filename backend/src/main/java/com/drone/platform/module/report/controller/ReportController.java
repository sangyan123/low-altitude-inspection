package com.drone.platform.module.report.controller;

import com.alibaba.excel.EasyExcel;
import com.drone.platform.common.Result;
import com.drone.platform.module.report.pdf.ReportPdfWriter;
import com.drone.platform.module.report.service.ReportService;
import com.drone.platform.module.report.vo.DepartmentReportVO;
import com.drone.platform.module.report.vo.ReportOverviewVO;
import com.drone.platform.module.report.vo.ReportTrendPointVO;
import com.drone.platform.module.report.vo.ReportVO;
import com.drone.platform.module.report.vo.export.DeptRow;
import com.drone.platform.module.report.vo.export.OverviewRow;
import com.drone.platform.module.report.vo.export.TrendRow;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/overview")
    public Result<ReportVO> overview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "day") String granularity,
            @RequestParam(required = false) Long deptId) {
        return Result.ok(reportService.buildReport(startDate, endDate, granularity, deptId));
    }

    @GetMapping("/trend")
    public Result<ReportVO> trend(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "day") String granularity,
            @RequestParam(required = false) Long deptId) {
        return Result.ok(reportService.buildReport(startDate, endDate, granularity, deptId));
    }

    @GetMapping("/department")
    public Result<ReportVO> department(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "day") String granularity,
            @RequestParam(required = false) Long deptId) {
        return Result.ok(reportService.buildReport(startDate, endDate, granularity, deptId));
    }

    /** 导出 Excel：三个 sheet（统计概览 / 业务趋势 / 部门明细） */
    @GetMapping("/export/excel")
    public void exportExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "day") String granularity,
            @RequestParam(required = false) Long deptId,
            HttpServletResponse response) throws IOException {
        ReportVO report = reportService.buildReport(startDate, endDate, granularity, deptId);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("统计报表_" + safe(report.getStartDate()) + "_" + safe(report.getEndDate()),
                StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        try (com.alibaba.excel.ExcelWriter writer = EasyExcel.write(response.getOutputStream()).build()) {
            writer.write(overviewRows(report), EasyExcel.writerSheet(0, "统计概览").head(OverviewRow.class).build());
            writer.write(trendRows(report), EasyExcel.writerSheet(1, "业务趋势").head(TrendRow.class).build());
            writer.write(deptRows(report), EasyExcel.writerSheet(2, "部门明细").head(DeptRow.class).build());
        }
    }

    /** 导出 PDF：概览 + 部门明细 */
    @GetMapping("/export/pdf")
    public void exportPdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "day") String granularity,
            @RequestParam(required = false) Long deptId,
            HttpServletResponse response) throws IOException {
        ReportVO report = reportService.buildReport(startDate, endDate, granularity, deptId);
        response.setContentType("application/pdf");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("统计报表_" + safe(report.getStartDate()) + "_" + safe(report.getEndDate()),
                StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment;filename*=utf-8''" + fileName + ".pdf");
        ReportPdfWriter.write(report, response.getOutputStream());
    }

    private List<OverviewRow> overviewRows(ReportVO report) {
        ReportOverviewVO o = report.getOverview();
        List<OverviewRow> rows = new ArrayList<>();
        rows.add(row("报表周期", safe(report.getStartDate()) + " 至 " + safe(report.getEndDate())));
        rows.add(row("统计粒度", granularityName(report.getGranularity())));
        rows.add(row("设备总数", str(o.getDeviceTotal())));
        rows.add(row("在线设备", str(o.getDeviceOnline())));
        rows.add(row("飞行中设备", str(o.getDeviceFlying())));
        rows.add(row("巡检任务", str(o.getPatrolTotal())));
        rows.add(row("巡检完成", str(o.getPatrolCompleted()) + "（" + rate(o.getPatrolCompletionRate()) + "）"));
        rows.add(row("运输任务", str(o.getTransportTotal())));
        rows.add(row("运输签收", str(o.getTransportSigned()) + "（" + rate(o.getTransportSignedRate()) + "）"));
        rows.add(row("飞行计划", str(o.getPlanTotal())));
        rows.add(row("审批通过", str(o.getPlanApproved()) + "（" + rate(o.getPlanApprovalRate()) + "）"));
        rows.add(row("AI识别次数", str(o.getAiRecognitionTotal())));
        rows.add(row("事件总数", str(o.getEventTotal())));
        rows.add(row("事件销号", str(o.getEventClosed()) + "（" + rate(o.getEventClosureRate()) + "）"));
        rows.add(row("应急事件", str(o.getEmergencyTotal())));
        rows.add(row("进行中应急", str(o.getEmergencyOngoing())));
        rows.add(row("成果文件", str(o.getDataResultTotal())));
        rows.add(row("影像/视频成果", str(o.getDataMediaTotal())));
        rows.add(row("三维模型成果", str(o.getDataModelTotal())));
        return rows;
    }

    private List<TrendRow> trendRows(ReportVO report) {
        List<TrendRow> rows = new ArrayList<>();
        for (ReportTrendPointVO t : report.getTrends()) {
            TrendRow row = new TrendRow();
            row.setTimeLabel(t.getTimeLabel());
            row.setDeviceCount(t.getDeviceCount());
            row.setPatrolCount(t.getPatrolCount());
            row.setTransportCount(t.getTransportCount());
            row.setPlanCount(t.getPlanCount());
            row.setAiCount(t.getAiCount());
            row.setEventCount(t.getEventCount());
            row.setEmergencyCount(t.getEmergencyCount());
            row.setDataCount(t.getDataCount());
            rows.add(row);
        }
        return rows;
    }

    private List<DeptRow> deptRows(ReportVO report) {
        List<DeptRow> rows = new ArrayList<>();
        for (DepartmentReportVO d : report.getDepartments()) {
            DeptRow row = new DeptRow();
            row.setDeptName(d.getDeptName());
            row.setDeviceCount(d.getDeviceCount());
            row.setPatrolCount(d.getPatrolCount());
            row.setTransportCount(d.getTransportCount());
            row.setPlanCount(d.getPlanCount());
            row.setEventCount(d.getEventCount());
            row.setDataCount(d.getDataCount());
            row.setTotalBusinessCount(d.getTotalBusinessCount());
            rows.add(row);
        }
        return rows;
    }

    private OverviewRow row(String metric, String value) {
        OverviewRow r = new OverviewRow();
        r.setMetric(metric);
        r.setValue(value);
        return r;
    }

    private String granularityName(String g) {
        if (g == null) return "按日";
        return switch (g) {
            case "month" -> "按月";
            case "week" -> "按周";
            default -> "按日";
        };
    }

    private String rate(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(0, RoundingMode.HALF_UP).toPlainString() + "%";
    }

    private String str(long value) {
        return String.valueOf(value);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
