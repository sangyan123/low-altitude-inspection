package com.drone.platform.module.report.vo.export;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/** 业务趋势导出行 */
@Data
public class TrendRow {
    @ExcelProperty("时间")
    private String timeLabel;
    @ExcelProperty("设备")
    private Long deviceCount;
    @ExcelProperty("巡检")
    private Long patrolCount;
    @ExcelProperty("运输")
    private Long transportCount;
    @ExcelProperty("计划")
    private Long planCount;
    @ExcelProperty("AI识别")
    private Long aiCount;
    @ExcelProperty("事件")
    private Long eventCount;
    @ExcelProperty("应急")
    private Long emergencyCount;
    @ExcelProperty("成果")
    private Long dataCount;
}
