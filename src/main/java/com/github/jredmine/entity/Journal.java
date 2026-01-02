package com.github.jredmine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 活动日志实体类
 * 对应数据库中的 `journals` 表
 * 用于记录任务的活动历史，包括评论、状态变更等
 *
 * @author panfeng
 */
@Data
@TableName("journals")
public class Journal {
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 被记录对象ID（如任务ID）
     */
    private Integer journalizedId;

    /**
     * 被记录类型（如 'Issue'）
     */
    private String journalizedType;

    /**
     * 操作用户ID
     */
    private Integer userId;

    /**
     * 备注/评论内容
     */
    private String notes;

    /**
     * 创建时间
     */
    private LocalDateTime createdOn;

    /**
     * 更新时间
     */
    private LocalDateTime updatedOn;

    /**
     * 更新者ID
     */
    private Integer updatedById;

    /**
     * 是否私有备注（只有项目成员可见）
     */
    private Boolean privateNotes;
}
