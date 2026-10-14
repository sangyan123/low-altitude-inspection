package com.drone.platform.module.data.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.drone.platform.common.BusinessException;
import com.drone.platform.module.data.entity.DataResult;
import com.drone.platform.module.data.mapper.DataResultMapper;
import com.drone.platform.module.system.entity.SysDept;
import com.drone.platform.module.system.entity.SysUser;
import com.drone.platform.module.system.mapper.SysDeptMapper;
import com.drone.platform.module.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DataResultService extends ServiceImpl<DataResultMapper, DataResult> {

    private final SysDeptMapper deptMapper;
    private final SysUserMapper userMapper;

    public Page<DataResult> page(int current, int size, String category, Integer dataType,
                                 Long deptId, Long flightRecordId, String keyword,
                                 LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<DataResult> wrapper = new LambdaQueryWrapper<>();
        if (dataType != null) {
            wrapper.eq(DataResult::getDataType, dataType);
        } else if ("media".equalsIgnoreCase(category)) {
            wrapper.in(DataResult::getDataType, List.of(1, 2));
        } else if ("model".equalsIgnoreCase(category)) {
            wrapper.in(DataResult::getDataType, List.of(3, 4, 5, 6));
        }
        if (deptId != null) wrapper.eq(DataResult::getDeptId, deptId);
        if (flightRecordId != null) wrapper.eq(DataResult::getFlightRecordId, flightRecordId);
        if (startDate != null) wrapper.ge(DataResult::getCreateTime, startDate.atStartOfDay());
        if (endDate != null) wrapper.lt(DataResult::getCreateTime, endDate.plusDays(1).atStartOfDay());
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(DataResult::getFileName, keyword)
                    .or().like(DataResult::getModelFormat, keyword)
                    .or().like(DataResult::getRemark, keyword));
        }
        wrapper.orderByDesc(DataResult::getId);

        Page<DataResult> result = page(new Page<>(current, size), wrapper);
        fillRelations(result.getRecords());
        return result;
    }

    public DataResult getDetail(Long id) {
        DataResult result = getById(id);
        if (result == null) throw new BusinessException("成果不存在");
        fillRelations(List.of(result));
        return result;
    }

    public void create(DataResult result) {
        validate(result);
        result.setId(null);
        if (result.getSubmitUserId() == null) {
            result.setSubmitUserId(result.getSubmitUserId());
        }
        if (result.getCreateTime() == null) result.setCreateTime(LocalDateTime.now());
        save(result);
    }

    public void update(DataResult result) {
        if (result.getId() == null) throw new BusinessException("成果ID不能为空");
        DataResult exists = getById(result.getId());
        if (exists == null) throw new BusinessException("成果不存在");
        validate(result);
        updateById(result);
    }

    public void delete(Long id) {
        DataResult result = getById(id);
        if (result == null) throw new BusinessException("成果不存在");
        removeById(id);
    }

    private void validate(DataResult result) {
        if (result.getDataType() == null || result.getDataType() < 1 || result.getDataType() > 6) {
            throw new BusinessException("成果类型不合法");
        }
        if (!StringUtils.hasText(result.getFileUrl())) throw new BusinessException("成果文件不能为空");
        if (!StringUtils.hasText(result.getFileName())) throw new BusinessException("成果名称不能为空");
    }

    private void fillRelations(List<DataResult> results) {
        if (results == null || results.isEmpty()) return;

        Set<Long> deptIds = collectIds(results, DataResult::getDeptId);
        Set<Long> userIds = collectIds(results, DataResult::getSubmitUserId);

        Map<Long, SysDept> deptMap = deptIds.isEmpty() ? Map.of()
                : deptMapper.selectBatchIds(deptIds).stream()
                        .collect(Collectors.toMap(SysDept::getId, Function.identity()));
        Map<Long, SysUser> userMap = userIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(SysUser::getId, Function.identity()));

        for (DataResult result : results) {
            SysDept dept = result.getDeptId() == null ? null : deptMap.get(result.getDeptId());
            result.setDeptName(dept == null ? null : dept.getDeptName());
            SysUser user = result.getSubmitUserId() == null ? null : userMap.get(result.getSubmitUserId());
            result.setSubmitUserName(user == null ? null
                    : StringUtils.hasText(user.getRealName()) ? user.getRealName() : user.getUsername());
            result.setDataTypeName(dataTypeName(result.getDataType()));
            result.setCategoryName(isMediaType(result.getDataType()) ? "影像/视频成果" : "三维模型成果");
        }
    }

    private Set<Long> collectIds(List<DataResult> records, Function<DataResult, Long> getter) {
        return records.stream()
                .map(getter)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private boolean isMediaType(Integer dataType) {
        return dataType != null && (dataType == 1 || dataType == 2);
    }

    private String dataTypeName(Integer dataType) {
        if (dataType == null) return "-";
        return switch (dataType) {
            case 1 -> "视频";
            case 2 -> "照片";
            case 3 -> "DOM";
            case 4 -> "DSM";
            case 5 -> "点云";
            case 6 -> "三维模型";
            default -> "未知";
        };
    }
}
