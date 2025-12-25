package com.github.jredmine.config;

import com.github.jredmine.util.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT认证过滤器
 * 在每个请求中检查JWT Token，如果有效则设置认证信息
 *
 * @author panfeng
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");

        // 检查Authorization头是否存在且以Bearer开头
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 提取Token（去掉"Bearer "前缀）
            final String jwt = authHeader.substring(7);

            // 验证Token并提取用户名
            if (jwtUtils.validateToken(jwt)) {
                final String username = jwtUtils.extractUsername(jwt);
                final Long userId = jwtUtils.extractUserId(jwt);

                // 如果用户名不为空且当前没有认证信息，则设置认证信息
                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    // 创建认证Token（这里简化处理，实际应该从数据库查询用户权限）
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            username,
                            null,
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    // 将用户ID存储到请求属性中，方便后续使用
                    request.setAttribute("userId", userId);
                }
            }
        } catch (Exception e) {
            log.error("JWT认证失败: {}", e.getMessage());
            // Token无效时不设置认证信息，继续过滤链
        }

        filterChain.doFilter(request, response);
    }
}

