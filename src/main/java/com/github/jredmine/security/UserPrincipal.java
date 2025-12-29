package com.github.jredmine.security;

import com.github.jredmine.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * 自定义 UserDetails 实现
 * 包含用户信息和权限信息
 *
 * @author panfeng
 */
@Getter
public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String username;
    private final String password;
    private final boolean admin;
    private final Collection<? extends GrantedAuthority> authorities;
    private final Set<String> permissions;

    public UserPrincipal(User user, Collection<? extends GrantedAuthority> authorities, Set<String> permissions) {
        this.id = user.getId();
        this.username = user.getLogin();
        this.password = user.getHashedPassword() != null ? user.getHashedPassword() : "";
        this.admin = Boolean.TRUE.equals(user.getAdmin());
        this.authorities = authorities;
        this.permissions = permissions;
    }

    /**
     * 创建系统管理员用户主体
     * 管理员拥有所有权限
     */
    public static UserPrincipal createAdmin(User user) {
        Set<String> allPermissions = Collections.emptySet(); // 管理员权限在 PermissionEvaluator 中处理
        Collection<GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_ADMIN")
        );
        return new UserPrincipal(user, authorities, allPermissions);
    }

    /**
     * 创建普通用户主体
     * 权限从项目角色中获取
     */
    public static UserPrincipal create(User user, Set<String> permissions) {
        Collection<GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_USER")
        );
        return new UserPrincipal(user, authorities, permissions);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    /**
     * 检查用户是否拥有指定权限
     *
     * @param permission 权限键
     * @return true 如果拥有权限，false 否则
     */
    public boolean hasPermission(String permission) {
        if (admin) {
            return true; // 管理员拥有所有权限
        }
        return permissions.contains(permission);
    }
}

