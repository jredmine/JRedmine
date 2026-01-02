package com.github.jredmine.dto.request.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * 分页参数验证器
 * 验证分页参数：如果为 null，则跳过验证（使用默认值）；如果不为 null，则必须 >= 1
 *
 * @author panfeng
 */
public class PageParamValidator implements ConstraintValidator<ValidPageParam, Integer> {

    @Override
    public void initialize(ValidPageParam constraintAnnotation) {
        // 无需初始化
    }

    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        // null 值跳过验证（将使用默认值）
        if (value == null) {
            return true;
        }
        // 非 null 值必须 >= 1
        return value >= 1;
    }
}
