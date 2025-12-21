package com.github.jredmine.constant;

/**
 * API 常量类
 */
public class ApiConstant {
    
    /**
     * API 响应状态
     */
    public static final String STATUS_SUCCESS = "success";
    public static final String STATUS_ERROR = "error";
    
    /**
     * API 响应消息
     */
    public static final String MESSAGE_SUCCESS = "操作成功";
    public static final String MESSAGE_ERROR = "操作失败";
    
    /**
     * 私有构造函数，防止实例化
     */
    private ApiConstant() {
        throw new UnsupportedOperationException("Utility class");
    }
}

