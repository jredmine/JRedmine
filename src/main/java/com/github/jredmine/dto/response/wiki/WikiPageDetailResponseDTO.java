package com.github.jredmine.dto.response.wiki;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * Wiki 页面详情响应 DTO（含最新内容）
 *
 * @author panfeng
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Wiki 页面详情（含最新内容）")
public class WikiPageDetailResponseDTO {

    @Schema(description = "页面 ID")
    private Long id;

    @Schema(description = "Wiki ID")
    private Long wikiId;

    @Schema(description = "项目 ID")
    private Long projectId;

    @Schema(description = "页面标题")
    private String title;

    @Schema(description = "父页面 ID")
    private Long parentId;

    @Schema(description = "是否保护")
    private Boolean isProtected;

    @Schema(description = "创建时间")
    private Date createdOn;

    @Schema(description = "正文（最新版本）")
    private String text;

    @Schema(description = "版本备注（最新版本）")
    private String comments;

    @Schema(description = "当前版本号")
    private Integer version;

    @Schema(description = "最后更新时间")
    private Date updatedOn;

    @Schema(description = "最后更新人 ID")
    private Long authorId;

    @Schema(description = "最后更新人姓名")
    private String authorName;
}
