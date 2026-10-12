-- ============================================================
-- 应急调度升级脚本（MySQL 8.0+）
-- 适用于已经执行过 sql/init_database.sql 的环境。
-- ============================================================

SET NAMES utf8mb4;

ALTER TABLE `emergency_event`
  ADD COLUMN `description` VARCHAR(500) DEFAULT NULL COMMENT '事件描述' AFTER `location_desc`,
  ADD COLUMN `assigned_device_id` BIGINT DEFAULT NULL COMMENT '调度无人机' AFTER `description`,
  ADD COLUMN `dispatch_user_id` BIGINT DEFAULT NULL COMMENT '调度人' AFTER `assigned_device_id`,
  ADD COLUMN `dispatch_time` DATETIME DEFAULT NULL COMMENT '调度时间' AFTER `dispatch_user_id`,
  ADD COLUMN `dispatch_remark` VARCHAR(500) DEFAULT NULL COMMENT '调度指令' AFTER `dispatch_time`,
  ADD COLUMN `close_remark` VARCHAR(500) DEFAULT NULL COMMENT '结束说明' AFTER `end_time`,
  ADD COLUMN `update_time` DATETIME DEFAULT NULL COMMENT '更新时间' AFTER `close_remark`;

ALTER TABLE `emergency_event`
  ADD KEY `idx_assigned_device` (`assigned_device_id`),
  ADD KEY `idx_dispatch_user` (`dispatch_user_id`);

ALTER TABLE `rescue_target`
  ADD COLUMN `target_name` VARCHAR(100) NOT NULL COMMENT '目标名称' AFTER `emergency_event_id`,
  ADD COLUMN `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1待核实 2已确认 3已处置' AFTER `target_name`,
  ADD COLUMN `found_time` DATETIME DEFAULT NULL COMMENT '发现时间' AFTER `found_by_device`,
  ADD COLUMN `update_time` DATETIME DEFAULT NULL COMMENT '更新时间' AFTER `remark`;

ALTER TABLE `rescue_target`
  ADD KEY `idx_target_status` (`status`);