package com.drone.platform.module.ai.controller;

import com.drone.platform.common.Result;
import com.drone.platform.module.ai.entity.AiAlgorithm;
import com.drone.platform.module.ai.service.AiAlgorithmService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ai/algorithm")
@RequiredArgsConstructor
public class AiAlgorithmController {

    private final AiAlgorithmService algorithmService;

    @GetMapping("/list")
    public Result<List<AiAlgorithm>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        return Result.ok(algorithmService.list(keyword, status));
    }

    @GetMapping("/{id}")
    public Result<AiAlgorithm> getById(@PathVariable Long id) {
        return Result.ok(algorithmService.getById(id));
    }

    @PostMapping
    public Result<Void> create(@RequestBody AiAlgorithm algorithm) {
        algorithmService.createAlgorithm(algorithm);
        return Result.ok();
    }

    @PutMapping
    public Result<Void> update(@RequestBody AiAlgorithm algorithm) {
        algorithmService.updateAlgorithm(algorithm);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        algorithmService.deleteAlgorithm(id);
        return Result.ok();
    }
}