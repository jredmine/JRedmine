package com.github.jredmine.dto.response.document;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 文档列表项响应 DTO
 *
 * @author panfeng
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "文档列表项")
public class DocumentListItemResponseDTO {

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

    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "创建时间")
    private Date createdOn;
}
