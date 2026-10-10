package com.drone.platform.module.system.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.drone.platform.module.system.entity.SysRole;
import com.drone.platform.module.system.entity.SysUserRole;
import com.drone.platform.module.system.mapper.SysRoleMapper;
import com.drone.platform.module.system.mapper.SysUserRoleMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SysRoleService extends ServiceImpl<SysRoleMapper, SysRole> {

    private final SysUserRoleMapper userRoleMapper;

    public SysRoleService(SysUserRoleMapper userRoleMapper) {
        this.userRoleMapper = userRoleMapper;
    }

    public List<SysRole> listAll() {
        return list();
    }

    public List<Long> getRoleIdsByUserId(Long userId) {
        return userRoleMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, userId))
                .stream().map(SysUserRole::getRoleId).toList();
    }

    public void assignRoles(Long userId, List<Long> roleIds) {
        userRoleMapper.delete(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, userId));
        for (Long roleId : roleIds) {
            SysUserRole ur = new SysUserRole();
            ur.setUserId(userId);
            ur.setRoleId(roleId);
            userRoleMapper.insert(ur);
        }
    }
}
