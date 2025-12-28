package com.github.jredmine.dto.response.project;

import lombok.Data;

import java.util.Date;

/**
 * 项目统计信息响应DTO
 *
 * @author panfeng
 */
@Data
public class ProjectStatisticsResponseDTO {
    /**
     * 项目ID
     */
    private Long projectId;

    /**
     * 项目名称
     */
    private String projectName;

    /**
     * 成员数量
     */
    private Integer memberCount;

    /**
     * 子项目数量
     */
    private Integer childrenCount;

    /**
     * 启用的模块数量
     */
    private Integer enabledModuleCount;

    /**
     * 跟踪器数量
     */
    private Integer trackerCount;

    /**
     * 任务统计（如果任务模块未实现，可能为null）
     */
    private IssueStatistics issueStatistics;

    /**
     * 工时统计（如果工时模块未实现，可能为null）
     */
    private TimeEntryStatistics timeEntryStatistics;

    /**
     * 最近更新时间
     */
    private Date lastUpdatedOn;

    /**
     * 任务统计信息
     */
    @Data
    public static class IssueStatistics {
        /**
         * 任务总数
         */
        private Integer totalCount;

        /**
         * 进行中的任务数
         */
        private Integer inProgressCount;

        /**
         * 已完成的任务数
         */
        private Integer completedCount;

        /**
         * 任务完成率（百分比，0-100）
         */
        private Double completionRate;
    }

    /**
     * 工时统计信息
     */
    @Data
    public static class TimeEntryStatistics {
        /**
         * 总工时（小时）
         */
        private Double totalHours;

        /**
         * 本月工时（小时）
         */
        private Double monthlyHours;
    }
}
