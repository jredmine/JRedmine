package com.github.jredmine.dto.request.timeentry;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

/**
 * 工时记录查询请求DTO
 *
 * @author panfeng
 * @since 2026-01-12
 */
@Data
@Schema(description = "工时记录查询请求")
public class TimeEntryQueryRequestDTO {
    
    @Schema(description = "项目ID")
    private Long projectId;
    
    @Schema(description = "任务ID")
    private Long issueId;
    
    @Schema(description = "工作人员ID")
    private Long userId;
    
    @Schema(description = "活动类型ID")
    private Long activityId;
    
    @Schema(description = "开始日期（含）", example = "2026-01-01")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;
    
    @Schema(description = "结束日期（含）", example = "2026-01-31")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;
    
    @Schema(description = "年份", example = "2026")
    private Integer year;
    
    @Schema(description = "月份（1-12）", example = "1")
    private Integer month;
    
    @Schema(description = "当前页码（从1开始）", example = "1")
    private Integer pageNum = 1;
    
    @Schema(description = "每页大小", example = "20")
    private Integer pageSize = 20;
    
    @Schema(description = "排序字段（spent_on, created_on, hours）", example = "spent_on")
    private String sortBy = "spent_on";
    
    @Schema(description = "排序方向（asc, desc）", example = "desc")
    private String sortOrder = "desc";
}
