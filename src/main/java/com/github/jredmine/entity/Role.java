package com.github.jredmine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 角色实体
 *
 * @author panfeng
 */
@Data
@TableName("roles")
public class Role {
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 角色名称
     */
    private String name;

    /**
     * 排序位置
     */
    private Integer position;

    /**
     * 是否可分配
     */
    private Boolean assignable = true;

    /**
     * 是否内置角色（0=自定义，1-5=内置）
     */
    private Integer builtin = 0;

    /**
     * 权限列表（JSON格式或序列化字符串）
     */
    private String permissions;

    /**
     * 任务可见性（all, default, own）
     */
    private String issuesVisibility = "default";

    /**
     * 用户可见性（all, members_of_visible_projects）
     */
    private String usersVisibility = "members_of_visible_projects";

    /**
     * 工时可见性（all, own, none）
     */
    private String timeEntriesVisibility = "all";

    /**
     * 是否管理所有角色
     */
    private Boolean allRolesManaged = true;

    /**
     * 设置（JSON格式）
     */
    private String settings;

    /**
     * 默认工时活动ID
     */
    private Integer defaultTimeEntryActivityId;
}

