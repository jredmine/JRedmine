package com.github.jredmine.controller;

import com.github.jredmine.dto.request.user.PasswordChangeRequestDTO;
import com.github.jredmine.dto.request.user.TokenRefreshRequestDTO;
import com.github.jredmine.dto.request.user.UserLoginRequestDTO;
import com.github.jredmine.dto.request.user.UserRegisterRequestDTO;
import com.github.jredmine.dto.response.ApiResponse;
import com.github.jredmine.dto.response.user.UserLoginResponseDTO;
import com.github.jredmine.dto.response.user.UserRegisterResponseDTO;
import com.github.jredmine.service.UserService;
import com.github.jredmine.enums.ResultCode;
import com.github.jredmine.exception.BusinessException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证控制器
 * 负责用户注册、登录等认证相关功能
 * 
 * @author panfeng
 */
@Tag(name = "认证管理", description = "用户注册、登录等认证相关接口")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "用户注册", description = "注册新用户账号")
    @PostMapping("/register")
    public ApiResponse<UserRegisterResponseDTO> register(
            @Valid @RequestBody UserRegisterRequestDTO userRegisterRequestDTO) {
        UserRegisterResponseDTO response = userService.register(userRegisterRequestDTO);
        return ApiResponse.success("用户注册成功", response);
    }

    @Operation(summary = "用户登录", description = "用户登录认证，返回JWT Token")
    @PostMapping("/login")
    public ApiResponse<UserLoginResponseDTO> login(
            @Valid @RequestBody UserLoginRequestDTO userLoginRequestDTO) {
        UserLoginResponseDTO response = userService.login(userLoginRequestDTO);
        return ApiResponse.success("登录成功", response);
    }

    @Operation(summary = "刷新Token", description = "刷新JWT Token，返回新的Token和用户信息")
    @PostMapping("/refresh")
    public ApiResponse<UserLoginResponseDTO> refreshToken(
            @Valid @RequestBody TokenRefreshRequestDTO tokenRefreshRequestDTO) {
        UserLoginResponseDTO response = userService.refreshToken(tokenRefreshRequestDTO);
        return ApiResponse.success("Token刷新成功", response);
    }

    @Operation(summary = "变更密码", description = "用户变更密码，需要提供旧密码和新密码", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/change-password")
    public ApiResponse<Void> changePassword(
            @Valid @RequestBody PasswordChangeRequestDTO passwordChangeRequestDTO) {
        // 从SecurityContext获取当前认证的用户名
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null
                || "anonymousUser".equals(authentication.getName())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "未认证，请先登录");
        }

        String username = authentication.getName();
        userService.changePassword(username, passwordChangeRequestDTO);
        return ApiResponse.success("密码变更成功", null);
    }
}
