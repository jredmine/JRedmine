package com.github.jredmine.dto.response.role;

import lombok.Data;

/**
 * 角色列表项响应DTO
 * 用于角色列表接口，包含列表展示所需的基本信息
 *
 * @author panfeng
 */
@Data
public class RoleListItemResponseDTO {
    private Integer id;
    private String name;
    private Integer position;
    private Boolean assignable;
    private Integer builtin;
}

