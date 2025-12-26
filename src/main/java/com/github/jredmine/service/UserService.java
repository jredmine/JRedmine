package com.github.jredmine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.jredmine.dto.request.user.PasswordChangeRequestDTO;
import com.github.jredmine.dto.request.user.TokenRefreshRequestDTO;
import com.github.jredmine.dto.request.user.UserCreateRequestDTO;
import com.github.jredmine.dto.request.user.UserLoginRequestDTO;
import com.github.jredmine.dto.request.user.UserRegisterRequestDTO;
import com.github.jredmine.dto.response.PageResponse;
import com.github.jredmine.dto.response.user.UserDetailResponseDTO;
import com.github.jredmine.dto.response.user.UserListItemResponseDTO;
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
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

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

            // 2. 验证并检查用户信息
            validateAndCheckUserInfo(requestDTO.getLogin(), requestDTO.getEmail());

            // 3. 创建用户（注册用户使用固定默认值）
            User user = buildUser(
                    requestDTO.getLogin(),
                    requestDTO.getPassword(),
                    requestDTO.getFirstname(),
                    requestDTO.getLastname(),
                    1, // status: 启用
                    false, // admin: 非管理员
                    "zh-CN", // language
                    "all" // mailNotification
            );

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
     * @param size    每页大小
     * @param login   登录名（可选，用于模糊查询）
     * @return 分页响应
     */
    public PageResponse<UserListItemResponseDTO> listUsers(Integer current, Integer size, String login) {
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
            // 按ID倒序（id是自增主键，按id排序性能最优，结果与按创建时间排序一致）
            queryWrapper.orderByDesc(User::getId);

            // 执行分页查询
            Page<User> result = userMapper.selectPage(page, queryWrapper);

            // 添加查询结果到上下文
            MDC.put("total", String.valueOf(result.getTotal()));
            log.info("用户列表查询成功，共查询到 {} 条记录", result.getTotal());

            // 转换为响应 DTO
            return PageResponse.of(
                    result.getRecords().stream()
                            .map(UserConverter.INSTANCE::toUserListItemResponseDTO)
                            .toList(),
                    (int) result.getTotal(),
                    (int) result.getCurrent(),
                    (int) result.getSize());
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

    /**
     * 创建新用户（管理员功能）
     * 
     * @param requestDTO 用户创建请求
     * @return 用户详情
     */
    public UserDetailResponseDTO createUser(UserCreateRequestDTO requestDTO) {
        // 使用 MDC 添加上下文信息
        MDC.put("operation", "create_user");
        MDC.put("login", requestDTO.getLogin());

        try {
            log.info("开始创建用户流程");

            // 1. 验证并检查用户信息
            validateAndCheckUserInfo(requestDTO.getLogin(), requestDTO.getEmail());

            // 2. 创建用户（管理员创建可以自定义参数）
            User user = buildUser(
                    requestDTO.getLogin(),
                    requestDTO.getPassword(),
                    requestDTO.getFirstname(),
                    requestDTO.getLastname(),
                    requestDTO.getStatus() != null ? requestDTO.getStatus() : 1,
                    requestDTO.getAdmin() != null ? requestDTO.getAdmin() : false,
                    requestDTO.getLanguage() != null ? requestDTO.getLanguage() : "zh-CN",
                    requestDTO.getMailNotification() != null ? requestDTO.getMailNotification() : "all");

            userMapper.insert(user);

            // 添加用户ID到上下文
            MDC.put("userId", String.valueOf(user.getId()));
            log.info("用户创建成功，用户ID: {}", user.getId());

            // 转换为响应 DTO
            return UserConverter.INSTANCE.toUserDetailResponseDTO(user);
        } finally {
            // 清理 MDC
            MDC.clear();
        }
    }

    /**
     * 验证并检查用户信息（公共方法）
     * 验证邮箱格式、检查用户名和邮箱是否已存在
     * 
     * @param login 登录名
     * @param email 邮箱
     */
    private void validateAndCheckUserInfo(String login, String email) {
        // 1. 验证邮箱格式
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            log.warn("用户操作失败：邮箱格式不正确");
            throw new BusinessException(ResultCode.PARAM_ERROR, "邮箱格式不正确");
        }

        // 2. 检查用户名是否已存在
        LambdaQueryWrapper<User> loginQueryWrapper = new LambdaQueryWrapper<>();
        loginQueryWrapper.eq(User::getLogin, login);
        User existsUserByLogin = userMapper.selectOne(loginQueryWrapper);

        if (existsUserByLogin != null) {
            log.warn("用户操作失败：用户名已存在");
            throw new BusinessException(ResultCode.USER_ALREADY_EXISTS);
        }

        // 3. 检查邮箱是否已存在（TODO: 需要email_addresses表支持，暂时跳过）
        // TODO: 实现邮箱唯一性检查
    }

    /**
     * 构建用户对象（公共方法）
     * 
     * @param login            登录名
     * @param password         密码（明文）
     * @param firstname        名字
     * @param lastname         姓氏
     * @param status           用户状态
     * @param admin            是否管理员
     * @param language         语言设置
     * @param mailNotification 邮件通知设置
     * @return 用户对象
     */
    private User buildUser(String login, String password, String firstname, String lastname,
            Integer status, Boolean admin, String language, String mailNotification) {
        User user = new User();
        user.setLogin(login);
        // 使用BCrypt加密密码
        user.setHashedPassword(passwordEncoder.encode(password));
        user.setFirstname(firstname);
        user.setLastname(lastname);
        user.setStatus(status);
        user.setAdmin(admin);
        user.setLanguage(language);
        user.setMailNotification(mailNotification);
        user.setCreatedOn(new Date());
        user.setUpdatedOn(new Date());
        return user;
    }

    /**
     * 获取当前登录用户信息
     * 根据用户名查询用户详情
     * 
     * @param username 用户名（登录名）
     * @return 用户详情
     */
    public UserDetailResponseDTO getCurrentUser(String username) {
        // 使用 MDC 添加上下文信息
        MDC.put("operation", "get_current_user");
        MDC.put("username", username);

        try {
            log.debug("开始查询当前用户信息，用户名: {}", username);

            // 根据登录名查询用户
            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(User::getLogin, username);
            User user = userMapper.selectOne(queryWrapper);

            if (user == null) {
                log.warn("用户不存在，用户名: {}", username);
                throw new BusinessException(ResultCode.USER_NOT_FOUND);
            }

            MDC.put("userId", String.valueOf(user.getId()));
            log.info("当前用户信息查询成功，用户ID: {}", user.getId());

            // 转换为响应 DTO
            return UserConverter.INSTANCE.toUserDetailResponseDTO(user);
        } finally {
            // 清理 MDC
            MDC.clear();
        }
    }

    /**
     * 刷新JWT Token
     * 
     * @param requestDTO Token刷新请求
     * @return 新的Token和用户信息
     */
    public UserLoginResponseDTO refreshToken(TokenRefreshRequestDTO requestDTO) {
        // 使用 MDC 添加上下文信息
        MDC.put("operation", "refresh_token");

        try {
            log.info("开始刷新Token流程");

            String token = requestDTO.getToken();

            // 1. 验证Token是否有效（即使快过期也可以，只要还没过期）
            if (!jwtUtils.validateToken(token)) {
                log.warn("Token刷新失败：Token无效或已过期");
                throw new BusinessException(ResultCode.UNAUTHORIZED, "Token无效或已过期");
            }

            // 2. 从Token中提取用户信息
            String username = jwtUtils.extractUsername(token);
            Long userId = jwtUtils.extractUserId(token);

            if (username == null || userId == null) {
                log.warn("Token刷新失败：无法从Token中提取用户信息");
                throw new BusinessException(ResultCode.UNAUTHORIZED, "Token格式错误");
            }

            MDC.put("username", username);
            MDC.put("userId", String.valueOf(userId));

            // 3. 验证用户是否存在且状态正常
            User user = userMapper.selectById(userId);
            if (user == null) {
                log.warn("Token刷新失败：用户不存在，用户ID: {}", userId);
                throw new BusinessException(ResultCode.USER_NOT_FOUND);
            }

            // 验证用户名是否匹配（防止Token被篡改）
            if (!user.getLogin().equals(username)) {
                log.warn("Token刷新失败：用户名不匹配");
                throw new BusinessException(ResultCode.UNAUTHORIZED, "Token无效");
            }

            // 4. 检查用户状态
            if (user.getStatus() == null || user.getStatus() != 1) {
                log.warn("Token刷新失败：用户账号已被禁用，用户ID: {}", userId);
                throw new BusinessException(ResultCode.FORBIDDEN, "用户账号已被禁用");
            }

            // 5. 生成新的JWT Token
            String newToken = jwtUtils.generateToken(user.getLogin(), user.getId());
            long expiresIn = jwtUtils.extractExpiration(newToken).getTime() - System.currentTimeMillis();

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
                    .token(newToken)
                    .tokenType("Bearer")
                    .expiresIn(expiresIn / 1000) // 转换为秒
                    .user(userInfo)
                    .build();

            log.info("Token刷新成功，用户ID: {}", user.getId());

            return response;
        } finally {
            // 清理 MDC
            MDC.clear();
        }
    }

    /**
     * 用户变更密码
     * 
     * @param username   用户名（登录名）
     * @param requestDTO 密码变更请求
     */
    public void changePassword(String username, PasswordChangeRequestDTO requestDTO) {
        // 使用 MDC 添加上下文信息
        MDC.put("operation", "change_password");
        MDC.put("username", username);

        try {
            log.info("开始用户变更密码流程，用户名: {}", username);

            // 1. 验证新密码和确认密码是否一致
            if (!requestDTO.getNewPassword().equals(requestDTO.getConfirmNewPassword())) {
                log.warn("密码变更失败：新密码和确认密码不匹配");
                throw new BusinessException(ResultCode.PARAM_ERROR, "新密码和确认密码不匹配");
            }

            // 2. 验证新密码和旧密码不能相同
            if (requestDTO.getOldPassword().equals(requestDTO.getNewPassword())) {
                log.warn("密码变更失败：新密码不能与旧密码相同");
                throw new BusinessException(ResultCode.PARAM_ERROR, "新密码不能与旧密码相同");
            }

            // 3. 查询用户
            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(User::getLogin, username);
            User user = userMapper.selectOne(queryWrapper);

            if (user == null) {
                log.warn("密码变更失败：用户不存在，用户名: {}", username);
                throw new BusinessException(ResultCode.USER_NOT_FOUND);
            }

            // 4. 验证旧密码是否正确
            if (!passwordEncoder.matches(requestDTO.getOldPassword(), user.getHashedPassword())) {
                log.warn("密码变更失败：旧密码错误，用户ID: {}", user.getId());
                throw new BusinessException(ResultCode.PASSWORD_ERROR, "旧密码错误");
            }

            // 5. 检查用户状态
            if (user.getStatus() == null || user.getStatus() != 1) {
                log.warn("密码变更失败：用户账号已被禁用，用户ID: {}", user.getId());
                throw new BusinessException(ResultCode.FORBIDDEN, "用户账号已被禁用");
            }

            // 6. 更新密码
            user.setHashedPassword(passwordEncoder.encode(requestDTO.getNewPassword()));
            user.setUpdatedOn(new Date());
            userMapper.updateById(user);

            MDC.put("userId", String.valueOf(user.getId()));
            log.info("密码变更成功，用户ID: {}", user.getId());
        } finally {
            // 清理 MDC
            MDC.clear();
        }
    }
}
