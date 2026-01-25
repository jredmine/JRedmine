package com.github.jredmine.dto.response.project;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 批量取消任务与版本关联响应DTO
 *
 * @author panfeng
 */
@Data
@Builder
@Schema(description = "批量取消任务与版本关联响应")
public class VersionIssuesBatchUnassignResponseDTO {

    @Schema(description = "成功取消关联的任务数量")
    private Integer successCount;

    @Schema(description = "失败的任务数量")
    private Integer failCount;

    @Schema(description = "成功取消关联的任务ID列表")
    private List<Long> successIssueIds;

    @Schema(description = "失败的任务ID列表")
    private List<Long> failIssueIds;

    @Schema(description = "错误信息列表（任务ID -> 错误信息）")
    private Map<Long, String> errors;
}