package com.github.jredmine.dto.request.project;

import com.github.jredmine.enums.VersionSharing;
import com.github.jredmine.enums.VersionStatus;
import com.github.jredmine.validator.EnumValue;
import lombok.Data;

import java.time.LocalDate;

/**
 * 更新版本请求DTO
 * 所有字段都是可选的，只更新提供的字段
 *
 * @author panfeng
 */
@Data
public class VersionUpdateRequestDTO {
    /**
     * 版本名称（可选）
     */
    private String name;

    /**
     * 版本描述（可选）
     */
    private String description;

    /**
     * 生效日期（可选）
     */
    private LocalDate effectiveDate;

    /**
     * Wiki页面标题（可选）
     */
    private String wikiPageTitle;

    /**
     * 状态（可选）
     * - 'open': 开放
     * - 'locked': 锁定
     * - 'closed': 关闭
     */
    @EnumValue(enumClass = VersionStatus.class, message = "状态值无效")
    private String status;

    /**
     * 共享方式（可选）
     * - 'none': 不共享
     * - 'descendants': 共享给子项目
     * - 'hierarchy': 共享给项目层次结构
     * - 'tree': 共享给项目树
     * - 'system': 系统共享
     */
    @EnumValue(enumClass = VersionSharing.class, message = "共享方式无效")
    private String sharing;
}
