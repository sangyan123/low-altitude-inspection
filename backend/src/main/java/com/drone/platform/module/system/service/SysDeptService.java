package com.drone.platform.module.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.drone.platform.common.BusinessException;
import com.drone.platform.module.system.entity.SysDept;
import com.drone.platform.module.system.mapper.SysDeptMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class SysDeptService extends ServiceImpl<SysDeptMapper, SysDept> {

    public List<SysDept> listAll(String keyword) {
        LambdaQueryWrapper<SysDept> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(SysDept::getDeptName, keyword)
                   .or().like(SysDept::getDeptCode, keyword);
        }
        wrapper.orderByAsc(SysDept::getSortOrder);
        return list(wrapper);
    }

    public void createDept(SysDept dept) {
        if (!StringUtils.hasText(dept.getDeptName())) {
            throw new BusinessException("部门名称不能为空");
        }
        save(dept);
    }

    public void updateDept(SysDept dept) {
        if (dept.getId() == null) {
            throw new BusinessException("部门ID不能为空");
        }
        updateById(dept);
    }

    public void deleteDept(Long id) {
        long childCount = count(new LambdaQueryWrapper<SysDept>().eq(SysDept::getParentId, id));
        if (childCount > 0) {
            throw new BusinessException("该部门下存在子部门，无法删除");
        }
        removeById(id);
    }
}
