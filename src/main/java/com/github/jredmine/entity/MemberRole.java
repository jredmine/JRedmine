package com.github.jredmine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 成员角色关联实体
 * 对应数据库中的 `member_roles` 表
 *
 * @author panfeng
 */
@Data
@TableName("member_roles")
public class MemberRole {
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 项目成员ID
     */
    private Integer memberId;

    /**
     * 角色ID
     */
    private Integer roleId;

    /**
     * 继承自（子项目继承父项目角色）
     */
    private Integer inheritedFrom;
}

