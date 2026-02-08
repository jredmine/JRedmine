package com.github.jredmine.dto.response.board;

import com.github.jredmine.dto.response.PageResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 论坛主题详情响应 DTO（含主题信息与分页回复列表）
 *
 * @author panfeng
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "论坛主题详情（含分页回复列表）")
public class MessageTopicDetailResponseDTO {

    @Schema(description = "主题信息")
    private MessageDetailResponseDTO topic;

    @Schema(description = "回复分页列表（按创建时间正序）")
    private PageResponse<MessageReplyListItemResponseDTO> replies;
}
