package com.github.jredmine.dto.response.project;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 版本响应DTO
 *
 * @author panfeng
 */
@Data
public class VersionResponseDTO {
    /**
     * 版本ID
     */
    private Integer id;

    /**
     * 项目ID
     */
    private Integer projectId;

    /**
     * 项目名称
     */
    private String projectName;

    /**
     * 版本名称
     */
    private String name;

    /**
     * 版本描述
     */
    private String description;

    /**
     * 生效日期
     */
    private LocalDate effectiveDate;

    /**
     * 创建时间
     */
    private LocalDateTime createdOn;

    /**
     * 更新时间
     */
    private LocalDateTime updatedOn;

    /**
     * Wiki页面标题
     */
    private String wikiPageTitle;

    /**
     * 状态
     * - 'open': 开放
     * - 'locked': 锁定
     * - 'closed': 关闭
     */
    private String status;

    /**
     * 共享方式
     * - 'none': 不共享
     * - 'descendants': 共享给子项目
     * - 'hierarchy': 共享给项目层次结构
     * - 'tree': 共享给项目树
     * - 'system': 系统共享
     */
    private String sharing;

    /**
     * 关联的任务数量
     */
    private Integer issueCount;
}
