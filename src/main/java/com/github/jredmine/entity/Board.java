package com.github.jredmine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 论坛板块实体类
 * 对应表 boards。
 *
 * @author panfeng
 */
@Data
@TableName("boards")
public class Board {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("project_id")
    private Integer projectId;

    @TableField("name")
    private String name;

    @TableField("description")
    private String description;

    @TableField("position")
    private Integer position;

    @TableField("topics_count")
    private Integer topicsCount;

    @TableField("messages_count")
    private Integer messagesCount;

    @TableField("last_message_id")
    private Integer lastMessageId;

    @TableField("parent_id")
    private Integer parentId;
}
