package com.github.jredmine.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.jredmine.entity.User;
import com.github.jredmine.mapper.user.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * 自定义 UserDetailsService
 * 从数据库加载用户信息和权限
 *
 * @author panfeng
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserMapper userMapper;
    private final ProjectPermissionService projectPermissionService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 查询用户
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getLogin, username);
        queryWrapper.isNull(User::getDeletedAt);
        User user = userMapper.selectOne(queryWrapper);

        if (user == null) {
            log.warn("用户不存在: {}", username);
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        // 如果是管理员，直接返回管理员用户主体
        if (Boolean.TRUE.equals(user.getAdmin())) {
            return UserPrincipal.createAdmin(user);
        }

        // 加载用户的所有权限（所有项目的权限并集）
        // 注意：实际权限检查应该在项目级别进行
        var permissions = projectPermissionService.getUserAllPermissions(user.getId());

        return UserPrincipal.create(user, permissions);
    }

    /**
     * 根据用户ID加载用户信息
     *
     * @param userId 用户ID
     * @return UserDetails
     */
    public UserDetails loadUserById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            log.warn("用户不存在: {}", userId);
            throw new UsernameNotFoundException("用户不存在: " + userId);
        }

        // 如果是管理员，直接返回管理员用户主体
        if (Boolean.TRUE.equals(user.getAdmin())) {
            return UserPrincipal.createAdmin(user);
        }

        // 加载用户的所有权限
        var permissions = projectPermissionService.getUserAllPermissions(user.getId());

        return UserPrincipal.create(user, permissions);
    }
}

