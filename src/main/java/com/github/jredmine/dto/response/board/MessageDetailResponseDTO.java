package com.github.jredmine.dto.response.board;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 论坛消息/主题详情响应 DTO
 *
 * @author panfeng
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "论坛消息/主题详情")
public class MessageDetailResponseDTO {

    @Schema(description = "消息 ID")
    private Integer id;

    @Schema(description = "板块 ID")
    private Integer boardId;

    @Schema(description = "父消息 ID，空表示主题帖")
    private Integer parentId;

    @Schema(description = "主题标题")
    private String subject;

    @Schema(description = "正文")
    private String content;

    @Schema(description = "作者 ID")
    private Integer authorId;

    @Schema(description = "作者姓名")
    private String authorName;

    @Schema(description = "回复数（主题帖时有意义）")
    private Integer repliesCount;

    @Schema(description = "创建时间")
    private Date createdOn;

    @Schema(description = "更新时间")
    private Date updatedOn;

    @Schema(description = "是否锁定")
    private Boolean locked;

    @Schema(description = "是否置顶")
    private Integer sticky;
}
