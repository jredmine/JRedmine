package com.github.jredmine.dto.response.timeentry;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 工时汇总响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "工时汇总响应")
public class TimeEntrySummaryResponseDTO {
    
    @Schema(description = "总工时（小时）")
    private Float totalHours;
    
    @Schema(description = "记录条数")
    private Long totalCount;
    
    @Schema(description = "平均工时（小时）")
    private Float averageHours;
    
    @Schema(description = "最小工时（小时）")
    private Float minHours;
    
    @Schema(description = "最大工时（小时）")
    private Float maxHours;
}
