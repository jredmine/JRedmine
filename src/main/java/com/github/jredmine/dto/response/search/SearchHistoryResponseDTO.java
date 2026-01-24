package com.github.jredmine.dto.response.search;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 搜索历史响应DTO
 *
 * @author panfeng
 */
@Data
@Builder
@Schema(description = "搜索历史记录")
public class SearchHistoryResponseDTO {

    @Schema(description = "历史记录ID")
    private Long id;

    @Schema(description = "搜索关键词")
    private String keyword;

    @Schema(description = "搜索类型")
    private String searchTypes;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "项目名称")
    private String projectName;

    @Schema(description = "搜索结果数量")
    private Integer resultCount;

    @Schema(description = "最后搜索时间")
    private LocalDateTime updatedOn;
}