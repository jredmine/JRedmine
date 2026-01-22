-- 为attachments表添加storage_type字段
-- 用于记录附件上传时的存储类型（local/oss），避免配置变更后无法找到历史文件

ALTER TABLE `attachments` 
ADD COLUMN `storage_type` VARCHAR(20) NOT NULL DEFAULT 'local' COMMENT '存储类型：local(本地存储)或oss(阿里云OSS存储)' 
AFTER `disk_directory`;

-- 为历史数据设置默认值（默认为local）
UPDATE `attachments` SET `storage_type` = 'local' WHERE `storage_type` IS NULL OR `storage_type` = '';

-- 添加索引以提高查询性能
CREATE INDEX `index_attachments_on_storage_type` ON `attachments` (`storage_type`);
