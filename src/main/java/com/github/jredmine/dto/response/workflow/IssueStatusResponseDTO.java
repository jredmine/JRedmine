package com.github.jredmine.dto.response.workflow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 任务状态响应 DTO
 *
 * @author panfeng
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueStatusResponseDTO {
    /**
     * 状态ID
     */
    private Integer id;

    /**
     * 状态名称
     */
    private String name;

    /**
     * 状态描述
     */
    private String description;

    /**
     * 是否已关闭状态
     */
    private Boolean isClosed;

    /**
     * 排序位置
     */
    private Integer position;

    /**
     * 默认完成度（0-100）
     */
    private Integer defaultDoneRatio;
}

