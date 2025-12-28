package com.github.jredmine.dto.response.project;

import lombok.Data;

import java.util.Date;

/**
 * 项目列表项响应DTO
 * 用于项目列表接口，包含列表展示所需的基本信息
 *
 * @author panfeng
 */
@Data
public class ProjectListItemResponseDTO {
    /**
     * 项目ID
     */
    private Long id;

    /**
     * 项目名称
     */
    private String name;

    /**
     * 项目描述
     */
    private String description;

    /**
     * 项目标识符
     */
    private String identifier;

    /**
     * 是否公开
     */
    private Boolean isPublic;

    /**
     * 项目状态（1=活跃，5=关闭，9=归档）
     */
    private Integer status;

    /**
     * 父项目ID
     */
    private Long parentId;

    /**
     * 创建时间
     */
    private Date createdOn;

    /**
     * 更新时间
     */
    private Date updatedOn;
}

