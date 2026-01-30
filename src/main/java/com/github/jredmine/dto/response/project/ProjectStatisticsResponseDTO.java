package com.github.jredmine.dto.response.project;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 项目统计信息响应DTO（统计报表模块）
 *
 * @author panfeng
 */
@Data
@Schema(description = "项目统计信息")
public class ProjectStatisticsResponseDTO {
    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "项目名称")
    private String projectName;

    @Schema(description = "项目状态")
    private Integer projectStatus;

    @Schema(description = "成员数量")
    private Integer memberCount;

    @Schema(description = "子项目数量")
    private Integer childrenCount;

    @Schema(description = "启用的模块数量")
    private Integer enabledModuleCount;

    @Schema(description = "跟踪器数量")
    private Integer trackerCount;

    @Schema(description = "版本数量")
    private Integer versionCount;

    @Schema(description = "任务统计")
    private IssueStatistics issueStatistics;

    @Schema(description = "工时统计")
    private TimeEntryStatistics timeEntryStatistics;

    @Schema(description = "最近更新时间")
    private Date lastUpdatedOn;

    /**
     * 任务统计信息
     */
    @Data
    @Schema(description = "任务统计信息")
    public static class IssueStatistics {
        @Schema(description = "任务总数")
        private Integer totalCount;

        @Schema(description = "待处理任务数（未开始）")
        private Integer pendingCount;

        @Schema(description = "进行中的任务数")
        private Integer inProgressCount;

        @Schema(description = "已完成的任务数")
        private Integer completedCount;

        @Schema(description = "任务完成率（百分比，0-100）")
        private Double completionRate;

        @Schema(description = "按状态统计")
        private List<StatusCountItem> byStatus;

        @Schema(description = "按跟踪器统计")
        private List<TrackerCountItem> byTracker;
    }

    /**
     * 工时统计信息
     */
    @Data
    @Schema(description = "工时统计信息")
    public static class TimeEntryStatistics {
        @Schema(description = "总工时（小时）")
        private Double totalHours;

        @Schema(description = "本月工时（小时）")
        private Double monthlyHours;

        @Schema(description = "本周工时（小时）")
        private Double weeklyHours;

        @Schema(description = "工时记录条数")
        private Integer entryCount;
    }

    /**
     * 按状态统计项
     */
    @Data
    @Schema(description = "按状态统计项")
    public static class StatusCountItem {
        @Schema(description = "状态ID")
        private Integer statusId;
        @Schema(description = "状态名称")
        private String statusName;
        @Schema(description = "任务数量")
        private Integer count;
        @Schema(description = "是否已关闭状态")
        private Boolean isClosed;
    }

    /**
     * 按跟踪器统计项
     */
    @Data
    @Schema(description = "按跟踪器统计项")
    public static class TrackerCountItem {
        @Schema(description = "跟踪器ID")
        private Integer trackerId;
        @Schema(description = "跟踪器名称")
        private String trackerName;
        @Schema(description = "任务数量")
        private Integer count;
    }
}
