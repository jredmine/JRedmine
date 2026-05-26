package com.github.jredmine.dto.response.issue;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 任务优先级（枚举）响应 DTO
 */
@Data
@Schema(description = "任务优先级")
public class IssuePriorityResponseDTO {
    @Schema(description = "优先级 ID")
    private Integer id;

    @Schema(description = "优先级名称")
    private String name;

    @Schema(description = "排序位置")
    private Integer position;
}
