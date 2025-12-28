package com.github.jredmine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 跟踪器实体类
 * 对应数据库中的 `trackers` 表
 * 
 * 跟踪器用于分类任务（如：Bug、Feature、Support等）
 * 跟踪器可以手动新增、修改、删除，系统初始化时会创建默认跟踪器
 *
 * @author panfeng
 */
@Data
@TableName("trackers")
public class Tracker {
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 跟踪器名称（如：Bug、Feature、Support）
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

