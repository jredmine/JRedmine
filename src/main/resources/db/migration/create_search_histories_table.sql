-- 创建搜索历史记录表
CREATE TABLE `search_histories` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `keyword` varchar(255) NOT NULL COMMENT '搜索关键词',
  `search_types` varchar(100) DEFAULT NULL COMMENT '搜索类型(issue,project,wiki)',
  `project_id` bigint DEFAULT NULL COMMENT '项目范围ID',
  `result_count` int DEFAULT 0 COMMENT '搜索结果数量',
  `created_on` datetime NOT NULL COMMENT '搜索时间',
  `updated_on` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_keyword_project` (`user_id`, `keyword`, `project_id`),
  KEY `idx_user_updated` (`user_id`, `updated_on`),
  KEY `idx_keyword_count` (`keyword`, `result_count`),
  KEY `idx_created_on` (`created_on`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='搜索历史记录表';