package com.redmine.jredmine.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.LogoutConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * @author panfeng
 * @create 2025-01-25-16:46
 */
@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // 关闭 CSRF 保护（开发阶段可以关闭，生产环境需根据需求开启）
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll() // 允许所有请求，不需要认证
                );

        return http.build();    }
}
