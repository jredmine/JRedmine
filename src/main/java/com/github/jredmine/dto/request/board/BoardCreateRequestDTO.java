package com.github.jredmine.dto.request.board;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 论坛板块创建请求 DTO
 *
 * @author panfeng
 */
@Data
@Schema(description = "论坛板块创建请求")
public class BoardCreateRequestDTO {

    @NotBlank(message = "板块名称不能为空")
    @Size(max = 255)
    @Schema(description = "板块名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Size(max = 255)
    @Schema(description = "板块描述")
    private String description;

    @Schema(description = "排序位置（数字越小越靠前）")
    private Integer position;

    @Schema(description = "父板块 ID（可选，用于板块层级）")
    private Integer parentId;
}
