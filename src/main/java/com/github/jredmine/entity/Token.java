package com.github.jredmine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * Token实体
 * 用于存储各种类型的Token，如密码重置Token、API Token等
 *
 * @author panfeng
 */
@Data
@TableName("tokens")
public class Token {
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 操作类型（如：password_reset, api等）
     */
    private String action;

    /**
     * Token值
     */
    private String value;

    /**
     * 创建时间
     */
    private Date createdOn;

    /**
     * 更新时间
     */
    private Date updatedOn;
}

