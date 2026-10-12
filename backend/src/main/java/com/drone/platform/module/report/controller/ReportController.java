package com.drone.platform.module.report.controller;

import com.drone.platform.common.Result;
import com.drone.platform.module.report.service.ReportService;
import com.drone.platform.module.report.vo.ReportVO;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

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
}