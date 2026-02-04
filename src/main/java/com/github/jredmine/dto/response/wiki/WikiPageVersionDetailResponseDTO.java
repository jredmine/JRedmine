package com.github.jredmine.dto.response.wiki;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * Wiki 页面指定版本详情响应 DTO（含完整正文）
 *
 * @author panfeng
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Wiki 页面指定版本详情")
public class WikiPageVersionDetailResponseDTO {

    @Schema(description = "页面 ID")
    private Long pageId;

    @Schema(description = "页面标题")
    private String pageTitle;

    @Schema(description = "版本号")
    private Integer version;

    @Schema(description = "正文")
    private String text;

    @Schema(description = "版本备注")
    private String comments;

    @Schema(description = "更新人 ID")
    private Long authorId;

    @Schema(description = "更新人姓名")
    private String authorName;

    @Schema(description = "更新时间")
    private Date updatedOn;
}
