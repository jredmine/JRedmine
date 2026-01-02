package com.github.jredmine.dto.response.issue;

import lombok.Data;

import java.util.List;

/**
 * 任务统计信息响应DTO
 *
 * @author panfeng
 */
@Data
public class IssueStatisticsResponseDTO {
    /**
     * 项目ID（如果指定了项目筛选）
     */
    private Long projectId;

    /**
     * 项目名称（如果指定了项目筛选）
     */
    private String projectName;

    /**
     * 任务总数
     */
    private Integer totalCount;

    /**
     * 进行中的任务数（状态不是已关闭）
     */
    private Integer inProgressCount;

    /**
     * 已完成的任务数（状态是已关闭）
     */
    private Integer completedCount;

    /**
     * 任务完成率（百分比，0-100）
     */
    private Double completionRate;

    /**
     * 按状态统计
     * Key: 状态ID, Value: 统计信息
     */
    private List<StatusStatistics> statusStatistics;

    /**
     * 按跟踪器统计
     * Key: 跟踪器ID, Value: 统计信息
     */
    private List<TrackerStatistics> trackerStatistics;

    /**
     * 按优先级统计
     * Key: 优先级ID, Value: 统计信息
     */
    private List<PriorityStatistics> priorityStatistics;

    /**
     * 按指派人统计
     * Key: 用户ID, Value: 统计信息
     */
    private List<AssigneeStatistics> assigneeStatistics;

    /**
     * 按创建者统计
     * Key: 用户ID, Value: 统计信息
     */
    private List<AuthorStatistics> authorStatistics;

    /**
     * 状态统计信息
     */
    @Data
    public static class StatusStatistics {
        /**
         * 状态ID
         */
        private Integer statusId;

        /**
         * 状态名称
         */
        private String statusName;

        /**
         * 任务数量
         */
        private Integer count;

        /**
         * 是否已关闭状态
         */
        private Boolean isClosed;
    }

    /**
     * 跟踪器统计信息
     */
    @Data
    public static class TrackerStatistics {
        /**
         * 跟踪器ID
         */
        private Integer trackerId;

        /**
         * 跟踪器名称
         */
        private String trackerName;

        /**
         * 任务数量
         */
        private Integer count;
    }

    /**
     * 优先级统计信息
     */
    @Data
    public static class PriorityStatistics {
        /**
         * 优先级ID
         */
        private Integer priorityId;

        /**
         * 优先级名称
         */
        private String priorityName;

        /**
         * 任务数量
         */
        private Integer count;
    }

    /**
     * 指派人统计信息
     */
    @Data
    public static class AssigneeStatistics {
        /**
         * 用户ID
         */
        private Long userId;

        /**
         * 用户名称
         */
        private String userName;

        /**
         * 任务数量
         */
        private Integer count;
    }

    /**
     * 创建者统计信息
     */
    @Data
    public static class AuthorStatistics {
        /**
         * 用户ID
         */
        private Long userId;

        /**
         * 用户名称
         */
        private String userName;

        /**
         * 任务数量
         */
        private Integer count;
    }
}
