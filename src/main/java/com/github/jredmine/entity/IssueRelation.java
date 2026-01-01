package com.github.jredmine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 任务关联实体类
 * 对应数据库中的 `issue_relations` 表
 *
 * @author panfeng
 */
@Data
@TableName("issue_relations")
public class IssueRelation {
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 源任务ID
     */
    private Integer issueFromId;

    /**
     * 目标任务ID
     */
    private Integer issueToId;

    /**
     * 关联类型
     * - relates: 相关
     * - duplicates: 重复
     * - duplicated: 被重复
     * - blocks: 阻塞
     * - blocked: 被阻塞
     * - precedes: 前置（用于甘特图）
     * - follows: 后置（用于甘特图）
     */
    private String relationType;

    /**
     * 延迟天数（仅用于 precedes/follows 类型）
     */
    private Integer delay;
}
