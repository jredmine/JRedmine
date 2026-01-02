package com.github.jredmine.dto.request.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 分页参数验证注解
 * 用于验证分页参数：如果为 null，则跳过验证（使用默认值）；如果不为 null，则必须 >= 1
 *
 * @author panfeng
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PageParamValidator.class)
@Documented
public @interface ValidPageParam {
    String message() default "分页参数必须大于0";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
