package com.github.jredmine.dto.request.issue;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 批量添加任务关注者请求DTO
 *
 * @author panfeng
 */
@Data
public class IssueWatcherBatchAddRequestDTO {
    /**
     * 用户ID列表（必填）
     */
    @NotEmpty(message = "用户ID列表不能为空")
    private List<Long> userIds;
}
