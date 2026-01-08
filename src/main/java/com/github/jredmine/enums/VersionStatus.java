package com.github.jredmine.enums;

import lombok.Getter;

/**
 * 版本状态枚举
 *
 * @author panfeng
 */
@Getter
public enum VersionStatus {
    /**
     * 开放
     */
    OPEN("open", "开放"),

    /**
     * 锁定
     */
    LOCKED("locked", "锁定"),

    /**
     * 关闭
     */
    CLOSED("closed", "关闭");

    /**
     * 状态代码
     */
    private final String code;

    /**
     * 状态描述
     */
    private final String description;

    VersionStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据代码获取枚举
     *
     * @param code 状态代码
     * @return 版本状态枚举，如果不存在则返回 null
     */
    public static VersionStatus fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (VersionStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }

    /**
     * 验证代码是否有效
     *
     * @param code 状态代码
     * @return 是否有效
     */
    public static boolean isValid(String code) {
        return fromCode(code) != null;
    }
}
