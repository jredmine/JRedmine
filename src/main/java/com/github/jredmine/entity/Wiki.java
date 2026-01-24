package com.github.jredmine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * Wiki实体类
 *
 * @author panfeng
 */
@Data
@TableName("wikis")
public class Wiki {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("project_id")
    private Long projectId;

    @TableField("start_page")
    private String startPage;

    @TableField("status")
    private Integer status;
}