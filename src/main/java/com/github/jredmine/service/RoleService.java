package com.github.jredmine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.jredmine.dto.converter.RoleConverter;
import com.github.jredmine.dto.request.role.RoleCreateRequestDTO;
import com.github.jredmine.dto.response.role.RoleDetailResponseDTO;
import com.github.jredmine.entity.Role;
import com.github.jredmine.enums.Permission;
import com.github.jredmine.enums.ResultCode;
import com.github.jredmine.exception.BusinessException;
import com.github.jredmine.mapper.user.RoleMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 角色服务
 *
 * @author panfeng
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleMapper roleMapper;
    private final ObjectMapper objectMapper;

    /**
     * 创建角色
     *
     * @param requestDTO 角色创建请求
     * @return 创建的角色详情
     */
    public RoleDetailResponseDTO createRole(RoleCreateRequestDTO requestDTO) {
        // 使用 MDC 添加上下文信息
        MDC.put("operation", "create_role");

        try {
            log.info("开始创建角色，角色名称: {}", requestDTO.getName());

            // 1. 验证角色名称唯一性
            LambdaQueryWrapper<Role> nameQueryWrapper = new LambdaQueryWrapper<>();
            nameQueryWrapper.eq(Role::getName, requestDTO.getName());
            Role existingRole = roleMapper.selectOne(nameQueryWrapper);
            if (existingRole != null) {
                log.warn("角色创建失败：角色名称已存在，角色名称: {}", requestDTO.getName());
                throw new BusinessException(ResultCode.PARAM_INVALID, "角色名称已存在");
            }

            // 2. 验证权限列表有效性
            validatePermissions(requestDTO.getPermissions());

            // 3. 获取最大 position 值，新角色排在最后
            LambdaQueryWrapper<Role> positionQueryWrapper = new LambdaQueryWrapper<>();
            positionQueryWrapper.select(Role::getPosition)
                    .orderByDesc(Role::getPosition)
                    .last("LIMIT 1");
            Role maxPositionRole = roleMapper.selectOne(positionQueryWrapper);
            int nextPosition = (maxPositionRole != null && maxPositionRole.getPosition() != null)
                    ? maxPositionRole.getPosition() + 1 : 1;

            // 4. 将权限列表转换为JSON字符串
            String permissionsJson = objectMapper.writeValueAsString(requestDTO.getPermissions());

            // 5. 创建角色实体
            Role role = new Role();
            role.setName(requestDTO.getName());
            role.setPosition(requestDTO.getPosition() != null ? requestDTO.getPosition() : nextPosition);
            role.setAssignable(requestDTO.getAssignable() != null ? requestDTO.getAssignable() : true);
            role.setBuiltin(0); // 自定义角色，builtin = 0
            role.setPermissions(permissionsJson);
            role.setIssuesVisibility(requestDTO.getIssuesVisibility() != null
                    ? requestDTO.getIssuesVisibility() : "default");
            role.setUsersVisibility(requestDTO.getUsersVisibility() != null
                    ? requestDTO.getUsersVisibility() : "members_of_visible_projects");
            role.setTimeEntriesVisibility(requestDTO.getTimeEntriesVisibility() != null
                    ? requestDTO.getTimeEntriesVisibility() : "all");
            role.setAllRolesManaged(requestDTO.getAllRolesManaged() != null
                    ? requestDTO.getAllRolesManaged() : true);
            role.setSettings(requestDTO.getSettings());

            // 6. 保存到数据库
            roleMapper.insert(role);

            log.info("角色创建成功，角色ID: {}, 角色名称: {}", role.getId(), role.getName());

            // 7. 转换为响应 DTO
            return RoleConverter.INSTANCE.toRoleDetailResponseDTO(role);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("角色创建失败，角色名称: {}", requestDTO.getName(), e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "角色创建失败");
        } finally {
            // 清理 MDC
            MDC.clear();
        }
    }

    /**
     * 验证权限列表有效性
     *
     * @param permissions 权限列表
     */
    private void validatePermissions(List<String> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "权限列表不能为空");
        }

        List<String> invalidPermissions = new ArrayList<>();
        for (String permissionKey : permissions) {
            if (!Permission.isValidKey(permissionKey)) {
                invalidPermissions.add(permissionKey);
            }
        }

        if (!invalidPermissions.isEmpty()) {
            log.warn("权限验证失败：存在无效的权限，无效权限: {}", invalidPermissions);
            throw new BusinessException(ResultCode.PARAM_INVALID,
                    "存在无效的权限: " + String.join(", ", invalidPermissions));
        }
    }
}

