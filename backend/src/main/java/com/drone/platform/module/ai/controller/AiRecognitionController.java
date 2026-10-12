package com.drone.platform.module.ai.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.drone.platform.common.PageResult;
import com.drone.platform.common.Result;
import com.drone.platform.module.ai.entity.AiRecognitionResult;
import com.drone.platform.module.ai.service.AiRecognitionService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/ai/recognition")
@RequiredArgsConstructor
@Validated
public class AiRecognitionController {

    private final AiRecognitionService recognitionService;

    @GetMapping("/page")
    public Result<PageResult<AiRecognitionResult>> page(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long algoId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String keyword) {
        return Result.ok(PageResult.of(recognitionService.page(current, size, algoId, status, provider, keyword)));
    }

    @GetMapping("/{id}")
    public Result<AiRecognitionResult> getById(@PathVariable Long id) {
        return Result.ok(recognitionService.getById(id));
    }

    @PostMapping
    public Result<List<AiRecognitionResult>> recognize(
            @RequestBody @Validated RecognizeRequest request,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        List<AiRecognitionResult> results = recognitionService.recognize(
                userId, request.getAlgoId(), request.getImageUrl(), request.getFlightRecordId(),
                request.getLng(), request.getLat(), request.getProvider());
        return Result.ok(results);
    }

    @PostMapping("/{id}/confirm")
    public Result<Void> confirm(@PathVariable Long id, Authentication authentication) {
        recognitionService.confirm(id, (Long) authentication.getPrincipal());
        return Result.ok();
    }

    @PostMapping("/{id}/false-report")
    public Result<Void> falseReport(@PathVariable Long id, Authentication authentication) {
        recognitionService.markFalseReport(id, (Long) authentication.getPrincipal());
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        recognitionService.deleteResult(id);
        return Result.ok();
    }

    @Data
    public static class RecognizeRequest {
        @NotNull(message = "识别算法不能为空")
        private Long algoId;
        @NotBlank(message = "识别图片不能为空")
        private String imageUrl;
        private Long flightRecordId;
        private BigDecimal lng;
        private BigDecimal lat;
        private String provider;
    }
}
