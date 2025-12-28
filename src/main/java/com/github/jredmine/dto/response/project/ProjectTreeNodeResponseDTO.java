package com.github.jredmine.dto.response.project;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 项目树节点响应DTO
 * 用于项目树接口，包含树形结构所需的子项目列表
 *
 * @author panfeng
 */
@Data
public class ProjectTreeNodeResponseDTO {
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

    /**
     * 子项目列表
     */
    private List<ProjectTreeNodeResponseDTO> children;
}
