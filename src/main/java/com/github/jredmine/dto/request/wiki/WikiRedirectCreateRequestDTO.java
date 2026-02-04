package com.github.jredmine.dto.request.wiki;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Wiki 重定向创建请求 DTO
 *
 * @author panfeng
 */
@Data
@Schema(description = "Wiki 重定向创建请求")
public class WikiRedirectCreateRequestDTO {

    @Schema(description = "原标题（被访问时触发重定向）", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @Schema(description = "目标页面标题（重定向到此页）", requiredMode = Schema.RequiredMode.REQUIRED)
    private String redirectsTo;
}
