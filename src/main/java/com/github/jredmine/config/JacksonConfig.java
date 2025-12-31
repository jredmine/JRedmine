package com.github.jredmine.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

/**
 * Jackson 配置类
 * 配置日期时间序列化和反序列化格式
 *
 * @author panfeng
 */
@Configuration
public class JacksonConfig {

    /**
     * 日期格式：yyyy-MM-dd
     */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 日期时间格式：yyyy-MM-dd HH:mm:ss
     */
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 支持多种日期时间格式的 LocalDateTime 反序列化器
     * 支持格式：
     * - yyyy-MM-dd HH:mm:ss
     * - yyyy-MM-dd'T'HH:mm:ss（ISO 8601 格式）
     * - yyyy-MM-dd'T'HH:mm:ss.SSS（带毫秒）
     */
    private static final DateTimeFormatter LOCAL_DATETIME_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("[yyyy-MM-dd HH:mm:ss][yyyy-MM-dd'T'HH:mm:ss][yyyy-MM-dd'T'HH:mm:ss.SSS]")
            .toFormatter();

    /**
     * 自定义 LocalDate 反序列化器
     * 支持格式：
     * - yyyy-MM-dd
     * - yyyy-MM-dd HH:mm:ss（会自动截取日期部分）
     */
    public static class FlexibleLocalDateDeserializer extends StdDeserializer<LocalDate> {
        public FlexibleLocalDateDeserializer() {
            super(LocalDate.class);
        }

        @Override
        public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String text = p.getText().trim();
            if (text == null || text.isEmpty()) {
                return null;
            }

            try {
                // 先尝试解析为日期格式 yyyy-MM-dd
                if (text.length() == 10) {
                    return LocalDate.parse(text, DATE_FORMATTER);
                }
                // 如果包含时间部分，先解析为 LocalDateTime，然后提取日期部分
                if (text.length() >= 19) {
                    LocalDateTime dateTime = LocalDateTime.parse(text, DATETIME_FORMATTER);
                    return dateTime.toLocalDate();
                }
                // 尝试 ISO 格式
                return LocalDate.parse(text);
            } catch (Exception e) {
                throw new IOException("无法解析日期: " + text + "，支持的格式: yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss", e);
            }
        }
    }

    @Bean
    @Primary
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper objectMapper = builder.build();

        // 注册 JavaTimeModule
        JavaTimeModule javaTimeModule = new JavaTimeModule();

        // 配置 LocalDate 序列化和反序列化
        javaTimeModule.addSerializer(LocalDate.class, new LocalDateSerializer(DATE_FORMATTER));
        javaTimeModule.addDeserializer(LocalDate.class, new FlexibleLocalDateDeserializer());

        // 配置 LocalDateTime 序列化和反序列化
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DATETIME_FORMATTER));
        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(LOCAL_DATETIME_FORMATTER));

        objectMapper.registerModule(javaTimeModule);

        // 配置：忽略未知属性
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // 禁用将日期写为时间戳
        objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return objectMapper;
    }
}
