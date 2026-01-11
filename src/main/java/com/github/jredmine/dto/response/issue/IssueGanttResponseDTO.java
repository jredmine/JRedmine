package com.github.jredmine.dto.response.issue;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * 任务甘特图响应DTO
 *
 * @author panfeng
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "任务甘特图响应")
public class IssueGanttResponseDTO {
    
    @Schema(description = "甘特图任务列表")
    private List<IssueGanttItemResponseDTO> tasks;
    
    @Schema(description = "甘特图开始日期（所有任务中最早的开始日期）")
    private LocalDate ganttStartDate;
    
    @Schema(description = "甘特图结束日期（所有任务中最晚的截止日期）")
    private LocalDate ganttEndDate;
    
    @Schema(description = "任务总数")
    private Integer totalCount;
}
