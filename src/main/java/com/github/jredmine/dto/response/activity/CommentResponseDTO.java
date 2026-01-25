package com.github.jredmine.dto.response.activity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评论响应DTO
 *
 * @author panfeng
 */
@Data
@Builder
@Schema(description = "评论信息")
public class CommentResponseDTO {

    @Schema(description = "评论ID")
    private Long id;

    @Schema(description = "关联对象类型")
    private String objectType;

    @Schema(description = "关联对象ID")
    private Long objectId;

    @Schema(description = "评论内容")
    private String notes;

    @Schema(description = "是否为私有备注")
    private Boolean isPrivate;

    @Schema(description = "评论用户ID")
    private Long userId;

    @Schema(description = "评论用户名")
    private String userName;

    @Schema(description = "评论用户登录名")
    private String userLogin;

    @Schema(description = "创建时间")
    private LocalDateTime createdOn;

    @Schema(description = "更新时间")
    private LocalDateTime updatedOn;

    @Schema(description = "更新者ID")
    private Long updatedById;

    @Schema(description = "更新者名称")
    private String updatedByName;
}