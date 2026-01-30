package com.github.jredmine.dto.response.timeentry;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 工时统计报表响应 DTO（报表模块统一入口）
 *
 * @author panfeng
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "工时统计报表")
public class TimeEntryReportResponseDTO {

    @Schema(description = "汇总信息")
    private Summary summary;

    @Schema(description = "按用户统计")
    private List<GroupItem> byUser;

    @Schema(description = "按项目统计")
    private List<GroupItem> byProject;

    @Schema(description = "按活动类型统计")
    private List<GroupItem> byActivity;

    @Schema(description = "按日期趋势（日/周/月）")
    private List<PeriodItem> periodTrend;

    @Schema(description = "查询条件说明（如：2026年1月、2026-01-01~2026-01-31）")
    private String queryDescription;

    @Schema(description = "汇总信息")
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        @Schema(description = "总工时（小时）")
        private Double totalHours;
        @Schema(description = "记录总数")
        private Long totalCount;
        @Schema(description = "平均每条工时（小时）")
        private Double averageHours;
        @Schema(description = "参与用户数")
        private Long userCount;
        @Schema(description = "涉及项目数")
        private Long projectCount;
        @Schema(description = "最早记录日期")
        private String earliestDate;
        @Schema(description = "最晚记录日期")
        private String latestDate;
    }

    @Schema(description = "分组统计项")
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GroupItem {
        @Schema(description = "分组键（用户ID/项目ID/活动类型ID）")
        private String groupKey;
        @Schema(description = "分组名称")
        private String groupName;
        @Schema(description = "工时（小时）")
        private Double hours;
        @Schema(description = "记录数")
        private Long count;
        @Schema(description = "占比（%）")
        private Double percentage;
    }

    @Schema(description = "时间段统计项")
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PeriodItem {
        @Schema(description = "时间段标识")
        private String period;
        @Schema(description = "时间段描述")
        private String periodName;
        @Schema(description = "工时（小时）")
        private Double hours;
        @Schema(description = "记录数")
        private Long count;
        @Schema(description = "参与人数")
        private Long userCount;
    }
}
