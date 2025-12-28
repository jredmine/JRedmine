package com.github.jredmine.dto.response.project;

import lombok.Data;

import java.util.Date;

/**
 * 项目详情响应DTO
 * 用于项目详情接口，包含项目的完整信息
 *
 * @author panfeng
 */
@Data
public class ProjectDetailResponseDTO {
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
     * 项目主页
     */
    private String homepage;

    /**
     * 是否公开
     */
    private Boolean isPublic;

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

    /**
     * 项目标识符
     */
    private String identifier;

    /**
     * 项目状态（1=活跃，5=关闭，9=归档）
     */
    private Integer status;

    /**
     * 是否继承父项目成员
     */
    private Boolean inheritMembers;

    /**
     * 默认版本ID
     */
    private Long defaultVersionId;

    /**
     * 默认分配给用户ID
     */
    private Long defaultAssignedToId;

    /**
     * 默认任务查询ID
     */
    private Long defaultIssueQueryId;
}

