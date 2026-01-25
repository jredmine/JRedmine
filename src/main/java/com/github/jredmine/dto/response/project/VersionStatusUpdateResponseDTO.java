package com.github.jredmine.dto.response.project;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 版本状态更新响应DTO
 *
 * @author panfeng
 */
@Data
@Builder
@Schema(description = "版本状态更新响应")
public class VersionStatusUpdateResponseDTO {

    @Schema(description = "版本ID")
    private Integer versionId;

    @Schema(description = "版本名称")
    private String versionName;

    @Schema(description = "旧状态")
    private String oldStatus;

    @Schema(description = "新状态")
    private String newStatus;

    @Schema(description = "状态变更时间")
    private LocalDateTime updatedOn;

    @Schema(description = "操作人ID")
    private Long operatorId;

    @Schema(description = "操作人姓名")
    private String operatorName;

    @Schema(description = "备注信息")
    private String notes;

    @Schema(description = "状态变更记录ID（Journal ID）")
    private Integer journalId;
}