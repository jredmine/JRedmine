package com.github.jredmine.dto.response.wiki;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * Wiki 重定向响应 DTO（列表项/创建返回）
 *
 * @author panfeng
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Wiki 重定向")
public class WikiRedirectResponseDTO {

    @Schema(description = "重定向 ID")
    private Long id;

    @Schema(description = "Wiki ID")
    private Long wikiId;

    @Schema(description = "项目 ID")
    private Long projectId;

    @Schema(description = "原标题")
    private String title;

    @Schema(description = "目标页面标题")
    private String redirectsTo;

    @Schema(description = "目标 Wiki ID（同项目一般与 wikiId 相同）")
    private Long redirectsToWikiId;

    @Schema(description = "创建时间")
    private Date createdOn;
}
