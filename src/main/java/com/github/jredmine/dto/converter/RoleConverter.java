package com.github.jredmine.dto.converter;

import com.github.jredmine.dto.response.role.RoleDetailResponseDTO;
import com.github.jredmine.entity.Role;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.ArrayList;
import java.util.List;

/**
 * 角色转换器
 * 用于 Role 实体与 DTO 之间的转换
 *
 * @author panfeng
 */
@Mapper
public interface RoleConverter {
    RoleConverter INSTANCE = Mappers.getMapper(RoleConverter.class);
    
    // 使用静态 ObjectMapper 实例，避免每次创建
    ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 将 Role 实体转换为 RoleDetailResponseDTO
     * 注意：permissions 字段需要从字符串转换为 List<String>
     */
    default RoleDetailResponseDTO toRoleDetailResponseDTO(Role role) {
        if (role == null) {
            return null;
        }

        RoleDetailResponseDTO dto = new RoleDetailResponseDTO();
        dto.setId(role.getId());
        dto.setName(role.getName());
        dto.setPosition(role.getPosition());
        dto.setAssignable(role.getAssignable());
        dto.setBuiltin(role.getBuiltin());
        dto.setIssuesVisibility(role.getIssuesVisibility());
        dto.setUsersVisibility(role.getUsersVisibility());
        dto.setTimeEntriesVisibility(role.getTimeEntriesVisibility());
        dto.setAllRolesManaged(role.getAllRolesManaged());
        dto.setSettings(role.getSettings());
        dto.setDefaultTimeEntryActivityId(role.getDefaultTimeEntryActivityId());

        // 解析 permissions 字符串为 List<String>
        if (role.getPermissions() != null && !role.getPermissions().isEmpty()) {
            try {
                List<String> permissions = OBJECT_MAPPER.readValue(role.getPermissions(), new TypeReference<List<String>>() {});
                dto.setPermissions(permissions);
            } catch (Exception e) {
                // 如果解析失败，尝试按逗号分割（兼容旧格式）
                String[] perms = role.getPermissions().split(",");
                List<String> permissions = new ArrayList<>();
                for (String perm : perms) {
                    String trimmed = perm.trim();
                    if (!trimmed.isEmpty()) {
                        permissions.add(trimmed);
                    }
                }
                dto.setPermissions(permissions);
            }
        } else {
            dto.setPermissions(new ArrayList<>());
        }

        return dto;
    }
}

