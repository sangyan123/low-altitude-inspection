package com.drone.platform.module.ai.dto;

import com.drone.platform.module.ai.entity.AiAlgorithm;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class AiRecognitionRequest {
    private String imageUrl;
    private AiAlgorithm algorithm;
    private BigDecimal lng;
    private BigDecimal lat;
}