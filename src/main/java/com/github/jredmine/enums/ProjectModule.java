package com.github.jredmine.enums;

/**
 * 项目模块枚举
 * 定义项目中可启用的功能模块
 * 
 * @author panfeng
 */
public enum ProjectModule {
    /**
     * 任务管理模块
     */
    ISSUES("issues", "任务管理"),
    
    /**
     * Wiki 模块
     */
    WIKI("wiki", "Wiki"),
    
    /**
     * 论坛模块
     */
    BOARDS("boards", "论坛"),
    
    /**
     * 文档管理模块
     */
    DOCUMENTS("documents", "文档管理"),
    
    /**
     * 文件管理模块
     */
    FILES("files", "文件管理"),
    
    /**
     * 代码仓库模块
     */
    REPOSITORY("repository", "代码仓库"),
    
    /**
     * 时间跟踪模块
     */
    TIME_TRACKING("time_tracking", "时间跟踪"),
    
    /**
     * 新闻模块
     */
    NEWS("news", "新闻"),
    
    /**
     * 日历模块
     */
    CALENDAR("calendar", "日历"),
    
    /**
     * 甘特图模块
     */
    GANTT("gantt", "甘特图");
    
    private final String code;
    private final String name;
    
    ProjectModule(String code, String name) {
        this.code = code;
        this.name = name;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getName() {
        return name;
    }
    
    /**
     * 根据代码获取模块
     *
     * @param code 模块代码
     * @return 项目模块枚举，如果不存在则返回null
     */
    public static ProjectModule fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        for (ProjectModule module : values()) {
            if (module.code.equals(code)) {
                return module;
            }
        }
        return null;
    }
    
    /**
     * 检查代码是否有效
     *
     * @param code 模块代码
     * @return true 如果有效，false 否则
     */
    public static boolean isValidCode(String code) {
        return fromCode(code) != null;
    }
}

