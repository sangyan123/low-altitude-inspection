package com.drone.platform.module.ai.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class AiRecognitionResponse {
    private String provider;
    private String modelName;
    private String rawResponse;
    private List<Item> items;

    @Data
    public static class Item {
        private String resultType;
        private BigDecimal confidence;
        /** [x1,y1,x2,y2] */
        private int[] bbox;
        private String description;
    }
}