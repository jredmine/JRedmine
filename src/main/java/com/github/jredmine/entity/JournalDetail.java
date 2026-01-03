package com.github.jredmine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 活动详情实体类
 * 对应数据库中的 `journal_details` 表
 * 用于记录任务字段的变更详情（旧值和新值）
 *
 * @author panfeng
 */
@Data
@TableName("journal_details")
public class JournalDetail {
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 活动日志ID（关联 journals 表）
     */
    private Integer journalId;

    /**
     * 属性类型（如 'attr' 表示属性变更）
     */
    private String property;

    /**
     * 属性键（字段名，如 'status_id', 'assigned_to_id' 等）
     */
    private String propKey;

    /**
     * 旧值
     */
    private String oldValue;

    /**
     * 新值
     */
    private String value;
}
