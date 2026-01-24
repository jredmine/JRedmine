package com.github.jredmine.dto.request.activity;

import com.github.jredmine.dto.request.PageRequestDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 项目活动流查询请求DTO
 *
 * @author panfeng
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "项目活动流查询请求")
public class ProjectActivityRequestDTO extends PageRequestDTO {

    @Schema(description = "开始时间")
    private LocalDateTime startDate;

    @Schema(description = "结束时间")
    private LocalDateTime endDate;

    @Schema(description = "活动类型列表")
    private List<String> activityTypes;

    @Schema(description = "用户ID筛选")
    private Long userId;

    @Schema(description = "对象类型筛选")
    private String objectType;

    @Schema(description = "关键词搜索")
    private String keyword;
}