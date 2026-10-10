package com.drone.platform.module.task.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.drone.platform.common.BusinessException;
import com.drone.platform.module.device.entity.UavDevice;
import com.drone.platform.module.device.mapper.UavDeviceMapper;
import com.drone.platform.module.system.entity.SysUser;
import com.drone.platform.module.system.mapper.SysUserMapper;
import com.drone.platform.module.task.entity.PatrolTask;
import com.drone.platform.module.task.entity.TaskDispatch;
import com.drone.platform.module.task.mapper.PatrolTaskMapper;
import com.drone.platform.module.task.mapper.TaskDispatchMapper;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

@Service
public class PatrolTaskService extends ServiceImpl<PatrolTaskMapper, PatrolTask> {

    private final TaskDispatchMapper dispatchMapper;
    private final UavDeviceMapper deviceMapper;
    private final SysUserMapper userMapper;

    public PatrolTaskService(TaskDispatchMapper dispatchMapper, UavDeviceMapper deviceMapper,
                             @Lazy SysUserMapper userMapper) {
        this.dispatchMapper = dispatchMapper;
        this.deviceMapper = deviceMapper;
        this.userMapper = userMapper;
    }

    public Page<PatrolTask> page(int current, int size, Integer taskType, String sceneType,
                                 Long deptId, Integer status, String keyword) {
        LambdaQueryWrapper<PatrolTask> wrapper = new LambdaQueryWrapper<>();
        if (taskType != null) wrapper.eq(PatrolTask::getTaskType, taskType);
        if (StringUtils.hasText(sceneType)) wrapper.eq(PatrolTask::getSceneType, sceneType);
        if (deptId != null) wrapper.eq(PatrolTask::getDeptId, deptId);
        if (status != null) wrapper.eq(PatrolTask::getStatus, status);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(PatrolTask::getTaskName, keyword)
                    .or().like(PatrolTask::getTaskNo, keyword));
        }
        wrapper.orderByDesc(PatrolTask::getId);
        return page(new Page<>(current, size), wrapper);
    }

    public void create(PatrolTask task) {
        if (!StringUtils.hasText(task.getTaskName()) || !StringUtils.hasText(task.getSceneType())) {
            throw new BusinessException("任务名称和场景类型不能为空");
        }
        task.setTaskNo(generateTaskNo("PT"));
        if (task.getStatus() == null) task.setStatus(0);
        if (task.getPriority() == null) task.setPriority(2);
        save(task);
    }

    public void update(PatrolTask task) {
        if (task.getId() == null) {
            throw new BusinessException("任务ID不能为空");
        }
        updateById(task);
    }

    public void delete(Long id) {
        removeById(id);
    }

    /** 派单：绑定设备与飞手，任务进入执行中 */
    @Transactional
    public void dispatch(Long taskId, Long deviceId, Long userId) {
        PatrolTask task = getById(taskId);
        if (task == null) {
            throw new BusinessException("任务不存在");
        }
        if (task.getStatus() == 2 || task.getStatus() == 3) {
            throw new BusinessException("该任务已完成或已取消，无法派单");
        }
        UavDevice device = deviceMapper.selectById(deviceId);
        if (device == null) {
            throw new BusinessException("设备不存在");
        }
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("飞手不存在");
        }
        TaskDispatch dispatch = new TaskDispatch();
        dispatch.setTaskType(1);
        dispatch.setPatrolTaskId(taskId);
        dispatch.setDeviceId(deviceId);
        dispatch.setUserId(userId);
        dispatch.setDispatchTime(LocalDateTime.now());
        dispatch.setStatus(1);
        dispatchMapper.insert(dispatch);

        task.setStatus(1);
        updateById(task);
    }

    private String generateTaskNo(String prefix) {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int rand = new Random().nextInt(900) + 100;
        return prefix + date + rand;
    }
}
