package com.drone.platform.module.system.controller;

import com.drone.platform.common.Result;
import com.drone.platform.module.system.entity.SysDept;
import com.drone.platform.module.system.service.SysDeptService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/system/dept")
@RequiredArgsConstructor
public class SysDeptController {

    private final SysDeptService deptService;

    @GetMapping("/list")
    public Result<List<SysDept>> list(@RequestParam(required = false) String keyword) {
        return Result.ok(deptService.listAll(keyword));
    }

    @GetMapping("/{id}")
    public Result<SysDept> getById(@PathVariable Long id) {
        return Result.ok(deptService.getById(id));
    }

    @PostMapping
    public Result<Void> create(@RequestBody SysDept dept) {
        deptService.createDept(dept);
        return Result.ok();
    }

    @PutMapping
    public Result<Void> update(@RequestBody SysDept dept) {
        deptService.updateDept(dept);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        deptService.deleteDept(id);
        return Result.ok();
    }
}
