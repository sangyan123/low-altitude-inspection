package com.drone.platform.module.notification.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.drone.platform.common.PageResult;
import com.drone.platform.common.Result;
import com.drone.platform.module.notification.entity.SysNotification;
import com.drone.platform.module.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/page")
    public Result<PageResult<SysNotification>> page(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Integer isRead,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.ok(PageResult.of(notificationService.page(userId, current, size, isRead)));
    }

    @GetMapping("/unread-count")
    public Result<Long> unreadCount(Authentication authentication) {
        return Result.ok(notificationService.unreadCount((Long) authentication.getPrincipal()));
    }

    @PostMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Long id, Authentication authentication) {
        notificationService.markRead((Long) authentication.getPrincipal(), id);
        return Result.ok();
    }

    @PostMapping("/read-all")
    public Result<Void> markAllRead(Authentication authentication) {
        notificationService.markAllRead((Long) authentication.getPrincipal());
        return Result.ok();
    }
}