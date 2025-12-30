package com.github.jredmine.dto.request.workflow;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建工作流请求 DTO
 *
 * @author panfeng
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowCreateRequestDTO {
    /**
     * 跟踪器ID（0表示所有跟踪器）
     */
    @NotNull(message = "跟踪器ID不能为空")
    private Integer trackerId;

    /**
     * 旧状态ID（源状态，0表示所有状态）
     */
    @NotNull(message = "旧状态ID不能为空")
    private Integer oldStatusId;

    /**
     * 新状态ID（目标状态）
     */
    @NotNull(message = "新状态ID不能为空")
    private Integer newStatusId;

    /**
     * 角色ID（0表示所有角色）
     */
    @NotNull(message = "角色ID不能为空")
    private Integer roleId;

    /**
     * 是否只有指派人可以执行
     */
    @Builder.Default
    private Boolean assignee = false;

    /**
     * 是否只有创建者可以执行
     */
    @Builder.Default
    private Boolean author = false;

    /**
     * 类型（'transition'=状态转换，'field'=字段规则）
     */
    @NotNull(message = "类型不能为空")
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

