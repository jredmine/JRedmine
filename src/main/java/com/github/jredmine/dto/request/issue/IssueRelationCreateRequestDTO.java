package com.github.jredmine.dto.request.issue;

import com.github.jredmine.enums.IssueRelationType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建任务关联请求DTO
 *
 * @author panfeng
 */
@Data
@Schema(description = "创建任务关联请求")
public class IssueRelationCreateRequestDTO {
    /**
     * 目标任务ID（必填）
     */
    @NotNull(message = "目标任务ID不能为空")
    @Schema(description = "目标任务ID", example = "2")
    private Long targetIssueId;

    /**
     * 关联类型（必填）
     * 可选值：
     * - relates: 相关
     * - duplicates: 重复
     * - duplicated: 被重复
     * - blocks: 阻塞
     * - blocked: 被阻塞
     * - precedes: 前置（用于甘特图）
     * - follows: 后置（用于甘特图）
     * - copied_to: 复制到
     * - copied_from: 从...复制
     */
    @NotNull(message = "关联类型不能为空")
    @Schema(description = "关联类型", 
            example = "precedes",
            allowableValues = {"relates", "duplicates", "duplicated", "blocks", "blocked", 
                             "precedes", "follows", "copied_to", "copied_from"})
    private IssueRelationType relationType;

    /**
     * 延迟天数（可选，仅用于 precedes/follows 类型）
     */
    @Schema(description = "延迟天数（仅用于precedes/follows类型）", example = "0")
    private Integer delay;
}
