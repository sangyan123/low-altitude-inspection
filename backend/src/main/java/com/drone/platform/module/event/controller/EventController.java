package com.drone.platform.module.event.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.drone.platform.common.PageResult;
import com.drone.platform.common.Result;
import com.drone.platform.module.event.entity.EventInfo;
import com.drone.platform.module.event.service.EventService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/event")
@RequiredArgsConstructor
@Validated
public class EventController {

    private final EventService eventService;

    @GetMapping("/page")
    public Result<PageResult<EventInfo>> page(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) Integer eventSource,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.ok(PageResult.of(eventService.page(current, size, eventType, eventSource,
                deptId, status, keyword, startDate, endDate)));
    }

    @GetMapping("/{id}")
    public Result<EventInfo> detail(@PathVariable Long id) {
        return Result.ok(eventService.getDetail(id));
    }

    @PostMapping
    public Result<Long> create(@RequestBody @Validated CreateEventRequest request,
                               Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.ok(eventService.createManual(userId, request.getEventType(), request.getDescription(),
                request.getLng(), request.getLat(), request.getDeptId(), request.getRemark()));
    }

    @PostMapping("/{id}/dispatch")
    public Result<Void> dispatch(@PathVariable Long id,
                                 @RequestBody @Validated DispatchRequest request,
                                 Authentication authentication) {
        eventService.dispatch(id, (Long) authentication.getPrincipal(), request.getDeptId(), request.getRemark());
        return Result.ok();
    }

    @PostMapping("/{id}/handle")
    public Result<Void> handle(@PathVariable Long id,
                               @RequestBody @Validated HandleRequest request,
                               Authentication authentication) {
        eventService.handle(id, (Long) authentication.getPrincipal(), request.getRemark());
        return Result.ok();
    }

    @PostMapping("/{id}/verify")
    public Result<Void> verify(@PathVariable Long id,
                               @RequestBody @Validated VerifyRequest request,
                               Authentication authentication) {
        eventService.verify(id, (Long) authentication.getPrincipal(), request.getResult(), request.getRemark());
        return Result.ok();
    }

    @PostMapping("/{id}/close")
    public Result<Void> close(@PathVariable Long id,
                              @RequestBody(required = false) CloseRequest request,
                              Authentication authentication) {
        eventService.close(id, (Long) authentication.getPrincipal(), request == null ? null : request.getRemark());
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        eventService.deleteEvent(id);
        return Result.ok();
    }

    @Data
    public static class CreateEventRequest {
        @NotBlank(message = "事件类型不能为空")
        private String eventType;
        @NotBlank(message = "事件描述不能为空")
        private String description;
        private BigDecimal lng;
        private BigDecimal lat;
        private Long deptId;
        private String remark;
    }

    @Data
    public static class DispatchRequest {
        @NotNull(message = "处置部门不能为空")
        private Long deptId;
        private String remark;
    }

    @Data
    public static class HandleRequest {
        @NotBlank(message = "处置说明不能为空")
        private String remark;
    }

    @Data
    public static class VerifyRequest {
        @NotNull(message = "核验结果不能为空")
        private Integer result;
        private String remark;
    }

    @Data
    public static class CloseRequest {
        private String remark;
    }
}