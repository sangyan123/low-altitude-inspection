package com.drone.platform.module.task.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.drone.platform.common.PageResult;
import com.drone.platform.common.Result;
import com.drone.platform.module.task.entity.FlightPlan;
import com.drone.platform.module.task.service.FlightPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/task/plan")
@RequiredArgsConstructor
public class FlightPlanController {

    private final FlightPlanService planService;

    @GetMapping("/page")
    public Result<PageResult<FlightPlan>> page(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) Integer approvalStatus) {
        return Result.ok(PageResult.of(planService.page(current, size, deptId, approvalStatus)));
    }

    @GetMapping("/{id}")
    public Result<FlightPlan> getById(@PathVariable Long id) {
        return Result.ok(planService.getById(id));
    }

    @PostMapping
    public Result<Void> create(@RequestBody FlightPlan plan, Authentication authentication) {
        plan.setUserId((Long) authentication.getPrincipal());
        planService.create(plan);
        return Result.ok();
    }

    @PutMapping
    public Result<Void> update(@RequestBody FlightPlan plan) {
        planService.update(plan);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        planService.delete(id);
        return Result.ok();
    }

    @PostMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable Long id, @RequestBody Map<String, Object> body, Authentication authentication) {
        int status = ((Number) body.get("approvalStatus")).intValue();
        String remark = (String) body.get("remark");
        Long userId = (Long) authentication.getPrincipal();
        planService.approve(id, userId, status, remark);
        return Result.ok();
    }
}


