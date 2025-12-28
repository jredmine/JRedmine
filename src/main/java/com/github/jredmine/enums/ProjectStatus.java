package com.github.jredmine.enums;

/**
 * 项目状态枚举
 * 
 * @author panfeng
 */
public enum ProjectStatus {
    /**
     * 活跃状态
     * 项目正常进行中
     */
    ACTIVE(1, "活跃"),
    
    /**
     * 关闭状态
     * 项目已关闭，但数据保留
     */
    CLOSED(5, "关闭"),
    
    /**
     * 归档状态
     * 项目已归档，通常不显示在列表中
     */
    ARCHIVED(9, "归档");
    
    private final Integer code;
    private final String description;
    
    ProjectStatus(Integer code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public Integer getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 根据代码获取状态
     *
     * @param code 状态代码
     * @return 项目状态枚举，如果不存在则返回null
     */
    public static ProjectStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ProjectStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }
    
    /**
     * 检查代码是否有效
     *
     * @param code 状态代码
     * @return true 如果有效，false 否则
     */
    public static boolean isValidCode(Integer code) {
        return fromCode(code) != null;
    }
}

