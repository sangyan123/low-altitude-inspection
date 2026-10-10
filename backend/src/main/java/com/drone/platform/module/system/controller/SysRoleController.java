package com.drone.platform.module.system.controller;

import com.drone.platform.common.Result;
import com.drone.platform.module.system.entity.SysRole;
import com.drone.platform.module.system.service.SysRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/system/role")
@RequiredArgsConstructor
public class SysRoleController {

    private final SysRoleService roleService;

    @GetMapping("/list")
    public Result<List<SysRole>> list() {
        return Result.ok(roleService.listAll());
    }

    @PostMapping
    public Result<Void> create(@RequestBody SysRole role) {
        roleService.save(role);
        return Result.ok();
    }

    @PutMapping
    public Result<Void> update(@RequestBody SysRole role) {
        roleService.updateById(role);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        roleService.removeById(id);
        return Result.ok();
    }

    @GetMapping("/user/{userId}")
    public Result<List<Long>> getUserRoles(@PathVariable Long userId) {
        return Result.ok(roleService.getRoleIdsByUserId(userId));
    }

    @PostMapping("/user/{userId}/assign")
    public Result<Void> assignRoles(@PathVariable Long userId, @RequestBody Map<String, List<Long>> body) {
        roleService.assignRoles(userId, body.get("roleIds"));
        return Result.ok();
    }
}
