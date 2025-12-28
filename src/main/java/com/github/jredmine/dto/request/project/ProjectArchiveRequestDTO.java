package com.github.jredmine.dto.request.project;

import lombok.Data;

/**
 * 项目归档请求DTO
 *
 * @author panfeng
 */
@Data
public class ProjectArchiveRequestDTO {
    /**
     * 是否归档（true=归档，false=取消归档）
     */
    private Boolean archived;
}
