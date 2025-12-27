package com.github.jredmine.enums;

import lombok.Getter;

/**
 * 权限枚举
 * 定义系统中所有可用的权限常量
 * 
 * 权限是项目级别的，通过角色分配给项目成员
 * 系统管理员（admin = true）拥有所有权限，不受项目限制
 *
 * @author panfeng
 */
@Getter
public enum Permission {

    // ==================== 项目管理权限 ====================
    
    /**
     * 查看项目
     */
    VIEW_PROJECTS("view_projects", "查看项目", "projects", "查看项目列表和详情"),
    
    /**
     * 创建项目
     */
    CREATE_PROJECTS("create_projects", "创建项目", "projects", "创建新项目"),
    
    /**
     * 编辑项目
     */
    EDIT_PROJECTS("edit_projects", "编辑项目", "projects", "编辑项目信息"),
    
    /**
     * 删除项目
     */
    DELETE_PROJECTS("delete_projects", "删除项目", "projects", "删除或归档项目"),
    
    /**
     * 管理项目
     */
    MANAGE_PROJECTS("manage_projects", "管理项目", "projects", "管理项目（包括成员管理、模块管理等）"),

    // ==================== 任务管理权限 ====================
    
    /**
     * 查看任务
     */
    VIEW_ISSUES("view_issues", "查看任务", "issues", "查看项目中的任务"),
    
    /**
     * 创建任务
     */
    ADD_ISSUES("add_issues", "创建任务", "issues", "创建新任务"),
    
    /**
     * 编辑任务
     */
    EDIT_ISSUES("edit_issues", "编辑任务", "issues", "编辑任务信息"),
    
    /**
     * 删除任务
     */
    DELETE_ISSUES("delete_issues", "删除任务", "issues", "删除任务"),
    
    /**
     * 管理任务
     */
    MANAGE_ISSUES("manage_issues", "管理任务", "issues", "管理任务（包括分配、状态转换、优先级等）"),
    
    /**
     * 查看私有任务
     */
    VIEW_PRIVATE_ISSUES("view_private_issues", "查看私有任务", "issues", "查看标记为私有的任务"),

    // ==================== 工时管理权限 ====================
    
    /**
     * 查看工时
     */
    VIEW_TIME_ENTRIES("view_time_entries", "查看工时", "time_entries", "查看工时记录"),
    
    /**
     * 记录工时
     */
    LOG_TIME("log_time", "记录工时", "time_entries", "记录自己的工时"),
    
    /**
     * 编辑工时
     */
    EDIT_TIME_ENTRIES("edit_time_entries", "编辑工时", "time_entries", "编辑工时记录"),
    
    /**
     * 管理工时
     */
    MANAGE_TIME_ENTRIES("manage_time_entries", "管理工时", "time_entries", "管理所有工时记录"),

    // ==================== 文档管理权限 ====================
    
    /**
     * 查看文档
     */
    VIEW_DOCUMENTS("view_documents", "查看文档", "documents", "查看项目文档"),
    
    /**
     * 创建文档
     */
    ADD_DOCUMENTS("add_documents", "创建文档", "documents", "创建新文档"),
    
    /**
     * 编辑文档
     */
    EDIT_DOCUMENTS("edit_documents", "编辑文档", "documents", "编辑文档信息"),
    
    /**
     * 删除文档
     */
    DELETE_DOCUMENTS("delete_documents", "删除文档", "documents", "删除文档"),

    // ==================== Wiki 管理权限 ====================
    
    /**
     * 查看Wiki页面
     */
    VIEW_WIKI_PAGES("view_wiki_pages", "查看Wiki页面", "wiki", "查看Wiki页面"),
    
    /**
     * 编辑Wiki页面
     */
    EDIT_WIKI_PAGES("edit_wiki_pages", "编辑Wiki页面", "wiki", "编辑Wiki页面内容"),
    
    /**
     * 删除Wiki页面
     */
    DELETE_WIKI_PAGES("delete_wiki_pages", "删除Wiki页面", "wiki", "删除Wiki页面"),
    
    /**
     * 管理Wiki
     */
    MANAGE_WIKI("manage_wiki", "管理Wiki", "wiki", "管理Wiki（包括重命名、删除等）"),

    // ==================== 论坛管理权限 ====================
    
    /**
     * 查看消息
     */
    VIEW_MESSAGES("view_messages", "查看消息", "boards", "查看论坛消息"),
    
    /**
     * 创建消息
     */
    ADD_MESSAGES("add_messages", "创建消息", "boards", "创建新消息"),
    
    /**
     * 编辑消息
     */
    EDIT_MESSAGES("edit_messages", "编辑消息", "boards", "编辑自己创建的消息"),
    
    /**
     * 删除消息
     */
    DELETE_MESSAGES("delete_messages", "删除消息", "boards", "删除自己创建的消息"),
    
    /**
     * 管理论坛
     */
    MANAGE_BOARDS("manage_boards", "管理论坛", "boards", "管理论坛板块和消息"),

    // ==================== 代码仓库权限 ====================
    
    /**
     * 浏览代码仓库
     */
    BROWSE_REPOSITORY("browse_repository", "浏览代码仓库", "repositories", "浏览代码仓库内容"),
    
    /**
     * 提交代码
     */
    COMMIT_ACCESS("commit_access", "提交代码", "repositories", "提交代码到仓库"),
    
    /**
     * 管理代码仓库
     */
    MANAGE_REPOSITORY("manage_repository", "管理代码仓库", "repositories", "管理代码仓库配置"),

    // ==================== 版本管理权限 ====================
    
    /**
     * 查看版本
     */
    VIEW_VERSIONS("view_versions", "查看版本", "versions", "查看项目版本/里程碑"),
    
    /**
     * 管理版本
     */
    MANAGE_VERSIONS("manage_versions", "管理版本", "versions", "创建和管理项目版本/里程碑"),

    // ==================== 文件管理权限 ====================
    
    /**
     * 查看文件
     */
    VIEW_FILES("view_files", "查看文件", "files", "查看项目文件"),
    
    /**
     * 管理文件
     */
    MANAGE_FILES("manage_files", "管理文件", "files", "上传、删除项目文件"),

    // ==================== 新闻管理权限 ====================
    
    /**
     * 查看新闻
     */
    VIEW_NEWS("view_news", "查看新闻", "news", "查看项目新闻"),
    
    /**
     * 管理新闻
     */
    MANAGE_NEWS("manage_news", "管理新闻", "news", "创建和管理项目新闻"),

    // ==================== 系统设置权限 ====================
    
    /**
     * 管理系统设置
     */
    MANAGE_SETTINGS("manage_settings", "管理系统设置", "settings", "管理系统全局设置"),
    
    /**
     * 管理用户
     */
    MANAGE_USERS("manage_users", "管理用户", "settings", "管理系统用户（创建、删除、更新用户）"),
    
    /**
     * 管理角色
     */
    MANAGE_ROLES("manage_roles", "管理角色", "settings", "管理系统角色和权限");

    /**
     * 权限键（用于存储和比较）
     */
    private final String key;

    /**
     * 权限名称（中文显示名称）
     */
    private final String name;

    /**
     * 权限分类
     */
    private final String category;

    /**
     * 权限描述
     */
    private final String description;

    Permission(String key, String name, String category, String description) {
        this.key = key;
        this.name = name;
        this.category = category;
        this.description = description;
    }

    /**
     * 根据权限键获取权限枚举
     *
     * @param key 权限键
     * @return 权限枚举，如果不存在则返回null
     */
    public static Permission fromKey(String key) {
        for (Permission permission : values()) {
            if (permission.key.equals(key)) {
                return permission;
            }
        }
        return null;
    }

    /**
     * 检查权限键是否有效
     *
     * @param key 权限键
     * @return true 如果有效，false 否则
     */
    public static boolean isValidKey(String key) {
        return fromKey(key) != null;
    }

    /**
     * 根据分类获取权限列表
     *
     * @param category 权限分类
     * @return 权限列表
     */
    public static Permission[] getByCategory(String category) {
        return java.util.Arrays.stream(values())
                .filter(p -> p.category.equals(category))
                .toArray(Permission[]::new);
    }

    /**
     * 获取所有权限分类
     *
     * @return 权限分类列表
     */
    public static java.util.Set<String> getAllCategories() {
        return java.util.Arrays.stream(values())
                .map(Permission::getCategory)
                .collect(java.util.stream.Collectors.toSet());
    }
}

