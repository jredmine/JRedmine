package com.github.jredmine.dto.request.issue;

import lombok.Data;

/**
 * 更新任务分类请求DTO
 * 所有字段都是可选的，只更新提供的字段
 *
 * @author panfeng
 */
@Data
public class IssueCategoryUpdateRequestDTO {
    /**
     * 分类名称（可选）
     */
    private String name;

    /**
     * 默认指派人ID（可选）
     * 0 或 null 表示无默认指派人
     */
    private Long assignedToId;
}
