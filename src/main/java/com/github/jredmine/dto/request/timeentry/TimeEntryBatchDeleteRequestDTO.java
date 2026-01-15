package com.github.jredmine.dto.request.timeentry;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 工时记录批量删除请求DTO
 *
 * @author panfeng
 * @since 2026-01-14
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "工时记录批量删除请求")
public class TimeEntryBatchDeleteRequestDTO {
    
    @Schema(description = "要删除的工时记录ID列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "删除的工时记录ID列表不能为空")
    private List<Long> ids;
}
