package com.github.jredmine.dto.request.issue;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建任务关联请求DTO
 *
 * @author panfeng
 */
@Data
public class IssueRelationCreateRequestDTO {
    /**
     * 目标任务ID（必填）
     */
    @NotNull(message = "目标任务ID不能为空")
    private Long targetIssueId;

    /**
     * 关联类型（必填）
     * - relates: 相关
     * - duplicates: 重复
     * - duplicated: 被重复
     * - blocks: 阻塞
     * - blocked: 被阻塞
     * - precedes: 前置（用于甘特图）
     * - follows: 后置（用于甘特图）
     */
    @NotNull(message = "关联类型不能为空")
    private String relationType;

    /**
     * 延迟天数（可选，仅用于 precedes/follows 类型）
     */
    private Integer delay;
}
