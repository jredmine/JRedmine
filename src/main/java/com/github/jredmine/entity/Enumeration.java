package com.github.jredmine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 枚举实体类
 * 对应数据库中的 `enumerations` 表
 * 用于存储优先级、任务状态等枚举值
 *
 * @author panfeng
 */
@Data
@TableName("enumerations")
public class Enumeration {
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 枚举名称
     */
    private String name;

    /**
     * 位置（排序用）
     */
    private Integer position;

    /**
     * 是否默认值
     */
    private Boolean isDefault = false;

    /**
     * 类型
     * - 'IssuePriority': 任务优先级
     * - 'IssueStatus': 任务状态（已废弃，使用 issue_statuses 表）
     * - 'TimeEntryActivity': 工时活动
     * - 'DocumentCategory': 文档分类
     */
    private String type;

    /**
     * 是否激活
     */
    private Boolean active = true;

    /**
     * 项目ID（如果为项目特定枚举，否则为null）
     */
    private Integer projectId;

    /**
     * 父枚举ID
     */
    private Integer parentId;

    /**
     * 位置名称
     */
    private String positionName;
}
