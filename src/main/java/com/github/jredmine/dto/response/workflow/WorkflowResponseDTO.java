package com.github.jredmine.dto.response.workflow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 工作流响应 DTO
 *
 * @author panfeng
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowResponseDTO {
    /**
     * 工作流ID
     */
    private Integer id;

    /**
     * 跟踪器ID（0表示所有跟踪器）
     */
    private Integer trackerId;

    /**
     * 跟踪器名称
     */
    private String trackerName;

    /**
     * 旧状态ID（源状态，0表示所有状态）
     */
    private Integer oldStatusId;

    /**
     * 旧状态名称
     */
    private String oldStatusName;

    /**
     * 新状态ID（目标状态）
     */
    private Integer newStatusId;

    /**
     * 新状态名称
     */
    private String newStatusName;

    /**
     * 角色ID（0表示所有角色）
     */
    private Integer roleId;

    /**
     * 角色名称
     */
    private String roleName;

    /**
     * 是否只有指派人可以执行
     */
    private Boolean assignee;

    /**
     * 是否只有创建者可以执行
     */
    private Boolean author;

    /**
     * 类型（'transition'=状态转换，'field'=字段规则）
     */
    private String type;

    /**
     * 字段名（仅用于字段规则）
     */
    private String fieldName;

    /**
     * 规则（仅用于字段规则：'required'=必填，'readonly'=只读，'hidden'=隐藏）
     */
    private String rule;
}

