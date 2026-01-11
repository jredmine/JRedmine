package com.github.jredmine.dto.response.issue;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 任务详情响应DTO
 * 用于任务详情接口，包含任务的完整信息
 *
 * @author panfeng
 */
@Data
public class IssueDetailResponseDTO {
    /**
     * 任务ID
     */
    private Long id;

    /**
     * 跟踪器ID
     */
    private Integer trackerId;

    /**
     * 跟踪器名称
     */
    private String trackerName;

    /**
     * 项目ID
     */
    private Long projectId;

    /**
     * 项目名称
     */
    private String projectName;

    /**
     * 任务标题
     */
    private String subject;

    /**
     * 任务描述
     */
    private String description;

    /**
     * 截止日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate dueDate;

    /**
     * 任务分类ID
     */
    private Integer categoryId;

    /**
     * 任务分类名称
     */
    private String categoryName;

    /**
     * 任务状态ID
     */
    private Integer statusId;

    /**
     * 任务状态名称
     */
    private String statusName;

    /**
     * 分配给用户ID
     */
    private Long assignedToId;

    /**
     * 分配给用户名称
     */
    private String assignedToName;

    /**
     * 优先级ID
     */
    private Integer priorityId;

    /**
     * 优先级名称
     */
    private String priorityName;

    /**
     * 修复版本ID
     */
    private Long fixedVersionId;

    /**
     * 修复版本名称
     */
    private String fixedVersionName;

    /**
     * 创建者ID
     */
    private Long authorId;

    /**
     * 创建者名称
     */
    private String authorName;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdOn;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updatedOn;

    /**
     * 开始日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate startDate;

    /**
     * 完成度（0-100）
     */
    private Integer doneRatio;

    /**
     * 预估工时
     */
    private Float estimatedHours;

    /**
     * 父任务ID
     */
    private Long parentId;

    /**
     * 根任务ID
     */
    private Long rootId;

    /**
     * 是否私有
     */
    private Boolean isPrivate;

    /**
     * 关闭时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime closedOn;
    
    /**
     * 任务关联关系列表（所有类型）
     */
    private java.util.List<IssueRelationResponseDTO> relations;
}
