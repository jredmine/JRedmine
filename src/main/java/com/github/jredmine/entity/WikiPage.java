package com.github.jredmine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * Wiki页面实体类
 *
 * @author panfeng
 */
@Data
@TableName("wiki_pages")
public class WikiPage {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("wiki_id")
    private Long wikiId;

    @TableField("title")
    private String title;

    @TableField("created_on")
    private Date createdOn;

    @TableField("protected")
    private Boolean isProtected;

    @TableField("parent_id")
    private Long parentId;
}