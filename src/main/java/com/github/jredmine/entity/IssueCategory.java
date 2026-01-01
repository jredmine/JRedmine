package com.github.jredmine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 任务分类实体类
 * 对应数据库中的 `issue_categories` 表
 *
 * @author panfeng
 */
@Data
@TableName("issue_categories")
public class IssueCategory {
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 项目ID
     */
    private Integer projectId;

    /**
     * 分类名称
     */
    private String name;

    /**
     * 默认分配给用户ID
     */
    private Integer assignedToId;
}
