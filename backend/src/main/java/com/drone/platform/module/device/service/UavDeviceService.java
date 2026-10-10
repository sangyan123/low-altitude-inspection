package com.drone.platform.module.device.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.drone.platform.common.BusinessException;
import com.drone.platform.module.device.entity.UavDevice;
import com.drone.platform.module.device.mapper.UavDeviceMapper;
import com.drone.platform.module.device.entity.UavPayload;
import com.drone.platform.module.device.mapper.UavPayloadMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class UavDeviceService extends ServiceImpl<UavDeviceMapper, UavDevice> {

    private final UavPayloadMapper payloadMapper;

    public UavDeviceService(UavPayloadMapper payloadMapper) {
        this.payloadMapper = payloadMapper;
    }

    public Page<UavDevice> page(int current, int size, Long deptId, Integer status, Integer deviceType, String keyword) {
        LambdaQueryWrapper<UavDevice> wrapper = new LambdaQueryWrapper<>();
        if (deptId != null) wrapper.eq(UavDevice::getDeptId, deptId);
        if (status != null) wrapper.eq(UavDevice::getStatus, status);
        if (deviceType != null) wrapper.eq(UavDevice::getDeviceType, deviceType);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(UavDevice::getDeviceName, keyword)
                    .or().like(UavDevice::getDeviceSn, keyword)
                    .or().like(UavDevice::getModel, keyword));
        }
        wrapper.orderByDesc(UavDevice::getCreateTime);
        return page(new Page<>(current, size), wrapper);
    }

    public void createDevice(UavDevice device) {
        if (!StringUtils.hasText(device.getDeviceSn())) {
            throw new BusinessException("设备序列号不能为空");
        }
        long exists = count(new LambdaQueryWrapper<UavDevice>().eq(UavDevice::getDeviceSn, device.getDeviceSn()));
        if (exists > 0) {
            throw new BusinessException("设备序列号已存在");
        }
        save(device);
    }

    public void updateDevice(UavDevice device) {
        if (device.getId() == null) {
            throw new BusinessException("设备ID不能为空");
        }
        updateById(device);
    }

    public void deleteDevice(Long id) {
        payloadMapper.delete(new LambdaQueryWrapper<UavPayload>().eq(UavPayload::getDeviceId, id));
        removeById(id);
    }

    public List<UavPayload> getPayloads(Long deviceId) {
        return payloadMapper.selectList(new LambdaQueryWrapper<UavPayload>().eq(UavPayload::getDeviceId, deviceId));
    }

    public void assignDept(Long deviceId, Long deptId, Long userId) {
        UavDevice device = getById(deviceId);
        if (device == null) {
            throw new BusinessException("设备不存在");
        }
        device.setDeptId(deptId);
        updateById(device);
    }
}
