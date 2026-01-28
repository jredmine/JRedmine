package com.github.jredmine.dto.request.project;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 版本发布请求DTO
 *
 * @author panfeng
 */
@Data
@Schema(description = "版本发布请求")
public class VersionReleaseRequestDTO {

    @Schema(description = "发布备注信息（可选）", example = "版本已测试通过，正式发布")
    private String notes;
}