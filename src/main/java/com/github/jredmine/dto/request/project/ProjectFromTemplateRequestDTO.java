package com.github.jredmine.dto.request.project;

import lombok.Data;

/**
 * 从模板创建项目请求DTO
 *
 * @author panfeng
 */
@Data
public class ProjectFromTemplateRequestDTO {
    /**
     * 新项目名称
     */
    private String name;

    /**
     * 新项目标识符
     */
    private String identifier;

    /**
     * 是否复制成员（模板中的成员）
     */
    private Boolean copyMembers = false;
}
