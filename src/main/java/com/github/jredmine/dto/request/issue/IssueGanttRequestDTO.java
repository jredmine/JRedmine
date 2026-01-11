package com.github.jredmine.dto.request.issue;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.time.LocalDate;

/**
 * 任务甘特图查询请求DTO
 *
 * @author panfeng
 */
@Data
@Schema(description = "任务甘特图查询请求")
public class IssueGanttRequestDTO {
    
    @Schema(description = "项目ID（必填）")
    @Min(value = 1, message = "项目ID必须大于0")
    private Long projectId;
    
    @Schema(description = "是否包含子项目的任务", defaultValue = "false")
    private Boolean includeSubprojects = false;
    
    @Schema(description = "版本ID（可选，筛选特定版本的任务）")
    private Long versionId;
    
    @Schema(description = "跟踪器ID（可选，筛选特定跟踪器的任务）")
    private Integer trackerId;
    
    @Schema(description = "状态ID（可选，筛选特定状态的任务）")
    private Integer statusId;
    
    @Schema(description = "分配给用户ID（可选，筛选特定用户的任务）")
    private Long assignedToId;
    
    @Schema(description = "开始日期筛选（可选，格式：YYYY-MM-DD）")
    private LocalDate startDateFrom;
    
    @Schema(description = "截止日期筛选（可选，格式：YYYY-MM-DD）")
    private LocalDate dueDateTo;
    
    @Schema(description = "是否仅显示有日期的任务（默认false，显示所有任务）", defaultValue = "false")
    private Boolean onlyWithDates = false;
}
