package com.drone.platform.module.task.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("transport_task")
public class TransportTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String taskNo;
    /** 1血液运输 2疾控样本 3救援物资 */
    private Integer transportType;
    private Long deptId;
    private Long submitUserId;
    private BigDecimal originLng;
    private BigDecimal originLat;
    private String originName;
    private BigDecimal destLng;
    private BigDecimal destLat;
    private String destName;
    private String cargoName;
    private BigDecimal cargoWeightKg;
    private String receiverName;
    private String receiverPhone;
    private Long deviceId;
    /** 0待起飞 1运输中 2已到达 3已签收 4已取消 */
    private Integer status;
    private LocalDateTime departTime;
    private LocalDateTime arriveTime;
    private LocalDateTime signTime;
    private Long signUserId;
    private String remark;
    private LocalDateTime createTime;
}
