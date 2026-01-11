-- 为三张联合键中间表添加自增主键 id（推荐做法：保留原联合唯一键约束）
-- 目标表：
-- 1) projects_trackers
-- 2) project_template_roles
-- 3) roles_managed_roles

ALTER TABLE `projects_trackers`
  ADD COLUMN `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST;

ALTER TABLE `project_template_roles`
  ADD COLUMN `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST;

ALTER TABLE `roles_managed_roles`
  ADD COLUMN `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST;

