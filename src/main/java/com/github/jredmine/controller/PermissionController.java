package com.github.jredmine.controller;

import com.github.jredmine.dto.response.ApiResponse;
import com.github.jredmine.dto.response.permission.PermissionDTO;
import com.github.jredmine.service.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 权限管理控制器
 * 负责权限信息查询等管理功能
 *
 * @author panfeng
 */
@Tag(name = "权限管理", description = "权限信息查询等管理接口")
@RestController
@RequestMapping("/api/permissions")
public class PermissionController {

    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @Operation(
            summary = "获取系统所有权限列表",
            description = "获取系统中定义的所有可用权限列表，包括权限键、名称、分类和描述。需要认证",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping
    public ApiResponse<List<PermissionDTO>> getAllPermissions() {
        // 需要认证，但不需要管理员权限（普通用户也可以查看权限列表）
        List<PermissionDTO> permissions = permissionService.getAllPermissions();
        return ApiResponse.success(permissions);
    }
}

