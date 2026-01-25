package com.github.jredmine.dto.request.activity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 添加评论请求DTO
 *
 * @author panfeng
 */
@Data
@Schema(description = "添加评论请求")
public class CommentCreateRequestDTO {

    @Schema(description = "关联对象类型（Issue、Project等）", required = true)
    @NotBlank(message = "对象类型不能为空")
    private String objectType;

    @Schema(description = "关联对象ID", required = true)
    @NotNull(message = "对象ID不能为空")
    private Long objectId;

    @Schema(description = "评论内容", required = true)
    @NotBlank(message = "评论内容不能为空")
    private String notes;

    @Schema(description = "是否为私有备注（默认false）")
    private Boolean isPrivate = false;
}