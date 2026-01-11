package com.github.jredmine.controller;

import com.github.jredmine.dto.request.setting.EmailTestRequestDTO;
import com.github.jredmine.dto.request.setting.SettingUpdateRequestDTO;
import com.github.jredmine.dto.response.ApiResponse;
import com.github.jredmine.dto.response.setting.SettingGroupResponseDTO;
import com.github.jredmine.dto.response.setting.SettingResponseDTO;
import com.github.jredmine.service.SettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统设置控制器
 * 管理系统全局配置，包括基本设置、邮件设置、安全设置、附件设置等
 *
 * @author panfeng
 */
@Tag(name = "系统设置管理", description = "系统全局配置管理接口，包括基本设置、邮件配置、安全设置、附件设置等")
@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingController {

    private final SettingService settingService;

    @Operation(summary = "获取所有系统设置", 
            description = "获取所有系统设置项，按分类分组返回。需要管理员权限。", 
            security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ApiResponse<List<SettingGroupResponseDTO>> getAllSettings() {
        List<SettingGroupResponseDTO> result = settingService.getAllSettings();
        return ApiResponse.success(result);
    }

    @Operation(summary = "根据分类获取系统设置", 
            description = "根据分类代码获取该分类下的所有设置项。需要管理员权限。分类代码：general(基本设置)、email(邮件设置)、security(安全设置)、attachment(附件设置)、notification(通知设置)", 
            security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/category/{category}")
    public ApiResponse<List<SettingResponseDTO>> getSettingsByCategory(@PathVariable String category) {
        List<SettingResponseDTO> result = settingService.getSettingsByCategory(category);
        return ApiResponse.success(result);
    }

    @Operation(summary = "获取单个系统设置", 
            description = "根据设置项名称获取单个设置的值。需要管理员权限。", 
            security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{name}")
    public ApiResponse<String> getSetting(@PathVariable String name) {
        String value = settingService.getSetting(name);
        return ApiResponse.success(value);
    }

    @Operation(summary = "更新系统设置", 
            description = "更新指定的系统设置项。需要管理员权限。如果设置项不存在则创建，存在则更新。", 
            security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping
    public ApiResponse<SettingResponseDTO> updateSetting(@Valid @RequestBody SettingUpdateRequestDTO requestDTO) {
        SettingResponseDTO result = settingService.updateSetting(requestDTO);
        return ApiResponse.success("系统设置更新成功", result);
    }

    @Operation(summary = "重置设置为默认值", 
            description = "将指定的设置项重置为系统默认值。需要管理员权限。", 
            security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{name}")
    public ApiResponse<SettingResponseDTO> resetSetting(@PathVariable String name) {
        SettingResponseDTO result = settingService.resetSetting(name);
        return ApiResponse.success("设置已重置为默认值", result);
    }

    @Operation(summary = "测试邮件配置", 
            description = "发送测试邮件以验证邮件配置是否正确。需要管理员权限。", 
            security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/email/test")
    public ApiResponse<Void> testEmailConfiguration(@Valid @RequestBody EmailTestRequestDTO requestDTO) {
        settingService.testEmailConfiguration(requestDTO);
        return ApiResponse.success("测试邮件发送成功，请检查收件箱", null);
    }

    @Operation(summary = "清除设置缓存", 
            description = "清除系统设置的内存缓存，下次访问将重新从数据库加载。需要管理员权限。", 
            security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/cache/clear")
    public ApiResponse<Void> clearCache() {
        settingService.clearCache();
        return ApiResponse.success("设置缓存已清除", null);
    }
}
