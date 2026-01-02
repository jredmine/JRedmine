package com.github.jredmine.dto.request.issue;

import com.github.jredmine.dto.request.PageRequestDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 任务分类列表查询请求DTO
 *
 * @author panfeng
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class IssueCategoryListRequestDTO extends PageRequestDTO {
    /**
     * 分类名称（模糊查询，可选）
     */
    private String name;
}
