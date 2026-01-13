package com.github.jredmine.dto.request.timeentry;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 工时记录批量导入请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "工时记录批量导入请求")
public class TimeEntryBatchImportRequestDTO {
    
    @Schema(description = "项目ID或项目标识", example = "1")
    private String project;
    
    @Schema(description = "任务ID", example = "100")
    private Long issueId;
    
    @Schema(description = "用户ID或用户登录名", example = "1")
    private String user;
    
    @Schema(description = "活动类型ID或活动类型名称", example = "9")
    private String activity;
    
    @Schema(description = "工作日期，格式：yyyy-MM-dd", example = "2026-01-14")
    private LocalDate spentOn;
    
    @Schema(description = "工时（小时）", example = "8.0")
    private Float hours;
    
    @Schema(description = "备注", example = "完成XX功能开发")
    private String comments;
}
