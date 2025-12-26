package com.github.jredmine.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * 邮件配置类
 * 
 * 注意：Spring Boot 的 spring-boot-starter-mail 会自动配置 JavaMailSender。
 * 此配置类主要用于让 IDE 正确识别 JavaMailSender bean，避免 IDE 误报。
 * 
 * 使用 @ConditionalOnMissingBean 确保只在 Spring Boot 自动配置未生效时才创建，
 * 避免与自动配置冲突。
 * 
 * 如果不需要 IDE 提示，可以删除此配置类，Spring Boot 的自动配置已经足够。
 *
 * @author panfeng
 */
@Configuration
@ConditionalOnProperty(prefix = "spring.mail", name = "host")
public class MailConfig {

    /**
     * JavaMailSender Bean
     * 
     * 此方法主要用于让 IDE 识别 JavaMailSender bean。
     * 实际运行时，Spring Boot 的自动配置会优先创建此 bean，
     * 此方法不会被执行（因为 @ConditionalOnMissingBean）。
     */
    @Bean
    @ConditionalOnMissingBean(JavaMailSender.class)
    public JavaMailSender javaMailSender() {
        // 此方法不会被执行，因为 Spring Boot 会自动配置 JavaMailSender
        // 这里只是为了让 IDE 识别 bean 的存在
        return new JavaMailSenderImpl();
    }
}

