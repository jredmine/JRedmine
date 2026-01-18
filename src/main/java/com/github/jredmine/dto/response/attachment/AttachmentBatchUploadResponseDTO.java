package com.github.jredmine.dto.response.attachment;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 附件批量上传响应DTO
 *
 * @author panfeng
 * @since 2026-01-18
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "附件批量上传响应")
public class AttachmentBatchUploadResponseDTO {
    
    @Schema(description = "总文件数")
    private Integer totalCount;
    
    @Schema(description = "成功上传数")
    private Integer successCount;
    
    @Schema(description = "失败文件数")
    private Integer failureCount;
    
    @Schema(description = "成功上传的附件列表")
    private List<AttachmentResponseDTO> successes;
    
    @Schema(description = "失败文件详情")
    private List<FailureDetail> failures;
    
    /**
     * 失败文件详情
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "失败文件详情")
    public static class FailureDetail {
        
        @Schema(description = "文件名")
        private String filename;
        
        @Schema(description = "失败原因")
        private String reason;
    }
}
