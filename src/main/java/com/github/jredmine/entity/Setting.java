package com.github.jredmine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统设置实体类
 * 对应数据库中的 `settings` 表
 *
 * @author panfeng
 */
@Data
@TableName("settings")
public class Setting {
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 设置项名称（唯一键）
     */
    private String name;

    /**
     * 设置项值（支持长文本）
     */
    private String value;

    /**
     * 更新时间
     */
    private LocalDateTime updatedOn;
}
