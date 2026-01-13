package com.github.jredmine.dto.response.timeentry;

import com.github.jredmine.dto.response.project.ProjectSimpleResponseDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 项目工时报表响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "项目工时报表")
public class TimeEntryProjectReportDTO {
    
    @Schema(description = "项目信息")
    private ProjectSimpleResponseDTO project;
    
    @Schema(description = "总工时（小时）")
    private Float totalHours;
    
    @Schema(description = "记录总数")
    private Long totalCount;
    
    @Schema(description = "参与人数")
    private Long userCount;
    
    @Schema(description = "平均工时（小时）")
    private Float averageHours;
    
    @Schema(description = "最早记录日期")
    private String earliestDate;
    
    @Schema(description = "最晚记录日期")
    private String latestDate;
    
    @Schema(description = "成员工时详情")
    private List<UserTimeDetail> userDetails;
    
    @Schema(description = "活动类型工时详情")
    private List<ActivityTimeDetail> activityDetails;
    
    /**
     * 成员工时详情
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "成员工时详情")
    public static class UserTimeDetail {
        
        @Schema(description = "用户ID")
        private Long userId;
        
        @Schema(description = "用户名")
        private String userName;
        
        @Schema(description = "工时（小时）")
        private Float hours;
        
        @Schema(description = "记录数")
        private Long count;
        
        @Schema(description = "占比（%）")
        private Float percentage;
    }
    
    /**
     * 活动类型工时详情
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "活动类型工时详情")
    public static class ActivityTimeDetail {
        
        @Schema(description = "活动类型ID")
        private Long activityId;
        
        @Schema(description = "活动类型名称")
        private String activityName;
        
        @Schema(description = "工时（小时）")
        private Float hours;
        
        @Schema(description = "记录数")
        private Long count;
        
        @Schema(description = "占比（%）")
        private Float percentage;
    }
}
