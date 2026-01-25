package com.github.jredmine.dto.request.project;

import com.github.jredmine.enums.VersionStatus;
import com.github.jredmine.validator.EnumValue;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 更新版本状态请求DTO
 *
 * @author panfeng
 */
@Data
@Schema(description = "更新版本状态请求")
public class VersionStatusUpdateRequestDTO {

    @Schema(description = "版本状态（open/locked/closed）", required = true, example = "closed")
    @NotBlank(message = "状态不能为空")
    @EnumValue(enumClass = VersionStatus.class, message = "状态值无效，必须是 open、locked 或 closed")
    private String status;

    @Schema(description = "备注信息（可选）", example = "版本已完成，关闭版本")
    private String notes;
}