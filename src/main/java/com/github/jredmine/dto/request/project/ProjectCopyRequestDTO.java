package com.github.jredmine.dto.request.project;

import lombok.Data;

/**
 * 复制项目请求DTO
 *
 * @author panfeng
 */
@Data
public class ProjectCopyRequestDTO {
    /**
     * 新项目名称
     */
    private String name;

    /**
     * 新项目标识符
     */
    private String identifier;

    /**
     * 是否复制成员
     */
    private Boolean copyMembers = false;

    /**
     * 是否复制模块
     */
    private Boolean copyModules = false;

    /**
     * 是否复制跟踪器
     */
    private Boolean copyTrackers = false;

    /**
     * 是否复制版本（暂不支持）
     */
    private Boolean copyVersions = false;

    /**
     * 是否复制任务（暂不支持）
     */
    private Boolean copyIssues = false;
}
