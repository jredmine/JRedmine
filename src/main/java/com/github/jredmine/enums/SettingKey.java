package com.github.jredmine.enums;

import lombok.Getter;

/**
 * 系统设置键枚举
 * 定义所有支持的系统设置项
 *
 * @author panfeng
 */
@Getter
public enum SettingKey {
    // ========== 基本设置 ==========
    /**
     * 站点名称
     */
    SITE_TITLE("app_title", "站点名称", "JRedmine", SettingCategory.GENERAL),
    
    /**
     * 站点描述
     */
    SITE_DESCRIPTION("app_description", "站点描述", "项目管理系统", SettingCategory.GENERAL),
    
    /**
     * 默认语言
     */
    DEFAULT_LANGUAGE("default_language", "默认语言", "zh-CN", SettingCategory.GENERAL),
    
    /**
     * 时区
     */
    TIME_ZONE("time_zone", "时区", "Asia/Shanghai", SettingCategory.GENERAL),
    
    // ========== 邮件设置 ==========
    /**
     * 邮件发送是否启用
     */
    EMAIL_ENABLED("email_delivery", "邮件发送启用", "true", SettingCategory.EMAIL),
    
    /**
     * SMTP服务器地址
     */
    SMTP_SERVER("smtp_address", "SMTP服务器", "smtp.example.com", SettingCategory.EMAIL),
    
    /**
     * SMTP端口
     */
    SMTP_PORT("smtp_port", "SMTP端口", "587", SettingCategory.EMAIL),
    
    /**
     * SMTP用户名
     */
    SMTP_USERNAME("smtp_username", "SMTP用户名", "", SettingCategory.EMAIL),
    
    /**
     * SMTP密码
     */
    SMTP_PASSWORD("smtp_password", "SMTP密码", "", SettingCategory.EMAIL),
    
    /**
     * SMTP认证方式
     */
    SMTP_AUTHENTICATION("smtp_authentication", "SMTP认证", "plain", SettingCategory.EMAIL),
    
    /**
     * SMTP启用TLS
     */
    SMTP_ENABLE_STARTTLS("smtp_enable_starttls_auto", "启用TLS", "true", SettingCategory.EMAIL),
    
    /**
     * 发件人邮箱
     */
    EMAIL_FROM("emails_from", "发件人邮箱", "noreply@example.com", SettingCategory.EMAIL),
    
    // ========== 安全设置 ==========
    /**
     * 密码最小长度
     */
    PASSWORD_MIN_LENGTH("password_min_length", "密码最小长度", "8", SettingCategory.SECURITY),
    
    /**
     * 密码必须包含数字
     */
    PASSWORD_REQUIRE_DIGIT("password_require_digit", "密码必须包含数字", "true", SettingCategory.SECURITY),
    
    /**
     * 密码必须包含字母
     */
    PASSWORD_REQUIRE_LETTER("password_require_letter", "密码必须包含字母", "true", SettingCategory.SECURITY),
    
    /**
     * 密码必须包含特殊字符
     */
    PASSWORD_REQUIRE_SPECIAL("password_require_special", "密码必须包含特殊字符", "false", SettingCategory.SECURITY),
    
    /**
     * 会话超时时间（分钟）
     */
    SESSION_TIMEOUT("session_timeout", "会话超时时间(分钟)", "480", SettingCategory.SECURITY),
    
    /**
     * 登录失败最大次数
     */
    LOGIN_MAX_ATTEMPTS("login_max_attempts", "登录失败最大次数", "5", SettingCategory.SECURITY),
    
    /**
     * 登录失败锁定时间（分钟）
     */
    LOGIN_LOCK_TIME("login_lock_time", "登录锁定时间(分钟)", "30", SettingCategory.SECURITY),
    
    // ========== 附件设置 ==========
    /**
     * 附件最大大小（MB）
     */
    ATTACHMENT_MAX_SIZE("attachment_max_size", "附件最大大小(MB)", "20", SettingCategory.ATTACHMENT),
    
    /**
     * 允许的文件扩展名
     */
    ATTACHMENT_ALLOWED_EXTENSIONS("attachment_extensions_allowed", "允许的文件扩展名", 
            "jpg,jpeg,png,gif,pdf,doc,docx,xls,xlsx,ppt,pptx,txt,zip,rar", SettingCategory.ATTACHMENT),
    
    /**
     * 附件存储路径
     */
    ATTACHMENT_STORAGE_PATH("attachments_storage_path", "附件存储路径", "./files", SettingCategory.ATTACHMENT),
    
    /**
     * 缩略图是否启用
     */
    THUMBNAIL_ENABLED("thumbnails_enabled", "缩略图启用", "true", SettingCategory.ATTACHMENT),
    
    // ========== 通知设置 ==========
    /**
     * 默认通知给任务作者
     */
    DEFAULT_NOTIFICATION_AUTHOR("notified_events_author", "通知任务作者", "true", SettingCategory.NOTIFICATION),
    
    /**
     * 默认通知给任务指派人
     */
    DEFAULT_NOTIFICATION_ASSIGNEE("notified_events_assignee", "通知任务指派人", "true", SettingCategory.NOTIFICATION),
    
    /**
     * 默认通知给关注者
     */
    DEFAULT_NOTIFICATION_WATCHERS("notified_events_watchers", "通知关注者", "true", SettingCategory.NOTIFICATION);

    private final String key;
    private final String description;
    private final String defaultValue;
    private final SettingCategory category;

    SettingKey(String key, String description, String defaultValue, SettingCategory category) {
        this.key = key;
        this.description = description;
        this.defaultValue = defaultValue;
        this.category = category;
    }

    /**
     * 根据key获取枚举
     */
    public static SettingKey fromKey(String key) {
        for (SettingKey settingKey : values()) {
            if (settingKey.key.equals(key)) {
                return settingKey;
            }
        }
        return null;
    }

    /**
     * 系统设置分类
     */
    @Getter
    public enum SettingCategory {
        GENERAL("基本设置", "general"),
        EMAIL("邮件设置", "email"),
        SECURITY("安全设置", "security"),
        ATTACHMENT("附件设置", "attachment"),
        NOTIFICATION("通知设置", "notification");

        private final String name;
        private final String code;

        SettingCategory(String name, String code) {
            this.name = name;
            this.code = code;
        }
    }
}
