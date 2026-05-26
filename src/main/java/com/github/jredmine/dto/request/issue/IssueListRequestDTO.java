package com.github.jredmine.dto.request.issue;

import com.github.jredmine.dto.request.PageRequestDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 任务列表查询请求DTO
 * 用于封装任务列表查询的所有参数
 *
 * @author panfeng
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class IssueListRequestDTO extends PageRequestDTO {

    /**
     * 项目ID
     */
    private Long projectId;

    /**
     * 状态ID
     */
    private Integer statusId;

    /**
     * 按状态是否已关闭筛选（true=已关闭，false=打开）；与 statusId 同时传入时以 statusId 为准
     */
    private Boolean statusIsClosed;

    /**
     * 跟踪器ID
     */
    private Integer trackerId;

    /**
     * 优先级ID
     */
    private Integer priorityId;

    /**
     * 指派人ID（0 表示查询未分配的任务）
     */
    private Long assignedToId;

    /**
     * 创建者ID
     */
    private Long authorId;

    /**
     * 任务分类ID（0 表示查询无分类的任务）
     */
    private Integer categoryId;

    /**
     * 修复版本ID（0 表示查询无版本的任务）
     */
    private Long fixedVersionId;

    /**
     * 关键词（搜索标题和描述）
     */
    private String keyword;

    /**
     * 创建日期起（含当天）
     */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate createdOnFrom;

    /**
     * 创建日期止（含当天）
     */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate createdOnTo;

    /**
     * 更新日期起（含当天）
     */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate updatedOnFrom;

    /**
     * 更新日期止（含当天）
     */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate updatedOnTo;

    /**
     * 是否私有
     */
    private Boolean isPrivate;

    /**
     * 排序字段（created_on, updated_on, priority, due_date）
     */
    private String sortBy;

    /**
     * 排序方向（asc, desc）
     */
    private String sortOrder = "desc";
}
