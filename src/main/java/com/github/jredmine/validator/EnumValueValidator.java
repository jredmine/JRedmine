package com.github.jredmine.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 枚举值验证器
 *
 * @author panfeng
 */
public class EnumValueValidator implements ConstraintValidator<EnumValue, String> {

    private Class<? extends Enum<?>> enumClass;
    private String method;
    private boolean nullable;

    @Override
    public void initialize(EnumValue constraintAnnotation) {
        this.enumClass = constraintAnnotation.enumClass();
        this.method = constraintAnnotation.method();
        this.nullable = constraintAnnotation.nullable();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // 如果值为空
        if (value == null || value.trim().isEmpty()) {
            return nullable;
        }

        try {
            // 获取枚举的所有常量
            Enum<?>[] enumConstants = enumClass.getEnumConstants();
            if (enumConstants == null) {
                return false;
            }

            // 获取枚举值列表
            List<String> enumValues = Arrays.stream(enumConstants)
                    .map(e -> {
                        try {
                            Method getValueMethod = enumClass.getMethod(method);
                            Object result = getValueMethod.invoke(e);
                            return result != null ? result.toString() : null;
                        } catch (Exception ex) {
                            return null;
                        }
                    })
                    .filter(v -> v != null)
                    .collect(Collectors.toList());

            // 检查值是否在枚举值列表中
            boolean valid = enumValues.contains(value);

            if (!valid) {
                // 自定义错误消息，显示有效的枚举值
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                        String.format("无效的值 '%s'，有效值为: %s", value, String.join(", ", enumValues))
                ).addConstraintViolation();
            }

            return valid;
        } catch (Exception e) {
            return false;
        }
    }
}
