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
