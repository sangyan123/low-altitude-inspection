-- ============================================================
-- 市级一网统飞大平台 数据库初始化脚本 (MySQL 8.0+)
-- 坐标系统一使用 GCJ-02（高德坐标系）
-- 字符集: utf8mb4
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 一、系统管理模块
-- ============================================================

-- 1. 部门组织表
DROP TABLE IF EXISTS `sys_dept`;
CREATE TABLE `sys_dept` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `parent_id` BIGINT NOT NULL DEFAULT 0 COMMENT '父部门ID，顶级=0',
  `dept_name` VARCHAR(100) NOT NULL COMMENT '部门名称',
  `dept_code` VARCHAR(50) DEFAULT NULL COMMENT '部门编码',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `leader` VARCHAR(50) DEFAULT NULL COMMENT '负责人',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dept_code` (`dept_code`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='部门组织表';

-- 预置部门数据
INSERT INTO `sys_dept` (`id`, `parent_id`, `dept_name`, `dept_code`, `sort_order`) VALUES
(1, 0, '超级管理员', 'SUPER', 0),
(2, 1, '林业局', 'FORESTRY', 1),
(3, 1, '水利局', 'WATER', 2),
(4, 1, '交通运输局', 'TRANSPORT', 3),
(5, 1, '应急管理局', 'EMERGENCY', 4),
(6, 1, '公安局/政法委', 'POLICE', 5),
(7, 1, '农业农村局', 'AGRICULTURE', 6),
(8, 1, '市卫健委', 'HEALTH', 7);

-- 2. 用户表
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `dept_id` BIGINT NOT NULL COMMENT '所属部门',
  `username` VARCHAR(50) NOT NULL COMMENT '登录名',
  `password` VARCHAR(255) NOT NULL COMMENT '密码(BCrypt)',
  `real_name` VARCHAR(50) DEFAULT NULL COMMENT '姓名',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `avatar` VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
  `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_dept_id` (`dept_id`),
  CONSTRAINT `fk_user_dept` FOREIGN KEY (`dept_id`) REFERENCES `sys_dept` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户表';
-- 初始管理员账号（用户名: admin 密码: admin123）
INSERT INTO `sys_user` (`dept_id`, `username`, `password`, `real_name`, `phone`, `status`) VALUES
(1, 'admin', '$2b$10$NNdDI0z2/lhyrFHK1TsFNeQy1tIdYnwjZvAWeSmwCdASfHQUXOrFu', '系统管理员', '13800000000', 1);

-- 3. 角色表
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `role_name` VARCHAR(50) NOT NULL COMMENT '角色名称',
  `role_code` VARCHAR(50) NOT NULL COMMENT '角色编码',
  `remark` VARCHAR(200) DEFAULT NULL COMMENT '描述',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='角色表';

-- 预置角色
INSERT INTO `sys_role` (`id`, `role_name`, `role_code`, `remark`) VALUES
(1, '超级管理员', 'super_admin', '平台最高权限，管理所有部门和用户'),
(2, '部门管理员', 'dept_admin', '部门级管理员，管理本部门用户和任务'),
(3, '飞手', 'pilot', '无人机操作飞手'),
(4, '调度员', 'dispatcher', '任务调度员');

-- 4. 用户角色关联表
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `role_id` BIGINT NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
  KEY `idx_role_id` (`role_id`),
  CONSTRAINT `fk_ur_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_ur_role` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户角色关联表';

-- 5. 菜单权限表
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `parent_id` BIGINT NOT NULL DEFAULT 0 COMMENT '父菜单ID',
  `menu_name` VARCHAR(50) NOT NULL COMMENT '菜单名称',
  `path` VARCHAR(200) DEFAULT NULL COMMENT '路由路径',
  `component` VARCHAR(200) DEFAULT NULL COMMENT '前端组件',
  `permission` VARCHAR(100) DEFAULT NULL COMMENT '权限标识',
  `menu_type` TINYINT NOT NULL DEFAULT 1 COMMENT '1目录 2菜单 3按钮',
  `icon` VARCHAR(100) DEFAULT NULL COMMENT '图标',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `visible` TINYINT NOT NULL DEFAULT 1 COMMENT '1显示 0隐藏',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='菜单权限表';

-- 预置菜单
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `path`, `menu_type`, `sort_order`) VALUES
(1, 0, '首页大屏', '/dashboard', 2, 1),
(2, 0, '飞行态势', '/flight-situation', 2, 2),
(3, 0, '任务管理', '/task', 1, 3),
(4, 3, '巡检任务', '/task/patrol', 2, 1),
(5, 3, '运输任务', '/task/transport', 2, 2),
(6, 3, '航线库', '/task/route', 2, 3),
(7, 3, '飞行计划', '/task/flight-plan', 2, 4),
(8, 0, 'AI识别中心', '/ai', 1, 4),
(9, 8, '识别结果', '/ai/result', 2, 1),
(10, 8, '算法管理', '/ai/algorithm', 2, 2),
(11, 0, '事件管理', '/event', 2, 5),
(12, 0, '成果中心', '/data', 2, 6),
(13, 0, '应急调度', '/emergency', 2, 7),
(14, 0, '统计报表', '/report', 2, 8),
(15, 0, '系统管理', '/system', 1, 9),
(16, 15, '组织管理', '/system/dept', 2, 1),
(17, 15, '账户管理', '/system/user', 2, 2),
(18, 15, '角色管理', '/system/role', 2, 3),
(19, 15, '无人机管理', '/system/device', 2, 4);

-- 6. 角色菜单关联表
DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `role_id` BIGINT NOT NULL COMMENT '角色ID',
  `menu_id` BIGINT NOT NULL COMMENT '菜单ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_menu` (`role_id`, `menu_id`),
  CONSTRAINT `fk_rm_role` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_rm_menu` FOREIGN KEY (`menu_id`) REFERENCES `sys_menu` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='角色菜单关联表';

-- 超级管理员拥有所有菜单
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, `id` FROM `sys_menu`;

-- 7. 系统配置表
DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `config_key` VARCHAR(100) NOT NULL COMMENT '配置键',
  `config_value` VARCHAR(500) DEFAULT NULL COMMENT '配置值',
  `remark` VARCHAR(200) DEFAULT NULL COMMENT '说明',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='系统配置表';

-- 预置高德地图配置
INSERT INTO `sys_config` (`config_key`, `config_value`, `remark`) VALUES
('amap_key', '', '高德地图 Web端(JS API) Key'),
('amap_security_code', '', '高德地图安全密钥');

-- 8. 操作日志表
DROP TABLE IF EXISTS `sys_oper_log`;
CREATE TABLE `sys_oper_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT DEFAULT NULL COMMENT '操作人',
  `operation` VARCHAR(200) NOT NULL COMMENT '操作内容',
  `method` VARCHAR(200) DEFAULT NULL COMMENT '请求方法',
  `params` TEXT DEFAULT NULL COMMENT '请求参数',
  `ip` VARCHAR(50) DEFAULT NULL COMMENT 'IP地址',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='操作日志表';

-- ============================================================
-- 二、无人机管理模块
-- ============================================================

-- 9. 无人机设备表
DROP TABLE IF EXISTS `uav_device`;
CREATE TABLE `uav_device` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `device_sn` VARCHAR(100) NOT NULL COMMENT '设备序列号',
  `device_name` VARCHAR(100) NOT NULL COMMENT '设备名称',
  `model` VARCHAR(100) DEFAULT NULL COMMENT '型号',
  `device_type` TINYINT NOT NULL DEFAULT 1 COMMENT '1固定机巢 2车载机巢 3单兵手持',
  `dept_id` BIGINT DEFAULT NULL COMMENT '分配部门',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0离线 1在线 2飞行中 3充电中 4维护中',
  `battery` DECIMAL(5,2) DEFAULT NULL COMMENT '当前电量%',
  `lng` DECIMAL(10,6) DEFAULT NULL COMMENT '当前经度(GCJ-02)',
  `lat` DECIMAL(10,6) DEFAULT NULL COMMENT '当前纬度(GCJ-02)',
  `altitude` DECIMAL(8,2) DEFAULT NULL COMMENT '当前高度(m)',
  `comm_mode` TINYINT NOT NULL DEFAULT 0 COMMENT '通信链路 0离线 1:4G 2:5G 3自组网 4中继模式',
  `max_flight_time` INT DEFAULT NULL COMMENT '最大续航',
  `max_payload_kg` DECIMAL(5,2) DEFAULT NULL COMMENT '最大载重',
  `purchase_date` DATE DEFAULT NULL COMMENT '采购日期',
  `warranty_end` DATE DEFAULT NULL COMMENT '保修到期',
  `total_flight_hours` DECIMAL(10,2) DEFAULT 0 COMMENT '累计飞行时长',
  `total_flight_count` INT NOT NULL DEFAULT 0 COMMENT '累计飞行架次',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_sn` (`device_sn`),
  KEY `idx_dept_id` (`dept_id`),
  KEY `idx_status` (`status`),
  CONSTRAINT `fk_device_dept` FOREIGN KEY (`dept_id`) REFERENCES `sys_dept` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='无人机设备表';

-- 10. 设备载荷表
DROP TABLE IF EXISTS `uav_payload`;
CREATE TABLE `uav_payload` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `device_id` BIGINT NOT NULL COMMENT '设备ID',
  `payload_type` TINYINT NOT NULL COMMENT '1红外热成像 2激光雷达 3多光谱 4可见光 5喊话器 6抛投器',
  `payload_name` VARCHAR(100) NOT NULL COMMENT '载荷名称',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1正常 0故障',
  PRIMARY KEY (`id`),
  KEY `idx_device_id` (`device_id`),
  CONSTRAINT `fk_payload_device` FOREIGN KEY (`device_id`) REFERENCES `uav_device` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='设备载荷表';

-- 11. 设备分配记录表
DROP TABLE IF EXISTS `uav_device_assign`;
CREATE TABLE `uav_device_assign` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `device_id` BIGINT NOT NULL COMMENT '设备ID',
  `dept_id` BIGINT NOT NULL COMMENT '分配部门',
  `user_id` BIGINT DEFAULT NULL COMMENT '分配用户/飞手',
  `assign_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '分配时间',
  `unassign_time` DATETIME DEFAULT NULL COMMENT '回收时间',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1使用中 0已回收',
  PRIMARY KEY (`id`),
  KEY `idx_device_id` (`device_id`),
  KEY `idx_dept_id` (`dept_id`),
  KEY `idx_user_id` (`user_id`),
  CONSTRAINT `fk_assign_device` FOREIGN KEY (`device_id`) REFERENCES `uav_device` (`id`),
  CONSTRAINT `fk_assign_dept` FOREIGN KEY (`dept_id`) REFERENCES `sys_dept` (`id`),
  CONSTRAINT `fk_assign_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='设备分配记录表';

-- 12. 维保记录表
DROP TABLE IF EXISTS `uav_maintain_log`;
CREATE TABLE `uav_maintain_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `device_id` BIGINT NOT NULL COMMENT '设备ID',
  `maintain_type` TINYINT NOT NULL COMMENT '1保养 2维修 3更换配件',
  `maintain_content` VARCHAR(500) NOT NULL COMMENT '维保内容',
  `maintain_date` DATE NOT NULL COMMENT '维保日期',
  `operator` VARCHAR(50) DEFAULT NULL COMMENT '操作人',
  `cost` DECIMAL(10,2) DEFAULT NULL COMMENT '费用',
  PRIMARY KEY (`id`),
  KEY `idx_device_id` (`device_id`),
  CONSTRAINT `fk_maintain_device` FOREIGN KEY (`device_id`) REFERENCES `uav_device` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='维保记录表';

-- 13. 飞手资质表
DROP TABLE IF EXISTS `uav_pilot_license`;
CREATE TABLE `uav_pilot_license` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '飞手用户ID',
  `license_no` VARCHAR(100) NOT NULL COMMENT '证书编号',
  `license_type` VARCHAR(50) DEFAULT NULL COMMENT '证书类型',
  `issue_date` DATE DEFAULT NULL COMMENT '发证日期',
  `expire_date` DATE DEFAULT NULL COMMENT '有效期至',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  CONSTRAINT `fk_license_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='飞手资质表';

-- ============================================================
-- 三、空域与飞行计划模块
-- ============================================================

-- 14. 空域区域表
DROP TABLE IF EXISTS `airspace_zone`;
CREATE TABLE `airspace_zone` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `zone_name` VARCHAR(100) NOT NULL COMMENT '区域名称',
  `zone_type` TINYINT NOT NULL COMMENT '1禁飞区 2限飞区 3临时管控区',
  `geom` POLYGON NOT NULL SRID 0 COMMENT '地理围栏(GCJ-02)',
  `max_altitude` INT DEFAULT NULL COMMENT '限高(m)',
  `effective_start` DATETIME DEFAULT NULL COMMENT '生效时间',
  `effective_end` DATETIME DEFAULT NULL COMMENT '失效时间',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1生效 0失效',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_zone_type` (`zone_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='空域区域表';

-- 15. 飞行计划表
DROP TABLE IF EXISTS `flight_plan`;
CREATE TABLE `flight_plan` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `plan_no` VARCHAR(50) NOT NULL COMMENT '计划编号',
  `dept_id` BIGINT NOT NULL COMMENT '申报部门',
  `user_id` BIGINT NOT NULL COMMENT '申报人',
  `device_id` BIGINT DEFAULT NULL COMMENT '设备',
  `task_id` BIGINT DEFAULT NULL COMMENT '关联任务ID',
  `task_type` TINYINT DEFAULT 1 COMMENT '1巡检 2运输',
  `plan_start_time` DATETIME NOT NULL COMMENT '计划开始',
  `plan_end_time` DATETIME NOT NULL COMMENT '计划结束',
  `flight_area` POLYGON DEFAULT NULL SRID 0 COMMENT '飞行区域(GCJ-02)',
  `airspace_check` TINYINT NOT NULL DEFAULT 0 COMMENT '0未校验 1通过 2涉禁飞区',
  `approval_status` TINYINT NOT NULL DEFAULT 0 COMMENT '0待审 1通过 2驳回',
  `approval_user_id` BIGINT DEFAULT NULL COMMENT '审批人',
  `approval_time` DATETIME DEFAULT NULL COMMENT '审批时间',
  `approval_remark` VARCHAR(500) DEFAULT NULL COMMENT '审批意见',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_plan_no` (`plan_no`),
  KEY `idx_dept_id` (`dept_id`),
  KEY `idx_device_id` (`device_id`),
  KEY `idx_approval_status` (`approval_status`),
  CONSTRAINT `fk_plan_dept` FOREIGN KEY (`dept_id`) REFERENCES `sys_dept` (`id`),
  CONSTRAINT `fk_plan_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`),
  CONSTRAINT `fk_plan_device` FOREIGN KEY (`device_id`) REFERENCES `uav_device` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='飞行计划表';

-- ============================================================
-- 四、任务管理模块
-- ============================================================

-- 16. 航线模板表
DROP TABLE IF EXISTS `route_template`;
CREATE TABLE `route_template` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `route_name` VARCHAR(100) NOT NULL COMMENT '航线名称',
  `scene_type` VARCHAR(50) NOT NULL COMMENT '场景类型',
  `route_data` JSON NOT NULL COMMENT '航点数据(GCJ-02坐标数组)',
  `est_distance_km` DECIMAL(8,2) DEFAULT NULL COMMENT '预计航程',
  `est_duration_min` INT DEFAULT NULL COMMENT '预计时长',
  `dept_id` BIGINT DEFAULT NULL COMMENT '创建部门',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_scene_type` (`scene_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='航线模板表';

-- 17. 巡检任务表
DROP TABLE IF EXISTS `patrol_task`;
CREATE TABLE `patrol_task` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `task_no` VARCHAR(50) NOT NULL COMMENT '任务编号',
  `task_name` VARCHAR(200) NOT NULL COMMENT '任务名称',
  `task_type` TINYINT NOT NULL DEFAULT 1 COMMENT '1常态巡检 2临时任务 3应急任务',
  `scene_type` VARCHAR(50) NOT NULL COMMENT '场景类型',
  `dept_id` BIGINT NOT NULL COMMENT '提报部门',
  `submit_user_id` BIGINT NOT NULL COMMENT '提报人',
  `area_geom` POLYGON DEFAULT NULL SRID 0 COMMENT '目标区域(GCJ-02)',
  `route_id` BIGINT DEFAULT NULL COMMENT '关联航线',
  `priority` TINYINT NOT NULL DEFAULT 2 COMMENT '1紧急 2普通 3低',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0待派单 1执行中 2已完成 3已取消',
  `plan_start_time` DATETIME DEFAULT NULL COMMENT '计划开始',
  `plan_end_time` DATETIME DEFAULT NULL COMMENT '计划结束',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_no` (`task_no`),
  KEY `idx_dept_id` (`dept_id`),
  KEY `idx_status` (`status`),
  KEY `idx_scene_type` (`scene_type`),
  CONSTRAINT `fk_ptask_dept` FOREIGN KEY (`dept_id`) REFERENCES `sys_dept` (`id`),
  CONSTRAINT `fk_ptask_user` FOREIGN KEY (`submit_user_id`) REFERENCES `sys_user` (`id`),
  CONSTRAINT `fk_ptask_route` FOREIGN KEY (`route_id`) REFERENCES `route_template` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='巡检任务表';

-- 18. 运输任务表
DROP TABLE IF EXISTS `transport_task`;
CREATE TABLE `transport_task` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `task_no` VARCHAR(50) NOT NULL COMMENT '任务编号',
  `transport_type` TINYINT NOT NULL COMMENT '1血液运输 2疾控样本 3救援物资',
  `dept_id` BIGINT NOT NULL COMMENT '发起部门',
  `submit_user_id` BIGINT NOT NULL COMMENT '发起人',
  `origin_lng` DECIMAL(10,6) NOT NULL COMMENT '起运点经度(GCJ-02)',
  `origin_lat` DECIMAL(10,6) NOT NULL COMMENT '起运点纬度',
  `origin_name` VARCHAR(200) DEFAULT NULL COMMENT '起运点名称',
  `dest_lng` DECIMAL(10,6) NOT NULL COMMENT '目的地经度(GCJ-02)',
  `dest_lat` DECIMAL(10,6) NOT NULL COMMENT '目的地纬度',
  `dest_name` VARCHAR(200) DEFAULT NULL COMMENT '目的地名称',
  `cargo_name` VARCHAR(200) DEFAULT NULL COMMENT '物资名称',
  `cargo_weight_kg` DECIMAL(5,2) DEFAULT NULL COMMENT '物资重量',
  `receiver_name` VARCHAR(50) DEFAULT NULL COMMENT '接收人',
  `receiver_phone` VARCHAR(20) DEFAULT NULL COMMENT '接收人电话',
  `device_id` BIGINT DEFAULT NULL COMMENT '分配设备',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0待起飞 1运输中 2已到达 3已签收 4已取消',
  `depart_time` DATETIME DEFAULT NULL COMMENT '实际起飞时间',
  `arrive_time` DATETIME DEFAULT NULL COMMENT '实际到达时间',
  `sign_time` DATETIME DEFAULT NULL COMMENT '签收时间',
  `sign_user_id` BIGINT DEFAULT NULL COMMENT '签收人',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ttask_no` (`task_no`),
  KEY `idx_dept_id` (`dept_id`),
  KEY `idx_status` (`status`),
  CONSTRAINT `fk_ttask_dept` FOREIGN KEY (`dept_id`) REFERENCES `sys_dept` (`id`),
  CONSTRAINT `fk_ttask_user` FOREIGN KEY (`submit_user_id`) REFERENCES `sys_user` (`id`),
  CONSTRAINT `fk_ttask_device` FOREIGN KEY (`device_id`) REFERENCES `uav_device` (`id`),
  CONSTRAINT `fk_ttask_signer` FOREIGN KEY (`sign_user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='运输任务表';

-- 19. 任务派单表
DROP TABLE IF EXISTS `task_dispatch`;
CREATE TABLE `task_dispatch` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `task_type` TINYINT NOT NULL COMMENT '1巡检 2运输',
  `patrol_task_id` BIGINT DEFAULT NULL COMMENT '巡检任务ID',
  `transport_task_id` BIGINT DEFAULT NULL COMMENT '运输任务ID',
  `device_id` BIGINT NOT NULL COMMENT '设备ID',
  `user_id` BIGINT DEFAULT NULL COMMENT '操作飞手',
  `dispatch_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '派单时间',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0待执行 1执行中 2已完成 3已取消',
  PRIMARY KEY (`id`),
  KEY `idx_device_id` (`device_id`),
  KEY `idx_patrol_task` (`patrol_task_id`),
  KEY `idx_transport_task` (`transport_task_id`),
  CONSTRAINT `fk_disp_device` FOREIGN KEY (`device_id`) REFERENCES `uav_device` (`id`),
  CONSTRAINT `fk_disp_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='任务派单表';

-- ============================================================
-- 五、AI算法模块
-- ============================================================

-- 20. 算法表
DROP TABLE IF EXISTS `ai_algorithm`;
CREATE TABLE `ai_algorithm` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `algo_name` VARCHAR(100) NOT NULL COMMENT '算法名称',
  `algo_category` VARCHAR(50) NOT NULL COMMENT '算法分类',
  `algo_desc` VARCHAR(500) DEFAULT NULL COMMENT '描述',
  `model_version` VARCHAR(50) DEFAULT NULL COMMENT '模型版本',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='AI算法表';

-- 预置算法
INSERT INTO `ai_algorithm` (`algo_name`, `algo_category`, `algo_desc`) VALUES
('火点识别', '生态环保', '基于红外热成像识别森林火点'),
('违建识别', '城管', '识别违法建筑'),
('非法采砂识别', '水利应急', '识别河道非法采砂行为'),
('河道边坡坍塌识别', '水利应急', '识别河道边坡坍塌'),
('道路破损识别', '交通治安', '识别道路路面破损'),
('船舶违停识别', '交通治安', '识别航道违规停泊船舶'),
('违规捕鱼识别', '农业', '识别禁渔期违规捕鱼行为'),
('毁坏林地识别', '生态环保', '识别毁坏林地行为'),
('裸土扬尘识别', '城管', '识别裸土和扬尘'),
('垃圾堆放识别', '城管', '识别垃圾堆放'),
('人群聚集识别', '交通治安', '识别人群聚集'),
('山体滑坡识别', '水利应急', '识别山体滑坡隐患');

-- 21. AI识别结果表
DROP TABLE IF EXISTS `ai_recognition_result`;
CREATE TABLE `ai_recognition_result` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `flight_record_id` BIGINT NOT NULL COMMENT '关联飞行记录',
  `algo_id` BIGINT NOT NULL COMMENT '识别算法',
  `image_url` VARCHAR(500) DEFAULT NULL COMMENT '识别截图',
  `result_type` VARCHAR(100) DEFAULT NULL COMMENT '识别结果类型',
  `confidence` DECIMAL(5,4) DEFAULT NULL COMMENT '置信度(0~1)',
  `lng` DECIMAL(10,6) DEFAULT NULL COMMENT '经度(GCJ-02)',
  `lat` DECIMAL(10,6) DEFAULT NULL COMMENT '纬度(GCJ-02)',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0待确认 1已确认 2已推送事件 3误报',
  `confirm_user_id` BIGINT DEFAULT NULL COMMENT '确认人',
  `confirm_time` DATETIME DEFAULT NULL COMMENT '确认时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '识别时间',
  PRIMARY KEY (`id`),
  KEY `idx_flight_record` (`flight_record_id`),
  KEY `idx_algo_id` (`algo_id`),
  KEY `idx_status` (`status`),
  CONSTRAINT `fk_recog_flight` FOREIGN KEY (`flight_record_id`) REFERENCES `flight_record` (`id`),
  CONSTRAINT `fk_recog_algo` FOREIGN KEY (`algo_id`) REFERENCES `ai_algorithm` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='AI识别结果表';

-- ============================================================
-- 六、飞行记录与成果模块
-- ============================================================

-- 22. 飞行记录表
DROP TABLE IF EXISTS `flight_record`;
CREATE TABLE `flight_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `task_type` TINYINT NOT NULL DEFAULT 1 COMMENT '1巡检 2运输 3应急',
  `patrol_task_id` BIGINT DEFAULT NULL COMMENT '巡检任务ID',
  `transport_task_id` BIGINT DEFAULT NULL COMMENT '运输任务ID',
  `emergency_event_id` BIGINT DEFAULT NULL COMMENT '应急事件ID',
  `device_id` BIGINT NOT NULL COMMENT '设备ID',
  `user_id` BIGINT DEFAULT NULL COMMENT '操作员',
  `takeoff_time` DATETIME DEFAULT NULL COMMENT '起飞时间',
  `landing_time` DATETIME DEFAULT NULL COMMENT '降落时间',
  `flight_duration_s` INT DEFAULT NULL COMMENT '飞行时长(秒)',
  `flight_distance_km` DECIMAL(10,2) DEFAULT NULL COMMENT '飞行距离',
  `coverage_area_km2` DECIMAL(12,4) DEFAULT NULL COMMENT '覆盖面积',
  `trajectory_data` JSON DEFAULT NULL COMMENT '飞行轨迹(GCJ-02)',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_device_id` (`device_id`),
  KEY `idx_patrol_task` (`patrol_task_id`),
  KEY `idx_transport_task` (`transport_task_id`),
  KEY `idx_emergency_event` (`emergency_event_id`),
  CONSTRAINT `fk_record_device` FOREIGN KEY (`device_id`) REFERENCES `uav_device` (`id`),
  CONSTRAINT `fk_record_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='飞行记录表';

-- 23. 成果文件表
DROP TABLE IF EXISTS `data_result`;
CREATE TABLE `data_result` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `flight_record_id` BIGINT NOT NULL COMMENT '关联飞行记录',
  `data_type` TINYINT NOT NULL COMMENT '1视频 2照片 3DOM 4DSM 5点云 6三维模型',
  `file_name` VARCHAR(200) DEFAULT NULL COMMENT '文件名',
  `file_url` VARCHAR(500) NOT NULL COMMENT '文件路径',
  `thumbnail_url` VARCHAR(500) DEFAULT NULL COMMENT '缩略图',
  `file_size_mb` DECIMAL(10,2) DEFAULT NULL COMMENT '文件大小',
  `model_format` VARCHAR(50) DEFAULT NULL COMMENT '三维模型格式(3D Tiles/OBJ等)',
  `capture_time` DATETIME DEFAULT NULL COMMENT '采集时间',
  `lng` DECIMAL(10,6) DEFAULT NULL COMMENT '经度(GCJ-02)',
  `lat` DECIMAL(10,6) DEFAULT NULL COMMENT '纬度(GCJ-02)',
  `dept_id` BIGINT DEFAULT NULL COMMENT '归属部门',
  `auth_scope` VARCHAR(500) DEFAULT NULL COMMENT '授权范围(部门ID逗号分隔,空=公开)',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  PRIMARY KEY (`id`),
  KEY `idx_flight_record` (`flight_record_id`),
  KEY `idx_data_type` (`data_type`),
  KEY `idx_dept_id` (`dept_id`),
  CONSTRAINT `fk_result_flight` FOREIGN KEY (`flight_record_id`) REFERENCES `flight_record` (`id`),
  CONSTRAINT `fk_result_dept` FOREIGN KEY (`dept_id`) REFERENCES `sys_dept` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='成果文件表';

-- ============================================================
-- 七、事件闭环模块
-- ============================================================

-- 24. 事件表
DROP TABLE IF EXISTS `event_info`;
CREATE TABLE `event_info` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `event_no` VARCHAR(50) NOT NULL COMMENT '事件编号',
  `event_type` VARCHAR(100) NOT NULL COMMENT '事件类型',
  `event_source` TINYINT NOT NULL DEFAULT 1 COMMENT '1AI识别 2人工上报',
  `recognition_id` BIGINT DEFAULT NULL COMMENT '关联识别结果',
  `dept_id` BIGINT DEFAULT NULL COMMENT '处置部门',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0待处置 1已派单 2已处置 3已核验 4已销号',
  `push_system` VARCHAR(50) DEFAULT NULL COMMENT '推送系统',
  `push_time` DATETIME DEFAULT NULL COMMENT '推送时间',
  `dispatch_time` DATETIME DEFAULT NULL COMMENT '派单时间',
  `handle_time` DATETIME DEFAULT NULL COMMENT '处置完成时间',
  `handle_remark` VARCHAR(500) DEFAULT NULL COMMENT '处置说明',
  `verify_user_id` BIGINT DEFAULT NULL COMMENT '核验人',
  `verify_time` DATETIME DEFAULT NULL COMMENT '核验时间',
  `verify_result` TINYINT DEFAULT NULL COMMENT '1通过 2不通过',
  `lng` DECIMAL(10,6) DEFAULT NULL COMMENT '经度(GCJ-02)',
  `lat` DECIMAL(10,6) DEFAULT NULL COMMENT '纬度(GCJ-02)',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_event_no` (`event_no`),
  KEY `idx_status` (`status`),
  KEY `idx_dept_id` (`dept_id`),
  CONSTRAINT `fk_event_dept` FOREIGN KEY (`dept_id`) REFERENCES `sys_dept` (`id`),
  CONSTRAINT `fk_event_verify_user` FOREIGN KEY (`verify_user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='事件表';

-- 25. 事件处置记录表
DROP TABLE IF EXISTS `event_handle_log`;
CREATE TABLE `event_handle_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `event_id` BIGINT NOT NULL COMMENT '事件ID',
  `action` VARCHAR(100) NOT NULL COMMENT '动作(派单/处置/核验/销号)',
  `operator_id` BIGINT NOT NULL COMMENT '操作人',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '说明',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_event_id` (`event_id`),
  CONSTRAINT `fk_ehlog_event` FOREIGN KEY (`event_id`) REFERENCES `event_info` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_ehlog_user` FOREIGN KEY (`operator_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='事件处置记录表';

-- ============================================================
-- 八、应急管理模块
-- ============================================================

-- 26. 应急事件表
DROP TABLE IF EXISTS `emergency_event`;
CREATE TABLE `emergency_event` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `event_name` VARCHAR(200) NOT NULL COMMENT '事件名称',
  `event_type` TINYINT NOT NULL COMMENT '1洪涝 2山火 3地质灾害 4事故 5搜救',
  `level` TINYINT DEFAULT 2 COMMENT '1一般 2较大 3重大 4特别重大',
  `report_user_id` BIGINT DEFAULT NULL COMMENT '上报人',
  `lng` DECIMAL(10,6) DEFAULT NULL COMMENT '经度(GCJ-02)',
  `lat` DECIMAL(10,6) DEFAULT NULL COMMENT '纬度(GCJ-02)',
  `location_desc` VARCHAR(200) DEFAULT NULL COMMENT '位置描述',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0进行中 1已结束',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上报时间',
  `end_time` DATETIME DEFAULT NULL COMMENT '结束时间',
  PRIMARY KEY (`id`),
  KEY `idx_event_type` (`event_type`),
  CONSTRAINT `fk_emergency_user` FOREIGN KEY (`report_user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='应急事件表';

-- 27. 搜救目标表
DROP TABLE IF EXISTS `rescue_target`;
CREATE TABLE `rescue_target` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `emergency_event_id` BIGINT NOT NULL COMMENT '关联应急事件',
  `target_type` TINYINT NOT NULL DEFAULT 1 COMMENT '1受困人员 2火点 3险情点',
  `lng` DECIMAL(10,6) NOT NULL COMMENT '经度(GCJ-02)',
  `lat` DECIMAL(10,6) NOT NULL COMMENT '纬度(GCJ-02)',
  `found_by_device` BIGINT DEFAULT NULL COMMENT '发现设备',
  `tracking` TINYINT NOT NULL DEFAULT 0 COMMENT '1追踪中 0已停止',
  `shared` TINYINT NOT NULL DEFAULT 0 COMMENT '1已分享 0未分享',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '标注时间',
  PRIMARY KEY (`id`),
  KEY `idx_emergency_event` (`emergency_event_id`),
  CONSTRAINT `fk_target_event` FOREIGN KEY (`emergency_event_id`) REFERENCES `emergency_event` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_target_device` FOREIGN KEY (`found_by_device`) REFERENCES `uav_device` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='搜救目标表';

-- ============================================================
-- 九、消息通知模块
-- ============================================================

-- 28. 系统通知表
DROP TABLE IF EXISTS `sys_notification`;
CREATE TABLE `sys_notification` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `receiver_id` BIGINT NOT NULL COMMENT '接收人',
  `title` VARCHAR(200) NOT NULL COMMENT '标题',
  `content` VARCHAR(1000) DEFAULT NULL COMMENT '内容',
  `notify_type` TINYINT NOT NULL DEFAULT 1 COMMENT '1任务派单 2事件推送 3设备告警 4审批通知 5运输签收',
  `related_id` BIGINT DEFAULT NULL COMMENT '关联业务ID',
  `is_read` TINYINT NOT NULL DEFAULT 0 COMMENT '0未读 1已读',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '时间',
  PRIMARY KEY (`id`),
  KEY `idx_receiver_id` (`receiver_id`),
  KEY `idx_is_read` (`is_read`),
  CONSTRAINT `fk_notify_user` FOREIGN KEY (`receiver_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='系统通知表';

SET FOREIGN_KEY_CHECKS = 1;

