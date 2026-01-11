package com.github.jredmine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 角色管理关系实体
 * 对应数据库中的 `roles_managed_roles` 表
 * 主键：id（自增）
 * 唯一键：role_id + managed_role_id
 *
 * @author panfeng
 */
@Data
@TableName(value = "roles_managed_roles", autoResultMap = true)
public class RolesManagedRole {
    /**
     * 主键ID（自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 角色ID
     */
    private Integer roleId;

    /**
     * 被管理的角色ID
     */
    private Integer managedRoleId;
}
