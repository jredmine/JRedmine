package com.github.jredmine.util;

import com.github.jredmine.entity.User;
import com.github.jredmine.enums.ResultCode;
import com.github.jredmine.exception.BusinessException;
import com.github.jredmine.mapper.user.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 安全工具类
 * 用于获取当前登录用户信息和权限检查
 *
 * @author panfeng
 */
@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final UserMapper userMapper;

    /**
     * 获取当前登录用户的用户名
     *
     * @return 用户名
     * @throws BusinessException 如果未认证
     */
    public String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null
                || "anonymousUser".equals(authentication.getName())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "未认证，请先登录");
        }
        return authentication.getName();
    }

    /**
     * 获取当前登录用户ID
     *
     * @return 用户ID
     * @throws BusinessException 如果未认证
     */
    public Long getCurrentUserId() {
        String username = getCurrentUsername();
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getLogin, username);
        queryWrapper.isNull(User::getDeletedAt);
        User user = userMapper.selectOne(queryWrapper);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        return user.getId();
    }

    /**
     * 获取当前登录用户信息
     *
     * @return 用户实体
     * @throws BusinessException 如果未认证或用户不存在
     */
    public User getCurrentUser() {
        String username = getCurrentUsername();
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getLogin, username);
        queryWrapper.isNull(User::getDeletedAt);
        User user = userMapper.selectOne(queryWrapper);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        return user;
    }

    /**
     * 检查当前用户是否是管理员
     *
     * @return true 如果是管理员，false 否则
     */
    public boolean isAdmin() {
        try {
            User user = getCurrentUser();
            return Boolean.TRUE.equals(user.getAdmin());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 检查当前用户是否是管理员，如果不是则抛出异常
     *
     * @throws BusinessException 如果不是管理员
     */
    public void requireAdmin() {
        if (!isAdmin()) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权限，仅管理员可执行此操作");
        }
    }

    /**
     * 检查当前用户是否是管理员或操作的是自己的数据
     *
     * @param targetUserId 目标用户ID
     * @throws BusinessException 如果既不是管理员也不是操作自己的数据
     */
    public void requireAdminOrSelf(Long targetUserId) {
        if (isAdmin()) {
            return;
        }
        Long currentUserId = getCurrentUserId();
        if (!currentUserId.equals(targetUserId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权限，只能操作自己的数据");
        }
    }
}

