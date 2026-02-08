package com.github.jredmine.dto.request.board;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发主题请求 DTO
 *
 * @author panfeng
 */
@Data
@Schema(description = "发主题请求")
public class TopicCreateRequestDTO {

    @NotBlank(message = "主题标题不能为空")
    @Size(max = 255)
    @Schema(description = "主题标题", requiredMode = Schema.RequiredMode.REQUIRED)
    private String subject;

    @Schema(description = "正文（可选）")
    private String content;
}
