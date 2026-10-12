package com.drone.platform.module.data.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("data_result")
public class DataResult {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long flightRecordId;
    /** 1视频 2照片 3DOM 4DSM 5点云 6三维模型 */
    private Integer dataType;
    private String fileName;
    private String fileUrl;
    private String thumbnailUrl;
    private BigDecimal fileSizeMb;
    private String modelFormat;
    private LocalDateTime captureTime;
    private BigDecimal lng;
    private BigDecimal lat;
    private Long deptId;
    private String authScope;
    private Long submitUserId;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String deptName;
    @TableField(exist = false)
    private String submitUserName;
    @TableField(exist = false)
    private String dataTypeName;
    @TableField(exist = false)
    private String categoryName;
}