package com.github.jredmine.dto.request.document;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 项目级文档分类更新请求 DTO
 * 只更新请求中非空字段。
 *
 * @author panfeng
 */
@Data
@Schema(description = "项目级文档分类更新请求")
public class DocumentCategoryUpdateRequestDTO {

    @Size(max = 255)
    @Schema(description = "分类名称（有值则更新）")
    private String name;

    @Schema(description = "排序位置（有值则更新）")
    private Integer position;
}
