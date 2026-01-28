package com.github.jredmine.dto.response.project;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 版本路线图响应DTO
 *
 * @author panfeng
 */
@Data
@Builder
@Schema(description = "版本路线图")
public class VersionRoadmapResponseDTO {

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "项目名称")
    private String projectName;

    @Schema(description = "版本总数")
    private Long totalVersions;

    @Schema(description = "按状态分组的版本统计")
    private Map<String, Long> statusStatistics;

    @Schema(description = "时间线数据（按时间排序的版本列表）")
    private List<VersionTimelineItem> timeline;

    @Schema(description = "按时间段分组的版本（用于图表展示）")
    private Map<String, List<VersionTimelineItem>> timelineByPeriod;

    /**
     * 版本时间线项
     */
    @Data
    @Builder
    @Schema(description = "版本时间线项")
    public static class VersionTimelineItem {
        @Schema(description = "版本ID")
        private Integer versionId;

        @Schema(description = "版本名称")
        private String versionName;

        @Schema(description = "版本描述")
        private String description;

        @Schema(description = "生效日期")
        private LocalDate effectiveDate;

        @Schema(description = "状态")
        private String status;

        @Schema(description = "状态描述")
        private String statusDescription;

        @Schema(description = "关联的任务数量")
        private Long issueCount;

        @Schema(description = "已完成任务数量")
        private Long completedIssueCount;

        @Schema(description = "版本完成度百分比")
        private Double completionPercentage;

        @Schema(description = "创建时间")
        private LocalDate createdDate;

        @Schema(description = "是否已过期（生效日期已过但未关闭）")
        private Boolean isOverdue;

        @Schema(description = "是否即将到期（7天内到期）")
        private Boolean isUpcoming;

        @Schema(description = "距离生效日期的天数（负数表示已过期）")
        private Long daysUntilEffective;
    }
}