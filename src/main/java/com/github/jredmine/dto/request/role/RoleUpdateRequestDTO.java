package com.github.jredmine.dto.request.role;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 角色更新请求DTO
 *
 * @author panfeng
 */
@Data
public class RoleUpdateRequestDTO {

    /**
     * 角色名称（内置角色不能修改）
     */
    @Size(max = 255, message = "角色名称长度不能超过255个字符")
    private String name;

    /**
     * 排序位置
     */
    private Integer position;

    /**
     * 是否可分配
     */
    private Boolean assignable;

    /**
     * 权限列表
     */
    @Size(min = 1, message = "至少需要选择一个权限")
    private List<String> permissions;

    /**
     * 任务可见性（all, default, own）
     */
    private String issuesVisibility;

    /**
     * 用户可见性（all, members_of_visible_projects）
     */
    private String usersVisibility;

    /**
     * 工时可见性（all, own, none）
     */
    private String timeEntriesVisibility;

    /**
     * 是否管理所有角色
     */
    private Boolean allRolesManaged;

    /**
     * 设置（JSON格式，可选）
     */
    private String settings;
}

