package com.drone.platform.module.device.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.drone.platform.common.PageResult;
import com.drone.platform.common.Result;
import com.drone.platform.module.device.entity.UavDevice;
import com.drone.platform.module.device.entity.UavPayload;
import com.drone.platform.module.device.service.UavDeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/device")
@RequiredArgsConstructor
public class UavDeviceController {

    private final UavDeviceService deviceService;

    @GetMapping("/page")
    public Result<PageResult<UavDevice>> page(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer deviceType,
            @RequestParam(required = false) String keyword) {
        return Result.ok(PageResult.of(deviceService.page(current, size, deptId, status, deviceType, keyword)));
    }

    @GetMapping("/{id}")
    public Result<UavDevice> getById(@PathVariable Long id) {
        return Result.ok(deviceService.getById(id));
    }

    @GetMapping("/{id}/payloads")
    public Result<List<UavPayload>> getPayloads(@PathVariable Long id) {
        return Result.ok(deviceService.getPayloads(id));
    }

    @PostMapping
    public Result<Void> create(@RequestBody UavDevice device) {
        deviceService.createDevice(device);
        return Result.ok();
    }

    @PutMapping
    public Result<Void> update(@RequestBody UavDevice device) {
        deviceService.updateDevice(device);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        deviceService.deleteDevice(id);
        return Result.ok();
    }

    @PutMapping("/{id}/assign")
    public Result<Void> assign(@PathVariable Long id, @RequestBody Map<String, Long> body) {
        deviceService.assignDept(id, body.get("deptId"), body.get("userId"));
        return Result.ok();
    }
}
