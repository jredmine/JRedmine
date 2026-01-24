package com.github.jredmine.dto.response.activity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 字段变更DTO
 *
 * @author panfeng
 */
@Data
@Builder
@Schema(description = "字段变更信息")
public class FieldChangeDTO {

    @Schema(description = "属性类型")
    private String property;

    @Schema(description = "字段名")
    private String fieldName;

    @Schema(description = "字段显示名")
    private String fieldLabel;

    @Schema(description = "旧值")
    private String oldValue;

    @Schema(description = "新值")
    private String newValue;

    @Schema(description = "旧值显示名")
    private String oldValueDisplay;

    @Schema(description = "新值显示名")
    private String newValueDisplay;
}