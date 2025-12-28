package com.github.jredmine.dto.response.project;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 项目模板响应DTO
 *
 * @author panfeng
 */
@Data
public class ProjectTemplateResponseDTO {
    /**
     * 模板ID（项目ID）
     */
    private Long id;

    /**
     * 模板名称
     */
    private String name;

    /**
     * 模板描述
     */
    private String description;

    /**
     * 创建时间
     */
    private Date createdOn;

    /**
     * 更新时间
     */
    private Date updatedOn;

    /**
     * 启用的模块列表
     */
    private List<String> enabledModules;

    /**
     * 跟踪器ID列表
     */
    private List<Long> trackerIds;

    /**
     * 默认角色ID列表
     */
    private List<Integer> defaultRoles;
}
