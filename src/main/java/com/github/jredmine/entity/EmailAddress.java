package com.github.jredmine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 用户邮箱地址实体
 *
 * @author panfeng
 */
@Data
@TableName("email_addresses")
public class EmailAddress {
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 邮箱地址
     */
    private String address;

    /**
     * 是否默认邮箱
     */
    private Boolean isDefault = false;

    /**
     * 是否接收通知
     */
    private Boolean notify = true;

    /**
     * 创建时间
     */
    private Date createdOn;

    /**
     * 更新时间
     */
    private Date updatedOn;
}

