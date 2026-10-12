package com.drone.platform.module.ai.service.engine;

import com.drone.platform.module.ai.dto.AiRecognitionRequest;
import com.drone.platform.module.ai.dto.AiRecognitionResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 开发联调用的模拟识别引擎。
 * 后续接入 GLM / 通义 / 豆包 / 本地 YOLO 时，只需按同一接口新增实现类。
 */
@Component
public class MockVisionEngine implements AiRecognitionEngine {

    @Override
    public String provider() {
        return "mock";
    }

    @Override
    public boolean supports(String provider) {
        return provider().equalsIgnoreCase(provider);
    }

    @Override
    public AiRecognitionResponse recognize(AiRecognitionRequest request) {
        AiAlgorithmSnapshot algorithm = AiAlgorithmSnapshot.of(request);
        Random random = ThreadLocalRandom.current();
        boolean hasTarget = random.nextInt(100) < 82;
        AiRecognitionResponse response = new AiRecognitionResponse();
        response.setProvider(provider());
        response.setModelName("mock-vision-1.0");

        if (!hasTarget) {
            response.setRawResponse("{\"hasIssue\":false,\"items\":[]}");
            response.setItems(List.of());
            return response;
        }

        AiRecognitionResponse.Item item = new AiRecognitionResponse.Item();
        item.setResultType(StringUtils.hasText(algorithm.name()) ? algorithm.name() : "通用目标");
        item.setConfidence(BigDecimal.valueOf(0.72 + random.nextDouble() * 0.25)
                .setScale(4, RoundingMode.HALF_UP));
        item.setBbox(new int[]{random.nextInt(180) + 40, random.nextInt(140) + 40,
                random.nextInt(220) + 520, random.nextInt(180) + 420});
        item.setDescription(String.format("模拟识别：画面中疑似存在%s目标，建议人工确认。",
                item.getResultType()));

        response.setItems(List.of(item));
        response.setRawResponse(String.format(
                "{\"hasIssue\":true,\"provider\":\"%s\",\"model\":\"%s\",\"items\":[" +
                        "{\"type\":\"%s\",\"confidence\":%s,\"bbox\":[%d,%d,%d,%d]}]}",
                provider(), response.getModelName(), item.getResultType(),
                item.getConfidence(), item.getBbox()[0], item.getBbox()[1],
                item.getBbox()[2], item.getBbox()[3]));
        return response;
    }

    private record AiAlgorithmSnapshot(String name) {
        private static AiAlgorithmSnapshot of(AiRecognitionRequest request) {
            if (request.getAlgorithm() == null || !StringUtils.hasText(request.getAlgorithm().getAlgoName())) {
                return new AiAlgorithmSnapshot(null);
            }
            return new AiAlgorithmSnapshot(request.getAlgorithm().getAlgoName());
        }
    }
}