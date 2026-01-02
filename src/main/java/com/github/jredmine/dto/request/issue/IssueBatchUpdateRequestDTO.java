package com.github.jredmine.dto.request.issue;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 批量更新任务请求DTO
 * 用于批量更新多个任务的相同字段
 *
 * @author panfeng
 */
@Data
public class IssueBatchUpdateRequestDTO {
    /**
     * 任务ID列表（必填，至少包含一个任务ID）
     */
    @NotEmpty(message = "任务ID列表不能为空")
    private List<Long> issueIds;

    /**
     * 要更新的字段（复用单个更新的字段定义）
     * 所有字段都是可选的，只更新提供的字段
     */
    private IssueUpdateRequestDTO updateData;
}
