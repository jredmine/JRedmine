package com.github.jredmine.dto.response.setting;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 系统设置响应DTO
 *
 * @author panfeng
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "系统设置响应")
public class SettingResponseDTO {
    
    @Schema(description = "设置项ID")
    private Integer id;
    
    @Schema(description = "设置项名称")
    private String name;
    
    @Schema(description = "设置项值")
    private String value;
    
    @Schema(description = "设置项描述")
    private String description;
    
    @Schema(description = "设置分类")
    private String category;
    
    @Schema(description = "默认值")
    private String defaultValue;
    
    @Schema(description = "更新时间")
    private LocalDateTime updatedOn;
}
