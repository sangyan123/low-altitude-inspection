package com.drone.platform.module.device.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("uav_payload")
public class UavPayload {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long deviceId;
    private Integer payloadType;
    private String payloadName;
    private Integer status;
}
