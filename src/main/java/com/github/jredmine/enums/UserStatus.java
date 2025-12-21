package com.github.jredmine.enums;

/**
 * 用户状态枚举
 */
public enum UserStatus {
    /**
     * 活跃状态
     */
    ACTIVE(1, "活跃"),
    
    /**
     * 锁定状态
     */
    LOCKED(2, "锁定"),
    
    /**
     * 待激活状态
     */
    PENDING(3, "待激活");
    
    private final Integer code;
    private final String description;
    
    UserStatus(Integer code, String description) {
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
     */
    public static UserStatus fromCode(Integer code) {
        for (UserStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }
}

