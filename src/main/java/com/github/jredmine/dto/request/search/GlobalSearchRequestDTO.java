package com.github.jredmine.dto.request.search;

import com.github.jredmine.dto.request.PageRequestDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 全局搜索请求DTO
 *
 * @author panfeng
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "全局搜索请求")
public class GlobalSearchRequestDTO extends PageRequestDTO {

    @Schema(description = "搜索关键词", required = true)
    private String keyword;

    @Schema(description = "搜索类型列表（如：issue、project、wiki），为空则搜索所有类型")
    private List<String> types;

    @Schema(description = "项目ID，指定项目范围搜索（为空则全局搜索）")
    private Long projectId;

    @Schema(description = "是否包含私有内容（默认false）")
    private Boolean includePrivate = false;
}