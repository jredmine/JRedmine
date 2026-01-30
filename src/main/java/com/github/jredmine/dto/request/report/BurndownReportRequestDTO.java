package com.github.jredmine.dto.request.report;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 燃尽图报表请求 DTO
 *
 * @author panfeng
 */
@Data
@Schema(description = "燃尽图报表请求")
public class BurndownReportRequestDTO {

    @Schema(description = "项目ID", required = true)
    private Long projectId;

    @Schema(description = "版本ID（可选，不传则按项目下所有任务燃尽）")
    private Integer versionId;
}
