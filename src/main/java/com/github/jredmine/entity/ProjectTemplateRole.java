package com.github.jredmine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 项目模板默认角色关联表
 * 对应数据库中的 `project_template_roles` 表
 * 主键：id（自增）
 * 唯一键：project_id + role_id
 *
 * @author panfeng
 */
@Data
@TableName(value = "project_template_roles", autoResultMap = true)
public class ProjectTemplateRole {
    /**
     * 主键ID（自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 项目ID（模板ID）
     */
    private Long projectId;

    /**
     * 角色ID
     */
    private Integer roleId;
}
