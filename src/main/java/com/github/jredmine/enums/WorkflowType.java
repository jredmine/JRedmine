package com.github.jredmine.enums;

import lombok.Getter;

/**
 * 工作流类型枚举
 *
 * @author panfeng
 */
@Getter
public enum WorkflowType {
    /**
     * 状态转换规则
     * 定义哪些角色可以将任务从某个状态转换到另一个状态
     */
    TRANSITION("transition", "状态转换"),

    /**
     * 字段规则
     * 定义在状态转换时，字段的必填、只读或隐藏规则
     */
    FIELD("field", "字段规则");

    private final String code;
    private final String description;

    WorkflowType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据代码获取类型
     *
     * @param code 类型代码
     * @return 工作流类型枚举，如果不存在则返回null
     */
    public static WorkflowType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (WorkflowType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}

