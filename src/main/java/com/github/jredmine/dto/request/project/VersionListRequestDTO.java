package com.github.jredmine.dto.request.project;

import com.github.jredmine.enums.VersionStatus;
import com.github.jredmine.validator.EnumValue;
import lombok.Data;

/**
 * 版本列表查询请求DTO
 *
 * @author panfeng
 */
@Data
public class VersionListRequestDTO {
    /**
     * 页码（从1开始）
     */
    private Integer current = 1;

    /**
     * 每页大小
     */
    private Integer size = 10;

    /**
     * 版本名称（模糊查询）
     */
    private String name;

    /**
     * 状态筛选
     * - 'open': 开放
     * - 'locked': 锁定
     * - 'closed': 关闭
     */
    @EnumValue(enumClass = VersionStatus.class, message = "状态值无效")
    private String status;
}
