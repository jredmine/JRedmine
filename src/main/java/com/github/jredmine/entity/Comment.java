package com.github.jredmine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评论实体类
 * 对应表 comments，多态关联（commented_type + commented_id），如对消息的评论。
 *
 * @author panfeng
 */
@Data
@TableName("comments")
public class Comment {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("commented_type")
    private String commentedType;

    @TableField("commented_id")
    private Integer commentedId;

    @TableField("author_id")
    private Integer authorId;

    @TableField("content")
    private String content;

    @TableField("created_on")
    private LocalDateTime createdOn;

    @TableField("updated_on")
    private LocalDateTime updatedOn;
}
