package com.github.jredmine.dto.response.board;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 论坛回复列表项响应 DTO
 *
 * @author panfeng
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "论坛回复列表项")
public class MessageReplyListItemResponseDTO {

    @Schema(description = "消息 ID")
    private Integer id;

    @Schema(description = "父消息/主题 ID")
    private Integer parentId;

    @Schema(description = "正文")
    private String content;

    @Schema(description = "作者 ID")
    private Integer authorId;

    @Schema(description = "作者姓名")
    private String authorName;

    @Schema(description = "创建时间")
    private Date createdOn;

    @Schema(description = "更新时间")
    private Date updatedOn;
}
