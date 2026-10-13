package com.drone.platform.module.report.vo.export;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/** 统计概览导出行 */
@Data
public class OverviewRow {
    @ExcelProperty("指标")
    private String metric;
    @ExcelProperty("数值")
    private String value;
}
