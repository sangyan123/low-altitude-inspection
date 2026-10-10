package com.drone.platform.module.task.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.drone.platform.common.PageResult;
import com.drone.platform.common.Result;
import com.drone.platform.module.task.entity.TransportTask;
import com.drone.platform.module.task.service.TransportTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/task/transport")
@RequiredArgsConstructor
public class TransportTaskController {

    private final TransportTaskService taskService;

    @GetMapping("/page")
    public Result<PageResult<TransportTask>> page(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer transportType,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {
        return Result.ok(PageResult.of(taskService.page(current, size, transportType, deptId, status, keyword)));
    }

    @GetMapping("/{id}")
    public Result<TransportTask> getById(@PathVariable Long id) {
        return Result.ok(taskService.getById(id));
    }

    @PostMapping
    public Result<Void> create(@RequestBody TransportTask task, Authentication authentication) {
        task.setSubmitUserId((Long) authentication.getPrincipal());
        taskService.create(task);
        return Result.ok();
    }

    @PutMapping
    public Result<Void> update(@RequestBody TransportTask task) {
        taskService.update(task);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        taskService.delete(id);
        return Result.ok();
    }

    @PutMapping("/{id}/depart")
    public Result<Void> depart(@PathVariable Long id) {
        taskService.depart(id);
        return Result.ok();
    }

    @PutMapping("/{id}/arrive")
    public Result<Void> arrive(@PathVariable Long id) {
        taskService.arrive(id);
        return Result.ok();
    }

    @PutMapping("/{id}/sign")
    public Result<Void> sign(@PathVariable Long id, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        taskService.sign(id, userId);
        return Result.ok();
    }
}


