package com.drone.platform.module.ai.service.engine;

import com.drone.platform.common.BusinessException;
import com.drone.platform.module.ai.dto.AiRecognitionRequest;
import com.drone.platform.module.ai.dto.AiRecognitionResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 智谱 GLM 视觉理解引擎。
 * 图片优先从本地 uploads 目录读取并转换为 Base64，避免依赖公网回调地址。
 */
@Slf4j
@Component
public class GlmVisionEngine implements AiRecognitionEngine {

    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    /**
     * 直接把智谱 API Key 填到这里。
     * 注意：不要把真实 Key 提交到 Git。
     */
    private static final String GLM_API_KEY = "xxxxx";

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Value("${ai.glm.provider:glm}")
    private String provider;


    @Value("${ai.glm.model:glm-4.6v-flash}")
    private String model;

    @Value("${ai.glm.base-url:https://open.bigmodel.cn/api/paas/v4}")
    private String baseUrl;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Value("${ai.glm.timeout-seconds:60}")
    private int timeoutSeconds;

    public GlmVisionEngine(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(60_000);
        this.restTemplate = new RestTemplate(factory);
    }

    @Override
    public String provider() {
        return provider;
    }

    @Override
    public boolean supports(String selectedProvider) {
        return provider().equalsIgnoreCase(selectedProvider);
    }

    @Override
    public AiRecognitionResponse recognize(AiRecognitionRequest request) {
        if (!StringUtils.hasText(GLM_API_KEY)) {
            throw new BusinessException("GLM API Key 未配置，请在 GlmVisionEngine 中填写 GLM_API_KEY");
        }
        if (request.getAlgorithm() == null) {
            throw new BusinessException("识别算法不能为空");
        }

        String imageData = readImageAsDataUrl(request.getImageUrl());
        String prompt = buildPrompt(request.getAlgorithm().getAlgoName(),
                request.getAlgorithm().getAlgoCategory(),
                request.getAlgorithm().getAlgoDesc());

        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        body.put("temperature", 0.2);
        ArrayNode messages = body.putArray("messages");
        ObjectNode message = messages.addObject();
        message.put("role", "user");
        ArrayNode content = message.putArray("content");
        ObjectNode imagePart = content.addObject();
        imagePart.put("type", "image_url");
        imagePart.putObject("image_url").put("url", imageData);
        ObjectNode textPart = content.addObject();
        textPart.put("type", "text");
        textPart.put("text", prompt);

        String requestBody;
        try {
            requestBody = objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new BusinessException("构造GLM识别请求失败：" + e.getMessage());
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(GLM_API_KEY);

        String endpoint = normalizeBaseUrl(baseUrl) + "/chat/completions";
        if (timeoutSeconds != 60) {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(10_000);
            factory.setReadTimeout(timeoutSeconds * 1000);
            restTemplate.setRequestFactory(factory);
        }

        String rawResponse = null;
        BusinessException lastError = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                rawResponse = restTemplate.postForObject(URI.create(endpoint),
                        new HttpEntity<>(requestBody, headers), String.class);
                break;
            } catch (HttpStatusCodeException e) {
                int status = e.getStatusCode().value();
                String detail = e.getResponseBodyAsString();
                log.warn("GLM vision API call failed: attempt={}, status={}, body={}", attempt, status, detail);
                if (status == 429 || status >= 500) {
                    lastError = new BusinessException("GLM 识别请求失败：HTTP " + status + " - " + detail);
                    if (attempt < 3) {
                        try {
                            Thread.sleep(3000L * attempt);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new BusinessException("GLM 识别请求被中断");
                        }
                        continue;
                    }
                }
                throw new BusinessException("GLM 识别请求失败：HTTP " + status + " - " + detail);
            } catch (Exception e) {
                log.error("GLM vision API call failed", e);
                throw new BusinessException("GLM 识别请求失败：" + e.getMessage());
            }
        }
        if (lastError != null && !StringUtils.hasText(rawResponse)) {
            throw lastError;
        }
        if (!StringUtils.hasText(rawResponse)) {
            throw new BusinessException("GLM 识别返回为空");
        }

        String modelText = extractModelText(rawResponse);
        JsonNode normalized = parseNormalizedJson(modelText);
        return toResponse(normalized, rawResponse);
    }

    private String buildPrompt(String algorithmName, String algorithmCategory, String algorithmDescription) {
        String target = StringUtils.hasText(algorithmName) ? algorithmName : "城市治理异常";
        String category = StringUtils.hasText(algorithmCategory) ? algorithmCategory : "无人机巡查";
        String desc = StringUtils.hasText(algorithmDescription) ? algorithmDescription : "识别画面中的异常目标";
        return """
                你是无人机政务巡查视觉识别助手，请严格按照用户选择的巡查算法分析图片。

                巡查算法：%s
                算法分类：%s
                算法说明：%s

                请识别图片中与该算法相关的异常或目标。如果没有明确证据，hasIssue 返回 false，items 返回空数组。
                不要输出无关场景。结果必须只返回一个 JSON 对象，不要 Markdown，不要解释文字。

                JSON 字段要求：
                {
                  "hasIssue": true 或 false,
                  "items": [
                    {
                      "type": "识别结果类型，通常使用巡查算法名称",
                      "confidence": 0到1的小数,
                      "bbox": [x1, y1, x2, y2],
                      "description": "简要说明判断依据和目标位置"
                    }
                  ]
                }

                如果无法确定目标框，bbox 可以返回 null。
                """.formatted(target, category, desc);
    }

    private String readImageAsDataUrl(String imageUrl) {
        if (!StringUtils.hasText(imageUrl)) throw new BusinessException("识别图片不能为空");

        try {
            Path path = resolveLocalUpload(imageUrl);
            if (path == null || !Files.exists(path)) {
                throw new BusinessException("识别图片不存在，请重新上传");
            }
            String lower = path.toString().toLowerCase();
            String extension = lower.substring(lower.lastIndexOf('.') + 1);
            if (!IMAGE_EXTENSIONS.contains(extension)) {
                throw new BusinessException("GLM 识别仅支持 JPG、JPEG、PNG、WEBP 图片");
            }
            String mimeType = switch (extension) {
                case "jpg", "jpeg" -> "image/jpeg";
                case "png" -> "image/png";
                default -> "image/webp";
            };
            byte[] bytes = Files.readAllBytes(path);
            if (bytes.length > 10 * 1024 * 1024) {
                throw new BusinessException("识别图片不能超过 10MB");
            }
            return "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Read AI image failed: {}", imageUrl, e);
            throw new BusinessException("读取识别图片失败");
        }
    }

    private Path resolveLocalUpload(String imageUrl) throws Exception {
        URI uri = new URI(imageUrl.trim());
        String path = uri.isAbsolute() ? uri.getPath() : imageUrl.trim();

        int index = path.indexOf("/uploads/");
        if (index < 0) return null;

        String relative = path.substring(index + "/uploads/".length());
        relative = relative.replace("\\", "/");
        if (relative.contains("..")) throw new BusinessException("图片地址不合法");

        Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
        return root.resolve(relative).normalize();
    }

    private String extractModelText(String rawResponse) {
        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (content.isMissingNode()) return rawResponse;
            if (content.isTextual()) return content.asText();
            return objectMapper.writeValueAsString(content);
        } catch (Exception e) {
            return rawResponse;
        }
    }

    private JsonNode parseNormalizedJson(String modelText) {
        String text = modelText == null ? "" : modelText.trim();
        try {
            return objectMapper.readTree(text);
        } catch (Exception ignored) {
            // continue extracting JSON block
        }

        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            String candidate = text.substring(start, end + 1)
                    .replace("```json", "")
                    .replace("```", "");
            try {
                return objectMapper.readTree(candidate);
            } catch (Exception e) {
                log.warn("GLM returned invalid JSON: {}", text);
            }
        }
        throw new BusinessException("GLM 未返回有效的识别 JSON");
    }

    private AiRecognitionResponse toResponse(JsonNode normalized, String rawResponse) {
        AiRecognitionResponse response = new AiRecognitionResponse();
        response.setProvider(provider());
        response.setModelName(model);
        response.setRawResponse(rawResponse);

        boolean hasIssue = normalized.path("hasIssue").asBoolean(true);
        List<AiRecognitionResponse.Item> items = new ArrayList<>();
        JsonNode itemNodes = normalized.path("items");
        if (hasIssue && itemNodes.isArray()) {
            for (JsonNode node : itemNodes) {
                AiRecognitionResponse.Item item = new AiRecognitionResponse.Item();
                item.setResultType(node.path("type").asText("AI识别目标"));
                double confidence = node.path("confidence").asDouble(0.75);
                item.setConfidence(BigDecimal.valueOf(confidence)
                        .setScale(4, RoundingMode.HALF_UP));
                JsonNode bbox = node.path("bbox");
                if (bbox.isArray() && bbox.size() >= 4) {
                    int[] box = new int[4];
                    for (int i = 0; i < 4; i++) {
                        box[i] = Math.max(0, bbox.path(i).asInt());
                    }
                    item.setBbox(box);
                }
                item.setDescription(node.path("description").asText("GLM识别到疑似目标"));
                items.add(item);
            }
        }
        response.setItems(items);
        return response;
    }

    private String normalizeBaseUrl(String value) {
        return StringUtils.hasText(value)
                ? value.replaceAll("/+$", "")
                : "https://open.bigmodel.cn/api/paas/v4";
    }
}
