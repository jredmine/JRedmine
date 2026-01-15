package com.github.jredmine.dto.response.timeentry;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 工时记录批量删除响应DTO
 *
 * @author panfeng
 * @since 2026-01-14
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "工时记录批量删除响应")
public class TimeEntryBatchDeleteResponseDTO {
    
    @Schema(description = "总记录数")
    private Integer totalCount;
    
    @Schema(description = "成功删除数")
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
        
        @Schema(description = "工时记录ID")
        private Long timeEntryId;
        
        @Schema(description = "失败原因")
        private String reason;
    }
}
