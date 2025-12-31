package com.github.jredmine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 任务实体类
 * 对应数据库中的 `issues` 表
 *
 * @author panfeng
 */
@Data
@TableName("issues")
public class Issue {
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 跟踪器ID（Bug、Feature等）
     */
    private Integer trackerId;

    /**
     * 项目ID
     */
    private Long projectId;

    /**
     * 任务标题
     */
    private String subject;

    /**
     * 任务描述
     */
    private String description;

    /**
     * 截止日期
     */
    private LocalDate dueDate;

    /**
     * 任务分类ID
     */
    private Integer categoryId;

    /**
     * 任务状态ID
     */
    private Integer statusId;

    /**
     * 分配给用户ID
     */
    private Long assignedToId;

    /**
     * 优先级ID
     */
    private Integer priorityId;

    /**
     * 修复版本ID
     */
    private Long fixedVersionId;

    /**
     * 创建者ID
     */
    private Long authorId;

    /**
     * 乐观锁版本号
     */
    private Integer lockVersion = 0;

    /**
     * 创建时间
     */
    private LocalDateTime createdOn;

    /**
     * 更新时间
     */
    private LocalDateTime updatedOn;

    /**
     * 开始日期
     */
    private LocalDate startDate;

    /**
     * 完成度（0-100）
     */
    private Integer doneRatio = 0;

    /**
     * 预估工时
     */
    private Float estimatedHours;

    /**
     * 父任务ID
     */
    private Long parentId;

    /**
     * 根任务ID
     */
    private Long rootId;

    /**
     * 左值（树形结构）
     */
    private Integer lft;

    /**
     * 右值（树形结构）
     */
    private Integer rgt;

    /**
     * 是否私有
     */
    private Boolean isPrivate = false;

    /**
     * 关闭时间
     */
    private LocalDateTime closedOn;
}
