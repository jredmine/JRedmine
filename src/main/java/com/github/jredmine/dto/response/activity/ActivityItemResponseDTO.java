package com.github.jredmine.dto.response.activity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 活动项响应DTO
 *
 * @author panfeng
 */
@Data
@Builder
@Schema(description = "活动项信息")
public class ActivityItemResponseDTO {

    @Schema(description = "活动ID")
    private Long id;

    @Schema(description = "活动类型（comment, field_change, create, delete）")
    private String activityType;

    @Schema(description = "操作用户ID")
    private Long userId;

    @Schema(description = "操作用户名")
    private String userName;

    @Schema(description = "操作用户登录名")
    private String userLogin;

    @Schema(description = "关联对象类型（Issue, Project等）")
    private String objectType;

    @Schema(description = "关联对象ID")
    private Long objectId;

    @Schema(description = "关联对象标题")
    private String objectTitle;

    @Schema(description = "关联对象URL")
    private String objectUrl;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "项目名称")
    private String projectName;

    @Schema(description = "备注内容")
    private String notes;

    @Schema(description = "是否私有备注")
    private Boolean isPrivate;

    @Schema(description = "字段变更详情")
    private List<FieldChangeDTO> changes;

    @Schema(description = "活动时间")
    private LocalDateTime createdOn;

    @Schema(description = "更新时间")
    private LocalDateTime updatedOn;

    @Schema(description = "更新者ID")
    private Long updatedById;

    @Schema(description = "更新者名称")
    private String updatedByName;
}