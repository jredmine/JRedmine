package com.github.jredmine.dto.response.timeentry;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 工时记录批量导入响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "工时记录批量导入响应")
public class TimeEntryBatchImportResponseDTO {
    
    @Schema(description = "总记录数")
    private Integer totalCount;
    
    @Schema(description = "成功导入数")
    private Integer successCount;
    
    @Schema(description = "失败记录数")
    private Integer failureCount;
    
    @Schema(description = "失败记录详情")
    private List<FailureDetail> failures;
    
    /**
     * 失败记录详情
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "失败记录详情")
    public static class FailureDetail {
        
        @Schema(description = "行号（从1开始，不包括表头）")
        private Integer rowNumber;
        
        @Schema(description = "失败原因")
        private String reason;
        
        @Schema(description = "原始数据")
        private String rawData;
    }
}
