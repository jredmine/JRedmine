package com.github.jredmine.dto.request.activity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 更新评论请求DTO
 *
 * @author panfeng
 */
@Data
@Schema(description = "更新评论请求")
public class CommentUpdateRequestDTO {

    @Schema(description = "评论内容", required = true)
    @NotBlank(message = "评论内容不能为空")
    private String notes;

    @Schema(description = "是否为私有备注")
    private Boolean isPrivate;
}