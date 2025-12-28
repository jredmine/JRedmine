package com.github.jredmine.dto.response.tracker;

import lombok.Data;

/**
 * 跟踪器列表项响应DTO
 *
 * @author panfeng
 */
@Data
public class TrackerListItemResponseDTO {
    /**
     * 跟踪器ID
     */
    private Long id;

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
}

