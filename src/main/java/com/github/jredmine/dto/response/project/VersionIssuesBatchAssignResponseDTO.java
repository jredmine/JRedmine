package com.github.jredmine.dto.response.project;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 批量关联任务到版本响应DTO
 *
 * @author panfeng
 */
@Data
@Builder
@Schema(description = "批量关联任务到版本响应")
public class VersionIssuesBatchAssignResponseDTO {

    @Schema(description = "成功关联的任务数量")
    private Integer successCount;

    @Schema(description = "失败的任务数量")
    private Integer failCount;

    @Schema(description = "成功关联的任务ID列表")
    private List<Long> successIssueIds;

    @Schema(description = "失败的任务ID列表")
    private List<Long> failIssueIds;

    @Schema(description = "错误信息列表（任务ID -> 错误信息）")
    private java.util.Map<Long, String> errors;
}