package com.github.jredmine.dto.request.project;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;

/**
 * 更新项目请求DTO
 *
 * @author panfeng
 */
@Data
public class ProjectUpdateRequestDTO {
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
     * 项目标识符（URL友好，只能包含字母、数字、连字符、下划线）
     */
    @Pattern(regexp = "^[a-z0-9_-]+$", message = "项目标识符只能包含小写字母、数字、连字符和下划线")
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
     * 启用的模块列表
     */
    private List<String> enabledModules;

    /**
     * 跟踪器ID列表
     */
    private List<Long> trackerIds;
}
