package com.github.jredmine.dto.request.attachment;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 附件更新请求DTO
 *
 * @author panfeng
 * @since 2026-01-18
 */
@Data
@Schema(description = "附件更新请求")
public class AttachmentUpdateRequestDTO {
    
    @Schema(description = "文件描述")
    private String description;
    
    @Schema(description = "文件名")
    private String filename;
}
