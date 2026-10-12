package com.drone.platform.module.report.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ReportOverviewVO {
    private long deviceTotal;
    private long deviceOnline;
    private long deviceFlying;

    private long patrolTotal;
    private long patrolCompleted;
    private BigDecimal patrolCompletionRate;

    private long transportTotal;
    private long transportSigned;
    private BigDecimal transportSignedRate;

    private long planTotal;
    private long planApproved;
    private BigDecimal planApprovalRate;

    private long aiRecognitionTotal;
    private long eventTotal;
    private long eventClosed;
    private BigDecimal eventClosureRate;

    private long emergencyTotal;
    private long emergencyOngoing;

    private long dataResultTotal;
    private long dataMediaTotal;
    private long dataModelTotal;
}