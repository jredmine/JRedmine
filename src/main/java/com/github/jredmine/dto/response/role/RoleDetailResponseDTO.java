package com.github.jredmine.dto.response.role;

import lombok.Data;

import java.util.List;

/**
 * 角色详情响应DTO
 *
 * @author panfeng
 */
@Data
public class RoleDetailResponseDTO {
    private Integer id;
    private String name;
    private Integer position;
    private Boolean assignable;
    private Integer builtin;
    private List<String> permissions;
    private String issuesVisibility;
    private String usersVisibility;
    private String timeEntriesVisibility;
    private Boolean allRolesManaged;
    private String settings;
    private Integer defaultTimeEntryActivityId;
}

