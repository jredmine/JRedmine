package com.github.jredmine.dto.request.project;

import com.github.jredmine.enums.VersionSharing;
import com.github.jredmine.validator.EnumValue;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 更新版本共享方式请求DTO
 *
 * @author panfeng
 */
@Data
@Schema(description = "更新版本共享方式请求")
public class VersionSharingUpdateRequestDTO {

    @Schema(description = "版本共享方式（none/descendants/hierarchy/tree/system）", required = true, example = "hierarchy")
    @NotBlank(message = "共享方式不能为空")
    @EnumValue(enumClass = VersionSharing.class, message = "共享方式值无效，必须是 none、descendants、hierarchy、tree 或 system")
    private String sharing;

    @Schema(description = "备注信息（可选）", example = "将版本共享给项目层次结构")
    private String notes;
}