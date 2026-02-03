package com.github.jredmine.dto.response.wiki;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * Wiki 页面列表项响应 DTO
 *
 * @author panfeng
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Wiki 页面列表项")
public class WikiPageListItemResponseDTO {

    @Schema(description = "页面 ID")
    private Long id;

    @Schema(description = "页面标题")
    private String title;

    @Schema(description = "父页面 ID")
    private Long parentId;

    @Schema(description = "是否保护")
    private Boolean isProtected;

    @Schema(description = "创建时间")
    private Date createdOn;

    @Schema(description = "最后更新时间（来自最新内容）")
    private Date updatedOn;

    @Schema(description = "当前版本号")
    private Integer version;

    @Schema(description = "最后更新人姓名")
    private String authorName;
}
