package com.github.jredmine.dto.response.timeentry;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 工时活动类型响应 DTO
 */
@Data
@Schema(description = "工时活动类型")
public class TimeEntryActivityResponseDTO {

    @Schema(description = "活动类型 ID")
    private Long id;

    @Schema(description = "活动类型名称")
    private String name;

    @Schema(description = "是否默认")
    private Boolean isDefault;
}
