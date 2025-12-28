package com.github.jredmine.dto.request.project;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 成员角色分配请求DTO
 *
 * @author panfeng
 */
@Data
public class MemberRoleAssignRequestDTO {
    /**
     * 角色ID列表
     */
    @NotEmpty(message = "角色ID列表不能为空")
    private List<Integer> roleIds;
}

