package com.drone.platform.module.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("ai_recognition_result")
public class AiRecognitionResult {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long flightRecordId;
    private Long algoId;

    @TableField(exist = false)
    private String algoName;
    @TableField(exist = false)
    private String algoCategory;

    private String imageUrl;
    private String resultType;
    private BigDecimal confidence;
    private BigDecimal lng;
    private BigDecimal lat;
    private Integer status;
    private Long confirmUserId;
    private LocalDateTime confirmTime;
    private LocalDateTime createTime;

    /** 1飞行识别 2人工上传 */
    private Integer sourceType;
    private String provider;
    private String modelName;
    /** JSON数组，例如 [x1,y1,x2,y2] */
    private String bbox;
    /** AI完整返回JSON */
    private String rawResponse;
    private String errorMessage;
}