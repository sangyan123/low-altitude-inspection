package com.drone.platform.module.ai.service.engine;

import com.drone.platform.common.BusinessException;
import com.drone.platform.module.ai.dto.AiRecognitionRequest;
import com.drone.platform.module.ai.dto.AiRecognitionResponse;
import com.drone.platform.module.ai.entity.AiAlgorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * GlmVisionEngine 单元测试：本地图片读取、GLM响应解析与容错。
 */
@ExtendWith(MockitoExtension.class)
class GlmVisionEngineTest {

    @TempDir
    Path tempDir;

    @Mock
    private RestTemplate restTemplate;

    private GlmVisionEngine engine;

    @BeforeEach
    void setUp() {
        engine = new GlmVisionEngine(new com.fasterxml.jackson.databind.ObjectMapper());
        ReflectionTestUtils.setField(engine, "provider", "glm");
        ReflectionTestUtils.setField(engine, "model", "glm-4.6v-flash");
        ReflectionTestUtils.setField(engine, "baseUrl", "https://open.bigmodel.cn/api/paas/v4");
        ReflectionTestUtils.setField(engine, "uploadDir", tempDir.toString());
        ReflectionTestUtils.setField(engine, "timeoutSeconds", 60);
        ReflectionTestUtils.setField(engine, "restTemplate", restTemplate);
    }

    private AiRecognitionRequest request(String imageUrl) {
        AiAlgorithm algorithm = new AiAlgorithm();
        algorithm.setAlgoName("垃圾识别");
        algorithm.setAlgoCategory("市容环卫");
        algorithm.setAlgoDesc("识别暴露垃圾");
        return AiRecognitionRequest.builder()
                .algorithm(algorithm)
                .imageUrl(imageUrl)
                .build();
    }

    private String createTempImage() throws Exception {
        Path image = tempDir.resolve(Path.of("2026", "10", "13", "test.png"));
        Files.createDirectories(image.getParent());
        Files.write(image, new byte[]{(byte) 0x89, 'P', 'N', 'G'});
        return "/uploads/2026/10/13/test.png";
    }

    @Test
    @DisplayName("识别请求缺少算法时抛业务异常")
    void recognize_requires_algorithm() {
        assertThatThrownBy(() -> engine.recognize(AiRecognitionRequest.builder().imageUrl("x.png").build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("识别算法不能为空");
    }

    @Test
    @DisplayName("图片不存在时抛业务异常")
    void recognize_requires_existing_image() {
        assertThatThrownBy(() -> engine.recognize(request("/uploads/2026/01/01/none.png")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("识别图片不存在");
    }

    @Test
    @DisplayName("非图片扩展名被拒绝")
    void recognize_rejects_non_image_extension() throws Exception {
        Path file = tempDir.resolve(Path.of("2026", "10", "13", "test.bmp"));
        Files.createDirectories(file.getParent());
        Files.write(file, new byte[]{1, 2, 3});

        assertThatThrownBy(() -> engine.recognize(request("/uploads/2026/10/13/test.bmp")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("仅支持");
    }

    @Test
    @DisplayName("GLM返回标准内容时解析出目标列表")
    void recognize_parses_glm_response() throws Exception {
        String imageUrl = createTempImage();
        String content = "{\"hasIssue\":true,\"items\":[{\"type\":\"垃圾堆积\",\"confidence\":0.92,"
                + "\"bbox\":[1,2,3,4],\"description\":\"测试目标\"}]}";
        String escaped = content.replace("\"", "\\\"");
        String raw = "{\"choices\":[{\"message\":{\"content\":\"" + escaped + "\"}}]}";
        when(restTemplate.postForObject(any(URI.class), any(HttpEntity.class), eq(String.class)))
                .thenReturn(raw);

        AiRecognitionResponse response = engine.recognize(request(imageUrl));

        assertThat(response.getProvider()).isEqualTo("glm");
        assertThat(response.getModelName()).isEqualTo("glm-4.6v-flash");
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getResultType()).isEqualTo("垃圾堆积");
        assertThat(response.getItems().get(0).getConfidence()).isEqualByComparingTo("0.92");
        assertThat(response.getItems().get(0).getBbox()).containsExactly(1, 2, 3, 4);
    }

    @Test
    @DisplayName("hasIssue为false时返回空目标列表")
    void recognize_returns_empty_items_when_no_issue() throws Exception {
        String imageUrl = createTempImage();
        String content = "{\"hasIssue\":false,\"items\":[]}";
        String escaped = content.replace("\"", "\\\"");
        String raw = "{\"choices\":[{\"message\":{\"content\":\"" + escaped + "\"}}]}";
        when(restTemplate.postForObject(any(URI.class), any(HttpEntity.class), eq(String.class)))
                .thenReturn(raw);

        assertThat(engine.recognize(request(imageUrl)).getItems()).isEmpty();
    }

    @Test
    @DisplayName("GLM返回非JSON内容时抛业务异常")
    void recognize_rejects_invalid_json() throws Exception {
        String imageUrl = createTempImage();
        String raw = "{\"choices\":[{\"message\":{\"content\":\"抱歉，我无法识别该图片\"}}]}";
        when(restTemplate.postForObject(any(URI.class), any(HttpEntity.class), eq(String.class)))
                .thenReturn(raw);

        assertThatThrownBy(() -> engine.recognize(request(imageUrl)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("未返回有效的识别 JSON");
    }

    @Test
    @DisplayName("调用API异常时转换为业务异常")
    void recognize_wraps_api_errors() throws Exception {
        String imageUrl = createTempImage();
        when(restTemplate.postForObject(any(URI.class), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RuntimeException("connect timeout"));

        assertThatThrownBy(() -> engine.recognize(request(imageUrl)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("GLM 识别请求失败");
    }
}
