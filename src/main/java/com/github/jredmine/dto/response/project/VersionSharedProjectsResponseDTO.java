package com.github.jredmine.dto.response.project;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 版本共享项目列表响应DTO
 *
 * @author panfeng
 */
@Data
@Builder
@Schema(description = "版本共享项目列表")
public class VersionSharedProjectsResponseDTO {

    @Schema(description = "版本ID")
    private Integer versionId;

    @Schema(description = "版本名称")
    private String versionName;

    @Schema(description = "版本所属项目ID")
    private Long ownerProjectId;

    @Schema(description = "版本所属项目名称")
    private String ownerProjectName;

    @Schema(description = "共享方式")
    private String sharing;

    @Schema(description = "共享方式描述")
    private String sharingDescription;

    @Schema(description = "共享项目列表")
    private List<SharedProjectInfo> sharedProjects;

    /**
     * 共享项目信息
     */
    @Data
    @Builder
    @Schema(description = "共享项目信息")
    public static class SharedProjectInfo {
        @Schema(description = "项目ID")
        private Long projectId;

        @Schema(description = "项目名称")
        private String projectName;

        @Schema(description = "项目标识符")
        private String identifier;

        @Schema(description = "父项目ID")
        private Long parentId;

        @Schema(description = "项目状态")
        private Integer status;

        @Schema(description = "是否公开")
        private Boolean isPublic;

        @Schema(description = "关系类型（owner/descendant/parent/tree/system）")
        private String relationType;

        @Schema(description = "关系描述")
        private String relationDescription;
    }
}