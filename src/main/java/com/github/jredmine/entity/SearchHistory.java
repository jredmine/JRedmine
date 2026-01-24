package com.github.jredmine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 搜索历史记录实体类
 *
 * @author panfeng
 */
@Data
@TableName("search_histories")
public class SearchHistory {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("keyword")
    private String keyword;

    @TableField("search_types")
    private String searchTypes;

    @TableField("project_id")
    private Long projectId;

    @TableField("result_count")
    private Integer resultCount;

    @TableField("created_on")
    private LocalDateTime createdOn;

    @TableField("updated_on")
    private LocalDateTime updatedOn;
}