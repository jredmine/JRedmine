package com.github.jredmine.dto.response.issue;

import lombok.Data;

/**
 * 任务分类响应DTO
 *
 * @author panfeng
 */
@Data
public class IssueCategoryResponseDTO {
    /**
     * 分类ID
     */
    private Integer id;

    /**
     * 项目ID
     */
    private Long projectId;

    /**
     * 分类名称
     */
    private String name;

    /**
     * 默认指派人ID
     */
    private Long assignedToId;

    /**
     * 默认指派人名称
     */
    private String assignedToName;
}
