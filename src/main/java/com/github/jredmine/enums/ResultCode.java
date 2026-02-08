package com.github.jredmine.enums;

import lombok.Getter;

/**
 * 响应码枚举
 */
@Getter
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    FAILED(500, "操作失败"),

    // 参数错误
    PARAM_ERROR(400, "参数错误"),
    PARAM_MISSING(400, "参数缺失"),
    PARAM_INVALID(400, "参数无效"),

    // 认证授权错误
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),
    TOKEN_EXPIRED(401, "令牌已过期"),
    TOKEN_INVALID(401, "令牌无效"),

    // 业务错误
    USER_NOT_FOUND(404, "用户不存在"),
    USER_ALREADY_EXISTS(409, "用户已存在"),
    PASSWORD_ERROR(400, "密码错误"),
    EMAIL_ALREADY_EXISTS(409, "邮箱已存在"),
    PHONE_ALREADY_EXISTS(409, "手机号已存在"),

    // 角色错误
    ROLE_NOT_FOUND(404, "角色不存在"),
    ROLE_NAME_EXISTS(409, "角色名称已存在"),
    ROLE_CANNOT_MODIFY(400, "系统内置角色不能修改"),
    ROLE_CANNOT_DELETE(400, "系统内置角色不能删除"),

    // 项目错误
    PROJECT_NOT_FOUND(404, "项目不存在"),
    PROJECT_ACCESS_DENIED(403, "无权限访问该项目"),
    PROJECT_NAME_EXISTS(409, "项目名称已存在"),
    PROJECT_IDENTIFIER_EXISTS(409, "项目标识符已存在"),
    PROJECT_PARENT_NOT_FOUND(404, "父项目不存在"),
    PROJECT_MODULE_INVALID(400, "项目模块无效"),
    PROJECT_HAS_CHILDREN(400, "项目存在子项目，请先删除或归档子项目"),
    TRACKER_NOT_FOUND(404, "跟踪器不存在"),
    TRACKER_NAME_EXISTS(409, "跟踪器名称已存在"),
    TRACKER_IN_USE(400, "跟踪器正在被项目使用，不能删除"),

    // Wiki
    WIKI_NOT_ENABLED(400, "项目未启用 Wiki 模块"),
    WIKI_PAGE_NOT_FOUND(404, "Wiki 页面不存在"),
    WIKI_PAGE_TITLE_EXISTS(409, "该 Wiki 下已存在同名页面"),
    WIKI_VERSION_NOT_FOUND(404, "Wiki 版本不存在"),
    WIKI_REDIRECT_NOT_FOUND(404, "Wiki 重定向不存在"),
    WIKI_REDIRECT_TITLE_EXISTS(409, "该标题已是重定向或已存在同名页面"),
    WIKI_REDIRECT_TARGET_NOT_FOUND(404, "重定向目标页面不存在"),

    // 文档
    DOCUMENTS_NOT_ENABLED(400, "项目未启用文档模块"),
    DOCUMENT_CATEGORY_NOT_FOUND(400, "文档分类不存在或不可用于当前项目"),
    DOCUMENT_CATEGORY_NAME_EXISTS(409, "该项目下已存在同名文档分类"),
    DOCUMENT_CATEGORY_IN_USE(400, "该分类下仍有文档，无法删除"),
    DOCUMENT_CATEGORY_NOT_EDITABLE(400, "仅可编辑或删除项目级分类，不可操作全局分类"),
    DOCUMENT_NOT_FOUND(404, "文档不存在"),

    // 论坛
    BOARDS_NOT_ENABLED(400, "项目未启用论坛模块"),
    BOARD_NOT_FOUND(404, "论坛板块不存在"),
    BOARD_NAME_EXISTS(409, "该项目下已存在同名板块"),

    // 菜单错误
    MENU_NOT_FOUND(404, "菜单不存在"),

    // 系统错误
    SYSTEM_ERROR(500, "系统错误"),
    DATABASE_ERROR(500, "数据库错误"),
    NETWORK_ERROR(500, "网络错误");

    private final Integer code;
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
