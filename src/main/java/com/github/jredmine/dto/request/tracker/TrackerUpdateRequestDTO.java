package com.github.jredmine.dto.request.tracker;

import lombok.Data;

/**
 * 更新跟踪器请求DTO
 *
 * @author panfeng
 */
@Data
public class TrackerUpdateRequestDTO {
    /**
     * 跟踪器名称
     */
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
    private Boolean isInRoadmap;

    /**
     * 字段位（用于控制字段显示）
     */
    private Integer fieldsBits;

    /**
     * 默认状态ID
     */
    private Long defaultStatusId;
}
