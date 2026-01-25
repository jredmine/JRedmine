package com.github.jredmine.dto.response.project;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 版本进度跟踪响应DTO
 *
 * @author panfeng
 */
@Data
@Builder
@Schema(description = "版本进度跟踪信息")
public class VersionProgressResponseDTO {

    @Schema(description = "版本ID")
    private Integer versionId;

    @Schema(description = "版本名称")
    private String versionName;

    @Schema(description = "版本状态")
    private String versionStatus;

    @Schema(description = "版本生效日期")
    private LocalDate effectiveDate;

    // ==================== 总体进度 ====================

    @Schema(description = "总体完成度百分比（0-100）")
    private Double overallProgress;

    @Schema(description = "任务总数")
    private Long totalIssues;

    @Schema(description = "已完成任务数")
    private Long completedIssues;

    @Schema(description = "进行中任务数")
    private Long inProgressIssues;

    @Schema(description = "待处理任务数")
    private Long pendingIssues;

    // ==================== 任务完成情况 ====================

    @Schema(description = "按状态分组的任务统计")
    private Map<String, TaskStatusStatistics> taskStatusStatistics;

    @Schema(description = "按跟踪器分组的任务统计")
    private Map<String, Long> taskTrackerStatistics;

    @Schema(description = "按优先级分组的任务统计")
    private Map<String, Long> taskPriorityStatistics;

    // ==================== 时间进度 ====================

    @Schema(description = "开始日期")
    private LocalDate startDate;

    @Schema(description = "截止日期")
    private LocalDate dueDate;

    @Schema(description = "预计完成日期")
    private LocalDate estimatedCompletionDate;

    @Schema(description = "已用天数")
    private Long elapsedDays;

    @Schema(description = "剩余天数")
    private Long remainingDays;

    @Schema(description = "时间进度百分比（0-100）")
    private Double timeProgress;

    // ==================== 里程碑节点 ====================

    @Schema(description = "里程碑节点列表")
    private List<MilestoneNode> milestones;

    // ==================== 进度图表数据 ====================

    @Schema(description = "每日进度数据（用于折线图）")
    private List<DailyProgressData> dailyProgressData;

    @Schema(description = "完成度分布数据（用于饼图）")
    private Map<String, Long> completionDistribution;

    @Schema(description = "工时进度数据")
    private HoursProgressData hoursProgress;

    /**
     * 任务状态统计
     */
    @Data
    @Builder
    @Schema(description = "任务状态统计")
    public static class TaskStatusStatistics {
        @Schema(description = "状态名称")
        private String statusName;

        @Schema(description = "任务数量")
        private Long count;

        @Schema(description = "完成度百分比")
        private Double averageDoneRatio;
    }

    /**
     * 里程碑节点
     */
    @Data
    @Builder
    @Schema(description = "里程碑节点")
    public static class MilestoneNode {
        @Schema(description = "节点名称")
        private String name;

        @Schema(description = "节点日期")
        private LocalDate date;

        @Schema(description = "节点类型（start/middle/end/milestone）")
        private String type;

        @Schema(description = "节点描述")
        private String description;

        @Schema(description = "该节点应完成的任务数")
        private Long expectedIssues;

        @Schema(description = "该节点实际完成的任务数")
        private Long completedIssues;

        @Schema(description = "该节点完成度百分比")
        private Double progress;
    }

    /**
     * 每日进度数据
     */
    @Data
    @Builder
    @Schema(description = "每日进度数据")
    public static class DailyProgressData {
        @Schema(description = "日期")
        private LocalDate date;

        @Schema(description = "当日完成的任务数")
        private Long completedCount;

        @Schema(description = "累计完成的任务数")
        private Long cumulativeCompleted;

        @Schema(description = "当日完成度百分比")
        private Double progress;
    }

    /**
     * 工时进度数据
     */
    @Data
    @Builder
    @Schema(description = "工时进度数据")
    public static class HoursProgressData {
        @Schema(description = "预估总工时")
        private Double estimatedHours;

        @Schema(description = "已消耗工时")
        private Double spentHours;

        @Schema(description = "剩余工时")
        private Double remainingHours;

        @Schema(description = "工时完成度百分比")
        private Double progress;
    }
}