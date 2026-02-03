package com.github.jredmine.dto.request.wiki;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Wiki 页面更新请求 DTO
 *
 * @author panfeng
 */
@Data
@Schema(description = "Wiki 页面更新请求")
public class WikiPageUpdateRequestDTO {

    @Schema(description = "正文（有值则新增一条内容版本）")
    private String text;

    @Schema(description = "版本备注")
    private String comments;

    @Schema(description = "父页面 ID（可选）")
    private Long parentId;

    @Schema(description = "是否保护（可选）")
    private Boolean isProtected;
}
