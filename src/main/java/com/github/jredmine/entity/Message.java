package com.github.jredmine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 论坛消息实体类（主题帖或回复）
 * 对应表 messages。parent_id 为空表示主题帖，非空表示回复。
 *
 * @author panfeng
 */
@Data
@TableName("messages")
public class Message {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("board_id")
    private Integer boardId;

    @TableField("parent_id")
    private Integer parentId;

    @TableField("subject")
    private String subject;

    @TableField("content")
    private String content;

    @TableField("author_id")
    private Integer authorId;

    @TableField("replies_count")
    private Integer repliesCount;

    @TableField("last_reply_id")
    private Integer lastReplyId;

    @TableField("created_on")
    private Date createdOn;

    @TableField("updated_on")
    private Date updatedOn;

    @TableField("locked")
    private Boolean locked;

    @TableField("sticky")
    private Integer sticky;
}
