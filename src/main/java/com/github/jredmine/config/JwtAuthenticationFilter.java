package com.github.jredmine.config;

import com.github.jredmine.security.CustomUserDetailsService;
import com.github.jredmine.security.UserPrincipal;
import com.github.jredmine.util.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT认证过滤器
 * 在每个请求中检查JWT Token，如果有效则设置认证信息
 * 从数据库加载用户真实权限信息
 *
 * @author panfeng
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final CustomUserDetailsService userDetailsService;

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
                    // 从数据库加载用户信息和权限
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    
                    // 验证用户ID是否匹配
                    if (userDetails instanceof UserPrincipal) {
                        UserPrincipal userPrincipal = (UserPrincipal) userDetails;
                        if (!userPrincipal.getId().equals(userId)) {
                            log.warn("JWT Token中的用户ID与数据库中的用户ID不匹配，Token用户ID: {}, 数据库用户ID: {}", 
                                    userId, userPrincipal.getId());
                            filterChain.doFilter(request, response);
                            return;
                        }
                    }

                    // 创建认证Token，包含用户权限信息
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    // 将用户ID存储到请求属性中，方便后续使用
                    request.setAttribute("userId", userId);
                    
                    log.debug("JWT认证成功，用户: {}, 用户ID: {}, 是否管理员: {}", 
                            username, userId, userDetails instanceof UserPrincipal && ((UserPrincipal) userDetails).isAdmin());
                }
            }
        } catch (Exception e) {
            log.error("JWT认证失败: {}", e.getMessage(), e);
            // Token无效时不设置认证信息，继续过滤链
        }

        filterChain.doFilter(request, response);
    }
}

