package com.github.jredmine.dto.request.attachment;

import com.github.jredmine.dto.request.PageRequestDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 附件查询请求DTO
 *
 * @author panfeng
 * @since 2026-01-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "附件查询请求")
public class AttachmentQueryRequestDTO extends PageRequestDTO {
    
    @Schema(description = "容器类型")
    private String containerType;
    
    @Schema(description = "容器ID")
    private Long containerId;
    
    @Schema(description = "上传者ID")
    private Long authorId;
    
    @Schema(description = "文件名（模糊搜索）")
    private String filename;
    
    @Schema(description = "文件类型")
    private String contentType;
}
