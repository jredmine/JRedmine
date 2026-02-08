package com.github.jredmine.dto.response.board;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 论坛板块列表项响应 DTO（含统计）
 *
 * @author panfeng
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "论坛板块列表项（含统计）")
public class BoardListItemResponseDTO {

    @Schema(description = "板块 ID")
    private Integer id;

    @Schema(description = "项目 ID")
    private Integer projectId;

    @Schema(description = "板块名称")
    private String name;

    @Schema(description = "板块描述")
    private String description;

    @Schema(description = "排序位置")
    private Integer position;

    @Schema(description = "主题数")
    private Integer topicsCount;

    @Schema(description = "消息总数")
    private Integer messagesCount;

    @Schema(description = "最后一条消息 ID")
    private Integer lastMessageId;

    @Schema(description = "最后一条消息主题（若有）")
    private String lastMessageSubject;

    @Schema(description = "最后一条消息更新时间（若有）")
    private Date lastMessageUpdatedOn;

    @Schema(description = "父板块 ID")
    private Integer parentId;
}
