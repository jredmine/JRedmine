package com.github.jredmine.dto.response.search;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 搜索建议响应DTO
 *
 * @author panfeng
 */
@Data
@Builder
@Schema(description = "搜索建议响应")
public class SearchSuggestionResponseDTO {

    @Schema(description = "搜索建议关键词列表")
    private List<String> suggestions;

    @Schema(description = "热门搜索关键词列表")
    private List<String> hotKeywords;

    @Schema(description = "搜索历史关键词列表")
    private List<String> historyKeywords;
}