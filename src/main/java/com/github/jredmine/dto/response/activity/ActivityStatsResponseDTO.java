package com.github.jredmine.dto.response.activity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * 活动统计响应DTO
 *
 * @author panfeng
 */
@Data
@Builder
@Schema(description = "活动统计信息")
public class ActivityStatsResponseDTO {

    @Schema(description = "总活动数量")
    private Long totalCount;

    @Schema(description = "各类型活动数量统计")
    private Map<String, Long> typeCount;

    @Schema(description = "各用户活动数量统计")
    private Map<String, Long> userCount;

    @Schema(description = "各项目活动数量统计")
    private Map<String, Long> projectCount;

    @Schema(description = "最近7天每日活动数量")
    private Map<String, Long> dailyCount;
}