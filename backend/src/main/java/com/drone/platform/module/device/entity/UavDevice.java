package com.drone.platform.module.device.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("uav_device")
public class UavDevice {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String deviceSn;
    private String deviceName;
    private String model;
    private Integer deviceType;
    private Long deptId;
    private Integer status;
    private BigDecimal battery;
    private BigDecimal lng;
    private BigDecimal lat;
    private BigDecimal altitude;
    private Integer commMode;
    private Integer maxFlightTime;
    private BigDecimal maxPayloadKg;
    private LocalDate purchaseDate;
    private LocalDate warrantyEnd;
    private BigDecimal totalFlightHours;
    private Integer totalFlightCount;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
