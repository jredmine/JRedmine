package com.github.jredmine.enums;

import lombok.Getter;

/**
 * 版本共享方式枚举
 *
 * @author panfeng
 */
@Getter
public enum VersionSharing {
    /**
     * 不共享
     */
    NONE("none", "不共享"),

    /**
     * 共享给子项目
     */
    DESCENDANTS("descendants", "共享给子项目"),

    /**
     * 共享给项目层次结构
     */
    HIERARCHY("hierarchy", "共享给项目层次结构"),

    /**
     * 共享给项目树
     */
    TREE("tree", "共享给项目树"),

    /**
     * 系统共享
     */
    SYSTEM("system", "系统共享");

    /**
     * 共享方式代码
     */
    private final String code;

    /**
     * 共享方式描述
     */
    private final String description;

    VersionSharing(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据代码获取枚举
     *
     * @param code 共享方式代码
     * @return 版本共享枚举，如果不存在则返回 null
     */
    public static VersionSharing fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (VersionSharing sharing : values()) {
            if (sharing.code.equals(code)) {
                return sharing;
            }
        }
        return null;
    }

    /**
     * 验证代码是否有效
     *
     * @param code 共享方式代码
     * @return 是否有效
     */
    public static boolean isValid(String code) {
        return fromCode(code) != null;
    }
}
