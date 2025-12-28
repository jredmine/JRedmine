package com.github.jredmine.dto.request.project;

import lombok.Data;

import java.util.List;

/**
 * 创建项目模板请求DTO
 *
 * @author panfeng
 */
@Data
public class ProjectTemplateCreateRequestDTO {
    /**
     * 模板名称
     */
    private String name;

    /**
     * 模板描述
     */
    private String description;

    /**
     * 启用的模块列表
     */
    private List<String> enabledModules;

    /**
     * 跟踪器ID列表
     */
    private List<Long> trackerIds;

    /**
     * 默认角色ID列表（创建项目时自动分配给创建者的角色）
     */
    private List<Integer> defaultRoles;
}
