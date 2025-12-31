package com.github.jredmine.dto.request.issue;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 创建任务请求DTO
 *
 * @author panfeng
 */
@Data
public class IssueCreateRequestDTO {
    /**
     * 项目ID
     */
    @NotNull(message = "项目ID不能为空")
    private Long projectId;

    /**
     * 跟踪器ID
     */
    @NotNull(message = "跟踪器ID不能为空")
    private Integer trackerId;

    /**
     * 任务标题
     */
    @NotBlank(message = "任务标题不能为空")
    private String subject;

    /**
     * 任务描述
     */
    private String description;

    /**
     * 状态ID（可选，默认使用跟踪器的默认状态）
     */
    private Integer statusId;

    /**
     * 优先级ID
     */
    @NotNull(message = "优先级ID不能为空")
    private Integer priorityId;

    /**
     * 分配给用户ID
     */
    private Long assignedToId;

    /**
     * 任务分类ID
     */
    private Integer categoryId;

    /**
     * 修复版本ID
     */
    private Long fixedVersionId;

    /**
     * 开始日期（支持格式：yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss）
     * 如果传入带时间部分的字符串，会自动提取日期部分
     */
    private LocalDate startDate;

    /**
     * 截止日期（支持格式：yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss）
     * 如果传入带时间部分的字符串，会自动提取日期部分
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
     * 父任务ID
     */
    private Long parentId;

    /**
     * 是否私有
     */
    private Boolean isPrivate = false;
}
