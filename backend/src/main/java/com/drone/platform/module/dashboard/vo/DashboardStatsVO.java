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

    /** 事件统计：0待处置 1已派单 2已处置 3已核验 4已销号 */
    private long eventTotal;
    private long eventPending;
    private long eventDispatched;
    private long eventHandled;
    private long eventVerified;
    private long eventClosed;
    private long todayEventCount;
    private long activeEventCount;

    /** 今日新增巡检 + 运输任务 */
    private long todayTaskCount;
    /** 当前执行中的巡检 + 运输任务 */
    private long activeTaskCount;

    /** 近30天业务趋势（按日） */
    private List<TrendPoint> trends;

    /** 环比对比（今日 vs 昨日） */
    private CompareBlock compare;

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

    @Data
    public static class TrendPoint {
        /** MM-dd */
        private String date;
        private long patrolCount;
        private long transportCount;
        private long eventCount;
    }

    @Data
    public static class CompareBlock {
        private long yesterdayTaskCount;
        private double taskChangePct;
        private long yesterdayEventCount;
        private double eventChangePct;
        private long todayAiCount;
        private long yesterdayAiCount;
        private double aiChangePct;
    }

}


