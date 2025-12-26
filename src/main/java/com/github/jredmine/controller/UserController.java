package com.github.jredmine.controller;

import com.github.jredmine.dto.request.user.UserCreateRequestDTO;
import com.github.jredmine.dto.request.user.UserStatusUpdateRequestDTO;
import com.github.jredmine.dto.request.user.UserUpdateRequestDTO;
import com.github.jredmine.dto.response.ApiResponse;
import com.github.jredmine.dto.response.PageResponse;
import com.github.jredmine.dto.response.user.UserDetailResponseDTO;
import com.github.jredmine.dto.response.user.UserListItemResponseDTO;
import com.github.jredmine.enums.ResultCode;
import com.github.jredmine.exception.BusinessException;
import com.github.jredmine.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户管理控制器
 * 负责用户信息的查询、更新等管理功能
 * 
 * @author panfeng
 */
@Tag(name = "用户管理", description = "用户信息查询、更新等管理接口")
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "分页查询用户列表", description = "分页查询用户列表，支持按登录名模糊查询", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping
    public ApiResponse<PageResponse<UserListItemResponseDTO>> listUsers(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String login) {
        PageResponse<UserListItemResponseDTO> response = userService.listUsers(current, size, login);
        return ApiResponse.success(response);
    }

    @Operation(summary = "创建新用户", description = "管理员创建新用户，可以设置管理员权限、用户状态等", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping
    public ApiResponse<UserDetailResponseDTO> createUser(
            @Valid @RequestBody UserCreateRequestDTO userCreateRequestDTO) {
        UserDetailResponseDTO response = userService.createUser(userCreateRequestDTO);
        return ApiResponse.success("用户创建成功", response);
    }

    @Operation(summary = "查询用户详情", description = "根据用户ID查询用户详细信息", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/{id}")
    public ApiResponse<UserDetailResponseDTO> getUserById(@PathVariable Long id) {
        UserDetailResponseDTO response = userService.getUserById(id);
        return ApiResponse.success(response);
    }

    @Operation(summary = "更新用户信息", description = "更新用户信息，可以更新名字、姓氏、管理员权限、用户状态等", security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/{id}")
    public ApiResponse<UserDetailResponseDTO> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequestDTO userUpdateRequestDTO) {
        UserDetailResponseDTO response = userService.updateUser(id, userUpdateRequestDTO);
        return ApiResponse.success("用户信息更新成功", response);
    }

    @Operation(summary = "更新用户状态", description = "启用/禁用/锁定用户账号，状态值：1=启用, 2=锁定, 3=待激活", security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/{id}/status")
    public ApiResponse<UserDetailResponseDTO> updateUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody UserStatusUpdateRequestDTO userStatusUpdateRequestDTO) {
        UserDetailResponseDTO response = userService.updateUserStatus(id, userStatusUpdateRequestDTO);
        return ApiResponse.success("用户状态更新成功", response);
    }

    @Operation(summary = "获取当前用户信息", description = "通过JWT Token获取当前登录用户的详细信息，无需传递用户ID", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/me")
    public ApiResponse<UserDetailResponseDTO> getCurrentUser() {
        // 从SecurityContext获取当前认证的用户名
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null
                || "anonymousUser".equals(authentication.getName())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "未认证，请先登录");
        }

        String username = authentication.getName();
        UserDetailResponseDTO response = userService.getCurrentUser(username);
        return ApiResponse.success(response);
    }
}
