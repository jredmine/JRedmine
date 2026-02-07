package com.github.jredmine.dto.response.document;

import com.github.jredmine.dto.response.attachment.AttachmentResponseDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

/**
 * 文档详情响应 DTO
 *
 * @author panfeng
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "文档详情")
public class DocumentDetailResponseDTO {

    @Schema(description = "文档 ID")
    private Integer id;

    @Schema(description = "项目 ID")
    private Integer projectId;

    @Schema(description = "文档标题")
    private String title;

    @Schema(description = "文档描述")
    private String description;

    @Schema(description = "分类 ID，0 表示未分类")
    private Integer categoryId;

    @Schema(description = "分类名称（若有）")
    private String categoryName;

    @Schema(description = "创建时间")
    private Date createdOn;

    @Schema(description = "附件数量")
    private Integer attachmentCount;

    @Schema(description = "附件列表（按创建时间倒序，最多返回一定条数）")
    private List<AttachmentResponseDTO> attachments;
}
