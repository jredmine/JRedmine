package com.github.jredmine.dto.response.project;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 项目简单信息响应DTO
 *
 * @author panfeng
 * @since 2026-01-12
 */
@Data
@Schema(description = "项目简单信息")
public class ProjectSimpleResponseDTO {
    
    @Schema(description = "项目ID")
    private Long id;
    
    @Schema(description = "项目名称")
    private String name;
    
    @Schema(description = "项目标识符")
    private String identifier;
}
