package com.drone.platform.module.notification.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.drone.platform.module.notification.entity.SysNotification;
import com.drone.platform.module.notification.mapper.SysNotificationMapper;
import com.drone.platform.websocket.service.WebSocketPushService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationService extends ServiceImpl<SysNotificationMapper, SysNotification> {

    private final WebSocketPushService webSocketPushService;

    public Long push(Long receiverId, String title, String content, Integer notifyType, Long relatedId) {
        if (receiverId == null) return null;
        SysNotification notification = new SysNotification();
        notification.setReceiverId(receiverId);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setNotifyType(notifyType == null ? 1 : notifyType);
        notification.setRelatedId(relatedId);
        notification.setIsRead(0);
        notification.setCreateTime(LocalDateTime.now());
        save(notification);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "NOTIFICATION");
        payload.put("id", notification.getId());
        payload.put("title", title);
        payload.put("content", content);
        payload.put("notifyType", notification.getNotifyType());
        payload.put("relatedId", relatedId);
        payload.put("createTime", notification.getCreateTime());
        webSocketPushService.pushToUser(receiverId, payload);
        return notification.getId();
    }

    public Page<SysNotification> page(Long receiverId, int current, int size, Integer isRead) {
        LambdaQueryWrapper<SysNotification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysNotification::getReceiverId, receiverId);
        if (isRead != null) wrapper.eq(SysNotification::getIsRead, isRead);
        wrapper.orderByDesc(SysNotification::getId);
        return page(new Page<>(current, size), wrapper);
    }

    public long unreadCount(Long receiverId) {
        return count(new LambdaQueryWrapper<SysNotification>()
                .eq(SysNotification::getReceiverId, receiverId)
                .eq(SysNotification::getIsRead, 0));
    }

    public void markRead(Long receiverId, Long id) {
        SysNotification notification = getById(id);
        if (notification == null || !receiverId.equals(notification.getReceiverId())) {
            throw new IllegalArgumentException("通知不存在");
        }
        notification.setIsRead(1);
        updateById(notification);
    }

    public void markAllRead(Long receiverId) {
        SysNotification entity = new SysNotification();
        entity.setIsRead(1);
        update(entity, new LambdaQueryWrapper<SysNotification>()
                .eq(SysNotification::getReceiverId, receiverId)
                .eq(SysNotification::getIsRead, 0));
    }
}