package com.github.jredmine.dto.response.project;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 版本发布响应DTO
 *
 * @author panfeng
 */
@Data
@Builder
@Schema(description = "版本发布响应")
public class VersionReleaseResponseDTO {

    @Schema(description = "版本ID")
    private Integer versionId;

    @Schema(description = "版本名称")
    private String versionName;

    @Schema(description = "旧状态")
    private String oldStatus;

    @Schema(description = "新状态")
    private String newStatus;

    @Schema(description = "发布日期")
    private LocalDate releaseDate;

    @Schema(description = "发布时间")
    private LocalDateTime releasedOn;

    @Schema(description = "操作人ID")
    private Long operatorId;

    @Schema(description = "操作人姓名")
    private String operatorName;

    @Schema(description = "备注信息")
    private String notes;

    @Schema(description = "发布记录ID（Journal ID）")
    private Integer journalId;

    @Schema(description = "版本统计信息")
    private ReleaseStatistics statistics;

    /**
     * 发布统计信息
     */
    @Data
    @Builder
    @Schema(description = "发布统计信息")
    public static class ReleaseStatistics {
        @Schema(description = "关联的任务总数")
        private Long totalIssues;

        @Schema(description = "已完成任务数")
        private Long completedIssues;

        @Schema(description = "未完成任务数")
        private Long incompleteIssues;

        @Schema(description = "版本完成度百分比")
        private Double completionPercentage;
    }
}