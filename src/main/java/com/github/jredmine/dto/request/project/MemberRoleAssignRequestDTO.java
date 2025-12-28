package com.github.jredmine.dto.request.project;

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
     * 角色ID列表（可选，用于更新接口时可以传空数组清空角色）
     */
    private List<Integer> roleIds;
}
