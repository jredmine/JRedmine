package com.github.jredmine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * Wiki 内容版本快照实体类（对应 wiki_content_versions 表）
 * 每次向 wiki_contents 插入新版本时同步写入，用于持久化版本历史快照。
 *
 * @author panfeng
 */
@Data
@TableName("wiki_content_versions")
public class WikiContentVersion {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("wiki_content_id")
    private Long wikiContentId;

    @TableField("page_id")
    private Long pageId;

    @TableField("author_id")
    private Long authorId;

    /**
     * 正文快照（与 wiki_contents.text 一致，存为 blob）
     */
    @TableField("data")
    private byte[] data;

    @TableField("compression")
    private String compression;

    @TableField("comments")
    private String comments;

    @TableField("updated_on")
    private Date updatedOn;

    @TableField("version")
    private Integer version;
}
