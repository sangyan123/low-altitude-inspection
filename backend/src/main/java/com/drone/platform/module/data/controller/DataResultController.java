package com.drone.platform.module.data.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.drone.platform.common.PageResult;
import com.drone.platform.common.Result;
import com.drone.platform.module.data.entity.DataResult;
import com.drone.platform.module.data.service.DataResultService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/data/result")
@RequiredArgsConstructor
public class DataResultController {

    private final DataResultService dataResultService;

    @GetMapping("/page")
    public Result<PageResult<DataResult>> page(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer dataType,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) Long flightRecordId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.ok(PageResult.of(dataResultService.page(current, size, category, dataType,
                deptId, flightRecordId, keyword, startDate, endDate)));
    }

    @GetMapping("/{id}")
    public Result<DataResult> detail(@PathVariable Long id) {
        return Result.ok(dataResultService.getDetail(id));
    }

    @PostMapping
    public Result<Void> create(@RequestBody DataResult result, Authentication authentication) {
        if (result.getSubmitUserId() == null) {
            result.setSubmitUserId((Long) authentication.getPrincipal());
        }
        dataResultService.create(result);
        return Result.ok();
    }

    @PutMapping
    public Result<Void> update(@RequestBody DataResult result) {
        dataResultService.update(result);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        dataResultService.delete(id);
        return Result.ok();
    }
}