package com.github.jredmine.dto.response.project;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 项目成员响应DTO
 *
 * @author panfeng
 */
@Data
public class ProjectMemberResponseDTO {
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
     * 用户邮箱（从 email_addresses 表获取）
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
     * 角色列表（角色ID和名称）
     */
    private List<MemberRoleInfo> roles;

    /**
     * 成员角色信息
     */
    @Data
    public static class MemberRoleInfo {
        /**
         * 角色ID
         */
        private Integer roleId;

        /**
         * 角色名称
         */
        private String roleName;

        /**
         * 是否继承自父项目
         */
        private Boolean inherited;
    }
}
