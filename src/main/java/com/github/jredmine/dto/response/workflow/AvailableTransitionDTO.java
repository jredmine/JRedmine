package com.github.jredmine.dto.response.workflow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 可用状态转换 DTO
 *
 * @author panfeng
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailableTransitionDTO {
    /**
     * 目标状态ID
     */
    private Integer statusId;

    /**
     * 目标状态名称
     */
    private String statusName;

    /**
     * 是否只有指派人可以执行
     */
    private Boolean assignee;

    /**
     * 是否只有创建者可以执行
     */
    private Boolean author;
}

