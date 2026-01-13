package com.github.jredmine.dto.response.timeentry;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 时间段工时报表响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "时间段工时报表")
public class TimeEntryPeriodReportDTO {
    
    @Schema(description = "时间段类型：day-按日, week-按周, month-按月")
    private String periodType;
    
    @Schema(description = "开始日期")
    private String startDate;
    
    @Schema(description = "结束日期")
    private String endDate;
    
    @Schema(description = "总工时（小时）")
    private Float totalHours;
    
    @Schema(description = "记录总数")
    private Long totalCount;
    
    @Schema(description = "平均每日工时（小时）")
    private Float averageDailyHours;
    
    @Schema(description = "最高单日工时（小时）")
    private Float maxDailyHours;
    
    @Schema(description = "最低单日工时（小时）")
    private Float minDailyHours;
    
    @Schema(description = "工时趋势数据")
    private List<PeriodTimeDetail> periodDetails;
    
    /**
     * 时间段工时详情
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "时间段工时详情")
    public static class PeriodTimeDetail {
        
        @Schema(description = "时间段标识（如：2026-01-13、2026-W02、2026-01）")
        private String period;
        
        @Schema(description = "时间段描述（如：2026年1月13日、2026年第2周、2026年1月）")
        private String periodName;
        
        @Schema(description = "工时（小时）")
        private Float hours;
        
        @Schema(description = "记录数")
        private Long count;
        
        @Schema(description = "参与人数")
        private Long userCount;
    }
}
