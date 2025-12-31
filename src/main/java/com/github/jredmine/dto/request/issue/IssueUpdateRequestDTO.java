package com.github.jredmine.dto.request.issue;

import lombok.Data;

import java.time.LocalDate;

/**
 * 更新任务请求DTO
 * 所有字段都是可选的，只更新提供的字段
 *
 * @author panfeng
 */
@Data
public class IssueUpdateRequestDTO {
    /**
     * 任务标题
     */
    private String subject;

    /**
     * 任务描述
     */
    private String description;

    /**
     * 状态ID
     */
    private Integer statusId;

    /**
     * 优先级ID
     */
    private Integer priorityId;

    /**
     * 分配给用户ID（0 表示取消分配）
     */
    private Long assignedToId;

    /**
     * 任务分类ID（0 表示无分类）
     */
    private Integer categoryId;

    /**
     * 修复版本ID（0 表示无版本）
     */
    private Long fixedVersionId;

    /**
     * 开始日期
     */
    private LocalDate startDate;

    /**
     * 截止日期
     */
    private LocalDate dueDate;

    /**
     * 预估工时
     */
    private Float estimatedHours;

    /**
     * 完成度（0-100）
     */
    private Integer doneRatio;

    /**
     * 父任务ID（0 表示无父任务）
     */
    private Long parentId;

    /**
     * 是否私有
     */
    private Boolean isPrivate;

    /**
     * 乐观锁版本号（用于并发控制）
     */
    private Integer lockVersion;
}
