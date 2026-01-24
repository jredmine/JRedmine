package com.github.jredmine.dto.request.activity;

import com.github.jredmine.dto.request.PageRequestDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 活动流查询请求DTO
 *
 * @author panfeng
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "活动流查询请求")
public class ActivityQueryRequestDTO extends PageRequestDTO {

    @Schema(description = "开始时间")
    private LocalDateTime startDate;

    @Schema(description = "结束时间")
    private LocalDateTime endDate;

    @Schema(description = "活动类型列表（comment, field_change, create, delete）")
    private List<String> activityTypes;

    @Schema(description = "用户ID筛选")
    private Long userId;

    @Schema(description = "项目ID筛选")
    private Long projectId;

    @Schema(description = "对象类型筛选（Issue, Project, Wiki等）")
    private String objectType;

    @Schema(description = "对象ID筛选")
    private Long objectId;

    @Schema(description = "是否包含私有备注（默认false）")
    private Boolean includePrivate = false;

    @Schema(description = "关键词搜索（在备注内容中搜索）")
    private String keyword;
}