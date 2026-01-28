package com.github.jredmine.dto.response.project;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 版本共享方式更新响应DTO
 *
 * @author panfeng
 */
@Data
@Builder
@Schema(description = "版本共享方式更新响应")
public class VersionSharingUpdateResponseDTO {

    @Schema(description = "版本ID")
    private Integer versionId;

    @Schema(description = "版本名称")
    private String versionName;

    @Schema(description = "旧共享方式")
    private String oldSharing;

    @Schema(description = "新共享方式")
    private String newSharing;

    @Schema(description = "旧共享方式描述")
    private String oldSharingDescription;

    @Schema(description = "新共享方式描述")
    private String newSharingDescription;

    @Schema(description = "共享方式变更时间")
    private LocalDateTime updatedOn;

    @Schema(description = "操作人ID")
    private Long operatorId;

    @Schema(description = "操作人姓名")
    private String operatorName;

    @Schema(description = "备注信息")
    private String notes;

    @Schema(description = "共享方式变更记录ID（Journal ID）")
    private Integer journalId;
}