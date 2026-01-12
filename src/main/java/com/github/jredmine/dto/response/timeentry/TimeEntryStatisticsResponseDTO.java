package com.github.jredmine.dto.response.timeentry;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 工时统计响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "工时统计响应")
public class TimeEntryStatisticsResponseDTO {
    
    @Schema(description = "总工时（小时）")
    private Float totalHours;
    
    @Schema(description = "记录条数")
    private Long totalCount;
    
    @Schema(description = "平均工时（小时）")
    private Float averageHours;
    
    @Schema(description = "分组统计详情")
    private List<GroupStatistics> groups;
    
    /**
     * 分组统计详情
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "分组统计详情")
    public static class GroupStatistics {
        
        @Schema(description = "分组键（项目ID/用户ID/活动类型ID/日期）")
        private String groupKey;
        
        @Schema(description = "分组名称（项目名/用户名/活动类型名/日期）")
        private String groupName;
        
        @Schema(description = "该组工时（小时）")
        private Float hours;
        
        @Schema(description = "该组记录数")
        private Long count;
        
        @Schema(description = "占总工时的百分比")
        private Float percentage;
    }
}
