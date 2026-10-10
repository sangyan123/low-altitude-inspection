package com.drone.platform.module.task.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.drone.platform.common.PageResult;
import com.drone.platform.common.Result;
import com.drone.platform.module.task.entity.PatrolTask;
import com.drone.platform.module.task.service.PatrolTaskService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/task/patrol")
@RequiredArgsConstructor
public class PatrolTaskController {

    private final PatrolTaskService taskService;

    @GetMapping("/page")
    public Result<PageResult<PatrolTask>> page(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer taskType,
            @RequestParam(required = false) String sceneType,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {
        return Result.ok(PageResult.of(taskService.page(current, size, taskType, sceneType, deptId, status, keyword)));
    }

    @GetMapping("/{id}")
    public Result<PatrolTask> getById(@PathVariable Long id) {
        return Result.ok(taskService.getById(id));
    }

    @PostMapping
    public Result<Void> create(@RequestBody PatrolTask task, Authentication authentication) {
        task.setSubmitUserId((Long) authentication.getPrincipal());
        taskService.create(task);
        return Result.ok();
    }

    @PutMapping
    public Result<Void> update(@RequestBody PatrolTask task) {
        taskService.update(task);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        taskService.delete(id);
        return Result.ok();
    }

    @PostMapping("/{id}/dispatch")
    public Result<Void> dispatch(@PathVariable Long id, @RequestBody Map<String, Long> body) {
        taskService.dispatch(id, body.get("deviceId"), body.get("userId"));
        return Result.ok();
    }
}

