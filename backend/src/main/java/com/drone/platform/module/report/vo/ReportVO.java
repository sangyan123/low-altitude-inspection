package com.drone.platform.module.report.vo;

import lombok.Data;

import java.util.List;

@Data
public class ReportVO {
    private String startDate;
    private String endDate;
    private String granularity;
    private ReportOverviewVO overview;
    private List<ReportTrendPointVO> trends;
    private List<DepartmentReportVO> departments;
    private List<NameValueVO> patrolStatus;
    private List<NameValueVO> transportStatus;
    private List<NameValueVO> eventStatus;
    private List<NameValueVO> emergencyType;
    private List<NameValueVO> dataType;
}