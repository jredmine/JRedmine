package com.github.jredmine.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * 枚举值验证注解
 * 用于验证字符串是否为指定枚举类的有效值
 *
 * @author panfeng
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = EnumValueValidator.class)
@Documented
public @interface EnumValue {
    /**
     * 枚举类
     */
    Class<? extends Enum<?>> enumClass();

    /**
     * 获取枚举值的方法名，默认为 "getCode"
     */
    String method() default "getCode";

    /**
     * 错误消息
     */
    String message() default "无效的枚举值";

    /**
     * 是否允许为空
     */
    boolean nullable() default true;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
