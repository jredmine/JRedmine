package com.github.jredmine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * Wiki内容实体类
 *
 * @author panfeng
 */
@Data
@TableName("wiki_contents")
public class WikiContent {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("page_id")
    private Long pageId;

    @TableField("author_id")
    private Long authorId;

    @TableField("text")
    private String text;

    @TableField("comments")
    private String comments;

    @TableField("updated_on")
    private Date updatedOn;

    @TableField("version")
    private Integer version;
}