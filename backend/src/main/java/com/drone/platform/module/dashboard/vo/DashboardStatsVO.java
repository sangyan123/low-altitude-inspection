package com.drone.platform.module.dashboard.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class DashboardStatsVO {
    /** 设备统计 */
    private long deviceTotal;
    private long onlineCount;
    private long flyingCount;
    private long chargingCount;
    private long offlineCount;
    private long maintainCount;
    private long locatedCount;
    private BigDecimal avgBattery;

    /** 巡检任务统计：0待派单 1执行中 2已完成 3已取消 */
    private long patrolTotal;
    private long patrolPending;
    private long patrolActive;
    private long patrolCompleted;
    private long patrolCancelled;

    /** 运输任务统计：0待起飞 1运输中 2已到达 3已签收 4已取消 */
    private long transportTotal;
    private long transportPending;
    private long transportActive;
    private long transportArrived;
    private long transportSigned;
    private long transportCancelled;

    /** 飞行计划统计：0待审 1通过 2驳回 */
    private long planTotal;
    private long planPending;
    private long planApproved;
    private long planRejected;

    /** 今日新增巡检 + 运输任务 */
    private long todayTaskCount;
    /** 当前执行中的巡检 + 运输任务 */
    private long activeTaskCount;

    /** 大屏地图设备分布 */
    private List<DeviceMarker> devices;

    @Data
    public static class DeviceMarker {
        private Long id;
        private String deviceName;
        private String deviceSn;
        private String model;
        private Integer status;
        private BigDecimal battery;
        private BigDecimal lng;
        private BigDecimal lat;
        private BigDecimal altitude;
        private Long deptId;
        private Integer totalFlightCount;
    }
}