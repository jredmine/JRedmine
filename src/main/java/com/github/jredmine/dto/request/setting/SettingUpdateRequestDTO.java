package com.github.jredmine.dto.request.setting;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 更新系统设置请求DTO
 *
 * @author panfeng
 */
@Data
@Schema(description = "更新系统设置请求")
public class SettingUpdateRequestDTO {
    
    @NotBlank(message = "设置项名称不能为空")
    @Schema(description = "设置项名称", example = "app_title")
    private String name;
    
    @Schema(description = "设置项值", example = "我的项目管理系统")
    private String value;
}
