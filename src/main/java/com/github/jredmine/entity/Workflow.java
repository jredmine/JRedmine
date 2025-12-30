package com.github.jredmine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 工作流实体类
 * 对应数据库中的 `workflows` 表
 *
 * @author panfeng
 */
@Data
@TableName("workflows")
public class Workflow {
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 跟踪器ID（0表示所有跟踪器）
     */
    private Integer trackerId = 0;

    /**
     * 旧状态ID（源状态，0表示所有状态）
     */
    private Integer oldStatusId = 0;

    /**
     * 新状态ID（目标状态）
     */
    private Integer newStatusId = 0;

    /**
     * 角色ID（0表示所有角色）
     */
    private Integer roleId = 0;

    /**
     * 是否只有指派人可以执行（true=只有指派人，false=不限制）
     */
    private Boolean assignee = false;

    /**
     * 是否只有创建者可以执行（true=只有创建者，false=不限制）
     */
    private Boolean author = false;

    /**
     * 类型
     * - 'transition': 状态转换规则
     * - 'field': 字段规则
     */
    private String type;

    /**
     * 字段名（仅用于字段规则）
     * 如：'assigned_to', 'priority', 'description' 等
     */
    private String fieldName;

    /**
     * 规则（仅用于字段规则）
     * - 'required': 必填
     * - 'readonly': 只读
     * - 'hidden': 隐藏
     */
    private String rule;
}

