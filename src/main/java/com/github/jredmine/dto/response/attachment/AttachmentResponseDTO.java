package com.github.jredmine.dto.response.attachment;

import com.github.jredmine.dto.response.user.UserSimpleResponseDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 附件响应DTO
 *
 * @author panfeng
 * @since 2026-01-18
 */
@Data
@Schema(description = "附件响应")
public class AttachmentResponseDTO {
    
    @Schema(description = "附件ID")
    private Long id;
    
    @Schema(description = "容器类型")
    private String containerType;
    
    @Schema(description = "容器ID")
    private Long containerId;
    
    @Schema(description = "原始文件名")
    private String filename;
    
    @Schema(description = "文件大小（字节）")
    private Long filesize;
    
    @Schema(description = "文件类型（MIME类型）")
    private String contentType;
    
    @Schema(description = "下载次数")
    private Integer downloads;
    
    @Schema(description = "上传者信息")
    private UserSimpleResponseDTO author;
    
    @Schema(description = "创建时间")
    private LocalDateTime createdOn;
    
    @Schema(description = "文件描述")
    private String description;
    
    @Schema(description = "下载URL")
    private String downloadUrl;
    
    @Schema(description = "缩略图URL（仅图片文件）")
    private String thumbnailUrl;
    
    @Schema(description = "是否为图片文件")
    private Boolean isImage;
}
