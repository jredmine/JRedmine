package com.github.jredmine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.jredmine.dto.request.user.UserLoginRequestDTO;
import com.github.jredmine.dto.request.user.UserRegisterRequestDTO;
import com.github.jredmine.dto.response.PageResponse;
import com.github.jredmine.dto.response.user.UserDetailResponseDTO;
import com.github.jredmine.dto.response.user.UserLoginResponseDTO;
import com.github.jredmine.dto.response.user.UserRegisterResponseDTO;
import com.github.jredmine.entity.User;
import com.github.jredmine.dto.converter.UserConverter;
import com.github.jredmine.enums.ResultCode;
import com.github.jredmine.exception.BusinessException;
import com.github.jredmine.mapper.user.UserMapper;
import com.github.jredmine.util.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    // 邮箱格式验证正则表达式
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    /**
     * 用户注册
     */
    public UserRegisterResponseDTO register(UserRegisterRequestDTO requestDTO) {
        // 使用 MDC 添加上下文信息（用于结构化日志）
        MDC.put("operation", "user_register");
        MDC.put("login", requestDTO.getLogin());
        
        try {
            log.info("开始用户注册流程");
            
            // 1. 验证密码确认
            if (!requestDTO.getPassword().equals(requestDTO.getConfirmPassword())) {
                log.warn("用户注册失败：密码和确认密码不匹配");
                throw new BusinessException(ResultCode.PARAM_ERROR, "密码和确认密码不匹配");
            }

            // 2. 验证邮箱格式
            if (!EMAIL_PATTERN.matcher(requestDTO.getEmail()).matches()) {
                log.warn("用户注册失败：邮箱格式不正确");
                throw new BusinessException(ResultCode.PARAM_ERROR, "邮箱格式不正确");
            }

            // 3. 检查用户名是否已存在
            LambdaQueryWrapper<User> loginQueryWrapper = new LambdaQueryWrapper<>();
            loginQueryWrapper.eq(User::getLogin, requestDTO.getLogin());
            User existsUserByLogin = userMapper.selectOne(loginQueryWrapper);
            
            if (existsUserByLogin != null) {
                log.warn("用户注册失败：用户名已存在");
                throw new BusinessException(ResultCode.USER_ALREADY_EXISTS);
            }

            // 4. 检查邮箱是否已存在（TODO: 需要email_addresses表支持，暂时跳过）
            // TODO: 实现邮箱唯一性检查

            // 5. 创建用户并加密密码
            User user = new User();
            user.setLogin(requestDTO.getLogin());
            // 使用BCrypt加密密码
            user.setHashedPassword(passwordEncoder.encode(requestDTO.getPassword()));
            user.setFirstname(requestDTO.getFirstname());
            user.setLastname(requestDTO.getLastname());
            user.setStatus(1); // 默认启用状态
            user.setAdmin(false); // 默认非管理员
            user.setLanguage("zh-CN"); // 默认语言
            user.setMailNotification("all"); // 默认邮件通知设置
            user.setCreatedOn(new Date());
            user.setUpdatedOn(new Date());
            
            userMapper.insert(user);

            // 添加用户ID到上下文
            MDC.put("userId", String.valueOf(user.getId()));
            log.info("用户注册成功，用户ID: {}", user.getId());

            return UserConverter.INSTANCE.toUserRegisterResponseDTO(user);
        } finally {
            // 清理 MDC（重要：避免内存泄漏和上下文污染）
            MDC.clear();
        }
    }

    /**
     * 用户登录
     */
    public UserLoginResponseDTO login(UserLoginRequestDTO requestDTO) {
        // 使用 MDC 添加上下文信息
        MDC.put("operation", "user_login");
        MDC.put("login", requestDTO.getLogin());
        
        try {
            log.info("开始用户登录流程");
            
            // 1. 查询用户
            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(User::getLogin, requestDTO.getLogin());
            User user = userMapper.selectOne(queryWrapper);
            
            if (user == null) {
                log.warn("用户登录失败：用户不存在");
                throw new BusinessException(ResultCode.USER_NOT_FOUND);
            }

            // 2. 检查用户状态
            if (user.getStatus() == null || user.getStatus() != 1) {
                log.warn("用户登录失败：用户账号已被禁用");
                throw new BusinessException(ResultCode.FORBIDDEN, "用户账号已被禁用");
            }

            // 3. 验证密码
            if (!passwordEncoder.matches(requestDTO.getPassword(), user.getHashedPassword())) {
                log.warn("用户登录失败：密码错误");
                throw new BusinessException(ResultCode.PASSWORD_ERROR);
            }

            // 4. 更新最后登录时间
            user.setLastLoginOn(new Date());
            user.setUpdatedOn(new Date());
            userMapper.updateById(user);

            // 5. 生成JWT Token
            String token = jwtUtils.generateToken(user.getLogin(), user.getId());
            long expiresIn = jwtUtils.extractExpiration(token).getTime() - System.currentTimeMillis();

            // 6. 构建响应
            UserLoginResponseDTO.UserInfo userInfo = UserLoginResponseDTO.UserInfo.builder()
                    .id(user.getId())
                    .login(user.getLogin())
                    .firstname(user.getFirstname())
                    .lastname(user.getLastname())
                    .email("") // TODO: 从email_addresses表查询
                    .admin(user.getAdmin())
                    .status(user.getStatus())
                    .build();

            UserLoginResponseDTO response = UserLoginResponseDTO.builder()
                    .token(token)
                    .tokenType("Bearer")
                    .expiresIn(expiresIn / 1000) // 转换为秒
                    .user(userInfo)
                    .build();

            MDC.put("userId", String.valueOf(user.getId()));
            log.info("用户登录成功，用户ID: {}", user.getId());

            return response;
        } finally {
            // 清理 MDC
            MDC.clear();
        }
    }

    /**
     * 分页查询用户列表
     * 
     * @param current 当前页码（从1开始）
     * @param size 每页大小
     * @param login 登录名（可选，用于模糊查询）
     * @return 分页响应
     */
    public PageResponse<UserRegisterResponseDTO> listUsers(Integer current, Integer size, String login) {
        // 使用 MDC 添加上下文信息
        MDC.put("operation", "list_users");
        MDC.put("page", String.valueOf(current));
        MDC.put("size", String.valueOf(size));
        
        try {
            log.debug("开始查询用户列表，页码: {}, 每页大小: {}, 登录名过滤: {}", current, size, login);
            
            // 创建分页对象（MyBatis Plus 分页从1开始）
            Page<User> page = new Page<>(current, size);
            
            // 构建查询条件
            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            if (login != null && !login.trim().isEmpty()) {
                queryWrapper.like(User::getLogin, login);
            }
            // 按创建时间倒序
            queryWrapper.orderByDesc(User::getId);
            
            // 执行分页查询
            Page<User> result = userMapper.selectPage(page, queryWrapper);
            
            // 添加查询结果到上下文
            MDC.put("total", String.valueOf(result.getTotal()));
            log.info("用户列表查询成功，共查询到 {} 条记录", result.getTotal());
            
            // 转换为响应 DTO
            return PageResponse.of(
                result.getRecords().stream()
                    .map(UserConverter.INSTANCE::toUserRegisterResponseDTO)
                    .toList(),
                (int) result.getTotal(),
                (int) result.getCurrent(),
                (int) result.getSize()
            );
        } finally {
            // 清理 MDC
            MDC.clear();
        }
    }

    /**
     * 根据ID查询用户详情
     * 
     * @param id 用户ID
     * @return 用户详情
     */
    public UserDetailResponseDTO getUserById(Long id) {
        // 使用 MDC 添加上下文信息
        MDC.put("operation", "get_user_by_id");
        MDC.put("userId", String.valueOf(id));
        
        try {
            log.debug("开始查询用户详情，用户ID: {}", id);
            
            // 查询用户
            User user = userMapper.selectById(id);
            
            if (user == null) {
                log.warn("用户不存在，用户ID: {}", id);
                throw new BusinessException(ResultCode.USER_NOT_FOUND);
            }
            
            log.info("用户详情查询成功，用户ID: {}", id);
            
            // 转换为响应 DTO
            return UserConverter.INSTANCE.toUserDetailResponseDTO(user);
        } finally {
            // 清理 MDC
            MDC.clear();
        }
    }
}
