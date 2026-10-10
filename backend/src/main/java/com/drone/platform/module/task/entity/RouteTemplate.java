package com.drone.platform.module.task.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("route_template")
public class RouteTemplate {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String routeName;
    private String sceneType;
    private String routeData;
    private BigDecimal estDistanceKm;
    private Integer estDurationMin;
    private Long deptId;
    private Integer status;
    private LocalDateTime createTime;
}
