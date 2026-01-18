package com.github.jredmine.dto.request.attachment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 附件上传请求DTO
 *
 * @author panfeng
 * @since 2026-01-18
 */
@Data
@Schema(description = "附件上传请求")
public class AttachmentUploadRequestDTO {
    
    @Schema(description = "容器类型（如：Issue、Project、Document、WikiPage等）")
    private String containerType;
    
    @Schema(description = "容器ID")
    private Long containerId;
    
    @Schema(description = "文件描述")
    private String description;
}
