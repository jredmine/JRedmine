package com.github.jredmine.dto.response.report;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * 燃尽图报表响应 DTO
 *
 * @author panfeng
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "燃尽图报表")
public class BurndownReportResponseDTO {

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "项目名称")
    private String projectName;

    @Schema(description = "版本ID（按版本燃尽时有值）")
    private Integer versionId;

    @Schema(description = "版本名称（按版本燃尽时有值）")
    private String versionName;

    @Schema(description = "图表开始日期")
    private LocalDate startDate;

    @Schema(description = "图表结束日期")
    private LocalDate endDate;

    @Schema(description = "总任务数（燃尽起点）")
    private Long totalIssues;

    @Schema(description = "已完成任务数（当前）")
    private Long completedIssues;

    @Schema(description = "剩余任务数（当前）")
    private Long remainingIssues;

    @Schema(description = "理想燃尽线（每日理想剩余量，用于绘图）")
    private List<BurndownPoint> idealLine;

    @Schema(description = "实际燃尽线（每日实际剩余量，用于绘图）")
    private List<BurndownPoint> actualLine;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "燃尽图数据点")
    public static class BurndownPoint {
        @Schema(description = "日期")
        private LocalDate date;
        @Schema(description = "剩余任务数")
        private Long remaining;
        @Schema(description = "当日已完成数（仅 actualLine 有值）")
        private Long completedThatDay;
    }
}
