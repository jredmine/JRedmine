package com.github.jredmine.dto.response.search;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 搜索结果项DTO
 *
 * @author panfeng
 */
@Data
@Builder
@Schema(description = "搜索结果项")
public class SearchResultItemDTO {

    @Schema(description = "结果类型（issue、project、wiki等）")
    private String type;

    @Schema(description = "记录ID")
    private Long id;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "描述/内容摘要")
    private String description;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "项目名称")
    private String projectName;

    @Schema(description = "作者ID")
    private Long authorId;

    @Schema(description = "作者名称")
    private String authorName;

    @Schema(description = "创建时间")
    private LocalDateTime createdOn;

    @Schema(description = "更新时间")
    private LocalDateTime updatedOn;

    @Schema(description = "访问URL")
    private String url;

    @Schema(description = "是否私有")
    private Boolean isPrivate;

    @Schema(description = "状态（任务状态、项目状态等）")
    private String status;

    @Schema(description = "状态名称")
    private String statusName;

    @Schema(description = "优先级（仅任务）")
    private Integer priority;

    @Schema(description = "优先级名称（仅任务）")
    private String priorityName;
}