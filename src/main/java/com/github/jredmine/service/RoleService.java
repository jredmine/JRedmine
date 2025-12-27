package com.github.jredmine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.jredmine.dto.converter.RoleConverter;
import com.github.jredmine.dto.request.role.RoleCreateRequestDTO;
import com.github.jredmine.dto.request.role.RoleUpdateRequestDTO;
import com.github.jredmine.dto.response.PageResponse;
import com.github.jredmine.dto.response.role.RoleDetailResponseDTO;
import com.github.jredmine.dto.response.role.RoleListItemResponseDTO;
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
     * 分页查询角色列表
     *
     * @param current    当前页码
     * @param size       每页数量
     * @param name       角色名称（模糊查询）
     * @param builtin    是否内置角色（0=自定义，1-5=内置）
     * @param assignable 是否可分配
     * @return 分页响应
     */
    public PageResponse<RoleListItemResponseDTO> listRoles(
            Integer current, Integer size, String name, Integer builtin, Boolean assignable) {
        // 使用 MDC 添加上下文信息
        MDC.put("operation", "list_roles");

        try {
            log.debug("开始查询角色列表，页码: {}, 每页数量: {}", current, size);

            // 创建分页对象
            Page<Role> page = new Page<>(current, size);

            // 构建查询条件
            LambdaQueryWrapper<Role> queryWrapper = new LambdaQueryWrapper<>();
            if (name != null && !name.trim().isEmpty()) {
                queryWrapper.like(Role::getName, name);
            }
            if (builtin != null) {
                queryWrapper.eq(Role::getBuiltin, builtin);
            }
            if (assignable != null) {
                queryWrapper.eq(Role::getAssignable, assignable);
            }
            // 按 position 排序，如果 position 相同则按 id 排序
            queryWrapper.orderByAsc(Role::getPosition).orderByAsc(Role::getId);

            // 执行分页查询
            Page<Role> result = roleMapper.selectPage(page, queryWrapper);

            // 添加查询结果到上下文
            MDC.put("total", String.valueOf(result.getTotal()));
            log.info("角色列表查询成功，共查询到 {} 条记录", result.getTotal());

            // 转换为响应 DTO
            return PageResponse.of(
                    result.getRecords().stream()
                            .map(RoleConverter.INSTANCE::toRoleListItemResponseDTO)
                            .toList(),
                    (int) result.getTotal(),
                    (int) result.getCurrent(),
                    (int) result.getSize());
        } finally {
            // 清理 MDC
            MDC.clear();
        }
    }

    /**
     * 根据ID查询角色详情
     *
     * @param id 角色ID
     * @return 角色详情
     */
    public RoleDetailResponseDTO getRoleById(Integer id) {
        // 使用 MDC 添加上下文信息
        MDC.put("operation", "get_role_by_id");
        MDC.put("roleId", String.valueOf(id));

        try {
            log.debug("开始查询角色详情，角色ID: {}", id);

            // 查询角色
            Role role = roleMapper.selectById(id);

            if (role == null) {
                log.warn("角色不存在，角色ID: {}", id);
                throw new BusinessException(ResultCode.ROLE_NOT_FOUND);
            }

            log.info("角色详情查询成功，角色ID: {}", id);

            // 转换为响应 DTO
            return RoleConverter.INSTANCE.toRoleDetailResponseDTO(role);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("角色详情查询失败，角色ID: {}", id, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "角色详情查询失败");
        } finally {
            // 清理 MDC
            MDC.clear();
        }
    }

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
     * 更新角色
     *
     * @param id         角色ID
     * @param requestDTO 角色更新请求
     * @return 更新后的角色详情
     */
    public RoleDetailResponseDTO updateRole(Integer id, RoleUpdateRequestDTO requestDTO) {
        // 使用 MDC 添加上下文信息
        MDC.put("operation", "update_role");
        MDC.put("roleId", String.valueOf(id));

        try {
            log.info("开始更新角色，角色ID: {}", id);

            // 1. 查询角色是否存在
            Role role = roleMapper.selectById(id);
            if (role == null) {
                log.warn("角色更新失败：角色不存在，角色ID: {}", id);
                throw new BusinessException(ResultCode.ROLE_NOT_FOUND);
            }

            // 2. 检查是否是内置角色
            boolean isBuiltin = role.getBuiltin() != null && role.getBuiltin() > 0;

            // 3. 内置角色只能更新部分字段（permissions、可见性等），不能修改 name、builtin
            if (isBuiltin) {
                log.debug("更新内置角色，角色ID: {}, builtin: {}", id, role.getBuiltin());
                // 内置角色不能修改名称
                if (requestDTO.getName() != null && !requestDTO.getName().equals(role.getName())) {
                    log.warn("角色更新失败：内置角色不能修改名称，角色ID: {}", id);
                    throw new BusinessException(ResultCode.ROLE_CANNOT_MODIFY, "内置角色不能修改名称");
                }
            } else {
                // 自定义角色可以修改名称，但需要验证唯一性
                if (requestDTO.getName() != null && !requestDTO.getName().equals(role.getName())) {
                    LambdaQueryWrapper<Role> nameQueryWrapper = new LambdaQueryWrapper<>();
                    nameQueryWrapper.eq(Role::getName, requestDTO.getName())
                            .ne(Role::getId, id);
                    Role existingRole = roleMapper.selectOne(nameQueryWrapper);
                    if (existingRole != null) {
                        log.warn("角色更新失败：角色名称已存在，角色名称: {}", requestDTO.getName());
                        throw new BusinessException(ResultCode.PARAM_INVALID, "角色名称已存在");
                    }
                    role.setName(requestDTO.getName());
                }
            }

            // 4. 验证权限列表有效性（如果提供了权限列表）
            if (requestDTO.getPermissions() != null) {
                validatePermissions(requestDTO.getPermissions());
                // 将权限列表转换为JSON字符串
                String permissionsJson = objectMapper.writeValueAsString(requestDTO.getPermissions());
                role.setPermissions(permissionsJson);
            }

            // 5. 更新其他字段（只更新提供的字段）
            if (requestDTO.getPosition() != null) {
                role.setPosition(requestDTO.getPosition());
            }
            if (requestDTO.getAssignable() != null) {
                role.setAssignable(requestDTO.getAssignable());
            }
            if (requestDTO.getIssuesVisibility() != null) {
                role.setIssuesVisibility(requestDTO.getIssuesVisibility());
            }
            if (requestDTO.getUsersVisibility() != null) {
                role.setUsersVisibility(requestDTO.getUsersVisibility());
            }
            if (requestDTO.getTimeEntriesVisibility() != null) {
                role.setTimeEntriesVisibility(requestDTO.getTimeEntriesVisibility());
            }
            if (requestDTO.getAllRolesManaged() != null) {
                role.setAllRolesManaged(requestDTO.getAllRolesManaged());
            }
            if (requestDTO.getSettings() != null) {
                role.setSettings(requestDTO.getSettings());
            }

            // 6. 更新到数据库
            roleMapper.updateById(role);

            log.info("角色更新成功，角色ID: {}, 角色名称: {}", role.getId(), role.getName());

            // 7. 转换为响应 DTO
            return RoleConverter.INSTANCE.toRoleDetailResponseDTO(role);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("角色更新失败，角色ID: {}", id, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "角色更新失败");
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

