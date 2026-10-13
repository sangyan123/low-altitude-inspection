package com.drone.platform.module.report.vo.export;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/** 部门明细导出行 */
@Data
public class DeptRow {
    @ExcelProperty("部门")
    private String deptName;
    @ExcelProperty("设备")
    private Long deviceCount;
    @ExcelProperty("巡检")
    private Long patrolCount;
    @ExcelProperty("运输")
    private Long transportCount;
    @ExcelProperty("计划")
    private Long planCount;
    @ExcelProperty("事件")
    private Long eventCount;
    @ExcelProperty("成果")
    private Long dataCount;
    @ExcelProperty("业务合计")
    private Long totalBusinessCount;
}
