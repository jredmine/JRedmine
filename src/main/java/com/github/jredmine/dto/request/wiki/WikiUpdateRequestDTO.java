package com.github.jredmine.dto.request.wiki;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Wiki 设置更新请求 DTO（如首页）
 *
 * @author panfeng
 */
@Data
@Schema(description = "Wiki 设置更新请求")
public class WikiUpdateRequestDTO {

    @Schema(description = "Wiki 首页标题（对应 start_page，须为已存在的页面标题）")
    private String startPage;
}
