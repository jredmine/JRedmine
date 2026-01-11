package com.github.jredmine.dto.response.issue;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 任务甘特图依赖关系DTO
 *
 * @author panfeng
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "任务甘特图依赖关系")
public class IssueGanttDependencyDTO {
    
    @Schema(description = "依赖的任务ID（被依赖任务ID）")
    private Long issueId;
    
    @Schema(description = "依赖类型（precedes: 前置, follows: 后置, blocks: 阻塞, blocked: 被阻塞）")
    private String relationType;
    
    @Schema(description = "延迟天数（仅用于precedes/follows类型）")
    private Integer delay;
}
