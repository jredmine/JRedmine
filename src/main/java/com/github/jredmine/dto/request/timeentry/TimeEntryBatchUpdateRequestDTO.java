package com.github.jredmine.dto.request.timeentry;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * 工时记录批量更新请求DTO
 *
 * @author panfeng
 * @since 2026-01-15
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "工时记录批量更新请求")
public class TimeEntryBatchUpdateRequestDTO {
    
    @Schema(description = "要更新的工时记录列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "更新的工时记录列表不能为空")
    private List<TimeEntryUpdateItem> items;
    
    /**
     * 单条工时记录更新项
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "单条工时记录更新项")
    public static class TimeEntryUpdateItem {
        
        @Schema(description = "工时记录ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "工时记录ID不能为空")
        private Long id;
        
        @Schema(description = "任务ID（可选）")
        private Long issueId;
        
        @Schema(description = "工作人员ID（可选）")
        private Long userId;
        
        @Schema(description = "工时（小时）", example = "2.5")
        @Positive(message = "工时必须大于0")
        private Float hours;
        
        @Schema(description = "工作日期", example = "2026-01-15")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate spentOn;
        
        @Schema(description = "活动类型ID")
        private Long activityId;
        
        @Schema(description = "备注说明", maxLength = 1024)
        private String comments;
    }
}
