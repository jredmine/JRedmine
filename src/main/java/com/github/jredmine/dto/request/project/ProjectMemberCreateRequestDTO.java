package com.github.jredmine.dto.request.project;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 新增项目成员请求DTO
 *
 * @author panfeng
 */
@Data
public class ProjectMemberCreateRequestDTO {
    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 角色ID列表
     */
    @NotEmpty(message = "角色ID列表不能为空")
    private List<Integer> roleIds;

    /**
     * 邮件通知设置
     */
    private Boolean mailNotification = false;
}
