package com.github.jredmine.dto.request.board;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 论坛板块更新请求 DTO
 * 各字段均可选，只更新请求中非空字段。
 *
 * @author panfeng
 */
@Data
@Schema(description = "论坛板块更新请求")
public class BoardUpdateRequestDTO {

    @Size(max = 255)
    @Schema(description = "板块名称（有值则更新）")
    private String name;

    @Size(max = 255)
    @Schema(description = "板块描述（有值则更新）")
    private String description;

    @Schema(description = "排序位置（有值则更新）")
    private Integer position;
}
