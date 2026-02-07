package com.github.jredmine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 文档实体类
 * 对应表 documents，存储文档元数据（标题、描述、分类）；实际文件通过附件关联（container_type=Document, container_id=id）。
 *
 * @author panfeng
 */
@Data
@TableName("documents")
public class Document {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("project_id")
    private Integer projectId;

    @TableField("category_id")
    private Integer categoryId;

    @TableField("title")
    private String title;

    @TableField("description")
    private String description;

    @TableField("created_on")
    private Date createdOn;
}
