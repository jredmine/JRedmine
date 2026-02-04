package com.github.jredmine.dto.response.wiki;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * Wiki 页面版本列表项响应 DTO
 *
 * @author panfeng
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Wiki 页面版本列表项")
public class WikiPageVersionListItemResponseDTO {

    @Schema(description = "版本号")
    private Integer version;

    @Schema(description = "更新人 ID")
    private Long authorId;

    @Schema(description = "更新人姓名")
    private String authorName;

    @Schema(description = "更新时间")
    private Date updatedOn;

    @Schema(description = "版本备注")
    private String comments;
}
