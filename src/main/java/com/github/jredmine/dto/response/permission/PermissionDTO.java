package com.github.jredmine.dto.response.permission;

import lombok.Data;

/**
 * 权限信息响应DTO
 *
 * @author panfeng
 */
@Data
public class PermissionDTO {
    /**
     * 权限键（用于存储和比较）
     */
    private String key;

    /**
     * 权限名称（中文显示名称）
     */
    private String name;

    /**
     * 权限分类
     */
    private String category;

    /**
     * 权限描述
     */
    private String description;
}

