package com.drone.platform.module.ai.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.drone.platform.common.BusinessException;
import com.drone.platform.module.ai.dto.AiRecognitionRequest;
import com.drone.platform.module.ai.dto.AiRecognitionResponse;
import com.drone.platform.module.ai.entity.AiAlgorithm;
import com.drone.platform.module.ai.entity.AiRecognitionResult;
import com.drone.platform.module.ai.mapper.AiAlgorithmMapper;
import com.drone.platform.module.ai.mapper.AiRecognitionResultMapper;
import com.drone.platform.module.ai.service.engine.AiRecognitionEngine;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AiRecognitionService 单元测试：识别流程、结果处置与守卫逻辑。
 */
@ExtendWith(MockitoExtension.class)
class AiRecognitionServiceTest {

    @Mock
    private AiAlgorithmMapper algorithmMapper;

    @Mock
    private AiRecognitionResultMapper resultMapper;

    @Mock
    private AiRecognitionEngine engine;

    private AiRecognitionService service;

    @BeforeEach
    void initTableMeta() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), AiRecognitionResult.class);
    }

    @BeforeEach
    void setUp() {
        service = new AiRecognitionService(algorithmMapper, List.of(engine), new ObjectMapper());
        ReflectionTestUtils.setField(service, "baseMapper", resultMapper);
    }

    private AiAlgorithm enabledAlgorithm() {
        AiAlgorithm algorithm = new AiAlgorithm();
        algorithm.setId(1L);
        algorithm.setAlgoName("垃圾识别");
        algorithm.setAlgoCategory("市容环卫");
        algorithm.setAlgoDesc("识别暴露垃圾");
        algorithm.setStatus(1);
        return algorithm;
    }

    private AiRecognitionResponse responseWithItem() {
        AiRecognitionResponse.Item item = new AiRecognitionResponse.Item();
        item.setResultType("垃圾堆积");
        item.setConfidence(new BigDecimal("0.9000"));
        item.setBbox(new int[]{10, 20, 110, 120});
        item.setDescription("画面发现成堆生活垃圾");

        AiRecognitionResponse response = new AiRecognitionResponse();
        response.setProvider("mock");
        response.setModelName("mock-vision-1.0");
        response.setRawResponse("{\"hasIssue\":true}");
        response.setItems(List.of(item));
        return response;
    }

    @Test
    @DisplayName("识别：算法不存在/停用/图片为空分别抛异常")
    void recognize_input_guards() {
        when(algorithmMapper.selectById(1L)).thenReturn(null);
        assertThatThrownBy(() -> service.recognize(1L, 1L, "img.jpg", null, null, null, "mock"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("识别算法不存在");

        AiAlgorithm disabled = enabledAlgorithm();
        disabled.setStatus(0);
        when(algorithmMapper.selectById(1L)).thenReturn(disabled);
        assertThatThrownBy(() -> service.recognize(1L, 1L, "img.jpg", null, null, null, "mock"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已停用");

        when(algorithmMapper.selectById(1L)).thenReturn(enabledAlgorithm());
        assertThatThrownBy(() -> service.recognize(1L, 1L, " ", null, null, null, "mock"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("识别图片不能为空");
    }

    @Test
    @DisplayName("识别：不支持引擎的provider被拒绝")
    void recognize_rejects_unknown_provider() {
        when(algorithmMapper.selectById(1L)).thenReturn(enabledAlgorithm());
        when(engine.supports("unknown")).thenReturn(false);

        assertThatThrownBy(() -> service.recognize(1L, 1L, "img.jpg", null, null, null, "unknown"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("暂不支持的识别引擎");
    }

    @Test
    @DisplayName("识别成功：结果逐条落库并填充模型信息与bbox")
    void recognize_success_persists_items() {
        when(algorithmMapper.selectById(1L)).thenReturn(enabledAlgorithm());
        when(engine.supports("mock")).thenReturn(true);
        when(engine.recognize(any(AiRecognitionRequest.class))).thenReturn(responseWithItem());

        List<AiRecognitionResult> results = service.recognize(1L, 1L, "img.jpg", null,
                new BigDecimal("101.7"), new BigDecimal("26.5"), "mock");

        assertThat(results).hasSize(1);
        verify(resultMapper).insert(any(AiRecognitionResult.class));
        ArgumentCaptor<AiRecognitionResult> captor = ArgumentCaptor.forClass(AiRecognitionResult.class);
        verify(resultMapper).insert(captor.capture());
        AiRecognitionResult saved = captor.getValue();
        assertThat(saved.getAlgoId()).isEqualTo(1L);
        assertThat(saved.getResultType()).isEqualTo("垃圾堆积");
        assertThat(saved.getConfidence()).isEqualByComparingTo("0.9000");
        assertThat(saved.getStatus()).isZero();
        assertThat(saved.getSourceType()).isEqualTo(2);
        assertThat(saved.getProvider()).isEqualTo("mock");
        assertThat(saved.getModelName()).isEqualTo("mock-vision-1.0");
        assertThat(saved.getBbox()).contains("10", "110");
        assertThat(saved.getLng()).isEqualByComparingTo("101.7");
    }

    @Test
    @DisplayName("确认：已推送事件的结果不可重复确认")
    void confirm_rejects_pushed_result() {
        AiRecognitionResult result = new AiRecognitionResult();
        result.setId(5L);
        result.setStatus(2);
        when(resultMapper.selectById(5L)).thenReturn(result);

        assertThatThrownBy(() -> service.confirm(5L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已推送事件");
    }

    @Test
    @DisplayName("确认成功：状态转已确认并记录确认人与时间")
    void confirm_success() {
        AiRecognitionResult result = new AiRecognitionResult();
        result.setId(5L);
        result.setStatus(0);
        when(resultMapper.selectById(5L)).thenReturn(result);

        service.confirm(5L, 7L);

        assertThat(result.getStatus()).isEqualTo(1);
        assertThat(result.getConfirmUserId()).isEqualTo(7L);
        assertThat(result.getConfirmTime()).isNotNull();
        verify(resultMapper).updateById(result);
    }

    @Test
    @DisplayName("误报标记成功：状态转误报")
    void markFalseReport_success() {
        AiRecognitionResult result = new AiRecognitionResult();
        result.setId(5L);
        result.setStatus(0);
        when(resultMapper.selectById(5L)).thenReturn(result);

        service.markFalseReport(5L, 7L);

        assertThat(result.getStatus()).isEqualTo(3);
        assertThat(result.getConfirmUserId()).isEqualTo(7L);
        verify(resultMapper).updateById(result);
    }

    @Test
    @DisplayName("删除：已推送事件的结果不可删除，其余正常删除")
    void deleteResult_guards() {
        AiRecognitionResult pushed = new AiRecognitionResult();
        pushed.setId(5L);
        pushed.setStatus(2);
        when(resultMapper.selectById(5L)).thenReturn(pushed);
        assertThatThrownBy(() -> service.deleteResult(5L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能删除");

        AiRecognitionResult normal = new AiRecognitionResult();
        normal.setId(6L);
        normal.setStatus(0);
        when(resultMapper.selectById(6L)).thenReturn(normal);
        service.deleteResult(6L);
        verify(resultMapper).deleteById(6L);
    }
}

