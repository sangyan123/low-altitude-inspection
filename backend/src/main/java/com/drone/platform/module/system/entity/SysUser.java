package com.drone.platform.module.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class SysUser {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long deptId;
    private Long roleId;
    private String username;
    private String password;
    private String realName;
    private String phone;
    private String email;
    private String avatar;
    private LocalDateTime lastLoginTime;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

