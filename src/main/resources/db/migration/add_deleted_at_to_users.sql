-- 添加软删除字段到 users 表
-- 执行时间：2024-01-XX
-- 说明：为 users 表添加 deleted_at 字段，支持软删除功能

ALTER TABLE `users` 
ADD COLUMN `deleted_at` datetime DEFAULT NULL COMMENT '删除时间（软删除），NULL表示未删除，有值表示删除时间' 
AFTER `twofa_required`;

-- 为 deleted_at 字段添加索引，提高查询性能
CREATE INDEX `index_users_on_deleted_at` ON `users` (`deleted_at`);

