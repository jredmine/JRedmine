package com.github.jredmine.dto.response.issue;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * 任务甘特图项响应DTO
 *
 * @author panfeng
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "任务甘特图项响应")
public class IssueGanttItemResponseDTO {
    
    @Schema(description = "任务ID")
    private Long id;
    
    @Schema(description = "任务标题")
    private String subject;
    
    @Schema(description = "项目ID")
    private Long projectId;
    
    @Schema(description = "项目名称")
    private String projectName;
    
    @Schema(description = "跟踪器ID")
    private Integer trackerId;
    
    @Schema(description = "跟踪器名称")
    private String trackerName;
    
    @Schema(description = "状态ID")
    private Integer statusId;
    
    @Schema(description = "状态名称")
    private String statusName;
    
    @Schema(description = "状态是否已关闭")
    private Boolean statusClosed;
    
    @Schema(description = "优先级ID")
    private Integer priorityId;
    
    @Schema(description = "优先级名称")
    private String priorityName;
    
    @Schema(description = "分配给用户ID")
    private Long assignedToId;
    
    @Schema(description = "分配给用户名称")
    private String assignedToName;
    
    @Schema(description = "父任务ID")
    private Long parentId;
    
    @Schema(description = "开始日期（YYYY-MM-DD）")
    private LocalDate startDate;
    
    @Schema(description = "截止日期（YYYY-MM-DD）")
    private LocalDate dueDate;
    
    @Schema(description = "完成度（0-100）")
    private Integer doneRatio;
    
    @Schema(description = "预估工时")
    private Float estimatedHours;
    
    @Schema(description = "修复版本ID")
    private Long fixedVersionId;
    
    @Schema(description = "修复版本名称")
    private String fixedVersionName;
    
    @Schema(description = "是否为里程碑（根据任务树中的层级判断）")
    private Boolean isMilestone;
    
    @Schema(description = "任务的前置依赖（precedes关系）")
    private List<IssueGanttDependencyDTO> dependencies;
}
