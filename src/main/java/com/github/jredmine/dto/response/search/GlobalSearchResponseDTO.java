package com.github.jredmine.dto.response.search;

import com.github.jredmine.dto.response.PageResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 全局搜索响应DTO
 *
 * @author panfeng
 */
@Data
@Builder
@Schema(description = "全局搜索响应")
public class GlobalSearchResponseDTO {

    @Schema(description = "搜索关键词")
    private String keyword;

    @Schema(description = "总结果数量")
    private Long totalCount;

    @Schema(description = "各类型结果数量统计")
    private Map<String, Long> typeCounts;

    @Schema(description = "分页的搜索结果")
    private PageResponse<SearchResultItemDTO> results;

    @Schema(description = "按类型分组的结果（用于展示分类标签）")
    private Map<String, List<SearchResultItemDTO>> groupedResults;
}