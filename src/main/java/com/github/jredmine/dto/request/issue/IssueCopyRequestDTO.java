package com.github.jredmine.dto.request.issue;

import lombok.Data;

/**
 * 复制任务请求DTO
 *
 * @author panfeng
 */
@Data
public class IssueCopyRequestDTO {
    /**
     * 目标项目ID（可选，如果不提供则使用源任务的项目）
     */
    private Long targetProjectId;

    /**
     * 新任务标题（可选，如果不提供则在原标题后加" (副本)"）
     */
    private String subject;

    /**
     * 是否复制子任务（默认 false）
     */
    private Boolean copyChildren = false;

    /**
     * 是否复制关联关系（默认 false）
     */
    private Boolean copyRelations = false;

    /**
     * 是否复制关注者（默认 false）
     */
    private Boolean copyWatchers = false;

    /**
     * 是否复制评论/活动日志（默认 false）
     */
    private Boolean copyJournals = false;
}
