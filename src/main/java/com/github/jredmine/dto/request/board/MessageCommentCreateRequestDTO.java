package com.github.jredmine.dto.request.board;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 消息评论创建请求 DTO
 *
 * @author panfeng
 */
@Data
@Schema(description = "消息评论创建请求")
public class MessageCommentCreateRequestDTO {

    @NotBlank(message = "评论内容不能为空")
    @Schema(description = "评论内容", required = true)
    private String notes;

    @Schema(description = "是否为私有备注（仅项目成员可见，默认 false）")
    private Boolean isPrivate = false;
}
