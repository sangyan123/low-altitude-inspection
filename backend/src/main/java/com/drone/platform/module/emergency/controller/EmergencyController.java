package com.drone.platform.module.emergency.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.drone.platform.common.PageResult;
import com.drone.platform.common.Result;
import com.drone.platform.module.emergency.entity.EmergencyEvent;
import com.drone.platform.module.emergency.entity.RescueTarget;
import com.drone.platform.module.emergency.service.EmergencyService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/emergency")
@RequiredArgsConstructor
public class EmergencyController {

    private final EmergencyService emergencyService;

    @GetMapping("/page")
    public Result<PageResult<EmergencyEvent>> page(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer eventType,
            @RequestParam(required = false) Integer level,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.ok(PageResult.of(emergencyService.page(current, size, eventType, level,
                status, keyword, startDate, endDate)));
    }

    @GetMapping("/{id}")
    public Result<EmergencyEvent> detail(@PathVariable Long id) {
        return Result.ok(emergencyService.getDetail(id));
    }

    @PostMapping
    public Result<Long> create(@RequestBody EmergencyEvent event, Authentication authentication) {
        return Result.ok(emergencyService.create((Long) authentication.getPrincipal(), event));
    }

    @PutMapping
    public Result<Void> update(@RequestBody EmergencyEvent event) {
        emergencyService.update(event);
        return Result.ok();
    }

    @PostMapping("/{id}/dispatch")
    public Result<Void> dispatch(@PathVariable Long id,
                                 @RequestBody DispatchRequest request,
                                 Authentication authentication) {
        emergencyService.dispatch(id, (Long) authentication.getPrincipal(), request.getDeviceId(), request.getRemark());
        return Result.ok();
    }

    @PostMapping("/{id}/finish")
    public Result<Void> finish(@PathVariable Long id,
                               @RequestBody(required = false) FinishRequest request,
                               Authentication authentication) {
        emergencyService.finish(id, (Long) authentication.getPrincipal(), request == null ? null : request.getRemark());
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        emergencyService.deleteEvent(id);
        return Result.ok();
    }

    @GetMapping("/{eventId}/target/list")
    public Result<List<RescueTarget>> targets(@PathVariable Long eventId) {
        return Result.ok(emergencyService.listTargets(eventId));
    }

    @PostMapping("/target")
    public Result<RescueTarget> createTarget(@RequestBody RescueTarget target) {
        return Result.ok(emergencyService.createTarget(target));
    }

    @PutMapping("/target")
    public Result<Void> updateTarget(@RequestBody RescueTarget target) {
        emergencyService.updateTarget(target);
        return Result.ok();
    }

    @PostMapping("/target/{id}/tracking")
    public Result<Void> tracking(@PathVariable Long id, @RequestBody TrackingRequest request) {
        emergencyService.setTracking(id, request.getTracking() != null && request.getTracking());
        return Result.ok();
    }

    @DeleteMapping("/target/{id}")
    public Result<Void> deleteTarget(@PathVariable Long id) {
        emergencyService.deleteTarget(id);
        return Result.ok();
    }

    @Data
    public static class DispatchRequest {
        @NotNull(message = "调度无人机不能为空")
        private Long deviceId;
        private String remark;
    }

    @Data
    public static class FinishRequest {
        private String remark;
    }

    @Data
    public static class TrackingRequest {
        private Boolean tracking;
    }
}