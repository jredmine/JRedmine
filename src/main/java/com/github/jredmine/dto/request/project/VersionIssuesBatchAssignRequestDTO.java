package com.github.jredmine.dto.request.project;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 批量关联任务到版本请求DTO
 *
 * @author panfeng
 */
@Data
@Schema(description = "批量关联任务到版本请求")
public class VersionIssuesBatchAssignRequestDTO {

    @Schema(description = "任务ID列表", required = true)
    @NotEmpty(message = "任务ID列表不能为空")
    private List<Long> issueIds;
}