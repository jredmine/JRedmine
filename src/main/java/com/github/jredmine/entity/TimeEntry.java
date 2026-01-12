package com.github.jredmine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 工时记录实体
 *
 * @author panfeng
 * @since 2026-01-12
 */
@Data
@TableName("time_entries")
public class TimeEntry {
    
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 项目ID
     */
    @TableField("project_id")
    private Long projectId;
    
    /**
     * 记录创建者ID
     */
    @TableField("author_id")
    private Long authorId;
    
    /**
     * 实际工作人员ID
     */
    @TableField("user_id")
    private Long userId;
    
    /**
     * 关联任务ID（可选）
     */
    @TableField("issue_id")
    private Long issueId;
    
    /**
     * 工时（小时）
     */
    @TableField("hours")
    private Float hours;
    
    /**
     * 备注说明
     */
    @TableField("comments")
    private String comments;
    
    /**
     * 活动类型ID（引用 enumerations 表）
     */
    @TableField("activity_id")
    private Long activityId;
    
    /**
     * 工作日期
     */
    @TableField("spent_on")
    private LocalDate spentOn;
    
    /**
     * 年份（冗余字段）
     */
    @TableField("tyear")
    private Integer tyear;
    
    /**
     * 月份（冗余字段）
     */
    @TableField("tmonth")
    private Integer tmonth;
    
    /**
     * 周数（冗余字段）
     */
    @TableField("tweek")
    private Integer tweek;
    
    /**
     * 创建时间
     */
    @TableField("created_on")
    private LocalDateTime createdOn;
    
    /**
     * 更新时间
     */
    @TableField("updated_on")
    private LocalDateTime updatedOn;
}
