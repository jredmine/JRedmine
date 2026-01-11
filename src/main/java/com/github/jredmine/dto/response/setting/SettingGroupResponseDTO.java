package com.github.jredmine.dto.response.setting;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 系统设置分组响应DTO
 *
 * @author panfeng
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "系统设置分组响应")
public class SettingGroupResponseDTO {
    
    @Schema(description = "分类代码")
    private String category;
    
    @Schema(description = "分类名称")
    private String categoryName;
    
    @Schema(description = "设置项列表")
    private List<SettingResponseDTO> settings;
}
