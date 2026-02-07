package com.github.jredmine.dto.response.document;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文档分类响应 DTO（供下拉/筛选）
 * 来自 enumerations 表，type=DocumentCategory；projectId 为 null 表示全局分类。
 *
 * @author panfeng
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "文档分类（供下拉/筛选）")
public class DocumentCategoryResponseDTO {

    @Schema(description = "分类 ID")
    private Integer id;

    @Schema(description = "分类名称")
    private String name;

    @Schema(description = "排序位置")
    private Integer position;

    @Schema(description = "项目 ID，null 表示全局分类")
    private Integer projectId;
}
