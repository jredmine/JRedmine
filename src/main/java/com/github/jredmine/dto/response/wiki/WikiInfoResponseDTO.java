package com.github.jredmine.dto.response.wiki;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 项目 Wiki 信息响应 DTO（初始化/获取）
 *
 * @author panfeng
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "项目 Wiki 信息")
public class WikiInfoResponseDTO {

    @Schema(description = "Wiki 主表 ID")
    private Long id;

    @Schema(description = "项目 ID")
    private Long projectId;

    @Schema(description = "项目名称")
    private String projectName;

    @Schema(description = "Wiki 首页标题（start_page）")
    private String startPage;

    @Schema(description = "状态（1=正常）")
    private Integer status;
}
