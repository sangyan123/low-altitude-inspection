-- ============================================================
-- AI识别中心升级脚本（MySQL 8.0+）
-- 适用于已经执行过 sql/init_database.sql 的环境。
-- ============================================================

SET NAMES utf8mb4;

-- 人工上传识别不一定有飞行记录，因此允许为空
ALTER TABLE `ai_recognition_result`
  MODIFY COLUMN `flight_record_id` BIGINT NULL COMMENT '关联飞行记录，人工上传可为空';

-- 增加识别来源、引擎信息、目标框与原始返回，便于切换真实AI引擎后追溯
ALTER TABLE `ai_recognition_result`
  ADD COLUMN `source_type` TINYINT NOT NULL DEFAULT 1 COMMENT '1飞行识别 2人工上传' AFTER `confirm_time`,
  ADD COLUMN `provider` VARCHAR(50) DEFAULT NULL COMMENT '识别提供方' AFTER `source_type`,
  ADD COLUMN `model_name` VARCHAR(100) DEFAULT NULL COMMENT '模型名称' AFTER `provider`,
  ADD COLUMN `bbox` JSON DEFAULT NULL COMMENT '目标框[x1,y1,x2,y2]' AFTER `model_name`,
  ADD COLUMN `raw_response` JSON DEFAULT NULL COMMENT 'AI原始返回' AFTER `bbox`,
  ADD COLUMN `error_message` VARCHAR(500) DEFAULT NULL COMMENT '识别说明或失败原因' AFTER `raw_response`;

ALTER TABLE `ai_recognition_result`
  ADD KEY `idx_source_type` (`source_type`),
  ADD KEY `idx_provider` (`provider`);