package com.drone.platform.module.report.vo;

import lombok.Data;

@Data
public class DepartmentReportVO {
    private Long deptId;
    private String deptName;
    private long deviceCount;
    private long patrolCount;
    private long transportCount;
    private long planCount;
    private long eventCount;
    private long dataCount;
    private long totalBusinessCount;
}