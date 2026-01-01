package com.github.jredmine.dto.request.issue;

import lombok.Data;

/**
 * 分配任务请求DTO
 *
 * @author panfeng
 */
@Data
public class IssueAssignRequestDTO {
    /**
     * 指派人ID（可选）
     * - null 或 0 表示取消分配
     * - 非空且不为0表示分配给指定用户
     */
    private Long assignedToId;

    /**
     * 乐观锁版本号（可选，用于并发控制）
     */
    private Integer lockVersion;
}
