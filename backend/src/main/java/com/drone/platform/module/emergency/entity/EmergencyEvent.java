package com.drone.platform.module.emergency.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("emergency_event")
public class EmergencyEvent {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String eventName;
    /** 1洪涝 2山火 3地质灾害 4事故 5搜救 */
    private Integer eventType;
    /** 1一般 2较大 3重大 4特别重大 */
    private Integer level;
    private Long reportUserId;
    private BigDecimal lng;
    private BigDecimal lat;
    private String locationDesc;
    private String description;
    /** 0进行中 1已结束 */
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime endTime;
    private String closeRemark;

    private Long assignedDeviceId;
    private Long dispatchUserId;
    private LocalDateTime dispatchTime;
    private String dispatchRemark;
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String reporterName;
    @TableField(exist = false)
    private String dispatchUserName;
    @TableField(exist = false)
    private String assignedDeviceName;
    @TableField(exist = false)
    private Long targetCount;
    @TableField(exist = false)
    private Long trackingCount;
    @TableField(exist = false)
    private List<RescueTarget> targets;
}