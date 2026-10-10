package com.drone.platform.module.task.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("patrol_task")
public class PatrolTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String taskNo;
    private String taskName;
    /** 1常态巡检 2临时任务 3应急任务 */
    private Integer taskType;
    private String sceneType;
    private Long deptId;
    private Long submitUserId;
    /** 目标区域多边形，暂不通过实体映射 */
    @TableField(exist = false)
    private String areaGeom;
    private Long routeId;
    /** 1紧急 2普通 3低 */
    private Integer priority;
    /** 0待派单 1执行中 2已完成 3已取消 */
    private Integer status;
    private LocalDateTime planStartTime;
    private LocalDateTime planEndTime;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
