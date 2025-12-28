package com.github.jredmine.dto.request.tracker;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建跟踪器请求DTO
 *
 * @author panfeng
 */
@Data
public class TrackerCreateRequestDTO {
    /**
     * 跟踪器名称
     */
    @NotBlank(message = "跟踪器名称不能为空")
    private String name;

    /**
     * 跟踪器描述
     */
    private String description;

    /**
     * 排序位置
     */
    private Integer position;

    /**
     * 是否在路线图中显示
     */
    private Boolean isInRoadmap = true;

    /**
     * 字段位（用于控制字段显示）
     */
    private Integer fieldsBits = 0;

    /**
     * 默认状态ID
     */
    private Long defaultStatusId;
}
