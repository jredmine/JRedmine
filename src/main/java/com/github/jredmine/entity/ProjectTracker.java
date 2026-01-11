package com.github.jredmine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 项目跟踪器关联实体类
 * 对应数据库中的 `projects_trackers` 表
 * 主键：id（自增）
 * 唯一键：project_id + tracker_id
 *
 * @author panfeng
 */
@Data
@TableName(value = "projects_trackers", autoResultMap = true)
public class ProjectTracker {
    /**
     * 主键ID（自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 项目ID
     */
    private Long projectId;

    /**
     * 跟踪器ID
     */
    private Long trackerId;
}

