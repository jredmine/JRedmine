package com.github.jredmine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 用户偏好设置实体
 *
 * @author panfeng
 */
@Data
@TableName("user_preferences")
public class UserPreference {
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 其他偏好设置（JSON格式）
     */
    private String others;

    /**
     * 是否隐藏邮箱
     */
    private Boolean hideMail = true;

    /**
     * 时区
     */
    private String timeZone;
}

