package com.github.jredmine.dto.request.board;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 回复主题请求 DTO
 *
 * @author panfeng
 */
@Data
@Schema(description = "回复主题请求")
public class ReplyCreateRequestDTO {

    @NotBlank(message = "回复内容不能为空")
    @Schema(description = "回复正文", requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;
}
