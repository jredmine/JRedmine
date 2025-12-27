package com.github.jredmine.dto.request.role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 角色创建请求DTO
 *
 * @author panfeng
 */
@Data
public class RoleCreateRequestDTO {

    @NotBlank(message = "角色名称是必填项")
    @Size(max = 255, message = "角色名称长度不能超过255个字符")
    private String name;

    /**
     * 排序位置
     */
    private Integer position;

    /**
     * 是否可分配（默认true）
     */
    private Boolean assignable = true;

    /**
     * 权限列表
     */
    @NotNull(message = "权限列表是必填项")
    @Size(min = 1, message = "至少需要选择一个权限")
    private List<String> permissions;

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
     * 是否管理所有角色（默认true）
     */
    private Boolean allRolesManaged = true;

    /**
     * 设置（JSON格式，可选）
     */
    private String settings;
}

