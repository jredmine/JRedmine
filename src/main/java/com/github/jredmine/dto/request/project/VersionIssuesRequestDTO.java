package com.github.jredmine.dto.request.project;

import com.github.jredmine.dto.request.PageRequestDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 版本关联任务查询请求DTO
 *
 * @author panfeng
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "版本关联任务查询请求")
public class VersionIssuesRequestDTO extends PageRequestDTO {

    @Schema(description = "状态ID筛选")
    private Integer statusId;

    @Schema(description = "跟踪器ID筛选")
    private Integer trackerId;

    @Schema(description = "优先级ID筛选")
    private Integer priorityId;

    @Schema(description = "指派人ID筛选（0表示查询未分配的任务）")
    private Long assignedToId;

    @Schema(description = "创建者ID筛选")
    private Long authorId;

    @Schema(description = "任务分类ID筛选（0表示查询无分类的任务）")
    private Integer categoryId;

    @Schema(description = "关键词搜索（在标题和描述中搜索）")
    private String keyword;

    @Schema(description = "是否私有筛选")
    private Boolean isPrivate;

    @Schema(description = "排序字段（created_on, updated_on, priority, due_date）")
    private String sortBy;

    @Schema(description = "排序方向（asc, desc），默认desc")
    private String sortOrder = "desc";
}