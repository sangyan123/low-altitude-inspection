package com.drone.platform.module.report.vo;

import lombok.Data;

@Data
public class ReportTrendPointVO {
    private String timeLabel;
    private long deviceCount;
    private long patrolCount;
    private long transportCount;
    private long planCount;
    private long aiCount;
    private long eventCount;
    private long emergencyCount;
    private long dataCount;
}