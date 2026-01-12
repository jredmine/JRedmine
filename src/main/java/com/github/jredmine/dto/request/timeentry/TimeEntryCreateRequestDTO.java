package com.github.jredmine.dto.request.timeentry;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDate;

/**
 * 创建工时记录请求DTO
 *
 * @author panfeng
 * @since 2026-01-12
 */
@Data
@Schema(description = "创建工时记录请求")
public class TimeEntryCreateRequestDTO {
    
    @Schema(description = "项目ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "项目ID不能为空")
    private Long projectId;
    
    @Schema(description = "任务ID（可选）")
    private Long issueId;
    
    @Schema(description = "工作人员ID（不填则为当前登录用户）")
    private Long userId;
    
    @Schema(description = "工时（小时）", requiredMode = Schema.RequiredMode.REQUIRED, example = "2.5")
    @NotNull(message = "工时不能为空")
    @Positive(message = "工时必须大于0")
    private Float hours;
    
    @Schema(description = "工作日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-01-12")
    @NotNull(message = "工作日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate spentOn;
    
    @Schema(description = "活动类型ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "活动类型不能为空")
    private Long activityId;
    
    @Schema(description = "备注说明", maxLength = 1024)
    private String comments;
}
