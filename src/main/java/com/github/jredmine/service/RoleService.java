package com.github.jredmine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.jredmine.dto.converter.RoleConverter;
import com.github.jredmine.dto.request.role.RoleCopyRequestDTO;
import com.github.jredmine.dto.request.role.RoleCreateRequestDTO;
import com.github.jredmine.dto.request.role.RoleUpdateRequestDTO;
import com.github.jredmine.dto.response.PageResponse;
import com.github.jredmine.dto.response.role.RoleDetailResponseDTO;
import com.github.jredmine.dto.response.role.RoleListItemResponseDTO;
import com.github.jredmine.entity.Role;
import com.github.jredmine.enums.Permission;
import com.github.jredmine.enums.ResultCode;
import com.github.jredmine.exception.BusinessException;
import com.github.jredmine.entity.MemberRole;
import com.github.jredmine.entity.RolesManagedRole;
import com.github.jredmine.mapper.user.MemberRoleMapper;
import com.github.jredmine.mapper.user.RoleMapper;
import com.github.jredmine.mapper.user.RolesManagedRoleMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final MemberRoleMapper memberRoleMapper;
    private final RolesManagedRoleMapper rolesManagedRoleMapper;
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
            // 设置默认值并验证：current 至少为 1，size 至少为 10
            Integer validCurrent = (current != null && current > 0) ? current : 1;
            Integer validSize = (size != null && size > 0) ? size : 10;

            log.debug("开始查询角色列表，页码: {}, 每页数量: {}", validCurrent, validSize);

            // 创建分页对象
            Page<Role> page = new Page<>(validCurrent, validSize);

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
     * 复制角色
     *
     * @param sourceId   源角色ID
     * @param requestDTO 复制请求（包含新角色名称）
     * @return 新创建的角色详情
     */
    public RoleDetailResponseDTO copyRole(Integer sourceId, RoleCopyRequestDTO requestDTO) {
        // 使用 MDC 添加上下文信息
        MDC.put("operation", "copy_role");
        MDC.put("sourceRoleId", String.valueOf(sourceId));

        try {
            log.info("开始复制角色，源角色ID: {}, 新角色名称: {}", sourceId, requestDTO.getName());

            // 1. 查询源角色是否存在
            Role sourceRole = roleMapper.selectById(sourceId);
            if (sourceRole == null) {
                log.warn("角色复制失败：源角色不存在，源角色ID: {}", sourceId);
                throw new BusinessException(ResultCode.ROLE_NOT_FOUND, "源角色不存在");
            }

            // 2. 验证新角色名称唯一性
            LambdaQueryWrapper<Role> nameQueryWrapper = new LambdaQueryWrapper<>();
            nameQueryWrapper.eq(Role::getName, requestDTO.getName());
            Role existingRole = roleMapper.selectOne(nameQueryWrapper);
            if (existingRole != null) {
                log.warn("角色复制失败：新角色名称已存在，角色名称: {}", requestDTO.getName());
                throw new BusinessException(ResultCode.PARAM_INVALID, "角色名称已存在");
            }

            // 3. 获取最大 position 值，新角色排在最后
            LambdaQueryWrapper<Role> positionQueryWrapper = new LambdaQueryWrapper<>();
            positionQueryWrapper.select(Role::getPosition)
                    .orderByDesc(Role::getPosition)
                    .last("LIMIT 1");
            Role maxPositionRole = roleMapper.selectOne(positionQueryWrapper);
            int nextPosition = (maxPositionRole != null && maxPositionRole.getPosition() != null)
                    ? maxPositionRole.getPosition() + 1 : 1;

            // 4. 创建新角色实体，复制源角色的所有配置
            Role newRole = new Role();
            newRole.setName(requestDTO.getName()); // 使用新名称
            newRole.setPosition(nextPosition); // 新角色排在最后
            newRole.setAssignable(sourceRole.getAssignable() != null ? sourceRole.getAssignable() : true);
            newRole.setBuiltin(0); // 新角色为自定义角色
            newRole.setPermissions(sourceRole.getPermissions()); // 复制权限配置
            newRole.setIssuesVisibility(sourceRole.getIssuesVisibility() != null
                    ? sourceRole.getIssuesVisibility() : "default");
            newRole.setUsersVisibility(sourceRole.getUsersVisibility() != null
                    ? sourceRole.getUsersVisibility() : "members_of_visible_projects");
            newRole.setTimeEntriesVisibility(sourceRole.getTimeEntriesVisibility() != null
                    ? sourceRole.getTimeEntriesVisibility() : "all");
            newRole.setAllRolesManaged(sourceRole.getAllRolesManaged() != null
                    ? sourceRole.getAllRolesManaged() : true);
            newRole.setSettings(sourceRole.getSettings()); // 复制设置
            newRole.setDefaultTimeEntryActivityId(sourceRole.getDefaultTimeEntryActivityId()); // 复制默认工时活动ID

            // 5. 保存到数据库
            roleMapper.insert(newRole);

            log.info("角色复制成功，源角色ID: {}, 新角色ID: {}, 新角色名称: {}", 
                    sourceId, newRole.getId(), newRole.getName());

            // 6. 转换为响应 DTO
            return RoleConverter.INSTANCE.toRoleDetailResponseDTO(newRole);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("角色复制失败，源角色ID: {}, 新角色名称: {}", sourceId, requestDTO.getName(), e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "角色复制失败");
        } finally {
            // 清理 MDC
            MDC.clear();
        }
    }

    /**
     * 删除角色
     *
     * @param id 角色ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(Integer id) {
        // 使用 MDC 添加上下文信息
        MDC.put("operation", "delete_role");
        MDC.put("roleId", String.valueOf(id));

        try {
            log.info("开始删除角色，角色ID: {}", id);

            // 1. 查询角色是否存在
            Role role = roleMapper.selectById(id);
            if (role == null) {
                log.warn("角色删除失败：角色不存在，角色ID: {}", id);
                throw new BusinessException(ResultCode.ROLE_NOT_FOUND);
            }

            // 2. 检查是否是内置角色
            boolean isBuiltin = role.getBuiltin() != null && role.getBuiltin() > 0;
            if (isBuiltin) {
                log.warn("角色删除失败：内置角色不能删除，角色ID: {}, builtin: {}", id, role.getBuiltin());
                throw new BusinessException(ResultCode.ROLE_CANNOT_DELETE, "内置角色不能删除");
            }

            // 3. 检查是否有项目成员使用此角色（member_roles表）
            LambdaQueryWrapper<MemberRole> memberRoleQueryWrapper = new LambdaQueryWrapper<>();
            memberRoleQueryWrapper.eq(MemberRole::getRoleId, id);
            Long memberRoleCount = memberRoleMapper.selectCount(memberRoleQueryWrapper);
            if (memberRoleCount > 0) {
                log.warn("角色删除失败：角色正在被项目成员使用，角色ID: {}, 使用数量: {}", id, memberRoleCount);
                throw new BusinessException(ResultCode.PARAM_INVALID,
                        "角色正在被 " + memberRoleCount + " 个项目成员使用，请先移除关联后再删除");
            }

            // 4. 删除角色管理关系（roles_managed_roles表）
            // 删除作为 role_id 的记录
            LambdaQueryWrapper<RolesManagedRole> roleQueryWrapper = new LambdaQueryWrapper<>();
            roleQueryWrapper.eq(RolesManagedRole::getRoleId, id);
            rolesManagedRoleMapper.delete(roleQueryWrapper);

            // 删除作为 managed_role_id 的记录
            LambdaQueryWrapper<RolesManagedRole> managedRoleQueryWrapper = new LambdaQueryWrapper<>();
            managedRoleQueryWrapper.eq(RolesManagedRole::getManagedRoleId, id);
            rolesManagedRoleMapper.delete(managedRoleQueryWrapper);

            // 5. 删除角色本身
            roleMapper.deleteById(id);

            log.info("角色删除成功，角色ID: {}, 角色名称: {}", id, role.getName());

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("角色删除失败，角色ID: {}", id, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "角色删除失败");
        } finally {
            // 清理 MDC
            MDC.clear();
        }
    }

    /**
     * 获取角色可管理的角色列表
     *
     * @param roleId 角色ID
     * @return 可管理的角色列表
     */
    public List<RoleListItemResponseDTO> getManagedRoles(Integer roleId) {
        // 使用 MDC 添加上下文信息
        MDC.put("operation", "get_managed_roles");
        MDC.put("roleId", String.valueOf(roleId));

        try {
            log.debug("开始获取角色可管理的角色列表，角色ID: {}", roleId);

            // 1. 查询角色是否存在
            Role role = roleMapper.selectById(roleId);
            if (role == null) {
                log.warn("角色不存在，角色ID: {}", roleId);
                throw new BusinessException(ResultCode.ROLE_NOT_FOUND);
            }

            // 2. 如果角色管理所有角色（all_roles_managed = true），返回所有角色
            if (role.getAllRolesManaged() != null && role.getAllRolesManaged()) {
                log.debug("角色管理所有角色，返回所有角色列表，角色ID: {}", roleId);
                List<Role> allRoles = roleMapper.selectList(null);
                return allRoles.stream()
                        .map(RoleConverter.INSTANCE::toRoleListItemResponseDTO)
                        .toList();
            }

            // 3. 查询 roles_managed_roles 表，获取可管理的角色ID列表
            LambdaQueryWrapper<RolesManagedRole> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(RolesManagedRole::getRoleId, roleId);
            List<RolesManagedRole> managedRoles = rolesManagedRoleMapper.selectList(queryWrapper);

            if (managedRoles.isEmpty()) {
                log.info("角色没有可管理的角色，角色ID: {}", roleId);
                return new ArrayList<>();
            }

            // 4. 根据 managed_role_id 查询角色详情
            List<Integer> managedRoleIds = managedRoles.stream()
                    .map(RolesManagedRole::getManagedRoleId)
                    .toList();

            List<Role> roles = roleMapper.selectBatchIds(managedRoleIds);

            log.info("获取角色可管理的角色列表成功，角色ID: {}, 可管理角色数量: {}", roleId, roles.size());

            // 5. 转换为响应 DTO
            return roles.stream()
                    .map(RoleConverter.INSTANCE::toRoleListItemResponseDTO)
                    .toList();

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取角色可管理的角色列表失败，角色ID: {}", roleId, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "获取可管理的角色列表失败");
        } finally {
            // 清理 MDC
            MDC.clear();
        }
    }

    /**
     * 批量更新角色管理关系
     *
     * @param roleId   角色ID
     * @param managedRoleIds 可管理的角色ID列表
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateManagedRoles(Integer roleId, List<Integer> managedRoleIds) {
        // 使用 MDC 添加上下文信息
        MDC.put("operation", "update_managed_roles");
        MDC.put("roleId", String.valueOf(roleId));

        try {
            log.info("开始批量更新角色管理关系，角色ID: {}, 可管理角色数量: {}", roleId, managedRoleIds.size());

            // 1. 查询角色是否存在
            Role role = roleMapper.selectById(roleId);
            if (role == null) {
                log.warn("角色不存在，角色ID: {}", roleId);
                throw new BusinessException(ResultCode.ROLE_NOT_FOUND);
            }

            // 2. 验证要管理的角色是否存在
            if (!managedRoleIds.isEmpty()) {
                List<Role> managedRoles = roleMapper.selectBatchIds(managedRoleIds);
                if (managedRoles.size() != managedRoleIds.size()) {
                    log.warn("部分角色不存在，角色ID: {}", roleId);
                    throw new BusinessException(ResultCode.ROLE_NOT_FOUND, "部分角色不存在");
                }

                // 3. 检查是否包含自己（不能管理自己）
                if (managedRoleIds.contains(roleId)) {
                    log.warn("角色不能管理自己，角色ID: {}", roleId);
                    throw new BusinessException(ResultCode.PARAM_INVALID, "角色不能管理自己");
                }
            }

            // 4. 删除旧的管理关系
            LambdaQueryWrapper<RolesManagedRole> deleteWrapper = new LambdaQueryWrapper<>();
            deleteWrapper.eq(RolesManagedRole::getRoleId, roleId);
            rolesManagedRoleMapper.delete(deleteWrapper);

            // 5. 添加新的管理关系
            if (!managedRoleIds.isEmpty()) {
                for (Integer managedRoleId : managedRoleIds) {
                    RolesManagedRole managedRole = new RolesManagedRole();
                    managedRole.setRoleId(roleId);
                    managedRole.setManagedRoleId(managedRoleId);
                    rolesManagedRoleMapper.insert(managedRole);
                }
            }

            // 6. 更新角色的 all_roles_managed 字段为 false（如果设置为管理所有角色，则不需要记录）
            // 注意：这里不自动更新 all_roles_managed，由用户通过更新角色接口设置

            log.info("批量更新角色管理关系成功，角色ID: {}, 可管理角色数量: {}", roleId, managedRoleIds.size());

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("批量更新角色管理关系失败，角色ID: {}", roleId, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "批量更新角色管理关系失败");
        } finally {
            // 清理 MDC
            MDC.clear();
        }
    }

    /**
     * 添加角色管理关系
     *
     * @param roleId        角色ID
     * @param managedRoleId 被管理的角色ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void addManagedRole(Integer roleId, Integer managedRoleId) {
        // 使用 MDC 添加上下文信息
        MDC.put("operation", "add_managed_role");
        MDC.put("roleId", String.valueOf(roleId));
        MDC.put("managedRoleId", String.valueOf(managedRoleId));

        try {
            log.info("开始添加角色管理关系，角色ID: {}, 被管理角色ID: {}", roleId, managedRoleId);

            // 1. 查询角色是否存在
            Role role = roleMapper.selectById(roleId);
            if (role == null) {
                log.warn("角色不存在，角色ID: {}", roleId);
                throw new BusinessException(ResultCode.ROLE_NOT_FOUND);
            }

            // 2. 查询被管理的角色是否存在
            Role managedRole = roleMapper.selectById(managedRoleId);
            if (managedRole == null) {
                log.warn("被管理的角色不存在，被管理角色ID: {}", managedRoleId);
                throw new BusinessException(ResultCode.ROLE_NOT_FOUND, "被管理的角色不存在");
            }

            // 3. 检查是否是自己（不能管理自己）
            if (roleId.equals(managedRoleId)) {
                log.warn("角色不能管理自己，角色ID: {}", roleId);
                throw new BusinessException(ResultCode.PARAM_INVALID, "角色不能管理自己");
            }

            // 4. 检查是否已存在
            LambdaQueryWrapper<RolesManagedRole> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(RolesManagedRole::getRoleId, roleId)
                    .eq(RolesManagedRole::getManagedRoleId, managedRoleId);
            RolesManagedRole existing = rolesManagedRoleMapper.selectOne(queryWrapper);
            if (existing != null) {
                log.warn("角色管理关系已存在，角色ID: {}, 被管理角色ID: {}", roleId, managedRoleId);
                throw new BusinessException(ResultCode.PARAM_INVALID, "角色管理关系已存在");
            }

            // 5. 添加管理关系
            RolesManagedRole managedRoleRelation = new RolesManagedRole();
            managedRoleRelation.setRoleId(roleId);
            managedRoleRelation.setManagedRoleId(managedRoleId);
            rolesManagedRoleMapper.insert(managedRoleRelation);

            log.info("添加角色管理关系成功，角色ID: {}, 被管理角色ID: {}", roleId, managedRoleId);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("添加角色管理关系失败，角色ID: {}, 被管理角色ID: {}", roleId, managedRoleId, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "添加角色管理关系失败");
        } finally {
            // 清理 MDC
            MDC.clear();
        }
    }

    /**
     * 删除角色管理关系
     *
     * @param roleId        角色ID
     * @param managedRoleId 被管理的角色ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void removeManagedRole(Integer roleId, Integer managedRoleId) {
        // 使用 MDC 添加上下文信息
        MDC.put("operation", "remove_managed_role");
        MDC.put("roleId", String.valueOf(roleId));
        MDC.put("managedRoleId", String.valueOf(managedRoleId));

        try {
            log.info("开始删除角色管理关系，角色ID: {}, 被管理角色ID: {}", roleId, managedRoleId);

            // 1. 查询角色是否存在
            Role role = roleMapper.selectById(roleId);
            if (role == null) {
                log.warn("角色不存在，角色ID: {}", roleId);
                throw new BusinessException(ResultCode.ROLE_NOT_FOUND);
            }

            // 2. 删除管理关系
            LambdaQueryWrapper<RolesManagedRole> deleteWrapper = new LambdaQueryWrapper<>();
            deleteWrapper.eq(RolesManagedRole::getRoleId, roleId)
                    .eq(RolesManagedRole::getManagedRoleId, managedRoleId);
            int deleted = rolesManagedRoleMapper.delete(deleteWrapper);

            if (deleted == 0) {
                log.warn("角色管理关系不存在，角色ID: {}, 被管理角色ID: {}", roleId, managedRoleId);
                throw new BusinessException(ResultCode.PARAM_INVALID, "角色管理关系不存在");
            }

            log.info("删除角色管理关系成功，角色ID: {}, 被管理角色ID: {}", roleId, managedRoleId);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("删除角色管理关系失败，角色ID: {}, 被管理角色ID: {}", roleId, managedRoleId, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "删除角色管理关系失败");
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

