package com.drone.platform.module.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.drone.platform.common.PageResult;
import com.drone.platform.common.Result;
import com.drone.platform.module.system.entity.SysUser;
import com.drone.platform.module.system.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/system/user")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserService userService;

    @GetMapping("/page")
    public Result<PageResult<SysUser>> page(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) Long roleId,
            @RequestParam(required = false) String keyword) {
        return Result.ok(PageResult.of(userService.page(current, size, deptId, roleId, keyword)));
    }

    @GetMapping("/{id}")
    public Result<SysUser> getById(@PathVariable Long id) {
        SysUser user = userService.getById(id);
        if (user != null) user.setPassword(null);
        return Result.ok(user);
    }

    @PostMapping
    public Result<Void> create(@RequestBody SysUser user) {
        userService.createUser(user);
        return Result.ok();
    }

    @PutMapping
    public Result<Void> update(@RequestBody SysUser user) {
        userService.updateUser(user);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.ok();
    }

    @PutMapping("/{id}/reset-password")
    public Result<Void> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> body) {
        userService.resetPassword(id, body.get("password"));
        return Result.ok();
    }
}

