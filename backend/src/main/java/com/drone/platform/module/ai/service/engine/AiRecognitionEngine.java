package com.drone.platform.module.ai.service.engine;

import com.drone.platform.module.ai.dto.AiRecognitionRequest;
import com.drone.platform.module.ai.dto.AiRecognitionResponse;

public interface AiRecognitionEngine {
    String provider();
    boolean supports(String provider);
    AiRecognitionResponse recognize(AiRecognitionRequest request);
}