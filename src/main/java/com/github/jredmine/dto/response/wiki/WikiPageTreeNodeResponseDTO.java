package com.github.jredmine.dto.response.wiki;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

/**
 * Wiki 页面树形节点 DTO（用于树形列表）
 *
 * @author panfeng
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Wiki 页面树形节点")
public class WikiPageTreeNodeResponseDTO {

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

    @Schema(description = "最后更新时间")
    private Date updatedOn;

    @Schema(description = "当前版本号")
    private Integer version;

    @Schema(description = "最后更新人姓名")
    private String authorName;

    @Schema(description = "子页面列表（树形嵌套）")
    private List<WikiPageTreeNodeResponseDTO> children;
}
