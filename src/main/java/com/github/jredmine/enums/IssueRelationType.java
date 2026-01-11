package com.github.jredmine.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 任务关联类型枚举
 *
 * @author panfeng
 */
@Getter
public enum IssueRelationType {
    /**
     * 相关
     */
    RELATES("relates", "相关"),
    
    /**
     * 重复
     */
    DUPLICATES("duplicates", "重复"),
    
    /**
     * 被重复
     */
    DUPLICATED("duplicated", "被重复"),
    
    /**
     * 阻塞
     */
    BLOCKS("blocks", "阻塞"),
    
    /**
     * 被阻塞
     */
    BLOCKED("blocked", "被阻塞"),
    
    /**
     * 前置（用于甘特图）
     */
    PRECEDES("precedes", "前置"),
    
    /**
     * 后置（用于甘特图）
     */
    FOLLOWS("follows", "后置"),
    
    /**
     * 复制到
     */
    COPIED_TO("copied_to", "复制到"),
    
    /**
     * 从...复制
     */
    COPIED_FROM("copied_from", "从...复制");

    private final String code;
    private final String description;

    IssueRelationType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 获取枚举值（用于JSON序列化）
     */
    @JsonValue
    public String getCode() {
        return code;
    }

    /**
     * 根据code获取枚举（用于JSON反序列化）
     */
    @JsonCreator
    public static IssueRelationType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (IssueRelationType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid IssueRelationType code: " + code);
    }

    /**
     * 检查是否支持延迟天数
     * 只有 precedes 和 follows 类型支持延迟天数
     */
    public boolean supportsDelay() {
        return this == PRECEDES || this == FOLLOWS;
    }

    /**
     * 检查是否为对称关系
     * 对称关系应该自动创建反向关联
     */
    public boolean isSymmetric() {
        return this == DUPLICATES 
            || this == DUPLICATED 
            || this == BLOCKS 
            || this == BLOCKED 
            || this == PRECEDES 
            || this == FOLLOWS
            || this == COPIED_TO
            || this == COPIED_FROM;
    }

    /**
     * 获取反向关联类型
     * 例如：BLOCKS 的反向是 BLOCKED
     */
    public IssueRelationType getReverse() {
        switch (this) {
            case DUPLICATES:
                return DUPLICATED;
            case DUPLICATED:
                return DUPLICATES;
            case BLOCKS:
                return BLOCKED;
            case BLOCKED:
                return BLOCKS;
            case PRECEDES:
                return FOLLOWS;
            case FOLLOWS:
                return PRECEDES;
            case COPIED_TO:
                return COPIED_FROM;
            case COPIED_FROM:
                return COPIED_TO;
            case RELATES:
            default:
                return RELATES; // 相关关系没有明确的反向
        }
    }

    @Override
    public String toString() {
        return code;
    }
}
