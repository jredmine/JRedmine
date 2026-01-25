package com.github.jredmine.dto.response.project;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.Map;

/**
 * 版本统计响应DTO
 *
 * @author panfeng
 */
@Data
@Builder
@Schema(description = "版本统计信息")
public class VersionStatisticsResponseDTO {

    @Schema(description = "版本ID")
    private Integer versionId;

    @Schema(description = "版本名称")
    private String versionName;

    @Schema(description = "版本状态")
    private String versionStatus;

    @Schema(description = "版本生效日期")
    private LocalDate effectiveDate;

    // ==================== 任务统计 ====================

    @Schema(description = "关联任务总数")
    private Long totalIssues;

    @Schema(description = "已完成任务数")
    private Long completedIssues;

    @Schema(description = "进行中任务数")
    private Long inProgressIssues;

    @Schema(description = "待处理任务数")
    private Long pendingIssues;

    @Schema(description = "已关闭任务数")
    private Long closedIssues;

    @Schema(description = "版本完成度百分比（0-100）")
    private Double completionPercentage;

    // ==================== 工时统计 ====================

    @Schema(description = "预估总工时（小时）")
    private Double estimatedHours;

    @Schema(description = "已消耗工时（小时）")
    private Double spentHours;

    @Schema(description = "剩余工时（小时）")
    private Double remainingHours;

    @Schema(description = "工时完成度百分比（0-100）")
    private Double hoursCompletionPercentage;

    // ==================== 时间统计 ====================

    @Schema(description = "最早开始日期")
    private LocalDate earliestStartDate;

    @Schema(description = "最晚截止日期")
    private LocalDate latestDueDate;

    @Schema(description = "预计完成时间")
    private LocalDate estimatedCompletionDate;

    // ==================== 按状态分组统计 ====================

    @Schema(description = "按状态分组的任务数量")
    private Map<String, Long> issuesByStatus;

    // ==================== 按跟踪器分组统计 ====================

    @Schema(description = "按跟踪器分组的任务数量")
    private Map<String, Long> issuesByTracker;

    // ==================== 按优先级分组统计 ====================

    @Schema(description = "按优先级分组的任务数量")
    private Map<String, Long> issuesByPriority;
}