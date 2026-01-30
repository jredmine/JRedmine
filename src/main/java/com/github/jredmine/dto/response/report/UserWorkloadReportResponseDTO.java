package com.github.jredmine.dto.response.report;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 用户工作量统计报表响应 DTO
 *
 * @author panfeng
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户工作量统计报表")
public class UserWorkloadReportResponseDTO {

    @Schema(description = "汇总信息")
    private Summary summary;

    @Schema(description = "用户工作量明细列表（按工时降序）")
    private List<UserWorkloadItem> items;

    @Schema(description = "查询条件说明")
    private String queryDescription;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "汇总信息")
    public static class Summary {
        @Schema(description = "统计用户数")
        private Long userCount;
        @Schema(description = "任务总数（被分配的任务）")
        private Long totalIssues;
        @Schema(description = "已完成任务数")
        private Long completedIssues;
        @Schema(description = "总工时（小时）")
        private Double totalHours;
        @Schema(description = "平均每人工时（小时）")
        private Double averageHoursPerUser;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "用户工作量明细")
    public static class UserWorkloadItem {
        @Schema(description = "用户ID")
        private Long userId;
        @Schema(description = "用户登录名")
        private String login;
        @Schema(description = "用户姓名")
        private String displayName;
        @Schema(description = "分配任务数")
        private Long issueCount;
        @Schema(description = "已完成任务数")
        private Long completedCount;
        @Schema(description = "进行中任务数")
        private Long inProgressCount;
        @Schema(description = "待处理任务数")
        private Long pendingCount;
        @Schema(description = "任务完成率（%，保留两位小数）")
        private Double completionRate;
        @Schema(description = "登记工时（小时，保留两位小数）")
        private Double totalHours;
        @Schema(description = "工时记录条数")
        private Long timeEntryCount;
    }
}
