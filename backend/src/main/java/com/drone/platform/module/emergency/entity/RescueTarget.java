package com.drone.platform.module.emergency.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("rescue_target")
public class RescueTarget {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long emergencyEventId;
    private String targetName;
    /** 1受困人员 2火点 3险情点 */
    private Integer targetType;
    private BigDecimal lng;
    private BigDecimal lat;
    private Long foundByDevice;
    private LocalDateTime foundTime;
    /** 1待核实 2已确认 3已处置 */
    private Integer status;
    /** 1追踪中 0已停止 */
    private Integer tracking;
    /** 1已分享 0未分享 */
    private Integer shared;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String targetTypeName;
    @TableField(exist = false)
    private String statusName;
    @TableField(exist = false)
    private String foundByDeviceName;
}