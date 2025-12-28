package com.github.jredmine.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 项目跟踪器关联实体类
 * 对应数据库中的 `projects_trackers` 表
 *
 * @author panfeng
 */
@Data
@TableName("projects_trackers")
public class ProjectTracker {
    /**
     * 项目ID
     */
    private Long projectId;

    /**
     * 跟踪器ID
     */
    private Long trackerId;
}

