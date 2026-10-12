package com.drone.platform.module.event.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("event_info")
public class EventInfo {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String eventNo;
    private String eventType;
    /** 1AI识别 2人工上报 */
    private Integer eventSource;
    private Long recognitionId;
    private Long deptId;
    /** 0待处置 1已派单 2已处置 3已核验 4已销号 */
    private Integer status;
    private String pushSystem;
    private LocalDateTime pushTime;
    private LocalDateTime dispatchTime;
    private LocalDateTime handleTime;
    private String handleRemark;
    private Long handleUserId;
    private Long verifyUserId;
    private LocalDateTime verifyTime;
    private Integer verifyResult;
    private String verifyRemark;
    private LocalDateTime closeTime;
    private BigDecimal lng;
    private BigDecimal lat;
    private String description;
    private Long submitUserId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String deptName;
    @TableField(exist = false)
    private String submitUserName;
    @TableField(exist = false)
    private String handleUserName;
    @TableField(exist = false)
    private String verifyUserName;

    @TableField(exist = false)
    private String imageUrl;
    @TableField(exist = false)
    private String resultType;
    @TableField(exist = false)
    private BigDecimal confidence;
    @TableField(exist = false)
    private String provider;
    @TableField(exist = false)
    private String modelName;
    @TableField(exist = false)
    private String rawResponse;

    @TableField(exist = false)
    private List<EventHandleLog> handleLogs;
}