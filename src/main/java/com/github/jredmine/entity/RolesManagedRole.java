package com.github.jredmine.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 角色管理关系实体
 * 对应数据库中的 `roles_managed_roles` 表
 *
 * @author panfeng
 */
@Data
@TableName("roles_managed_roles")
public class RolesManagedRole {
    /**
     * 角色ID
     */
    private Integer roleId;

    /**
     * 被管理的角色ID
     */
    private Integer managedRoleId;
}
