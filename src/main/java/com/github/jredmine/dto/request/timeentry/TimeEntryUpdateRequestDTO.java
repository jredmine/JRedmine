package com.github.jredmine.dto.request.timeentry;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDate;

/**
 * 更新工时记录请求DTO
 *
 * @author panfeng
 * @since 2026-01-12
 */
@Data
@Schema(description = "更新工时记录请求")
public class TimeEntryUpdateRequestDTO {
    
    @Schema(description = "项目ID")
    private Long projectId;
    
    @Schema(description = "任务ID（可选）")
    private Long issueId;
    
    @Schema(description = "工作人员ID")
    private Long userId;
    
    @Schema(description = "工时（小时）", example = "2.5")
    @Positive(message = "工时必须大于0")
    private Float hours;
    
    @Schema(description = "工作日期", example = "2026-01-12")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate spentOn;
    
    @Schema(description = "活动类型ID")
    private Long activityId;
    
    @Schema(description = "备注说明", maxLength = 1024)
    private String comments;
}
