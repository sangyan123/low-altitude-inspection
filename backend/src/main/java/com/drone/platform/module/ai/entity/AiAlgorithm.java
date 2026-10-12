package com.drone.platform.module.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_algorithm")
public class AiAlgorithm {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String algoName;
    private String algoCategory;
    private String algoDesc;
    private String modelVersion;
    private Integer status;
    private LocalDateTime createTime;
}