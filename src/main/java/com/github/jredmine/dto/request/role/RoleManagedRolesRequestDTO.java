package com.github.jredmine.dto.request.role;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 角色管理关系请求DTO
 * 用于批量设置角色可管理的其他角色
 *
 * @author panfeng
 */
@Data
public class RoleManagedRolesRequestDTO {

    /**
     * 可管理的角色ID列表
     */
    @NotEmpty(message = "可管理的角色ID列表不能为空")
    private List<Integer> managedRoleIds;
}

