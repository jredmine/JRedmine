package com.github.jredmine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("users")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String login;

    private String hashedPassword;

    private String firstname;

    private String lastname;

    private Boolean admin = false;

    private Integer status = 1;

    private Date lastLoginOn;

    private String language = "";

    private Integer authSourceId;

    private Date createdOn;

    private Date updatedOn;

    private String type;

    private String mailNotification = "";

    private String salt;

    private Boolean mustChangePasswd = false;

    private Date passwdChangedOn;

    private String twofaScheme;

    private String twofaTotpKey;

    private Integer twofaTotpLastUsedAt;

    private Boolean twofaRequired = false;

    /**
     * 删除时间（软删除）
     * NULL表示未删除，有值表示删除时间
     */
    private Date deletedAt;
}
