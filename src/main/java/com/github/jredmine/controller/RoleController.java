package com.github.jredmine.controller;

import com.github.jredmine.dto.request.role.RoleCopyRequestDTO;
import com.github.jredmine.dto.request.role.RoleCreateRequestDTO;
import com.github.jredmine.dto.request.role.RoleUpdateRequestDTO;
import com.github.jredmine.dto.response.ApiResponse;
import com.github.jredmine.dto.response.PageResponse;
import com.github.jredmine.dto.response.role.RoleDetailResponseDTO;
import com.github.jredmine.dto.response.role.RoleListItemResponseDTO;
import com.github.jredmine.service.RoleService;
import com.github.jredmine.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 角色管理控制器
 * 负责角色的创建、查询、更新等管理功能
 *
 * @author panfeng
 */
@Tag(name = "角色管理", description = "角色信息查询、更新等管理接口")
@RestController
@RequestMapping("/api/roles")
public class RoleController {

    private final RoleService roleService;
    private final SecurityUtils securityUtils;

    public RoleController(RoleService roleService, SecurityUtils securityUtils) {
        this.roleService = roleService;
        this.securityUtils = securityUtils;
    }

    @Operation(
            summary = "获取角色列表",
            description = "分页查询角色列表，支持按名称、是否内置、是否可分配等条件筛选。需要认证",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping
    public ApiResponse<PageResponse<RoleListItemResponseDTO>> listRoles(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer builtin,
            @RequestParam(required = false) Boolean assignable) {
        // 需要认证，但不需要管理员权限（普通用户也可以查看角色列表）
        PageResponse<RoleListItemResponseDTO> response = roleService.listRoles(current, size, name, builtin, assignable);
        return ApiResponse.success(response);
    }

    @Operation(
            summary = "获取角色详情",
            description = "根据角色ID查询角色详细信息，包括权限列表。需要认证",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/{id}")
    public ApiResponse<RoleDetailResponseDTO> getRoleById(@PathVariable Integer id) {
        // 需要认证，但不需要管理员权限（普通用户也可以查看角色详情）
        RoleDetailResponseDTO response = roleService.getRoleById(id);
        return ApiResponse.success(response);
    }

    @Operation(
            summary = "创建角色",
            description = "创建新角色（仅支持自定义角色，不能创建内置角色）。仅管理员可访问",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping
    public ApiResponse<RoleDetailResponseDTO> createRole(
            @Valid @RequestBody RoleCreateRequestDTO roleCreateRequestDTO) {
        // 仅管理员可创建角色
        securityUtils.requireAdmin();
        RoleDetailResponseDTO response = roleService.createRole(roleCreateRequestDTO);
        return ApiResponse.success("角色创建成功", response);
    }

    @Operation(
            summary = "更新角色",
            description = "更新角色信息。内置角色只能更新部分字段（如permissions），不能修改name、builtin等。仅管理员可访问",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PutMapping("/{id}")
    public ApiResponse<RoleDetailResponseDTO> updateRole(
            @PathVariable Integer id,
            @Valid @RequestBody RoleUpdateRequestDTO roleUpdateRequestDTO) {
        // 仅管理员可更新角色
        securityUtils.requireAdmin();
        RoleDetailResponseDTO response = roleService.updateRole(id, roleUpdateRequestDTO);
        return ApiResponse.success("角色更新成功", response);
    }

    @Operation(
            summary = "复制角色",
            description = "复制现有角色创建新角色，复制角色的所有配置（权限、可见性等），仅修改角色名称。新角色为自定义角色。仅管理员可访问",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/{id}/copy")
    public ApiResponse<RoleDetailResponseDTO> copyRole(
            @PathVariable Integer id,
            @Valid @RequestBody RoleCopyRequestDTO roleCopyRequestDTO) {
        // 仅管理员可复制角色
        securityUtils.requireAdmin();
        RoleDetailResponseDTO response = roleService.copyRole(id, roleCopyRequestDTO);
        return ApiResponse.success("角色复制成功", response);
    }

    @Operation(
            summary = "删除角色",
            description = "删除角色（仅支持自定义角色）。如果角色正在被项目成员使用，将返回错误。仅管理员可访问",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteRole(@PathVariable Integer id) {
        // 仅管理员可删除角色
        securityUtils.requireAdmin();
        roleService.deleteRole(id);
        return ApiResponse.success("角色删除成功", null);
    }
}

