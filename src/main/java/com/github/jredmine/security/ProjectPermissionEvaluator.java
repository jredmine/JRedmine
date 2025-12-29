package com.github.jredmine.security;

import com.github.jredmine.entity.Project;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * 项目权限评估器
 * 用于 @PreAuthorize 注解中的权限检查
 *
 * @author panfeng
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProjectPermissionEvaluator implements PermissionEvaluator {

    private final ProjectPermissionService projectPermissionService;

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal)) {
            return false;
        }

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        // 管理员拥有所有权限
        if (userPrincipal.isAdmin()) {
            return true;
        }

        // 如果目标对象是项目ID（Long）
        if (targetDomainObject instanceof Long) {
            Long projectId = (Long) targetDomainObject;
            String permissionKey = permission.toString();
            return projectPermissionService.hasPermission(userPrincipal.getId(), projectId, permissionKey);
        }

        // 如果目标对象是项目实体
        if (targetDomainObject instanceof Project) {
            Project project = (Project) targetDomainObject;
            String permissionKey = permission.toString();
            return projectPermissionService.hasPermission(userPrincipal.getId(), project.getId(), permissionKey);
        }

        return false;
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal)) {
            return false;
        }

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        // 管理员拥有所有权限
        if (userPrincipal.isAdmin()) {
            return true;
        }

        // 如果目标类型是项目
        if ("Project".equals(targetType) && targetId instanceof Long) {
            Long projectId = (Long) targetId;
            String permissionKey = permission.toString();
            return projectPermissionService.hasPermission(userPrincipal.getId(), projectId, permissionKey);
        }

        return false;
    }
}

