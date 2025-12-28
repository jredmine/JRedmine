package com.github.jredmine.dto.response.project;

import lombok.Data;

import java.util.Date;

/**
 * 项目成员连表查询结果DTO
 * 用于接收 mybatis-plus-join 连表查询的结果
 *
 * @author panfeng
 */
@Data
public class ProjectMemberJoinDTO {
    /**
     * 成员ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户登录名
     */
    private String login;

    /**
     * 用户名字
     */
    private String firstname;

    /**
     * 用户姓氏
     */
    private String lastname;

    /**
     * 用户邮箱
     */
    private String email;

    /**
     * 加入时间
     */
    private Date createdOn;

    /**
     * 邮件通知设置
     */
    private Boolean mailNotification;

    /**
     * 角色ID（可能为null，如果一个成员有多个角色，会有多条记录）
     */
    private Integer roleId;

    /**
     * 角色名称（可能为null）
     */
    private String roleName;

    /**
     * 继承自（子项目继承父项目角色，可能为null）
     */
    private Integer inheritedFrom;
}
