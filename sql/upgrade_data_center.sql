-- ============================================================
-- 成果中心升级脚本（MySQL 8.0+）
-- 适用于已经执行过 sql/init_database.sql 的环境。
-- ============================================================

SET NAMES utf8mb4;

-- 人工上传成果不一定有飞行记录
ALTER TABLE `data_result`
  MODIFY COLUMN `flight_record_id` BIGINT NULL COMMENT '关联飞行记录，人工上传可为空';

ALTER TABLE `data_result`
  ADD COLUMN `submit_user_id` BIGINT DEFAULT NULL COMMENT '上传人' AFTER `auth_scope`,
  ADD COLUMN `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注' AFTER `submit_user_id`,
  ADD COLUMN `update_time` DATETIME DEFAULT NULL COMMENT '更新时间' AFTER `remark`;

ALTER TABLE `data_result`
  ADD KEY `idx_submit_user` (`submit_user_id`);