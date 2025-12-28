package com.github.jredmine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 项目成员实体类
 * 对应数据库中的 `members` 表
 *
 * @author panfeng
 */
@Data
@TableName("members")
public class Member {
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 项目ID
     */
    private Long projectId;

    /**
     * 加入时间
     */
    private Date createdOn;

    /**
     * 邮件通知设置
     */
    private Boolean mailNotification = false;
}

