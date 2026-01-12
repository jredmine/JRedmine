package com.github.jredmine.dto.request.timeentry;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 工时统计查询请求DTO
 */
@Data
@Schema(description = "工时统计查询请求")
public class TimeEntryStatisticsRequestDTO {
    
    @Schema(description = "项目ID")
    private Long projectId;
    
    @Schema(description = "任务ID")
    private Long issueId;
    
    @Schema(description = "用户ID")
    private Long userId;
    
    @Schema(description = "活动类型ID")
    private Long activityId;
    
    @Schema(description = "开始日期，格式：yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;
    
    @Schema(description = "结束日期，格式：yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;
    
    @Schema(description = "年份，如：2026")
    private Integer year;
    
    @Schema(description = "月份，1-12")
    private Integer month;
    
    @Schema(description = "统计维度：project-按项目, user-按用户, activity-按活动类型, date-按日期")
    private String groupBy;
}
