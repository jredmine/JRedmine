package com.github.jredmine.dto.request.wiki;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Wiki 页面创建请求 DTO
 *
 * @author panfeng
 */
@Data
@Schema(description = "Wiki 页面创建请求")
public class WikiPageCreateRequestDTO {

    @Schema(description = "页面标题", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @Schema(description = "父页面 ID（可选，用于层级）")
    private Long parentId;

    @Schema(description = "是否保护（仅 manage_wiki 可编辑）")
    private Boolean isProtected;

    @Schema(description = "初始正文（可选）")
    private String text;

    @Schema(description = "版本备注（可选）")
    private String comments;
}
