package com.github.jredmine.service;

import com.github.jredmine.dto.response.permission.PermissionDTO;
import com.github.jredmine.enums.Permission;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 权限服务
 *
 * @author panfeng
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionService {

    /**
     * 获取系统所有权限列表
     *
     * @return 权限列表
     */
    public List<PermissionDTO> getAllPermissions() {
        // 使用 MDC 添加上下文信息
        MDC.put("operation", "get_all_permissions");

        try {
            log.debug("开始获取系统所有权限列表");

            // 将 Permission 枚举转换为 PermissionDTO
            List<PermissionDTO> permissions = Arrays.stream(Permission.values())
                    .map(permission -> {
                        PermissionDTO dto = new PermissionDTO();
                        dto.setKey(permission.getKey());
                        dto.setName(permission.getName());
                        dto.setCategory(permission.getCategory());
                        dto.setDescription(permission.getDescription());
                        return dto;
                    })
                    .collect(Collectors.toList());

            log.info("获取系统所有权限列表成功，共 {} 个权限", permissions.size());

            return permissions;
        } finally {
            // 清理 MDC
            MDC.clear();
        }
    }
}

