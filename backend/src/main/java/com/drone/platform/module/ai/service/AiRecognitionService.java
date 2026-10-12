package com.drone.platform.module.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.drone.platform.common.BusinessException;
import com.drone.platform.module.ai.dto.AiRecognitionRequest;
import com.drone.platform.module.ai.dto.AiRecognitionResponse;
import com.drone.platform.module.ai.entity.AiAlgorithm;
import com.drone.platform.module.ai.entity.AiRecognitionResult;
import com.drone.platform.module.ai.mapper.AiAlgorithmMapper;
import com.drone.platform.module.ai.mapper.AiRecognitionResultMapper;
import com.drone.platform.module.ai.service.engine.AiRecognitionEngine;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiRecognitionService extends ServiceImpl<AiRecognitionResultMapper, AiRecognitionResult> {

    private final AiAlgorithmMapper algorithmMapper;
    private final List<AiRecognitionEngine> engines;
    private final ObjectMapper objectMapper;

    public Page<AiRecognitionResult> page(int current, int size, Long algoId,
                                          Integer status, String provider, String keyword) {
        LambdaQueryWrapper<AiRecognitionResult> wrapper = new LambdaQueryWrapper<>();
        if (algoId != null) wrapper.eq(AiRecognitionResult::getAlgoId, algoId);
        if (status != null) wrapper.eq(AiRecognitionResult::getStatus, status);
        if (StringUtils.hasText(provider)) wrapper.eq(AiRecognitionResult::getProvider, provider);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(AiRecognitionResult::getResultType, keyword)
                    .or().like(AiRecognitionResult::getImageUrl, keyword)
                    .or().like(AiRecognitionResult::getModelName, keyword));
        }
        wrapper.orderByDesc(AiRecognitionResult::getId);

        Page<AiRecognitionResult> result = page(new Page<>(current, size), wrapper);
        fillAlgorithmInfo(result.getRecords());
        return result;
    }

    @Transactional
    public List<AiRecognitionResult> recognize(Long userId, Long algoId, String imageUrl,
                                               Long flightRecordId, BigDecimal lng, BigDecimal lat,
                                               String provider) {
        AiAlgorithm algorithm = algorithmMapper.selectById(algoId);
        if (algorithm == null) throw new BusinessException("识别算法不存在");
        if (algorithm.getStatus() == null || algorithm.getStatus() != 1) {
            throw new BusinessException("识别算法已停用");
        }
        if (!StringUtils.hasText(imageUrl)) throw new BusinessException("识别图片不能为空");

        String selectedProvider = StringUtils.hasText(provider) ? provider : "mock";
        AiRecognitionEngine engine = engines.stream()
                .filter(item -> item.supports(selectedProvider))
                .findFirst()
                .orElseThrow(() -> new BusinessException("暂不支持的识别引擎：" + selectedProvider));

        AiRecognitionRequest request = AiRecognitionRequest.builder()
                .imageUrl(imageUrl)
                .algorithm(algorithm)
                .lng(lng)
                .lat(lat)
                .build();

        AiRecognitionResponse response = engine.recognize(request);
        String rawResponse = StringUtils.hasText(response.getRawResponse()) ? response.getRawResponse() : serialize(response);
        LocalDateTime now = LocalDateTime.now();

        List<AiRecognitionResult> results = (response.getItems() == null ? List.<AiRecognitionResponse.Item>of() : response.getItems())
                .stream()
                .map(item -> toResult(item, response, rawResponse, algorithm, imageUrl,
                        flightRecordId, lng, lat, now))
                .toList();

        if (!results.isEmpty()) {
            results.forEach(this::save);
        }
        return results;
    }

    public void confirm(Long id, Long userId) {
        AiRecognitionResult result = getById(id);
        if (result == null) throw new BusinessException("识别结果不存在");
        if (result.getStatus() != null && result.getStatus() == 2) {
            throw new BusinessException("识别结果已推送事件，不能重复确认");
        }
        result.setStatus(1);
        result.setConfirmUserId(userId);
        result.setConfirmTime(LocalDateTime.now());
        updateById(result);
    }

    public void markFalseReport(Long id, Long userId) {
        AiRecognitionResult result = getById(id);
        if (result == null) throw new BusinessException("识别结果不存在");
        if (result.getStatus() != null && result.getStatus() == 2) {
            throw new BusinessException("识别结果已推送事件，不能标记误报");
        }
        result.setStatus(3);
        result.setConfirmUserId(userId);
        result.setConfirmTime(LocalDateTime.now());
        updateById(result);
    }

    public void deleteResult(Long id) {
        AiRecognitionResult result = getById(id);
        if (result == null) throw new BusinessException("识别结果不存在");
        if (result.getStatus() != null && result.getStatus() == 2) {
            throw new BusinessException("已推送事件的识别结果不能删除");
        }
        removeById(id);
    }

    private AiRecognitionResult toResult(AiRecognitionResponse.Item item,
                                         AiRecognitionResponse response,
                                         String rawResponse,
                                         AiAlgorithm algorithm,
                                         String imageUrl,
                                         Long flightRecordId,
                                         BigDecimal lng,
                                         BigDecimal lat,
                                         LocalDateTime now) {
        AiRecognitionResult result = new AiRecognitionResult();
        result.setFlightRecordId(flightRecordId);
        result.setAlgoId(algorithm.getId());
        result.setImageUrl(imageUrl);
        result.setResultType(item.getResultType());
        result.setConfidence(item.getConfidence());
        result.setLng(lng);
        result.setLat(lat);
        result.setStatus(0);
        result.setCreateTime(now);
        result.setSourceType(flightRecordId == null ? 2 : 1);
        result.setProvider(response.getProvider());
        result.setModelName(response.getModelName());
        result.setBbox(serialize(item.getBbox()));
        result.setRawResponse(rawResponse);
        result.setErrorMessage(item.getDescription());
        return result;
    }

    private void fillAlgorithmInfo(List<AiRecognitionResult> records) {
        if (records == null || records.isEmpty()) return;
        Collection<Long> algoIds = records.stream()
                .map(AiRecognitionResult::getAlgoId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        if (algoIds.isEmpty()) return;

        Map<Long, AiAlgorithm> algorithmMap = algorithmMapper.selectBatchIds(algoIds)
                .stream()
                .collect(Collectors.toMap(AiAlgorithm::getId, Function.identity()));
        records.forEach(record -> {
            AiAlgorithm algorithm = algorithmMap.get(record.getAlgoId());
            if (algorithm != null) {
                record.setAlgoName(algorithm.getAlgoName());
                record.setAlgoCategory(algorithm.getAlgoCategory());
            }
        });
    }

    private String serialize(Object value) {
        if (value == null) return null;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new BusinessException("AI识别结果序列化失败");
        }
    }
}
