package com.github.jredmine.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
//import org.springframework.security.authentication.AuthenticationManager;
//import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
//import org.springframework.security.config.annotation.web.configurers.LogoutConfigurer;
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.security.web.SecurityFilterChain;

/**
 * @author panfeng
 * @create 2025-01-25-16:46
 */
@Configuration
public class SecurityConfig {
    //@Bean
    //public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    //    http
    //            .csrf(AbstractHttpConfigurer::disable) // 关闭 CSRF 保护（开发阶段可以关闭，生产环境需根据需求开启）
    //            .authorizeHttpRequests(auth -> auth
    //                    .requestMatchers(HttpMethod.POST, "/api/users/register")
    //                    .permitAll()
    //                    .anyRequest()
    //                    .authenticated()
    //            );
    //    return http.build();
    //}
    //
    //@Bean
    //public PasswordEncoder passwordEncoder() {
    //    return new BCryptPasswordEncoder();
    //}
    //
    //@Bean
    //public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
    //    return config.getAuthenticationManager();
    //}
}
