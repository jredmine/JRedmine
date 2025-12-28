package com.github.jredmine.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 项目模板默认角色关联表
 * 对应数据库中的 `project_template_roles` 表
 *
 * @author panfeng
 */
@Data
@TableName("project_template_roles")
public class ProjectTemplateRole {
    /**
     * 项目ID（模板ID）
     */
    private Long projectId;

    /**
     * 角色ID
     */
    private Integer roleId;
}
