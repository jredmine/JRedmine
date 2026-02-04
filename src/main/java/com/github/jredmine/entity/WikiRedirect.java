package com.github.jredmine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * Wiki 重定向实体类（对应 wiki_redirects 表）
 *
 * @author panfeng
 */
@Data
@TableName("wiki_redirects")
public class WikiRedirect {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("wiki_id")
    private Long wikiId;

    @TableField("title")
    private String title;

    @TableField("redirects_to")
    private String redirectsTo;

    @TableField("created_on")
    private Date createdOn;

    @TableField("redirects_to_wiki_id")
    private Long redirectsToWikiId;
}
