package com.drone.platform.module.task.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("flight_plan")
public class FlightPlan {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String planNo;
    private Long deptId;
    private Long userId;
    private Long deviceId;
    private Long taskId;
    /** 1巡检 2运输 */
    private Integer taskType;
    private LocalDateTime planStartTime;
    private LocalDateTime planEndTime;
    @TableField(exist = false)
    private String flightArea;
    /** 0未校验 1通过 2涉禁飞区 */
    private Integer airspaceCheck;
    /** 0待审 1通过 2驳回 */
    private Integer approvalStatus;
    private Long approvalUserId;
    private LocalDateTime approvalTime;
    private String approvalRemark;
    private LocalDateTime createTime;
}
