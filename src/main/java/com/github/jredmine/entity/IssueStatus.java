package com.github.jredmine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 任务状态实体类
 * 对应数据库中的 `issue_statuses` 表
 *
 * @author panfeng
 */
@Data
@TableName("issue_statuses")
public class IssueStatus {
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 状态名称
     */
    private String name;

    /**
     * 状态描述
     */
    private String description;

    /**
     * 是否已关闭状态
     */
    private Boolean isClosed = false;

    /**
     * 排序位置
     */
    private Integer position;

    /**
     * 默认完成度（0-100）
     */
    private Integer defaultDoneRatio;
}

