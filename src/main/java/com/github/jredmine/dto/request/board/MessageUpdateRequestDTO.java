package com.github.jredmine.dto.request.board;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 论坛消息更新请求 DTO
 * 各字段均可选；subject、locked、sticky 仅对主题帖有效，回复一般只更新 content。
 *
 * @author panfeng
 */
@Data
@Schema(description = "论坛消息更新请求")
public class MessageUpdateRequestDTO {

    @Size(max = 255)
    @Schema(description = "主题标题（有值则更新，仅主题帖有效）")
    private String subject;

    @Schema(description = "正文（有值则更新）")
    private String content;

    @Schema(description = "是否锁定（有值则更新，仅主题帖有效）")
    private Boolean locked;

    @Schema(description = "是否置顶（有值则更新，仅主题帖有效）")
    private Integer sticky;
}
