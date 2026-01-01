package com.github.jredmine.dto.request.issue;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建任务关注者请求DTO
 *
 * @author panfeng
 */
@Data
public class IssueWatcherCreateRequestDTO {
    /**
     * 用户ID（必填）
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;
}
