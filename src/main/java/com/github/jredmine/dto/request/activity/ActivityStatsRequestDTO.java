package com.github.jredmine.dto.request.activity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 活动统计查询请求DTO
 *
 * @author panfeng
 */
@Data
@Schema(description = "活动统计查询请求")
public class ActivityStatsRequestDTO {

    @Schema(description = "开始时间")
    private LocalDateTime startDate;

    @Schema(description = "结束时间")
    private LocalDateTime endDate;

    @Schema(description = "项目ID筛选")
    private Long projectId;

    @Schema(description = "用户ID筛选")
    private Long userId;
}