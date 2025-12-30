package com.github.jredmine.dto.response.workflow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 工作流状态转换响应 DTO
 * 用于返回某个状态下可用的状态转换
 *
 * @author panfeng
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowTransitionResponseDTO {
    /**
     * 当前状态ID
     */
    private Integer currentStatusId;

    /**
     * 当前状态名称
     */
    private String currentStatusName;

    /**
     * 可转换的目标状态列表
     */
    private List<AvailableTransitionDTO> availableTransitions;
}

