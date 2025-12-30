package com.github.jredmine.enums;

import lombok.Getter;

/**
 * 工作流字段规则枚举
 *
 * @author panfeng
 */
@Getter
public enum WorkflowRule {
    /**
     * 必填
     * 字段在状态转换时必须填写
     */
    REQUIRED("required", "必填"),

    /**
     * 只读
     * 字段在状态转换时只读，不能修改
     */
    READONLY("readonly", "只读"),

    /**
     * 隐藏
     * 字段在状态转换时隐藏，不显示
     */
    HIDDEN("hidden", "隐藏");

    private final String code;
    private final String description;

    WorkflowRule(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据代码获取规则
     *
     * @param code 规则代码
     * @return 工作流规则枚举，如果不存在则返回null
     */
    public static WorkflowRule fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (WorkflowRule rule : values()) {
            if (rule.code.equals(code)) {
                return rule;
            }
        }
        return null;
    }
}

