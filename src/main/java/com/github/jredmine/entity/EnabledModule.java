package com.github.jredmine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 项目启用的模块实体类
 * 对应数据库中的 `enabled_modules` 表
 *
 * @author panfeng
 */
@Data
@TableName("enabled_modules")
public class EnabledModule {
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 项目ID（NULL表示全局模块）
     */
    private Long projectId;

    /**
     * 模块名称（如：issues、wiki、boards等）
     */
    private String name;
}

