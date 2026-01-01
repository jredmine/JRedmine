package com.github.jredmine.dto.request.issue;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建任务分类请求DTO
 *
 * @author panfeng
 */
@Data
public class IssueCategoryCreateRequestDTO {
    /**
     * 分类名称（必填）
     */
    @NotBlank(message = "分类名称不能为空")
    private String name;

    /**
     * 默认指派人ID（可选）
     * 0 或 null 表示无默认指派人
     */
    private Long assignedToId;
}
