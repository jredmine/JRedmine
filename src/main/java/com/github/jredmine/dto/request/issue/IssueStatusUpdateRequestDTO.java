package com.github.jredmine.dto.request.issue;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 更新任务状态请求DTO
 *
 * @author panfeng
 */
@Data
public class IssueStatusUpdateRequestDTO {
    /**
     * 新状态ID（必填）
     */
    @NotNull(message = "状态ID不能为空")
    private Integer statusId;

    /**
     * 备注/评论（可选）
     */
    private String notes;

    /**
     * 乐观锁版本号（可选，用于并发控制）
     */
    private Integer lockVersion;
}
