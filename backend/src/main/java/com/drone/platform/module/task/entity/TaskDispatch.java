package com.drone.platform.module.task.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("task_dispatch")
public class TaskDispatch {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 1巡检 2运输 */
    private Integer taskType;
    private Long patrolTaskId;
    private Long transportTaskId;
    private Long deviceId;
    private Long userId;
    private LocalDateTime dispatchTime;
    /** 0待执行 1执行中 2已完成 3已取消 */
    private Integer status;
}
