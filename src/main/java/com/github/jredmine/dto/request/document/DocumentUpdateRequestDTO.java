package com.github.jredmine.dto.request.document;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 文档更新请求 DTO
 * 各字段均可选，只更新请求中非空字段；categoryId 传 0 表示设为未分类。
 *
 * @author panfeng
 */
@Data
@Schema(description = "文档更新请求")
public class DocumentUpdateRequestDTO {

    @Size(max = 255)
    @Schema(description = "文档标题（有值则更新）")
    private String title;

    @Schema(description = "文档描述（有值则更新）")
    private String description;

    @Schema(description = "文档分类 ID（有值则更新，0 表示未分类）")
    private Integer categoryId;
}
