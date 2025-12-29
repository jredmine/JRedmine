package com.github.jredmine.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.jredmine.entity.Member;
import com.github.jredmine.entity.MemberRole;
import com.github.jredmine.entity.Role;
import com.github.jredmine.mapper.project.MemberMapper;
import com.github.jredmine.mapper.user.MemberRoleMapper;
import com.github.jredmine.mapper.user.RoleMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 项目权限服务
 * 用于查询用户的项目权限
 *
 * @author panfeng
 */
@Slf4j
@Service("projectPermissionService")
@RequiredArgsConstructor
public class ProjectPermissionService {

    private final MemberMapper memberMapper;
    private final MemberRoleMapper memberRoleMapper;
    private final RoleMapper roleMapper;
    private final ObjectMapper objectMapper;

    /**
     * 获取用户在指定项目中的所有权限
     *
     * @param userId    用户ID
     * @param projectId 项目ID
     * @return 权限集合
     */
    public Set<String> getUserProjectPermissions(Long userId, Long projectId) {
        // 查询用户是否是项目成员
        LambdaQueryWrapper<Member> memberQuery = new LambdaQueryWrapper<>();
        memberQuery.eq(Member::getUserId, userId)
                .eq(Member::getProjectId, projectId);
        Member member = memberMapper.selectOne(memberQuery);

        if (member == null) {
            return Collections.emptySet();
        }

        // 查询成员的所有角色
        LambdaQueryWrapper<MemberRole> memberRoleQuery = new LambdaQueryWrapper<>();
        memberRoleQuery.eq(MemberRole::getMemberId, member.getId().intValue());
        List<MemberRole> memberRoles = memberRoleMapper.selectList(memberRoleQuery);

        if (memberRoles.isEmpty()) {
            return Collections.emptySet();
        }

        // 获取所有角色ID
        List<Integer> roleIds = memberRoles.stream()
                .map(MemberRole::getRoleId)
                .distinct()
                .collect(Collectors.toList());

        // 查询角色信息
        List<Role> roles = roleMapper.selectBatchIds(roleIds);

        // 提取所有权限
        Set<String> permissions = new HashSet<>();
        for (Role role : roles) {
            if (role.getPermissions() != null && !role.getPermissions().isEmpty()) {
                try {
                    // 解析权限列表（可能是 JSON 数组或序列化字符串）
                    List<String> rolePermissions = parsePermissions(role.getPermissions());
                    permissions.addAll(rolePermissions);
                } catch (Exception e) {
                    log.warn("解析角色权限失败，角色ID: {}, 权限字符串: {}", role.getId(), role.getPermissions(), e);
                }
            }
        }

        return permissions;
    }

    /**
     * 获取用户在所有项目中的权限（用于全局权限检查）
     * 注意：这个方法主要用于 JWT 认证时加载用户权限，实际权限检查应该在项目级别进行
     *
     * @param userId 用户ID
     * @return 权限集合（所有项目权限的并集）
     */
    public Set<String> getUserAllPermissions(Long userId) {
        // 查询用户的所有项目成员关系
        LambdaQueryWrapper<Member> memberQuery = new LambdaQueryWrapper<>();
        memberQuery.eq(Member::getUserId, userId);
        List<Member> members = memberMapper.selectList(memberQuery);

        if (members.isEmpty()) {
            return Collections.emptySet();
        }

        // 获取所有成员ID
        List<Integer> memberIds = members.stream()
                .map(m -> m.getId().intValue())
                .collect(Collectors.toList());

        // 查询所有成员角色
        LambdaQueryWrapper<MemberRole> memberRoleQuery = new LambdaQueryWrapper<>();
        memberRoleQuery.in(MemberRole::getMemberId, memberIds);
        List<MemberRole> memberRoles = memberRoleMapper.selectList(memberRoleQuery);

        if (memberRoles.isEmpty()) {
            return Collections.emptySet();
        }

        // 获取所有角色ID
        List<Integer> roleIds = memberRoles.stream()
                .map(MemberRole::getRoleId)
                .distinct()
                .collect(Collectors.toList());

        // 查询角色信息
        List<Role> roles = roleMapper.selectBatchIds(roleIds);

        // 提取所有权限
        Set<String> permissions = new HashSet<>();
        for (Role role : roles) {
            if (role.getPermissions() != null && !role.getPermissions().isEmpty()) {
                try {
                    List<String> rolePermissions = parsePermissions(role.getPermissions());
                    permissions.addAll(rolePermissions);
                } catch (Exception e) {
                    log.warn("解析角色权限失败，角色ID: {}, 权限字符串: {}", role.getId(), role.getPermissions(), e);
                }
            }
        }

        return permissions;
    }

    /**
     * 检查用户是否在指定项目中拥有指定权限
     *
     * @param userId    用户ID
     * @param projectId 项目ID
     * @param permission 权限键
     * @return true 如果拥有权限，false 否则
     */
    public boolean hasPermission(Long userId, Long projectId, String permission) {
        Set<String> permissions = getUserProjectPermissions(userId, projectId);
        return permissions.contains(permission);
    }

    /**
     * 解析权限字符串
     * 支持 JSON 数组格式和序列化字符串格式
     *
     * @param permissionsStr 权限字符串
     * @return 权限列表
     */
    private List<String> parsePermissions(String permissionsStr) {
        if (permissionsStr == null || permissionsStr.trim().isEmpty()) {
            return Collections.emptyList();
        }

        try {
            // 尝试解析为 JSON 数组
            if (permissionsStr.trim().startsWith("[")) {
                return objectMapper.readValue(permissionsStr, new TypeReference<List<String>>() {});
            }

            // 尝试解析为序列化字符串（Ruby YAML 格式，如 "---\n- :view_issues\n- :add_issues"）
            // 这里简化处理，假设是简单的字符串列表，用换行符或逗号分隔
            if (permissionsStr.contains("\n")) {
                return Arrays.stream(permissionsStr.split("\n"))
                        .map(s -> s.replaceAll("^---\\s*", "").replaceAll("^-\\s*:?", "").trim())
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList());
            }

            // 尝试用逗号分隔
            if (permissionsStr.contains(",")) {
                return Arrays.stream(permissionsStr.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList());
            }

            // 单个权限
            return Collections.singletonList(permissionsStr.trim());
        } catch (Exception e) {
            log.warn("解析权限字符串失败: {}", permissionsStr, e);
            return Collections.emptyList();
        }
    }
}

