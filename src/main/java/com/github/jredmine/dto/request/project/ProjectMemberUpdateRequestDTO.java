package com.github.jredmine.dto.request.project;

import lombok.Data;

import java.util.List;

/**
 * 更新项目成员请求DTO
 *
 * @author panfeng
 */
@Data
public class ProjectMemberUpdateRequestDTO {
    /**
     * 角色ID列表
     */
    private List<Integer> roleIds;

    /**
     * 邮件通知设置
     */
    private Boolean mailNotification;
}
