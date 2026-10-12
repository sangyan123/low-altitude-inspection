package com.drone.platform.module.task.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.drone.platform.common.BusinessException;
import com.drone.platform.module.device.entity.UavDevice;
import com.drone.platform.module.device.mapper.UavDeviceMapper;
import com.drone.platform.module.task.entity.TransportTask;
import com.drone.platform.module.notification.service.NotificationService;
import com.drone.platform.module.task.mapper.TransportTaskMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

@Service
public class TransportTaskService extends ServiceImpl<TransportTaskMapper, TransportTask> {

    private final UavDeviceMapper deviceMapper;
    private final NotificationService notificationService;

    public TransportTaskService(UavDeviceMapper deviceMapper, NotificationService notificationService) {
        this.deviceMapper = deviceMapper;
        this.notificationService = notificationService;
    }

    public Page<TransportTask> page(int current, int size, Integer transportType,
                                    Long deptId, Integer status, String keyword) {
        LambdaQueryWrapper<TransportTask> wrapper = new LambdaQueryWrapper<>();
        if (transportType != null) wrapper.eq(TransportTask::getTransportType, transportType);
        if (deptId != null) wrapper.eq(TransportTask::getDeptId, deptId);
        if (status != null) wrapper.eq(TransportTask::getStatus, status);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(TransportTask::getTaskNo, keyword)
                    .or().like(TransportTask::getCargoName, keyword)
                    .or().like(TransportTask::getReceiverName, keyword));
        }
        wrapper.orderByDesc(TransportTask::getId);
        return page(new Page<>(current, size), wrapper);
    }

    public void create(TransportTask task) {
        if (task.getOriginLng() == null || task.getOriginLat() == null
                || task.getDestLng() == null || task.getDestLat() == null) {
            throw new BusinessException("起运点和目的地坐标不能为空");
        }
        if (!StringUtils.hasText(task.getCargoName())) {
            throw new BusinessException("物资名称不能为空");
        }
        // 校验设备载重
        if (task.getDeviceId() != null && task.getCargoWeightKg() != null) {
            UavDevice device = deviceMapper.selectById(task.getDeviceId());
            if (device == null) {
                throw new BusinessException("分配的设备不存在");
            }
            if (device.getMaxPayloadKg() != null
                    && task.getCargoWeightKg().compareTo(device.getMaxPayloadKg()) > 0) {
                throw new BusinessException("物资重量超过设备最大载重（" + device.getMaxPayloadKg() + "kg）");
            }
        }
        task.setTaskNo(generateTaskNo("TT"));
        if (task.getStatus() == null) task.setStatus(0);
        save(task);
    }

    public void update(TransportTask task) {
        if (task.getId() == null) {
            throw new BusinessException("任务ID不能为空");
        }
        updateById(task);
    }

    public void delete(Long id) {
        removeById(id);
    }

    /** 起飞 */
    public void depart(Long id) {
        TransportTask task = getById(id);
        if (task == null) throw new BusinessException("任务不存在");
        if (task.getStatus() != 0) throw new BusinessException("任务不是待起飞状态");
        task.setStatus(1);
        task.setDepartTime(LocalDateTime.now());
        updateById(task);
    }

    /** 到达 */
    public void arrive(Long id) {
        TransportTask task = getById(id);
        if (task == null) throw new BusinessException("任务不存在");
        if (task.getStatus() != 1) throw new BusinessException("任务不是运输中状态");
        task.setStatus(2);
        task.setArriveTime(LocalDateTime.now());
        updateById(task);
    }

    /** 签收 */
    public void sign(Long id, Long signUserId) {
        TransportTask task = getById(id);
        if (task == null) throw new BusinessException("任务不存在");
        if (task.getStatus() != 2) throw new BusinessException("任务未到达，无法签收");
        task.setStatus(3);
        task.setSignTime(LocalDateTime.now());
        task.setSignUserId(signUserId);
        updateById(task);
    }

    private String generateTaskNo(String prefix) {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int rand = new Random().nextInt(900) + 100;
        return prefix + date + rand;
    }
}
