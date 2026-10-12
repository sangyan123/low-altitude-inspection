-- ============================================================
-- 事件管理闭环升级脚本（MySQL 8.0+）
-- 适用于已经执行过 sql/init_database.sql 的环境。
-- ============================================================

SET NAMES utf8mb4;

ALTER TABLE `event_info`
  ADD COLUMN `description` VARCHAR(500) DEFAULT NULL COMMENT '事件描述' AFTER `lat`,
  ADD COLUMN `submit_user_id` BIGINT DEFAULT NULL COMMENT '上报人' AFTER `description`,
  ADD COLUMN `handle_user_id` BIGINT DEFAULT NULL COMMENT '处置人' AFTER `submit_user_id`,
  ADD COLUMN `verify_remark` VARCHAR(500) DEFAULT NULL COMMENT '核验说明' AFTER `verify_result`,
  ADD COLUMN `close_time` DATETIME DEFAULT NULL COMMENT '销号时间' AFTER `verify_remark`;

ALTER TABLE `event_info`
  ADD KEY `idx_event_source` (`event_source`),
  ADD KEY `idx_recognition_id` (`recognition_id`),
  ADD KEY `idx_submit_user` (`submit_user_id`),
  ADD KEY `idx_handle_user` (`handle_user_id`);