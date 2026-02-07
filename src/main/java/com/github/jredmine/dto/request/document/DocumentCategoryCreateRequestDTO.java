package com.github.jredmine.dto.request.document;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 项目级文档分类创建请求 DTO
 *
 * @author panfeng
 */
@Data
@Schema(description = "项目级文档分类创建请求")
public class DocumentCategoryCreateRequestDTO {

    @NotBlank(message = "分类名称不能为空")
    @Size(max = 255)
    @Schema(description = "分类名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "排序位置（数字越小越靠前）")
    private Integer position;
}
