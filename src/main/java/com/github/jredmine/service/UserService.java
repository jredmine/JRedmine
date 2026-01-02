package com.github.jredmine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.jredmine.dto.request.user.PasswordChangeRequestDTO;
import com.github.jredmine.dto.request.user.PasswordResetConfirmRequestDTO;
import com.github.jredmine.dto.request.user.PasswordResetRequestDTO;
import com.github.jredmine.dto.request.user.TokenRefreshRequestDTO;
import com.github.jredmine.dto.request.user.UserCreateRequestDTO;
import com.github.jredmine.dto.request.user.UserLoginRequestDTO;
import com.github.jredmine.dto.request.user.UserPreferenceUpdateRequestDTO;
import com.github.jredmine.dto.request.user.UserRegisterRequestDTO;
import com.github.jredmine.dto.request.user.UserStatusUpdateRequestDTO;
import com.github.jredmine.dto.request.user.UserUpdateRequestDTO;
import com.github.jredmine.dto.response.PageResponse;
import com.github.jredmine.dto.response.user.UserDetailResponseDTO;
import com.github.jredmine.dto.response.user.UserListItemResponseDTO;
import com.github.jredmine.dto.response.user.UserLoginResponseDTO;
import com.github.jredmine.dto.response.user.UserPreferenceResponseDTO;
import com.github.jredmine.dto.response.user.UserRegisterResponseDTO;
import com.github.jredmine.entity.EmailAddress;
import com.github.jredmine.entity.Token;
import com.github.jredmine.entity.User;
import com.github.jredmine.entity.UserPreference;
import com.github.jredmine.dto.converter.UserConverter;
import com.github.jredmine.enums.ResultCode;
import com.github.jredmine.enums.UserStatus;
import com.github.jredmine.exception.BusinessException;
import com.github.jredmine.mapper.user.EmailAddressMapper;
import com.github.jredmine.mapper.user.TokenMapper;
import com.github.jredmine.mapper.user.UserMapper;
import com.github.jredmine.mapper.user.UserPreferenceMapper;
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
    private final EmailAddressMapper emailAddressMapper;
    private final UserPreferenceMapper userPreferenceMapper;
    private final TokenMapper tokenMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final EmailService emailService;

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
                    UserStatus.ACTIVE.getCode(), // status: 启用
                    false, // admin: 非管理员
                    "zh-CN", // language
                    "all" // mailNotification
            );

            userMapper.insert(user);

            // 保存邮箱地址到 email_addresses 表
            saveEmailAddress(user.getId(), requestDTO.getEmail(), true);

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

            // 1. 查询用户（排除已删除的用户）
            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(User::getLogin, requestDTO.getLogin());
            queryWrapper.isNull(User::getDeletedAt);
            User user = userMapper.selectOne(queryWrapper);

            if (user == null) {
                log.warn("用户登录失败：用户不存在或已删除");
                throw new BusinessException(ResultCode.USER_NOT_FOUND);
            }

            // 2. 检查用户状态
            if (user.getStatus() == null || !UserStatus.ACTIVE.getCode().equals(user.getStatus())) {
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
                    .email(getUserEmail(user.getId()))
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
            // 设置默认值并验证：current 至少为 1，size 至少为 10
            Integer validCurrent = (current != null && current > 0) ? current : 1;
            Integer validSize = (size != null && size > 0) ? size : 10;

            log.debug("开始查询用户列表，页码: {}, 每页大小: {}, 登录名过滤: {}", validCurrent, validSize, login);

            // 创建分页对象（MyBatis Plus 分页从1开始）
            Page<User> page = new Page<>(validCurrent, validSize);

            // 构建查询条件
            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            // 过滤已删除的用户（软删除）
            queryWrapper.isNull(User::getDeletedAt);
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

            // 查询用户（排除已删除的用户）
            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(User::getId, id);
            queryWrapper.isNull(User::getDeletedAt);
            User user = userMapper.selectOne(queryWrapper);

            if (user == null) {
                log.warn("用户不存在或已删除，用户ID: {}", id);
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
            Integer status = requestDTO.getStatus();
            if (status != null && UserStatus.fromCode(status) == null) {
                log.warn("用户创建失败：状态值无效，状态: {}", status);
                throw new BusinessException(ResultCode.PARAM_ERROR, "用户状态值无效");
            }
            User user = buildUser(
                    requestDTO.getLogin(),
                    requestDTO.getPassword(),
                    requestDTO.getFirstname(),
                    requestDTO.getLastname(),
                    status != null ? status : UserStatus.ACTIVE.getCode(),
                    requestDTO.getAdmin() != null ? requestDTO.getAdmin() : false,
                    requestDTO.getLanguage() != null ? requestDTO.getLanguage() : "zh-CN",
                    requestDTO.getMailNotification() != null ? requestDTO.getMailNotification() : "all");

            userMapper.insert(user);

            // 保存邮箱地址到 email_addresses 表
            saveEmailAddress(user.getId(), requestDTO.getEmail(), true);

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

        // 2. 检查用户名是否已存在（排除已删除的用户，已删除的用户名可以被重新使用）
        LambdaQueryWrapper<User> loginQueryWrapper = new LambdaQueryWrapper<>();
        loginQueryWrapper.eq(User::getLogin, login);
        loginQueryWrapper.isNull(User::getDeletedAt);
        User existsUserByLogin = userMapper.selectOne(loginQueryWrapper);

        if (existsUserByLogin != null) {
            log.warn("用户操作失败：用户名已存在");
            throw new BusinessException(ResultCode.USER_ALREADY_EXISTS);
        }

        // 3. 检查邮箱是否已存在（从 email_addresses 表查询）
        LambdaQueryWrapper<EmailAddress> emailQueryWrapper = new LambdaQueryWrapper<>();
        emailQueryWrapper.eq(EmailAddress::getAddress, email);
        EmailAddress existsEmail = emailAddressMapper.selectOne(emailQueryWrapper);

        if (existsEmail != null) {
            log.warn("用户操作失败：邮箱已被使用");
            throw new BusinessException(ResultCode.PARAM_ERROR, "邮箱已被使用");
        }
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
     * 更新用户信息
     * 
     * @param id         用户ID
     * @param requestDTO 用户更新请求
     * @return 用户详情
     */
    public UserDetailResponseDTO updateUser(Long id, UserUpdateRequestDTO requestDTO) {
        // 使用 MDC 添加上下文信息
        MDC.put("operation", "update_user");
        MDC.put("userId", String.valueOf(id));

        try {
            log.info("开始更新用户信息，用户ID: {}", id);

            // 1. 查询用户是否存在（排除已删除的用户）
            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(User::getId, id);
            queryWrapper.isNull(User::getDeletedAt);
            User user = userMapper.selectOne(queryWrapper);
            if (user == null) {
                log.warn("用户更新失败：用户不存在或已删除，用户ID: {}", id);
                throw new BusinessException(ResultCode.USER_NOT_FOUND);
            }

            // 2. 如果提供了邮箱，验证并更新邮箱
            boolean hasUpdate = false;
            if (requestDTO.getEmail() != null && !requestDTO.getEmail().trim().isEmpty()) {
                if (!EMAIL_PATTERN.matcher(requestDTO.getEmail()).matches()) {
                    log.warn("用户更新失败：邮箱格式不正确，用户ID: {}", id);
                    throw new BusinessException(ResultCode.PARAM_ERROR, "邮箱格式不正确");
                }

                // 检查邮箱是否已被其他用户使用
                LambdaQueryWrapper<EmailAddress> emailQueryWrapper = new LambdaQueryWrapper<>();
                emailQueryWrapper.eq(EmailAddress::getAddress, requestDTO.getEmail());
                emailQueryWrapper.ne(EmailAddress::getUserId, id);
                EmailAddress existsEmail = emailAddressMapper.selectOne(emailQueryWrapper);

                if (existsEmail != null) {
                    log.warn("用户更新失败：邮箱已被其他用户使用，用户ID: {}", id);
                    throw new BusinessException(ResultCode.PARAM_ERROR, "邮箱已被其他用户使用");
                }

                // 更新或创建邮箱地址
                updateOrCreateEmailAddress(id, requestDTO.getEmail());
                hasUpdate = true;
            }

            // 3. 更新用户信息（只更新提供的字段）

            if (requestDTO.getFirstname() != null) {
                user.setFirstname(requestDTO.getFirstname());
                hasUpdate = true;
            }

            if (requestDTO.getLastname() != null) {
                user.setLastname(requestDTO.getLastname());
                hasUpdate = true;
            }

            // email 字段在 users 表中不存在，已通过 email_addresses 表管理（见步骤2）

            if (requestDTO.getAdmin() != null) {
                user.setAdmin(requestDTO.getAdmin());
                hasUpdate = true;
            }

            if (requestDTO.getStatus() != null) {
                user.setStatus(requestDTO.getStatus());
                hasUpdate = true;
            }

            if (requestDTO.getLanguage() != null) {
                user.setLanguage(requestDTO.getLanguage());
                hasUpdate = true;
            }

            if (requestDTO.getMailNotification() != null) {
                user.setMailNotification(requestDTO.getMailNotification());
                hasUpdate = true;
            }

            // 4. 如果有更新，保存到数据库
            if (hasUpdate) {
                user.setUpdatedOn(new Date());
                userMapper.updateById(user);
                log.info("用户信息更新成功，用户ID: {}", id);
            } else {
                log.debug("用户信息无变化，用户ID: {}", id);
            }

            // 转换为响应 DTO
            return UserConverter.INSTANCE.toUserDetailResponseDTO(user);
        } finally {
            // 清理 MDC
            MDC.clear();
        }
    }

    /**
     * 更新用户状态（启用/禁用）
     * 
     * @param id         用户ID
     * @param requestDTO 状态更新请求
     * @return 用户详情
     */
    public UserDetailResponseDTO updateUserStatus(Long id, UserStatusUpdateRequestDTO requestDTO) {
        // 使用 MDC 添加上下文信息
        MDC.put("operation", "update_user_status");
        MDC.put("userId", String.valueOf(id));
        MDC.put("status", String.valueOf(requestDTO.getStatus()));

        try {
            log.info("开始更新用户状态，用户ID: {}, 新状态: {}", id, requestDTO.getStatus());

            // 1. 验证状态值
            Integer status = requestDTO.getStatus();
            UserStatus userStatus = UserStatus.fromCode(status);
            if (status == null || userStatus == null) {
                log.warn("用户状态更新失败：状态值无效，用户ID: {}, 状态: {}", id, status);
                throw new BusinessException(ResultCode.PARAM_ERROR,
                        String.format("用户状态值无效，有效值为：%d(%s)、%d(%s)、%d(%s)",
                                UserStatus.ACTIVE.getCode(), UserStatus.ACTIVE.getDescription(),
                                UserStatus.LOCKED.getCode(), UserStatus.LOCKED.getDescription(),
                                UserStatus.PENDING.getCode(), UserStatus.PENDING.getDescription()));
            }

            // 2. 查询用户是否存在（排除已删除的用户）
            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(User::getId, id);
            queryWrapper.isNull(User::getDeletedAt);
            User user = userMapper.selectOne(queryWrapper);
            if (user == null) {
                log.warn("用户状态更新失败：用户不存在或已删除，用户ID: {}", id);
                throw new BusinessException(ResultCode.USER_NOT_FOUND);
            }

            // 3. 检查状态是否有变化
            if (user.getStatus() != null && user.getStatus().equals(status)) {
                log.debug("用户状态无变化，用户ID: {}, 状态: {}", id, status);
                return UserConverter.INSTANCE.toUserDetailResponseDTO(user);
            }

            // 4. 更新用户状态
            user.setStatus(status);
            user.setUpdatedOn(new Date());
            userMapper.updateById(user);

            log.info("用户状态更新成功，用户ID: {}, 新状态: {}", id, status);

            // 转换为响应 DTO
            return UserConverter.INSTANCE.toUserDetailResponseDTO(user);
        } finally {
            // 清理 MDC
            MDC.clear();
        }
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

            // 根据登录名查询用户（排除已删除的用户）
            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(User::getLogin, username);
            queryWrapper.isNull(User::getDeletedAt);
            User user = userMapper.selectOne(queryWrapper);

            if (user == null) {
                log.warn("用户不存在或已删除，用户名: {}", username);
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

            // 3. 验证用户是否存在且状态正常（排除已删除的用户）
            LambdaQueryWrapper<User> userQueryWrapper = new LambdaQueryWrapper<>();
            userQueryWrapper.eq(User::getId, userId);
            userQueryWrapper.isNull(User::getDeletedAt);
            User user = userMapper.selectOne(userQueryWrapper);
            if (user == null) {
                log.warn("Token刷新失败：用户不存在或已删除，用户ID: {}", userId);
                throw new BusinessException(ResultCode.USER_NOT_FOUND);
            }

            // 验证用户名是否匹配（防止Token被篡改）
            if (!user.getLogin().equals(username)) {
                log.warn("Token刷新失败：用户名不匹配");
                throw new BusinessException(ResultCode.UNAUTHORIZED, "Token无效");
            }

            // 4. 检查用户状态
            if (user.getStatus() == null || !UserStatus.ACTIVE.getCode().equals(user.getStatus())) {
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
                    .email(getUserEmail(user.getId()))
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

            // 3. 查询用户（排除已删除的用户）
            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(User::getLogin, username);
            queryWrapper.isNull(User::getDeletedAt);
            User user = userMapper.selectOne(queryWrapper);

            if (user == null) {
                log.warn("密码变更失败：用户不存在或已删除，用户名: {}", username);
                throw new BusinessException(ResultCode.USER_NOT_FOUND);
            }

            // 4. 验证旧密码是否正确
            if (!passwordEncoder.matches(requestDTO.getOldPassword(), user.getHashedPassword())) {
                log.warn("密码变更失败：旧密码错误，用户ID: {}", user.getId());
                throw new BusinessException(ResultCode.PASSWORD_ERROR, "旧密码错误");
            }

            // 5. 检查用户状态
            if (user.getStatus() == null || !UserStatus.ACTIVE.getCode().equals(user.getStatus())) {
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

    /**
     * 软删除用户
     * 
     * @param id 用户ID
     */
    public void deleteUser(Long id) {
        // 使用 MDC 添加上下文信息
        MDC.put("operation", "delete_user");
        MDC.put("userId", String.valueOf(id));

        try {
            log.info("开始软删除用户，用户ID: {}", id);

            // 1. 查询用户是否存在（排除已删除的用户）
            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(User::getId, id);
            queryWrapper.isNull(User::getDeletedAt);
            User user = userMapper.selectOne(queryWrapper);

            if (user == null) {
                log.warn("用户删除失败：用户不存在或已删除，用户ID: {}", id);
                throw new BusinessException(ResultCode.USER_NOT_FOUND);
            }

            // 2. 执行软删除（设置删除时间）
            user.setDeletedAt(new Date());
            user.setUpdatedOn(new Date());
            userMapper.updateById(user);

            log.info("用户软删除成功，用户ID: {}", id);
        } finally {
            // 清理 MDC
            MDC.clear();
        }
    }

    /**
     * 保存邮箱地址到 email_addresses 表
     * 
     * @param userId    用户ID
     * @param email     邮箱地址
     * @param isDefault 是否默认邮箱
     */
    private void saveEmailAddress(Long userId, String email, Boolean isDefault) {
        EmailAddress emailAddress = new EmailAddress();
        emailAddress.setUserId(userId);
        emailAddress.setAddress(email);
        emailAddress.setIsDefault(isDefault != null ? isDefault : true);
        emailAddress.setNotify(true);
        emailAddress.setCreatedOn(new Date());
        emailAddress.setUpdatedOn(new Date());
        emailAddressMapper.insert(emailAddress);
        log.debug("邮箱地址保存成功，用户ID: {}, 邮箱: {}", userId, email);
    }

    /**
     * 更新或创建邮箱地址
     * 如果用户已有默认邮箱，则更新；否则创建新邮箱
     * 
     * @param userId 用户ID
     * @param email  邮箱地址
     */
    private void updateOrCreateEmailAddress(Long userId, String email) {
        // 查询用户是否已有默认邮箱
        LambdaQueryWrapper<EmailAddress> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(EmailAddress::getUserId, userId);
        queryWrapper.eq(EmailAddress::getIsDefault, true);
        EmailAddress existingEmail = emailAddressMapper.selectOne(queryWrapper);

        if (existingEmail != null) {
            // 更新现有默认邮箱
            existingEmail.setAddress(email);
            existingEmail.setUpdatedOn(new Date());
            emailAddressMapper.updateById(existingEmail);
            log.debug("邮箱地址更新成功，用户ID: {}, 新邮箱: {}", userId, email);
        } else {
            // 创建新邮箱（设为默认）
            saveEmailAddress(userId, email, true);
        }
    }

    /**
     * 获取用户的默认邮箱地址
     * 
     * @param userId 用户ID
     * @return 邮箱地址，如果不存在则返回空字符串
     */
    private String getUserEmail(Long userId) {
        LambdaQueryWrapper<EmailAddress> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(EmailAddress::getUserId, userId);
        queryWrapper.eq(EmailAddress::getIsDefault, true);
        EmailAddress emailAddress = emailAddressMapper.selectOne(queryWrapper);
        return emailAddress != null ? emailAddress.getAddress() : "";
    }

    /**
     * 获取用户偏好设置
     * 
     * @param userId 用户ID
     * @return 用户偏好设置
     */
    public UserPreferenceResponseDTO getUserPreference(Long userId) {
        // 使用 MDC 添加上下文信息
        MDC.put("operation", "get_user_preference");
        MDC.put("userId", String.valueOf(userId));

        try {
            log.debug("开始查询用户偏好设置，用户ID: {}", userId);

            // 验证用户是否存在（排除已删除的用户）
            LambdaQueryWrapper<User> userQueryWrapper = new LambdaQueryWrapper<>();
            userQueryWrapper.eq(User::getId, userId);
            userQueryWrapper.isNull(User::getDeletedAt);
            User user = userMapper.selectOne(userQueryWrapper);

            if (user == null) {
                log.warn("用户偏好设置查询失败：用户不存在或已删除，用户ID: {}", userId);
                throw new BusinessException(ResultCode.USER_NOT_FOUND);
            }

            // 查询用户偏好设置
            LambdaQueryWrapper<UserPreference> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(UserPreference::getUserId, userId);
            UserPreference preference = userPreferenceMapper.selectOne(queryWrapper);

            // 如果不存在，返回默认值
            if (preference == null) {
                UserPreferenceResponseDTO response = new UserPreferenceResponseDTO();
                response.setUserId(userId);
                response.setHideMail(true); // 默认隐藏邮箱
                response.setTimeZone(null);
                response.setOthers(null);
                log.debug("用户偏好设置不存在，返回默认值，用户ID: {}", userId);
                return response;
            }

            // 转换为响应 DTO
            UserPreferenceResponseDTO response = new UserPreferenceResponseDTO();
            response.setUserId(preference.getUserId());
            response.setHideMail(preference.getHideMail());
            response.setTimeZone(preference.getTimeZone());
            response.setOthers(preference.getOthers());

            log.info("用户偏好设置查询成功，用户ID: {}", userId);
            return response;
        } finally {
            // 清理 MDC
            MDC.clear();
        }
    }

    /**
     * 更新用户偏好设置
     * 
     * @param userId     用户ID
     * @param requestDTO 偏好设置更新请求
     * @return 更新后的用户偏好设置
     */
    public UserPreferenceResponseDTO updateUserPreference(Long userId, UserPreferenceUpdateRequestDTO requestDTO) {
        // 使用 MDC 添加上下文信息
        MDC.put("operation", "update_user_preference");
        MDC.put("userId", String.valueOf(userId));

        try {
            log.info("开始更新用户偏好设置，用户ID: {}", userId);

            // 1. 验证用户是否存在（排除已删除的用户）
            LambdaQueryWrapper<User> userQueryWrapper = new LambdaQueryWrapper<>();
            userQueryWrapper.eq(User::getId, userId);
            userQueryWrapper.isNull(User::getDeletedAt);
            User user = userMapper.selectOne(userQueryWrapper);

            if (user == null) {
                log.warn("用户偏好设置更新失败：用户不存在或已删除，用户ID: {}", userId);
                throw new BusinessException(ResultCode.USER_NOT_FOUND);
            }

            // 2. 查询是否已存在偏好设置
            LambdaQueryWrapper<UserPreference> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(UserPreference::getUserId, userId);
            UserPreference preference = userPreferenceMapper.selectOne(queryWrapper);

            if (preference == null) {
                // 创建新的偏好设置
                preference = new UserPreference();
                preference.setUserId(userId);
                preference.setHideMail(requestDTO.getHideMail() != null ? requestDTO.getHideMail() : true);
                preference.setTimeZone(requestDTO.getTimeZone());
                preference.setOthers(requestDTO.getOthers());
                userPreferenceMapper.insert(preference);
                log.info("用户偏好设置创建成功，用户ID: {}", userId);
            } else {
                // 更新现有偏好设置（只更新提供的字段）
                boolean hasUpdate = false;

                if (requestDTO.getHideMail() != null) {
                    preference.setHideMail(requestDTO.getHideMail());
                    hasUpdate = true;
                }

                if (requestDTO.getTimeZone() != null) {
                    preference.setTimeZone(requestDTO.getTimeZone());
                    hasUpdate = true;
                }

                if (requestDTO.getOthers() != null) {
                    preference.setOthers(requestDTO.getOthers());
                    hasUpdate = true;
                }

                if (hasUpdate) {
                    userPreferenceMapper.updateById(preference);
                    log.info("用户偏好设置更新成功，用户ID: {}", userId);
                } else {
                    log.debug("用户偏好设置无变化，用户ID: {}", userId);
                }
            }

            // 转换为响应 DTO
            UserPreferenceResponseDTO response = new UserPreferenceResponseDTO();
            response.setUserId(preference.getUserId());
            response.setHideMail(preference.getHideMail());
            response.setTimeZone(preference.getTimeZone());
            response.setOthers(preference.getOthers());

            return response;
        } finally {
            // 清理 MDC
            MDC.clear();
        }
    }

    /**
     * 请求密码重置
     * 生成重置Token并发送邮件
     * 
     * @param requestDTO 密码重置请求
     */
    public void requestPasswordReset(PasswordResetRequestDTO requestDTO) {
        // 使用 MDC 添加上下文信息
        MDC.put("operation", "request_password_reset");

        try {
            log.info("开始处理密码重置请求，邮箱: {}", requestDTO.getEmail());

            // 1. 根据邮箱查找用户
            LambdaQueryWrapper<EmailAddress> emailQueryWrapper = new LambdaQueryWrapper<>();
            emailQueryWrapper.eq(EmailAddress::getAddress, requestDTO.getEmail());
            emailQueryWrapper.eq(EmailAddress::getIsDefault, true);
            EmailAddress emailAddress = emailAddressMapper.selectOne(emailQueryWrapper);

            if (emailAddress == null) {
                // 为了安全，即使邮箱不存在也返回成功（防止邮箱枚举攻击）
                log.warn("密码重置请求：邮箱不存在，邮箱: {}", requestDTO.getEmail());
                return;
            }

            Long userId = emailAddress.getUserId();

            // 2. 验证用户是否存在且未删除
            LambdaQueryWrapper<User> userQueryWrapper = new LambdaQueryWrapper<>();
            userQueryWrapper.eq(User::getId, userId);
            userQueryWrapper.isNull(User::getDeletedAt);
            User user = userMapper.selectOne(userQueryWrapper);

            if (user == null) {
                log.warn("密码重置请求：用户不存在或已删除，用户ID: {}", userId);
                return;
            }

            // 3. 删除该用户之前的密码重置Token（防止重复使用）
            LambdaQueryWrapper<Token> tokenQueryWrapper = new LambdaQueryWrapper<>();
            tokenQueryWrapper.eq(Token::getUserId, userId);
            tokenQueryWrapper.eq(Token::getAction, "password_reset");
            tokenMapper.delete(tokenQueryWrapper);

            // 4. 生成新的重置Token（使用UUID）
            String resetToken = java.util.UUID.randomUUID().toString().replace("-", "");

            // 5. 保存Token到数据库（有效期1小时）
            Token token = new Token();
            token.setUserId(userId);
            token.setAction("password_reset");
            token.setValue(resetToken);
            token.setCreatedOn(new Date());
            token.setUpdatedOn(new Date());
            tokenMapper.insert(token);

            // 6. 发送重置邮件
            String username = user.getFirstname() + " " + user.getLastname();
            emailService.sendPasswordResetEmail(requestDTO.getEmail(), username, resetToken);

            log.info("密码重置请求处理成功，用户ID: {}, 邮箱: {}", userId, requestDTO.getEmail());
        } catch (Exception e) {
            log.error("密码重置请求处理失败，邮箱: {}", requestDTO.getEmail(), e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "密码重置请求处理失败");
        } finally {
            // 清理 MDC
            MDC.clear();
        }
    }

    /**
     * 确认密码重置
     * 验证Token并重置密码
     * 
     * @param requestDTO 密码重置确认请求
     */
    public void confirmPasswordReset(PasswordResetConfirmRequestDTO requestDTO) {
        // 使用 MDC 添加上下文信息
        MDC.put("operation", "confirm_password_reset");

        try {
            log.info("开始处理密码重置确认，Token: {}", requestDTO.getToken());

            // 1. 验证新密码和确认密码是否一致
            if (!requestDTO.getNewPassword().equals(requestDTO.getConfirmPassword())) {
                log.warn("密码重置确认失败：新密码和确认密码不一致");
                throw new BusinessException(ResultCode.PARAM_INVALID, "新密码和确认密码不一致");
            }

            // 2. 查找Token
            LambdaQueryWrapper<Token> tokenQueryWrapper = new LambdaQueryWrapper<>();
            tokenQueryWrapper.eq(Token::getValue, requestDTO.getToken());
            tokenQueryWrapper.eq(Token::getAction, "password_reset");
            Token token = tokenMapper.selectOne(tokenQueryWrapper);

            if (token == null) {
                log.warn("密码重置确认失败：Token不存在或已失效，Token: {}", requestDTO.getToken());
                throw new BusinessException(ResultCode.PARAM_INVALID, "重置Token无效或已过期");
            }

            // 3. 验证Token是否过期（1小时有效期）
            long tokenAge = System.currentTimeMillis() - token.getCreatedOn().getTime();
            long oneHour = 60 * 60 * 1000; // 1小时（毫秒）

            if (tokenAge > oneHour) {
                log.warn("密码重置确认失败：Token已过期，Token: {}, 创建时间: {}",
                        requestDTO.getToken(), token.getCreatedOn());
                // 删除过期Token
                tokenMapper.deleteById(token.getId());
                throw new BusinessException(ResultCode.PARAM_INVALID, "重置Token已过期，请重新申请");
            }

            // 4. 验证用户是否存在且未删除
            LambdaQueryWrapper<User> userQueryWrapper = new LambdaQueryWrapper<>();
            userQueryWrapper.eq(User::getId, token.getUserId());
            userQueryWrapper.isNull(User::getDeletedAt);
            User user = userMapper.selectOne(userQueryWrapper);

            if (user == null) {
                log.warn("密码重置确认失败：用户不存在或已删除，用户ID: {}", token.getUserId());
                // 删除无效Token
                tokenMapper.deleteById(token.getId());
                throw new BusinessException(ResultCode.USER_NOT_FOUND);
            }

            // 5. 更新密码
            String encodedPassword = passwordEncoder.encode(requestDTO.getNewPassword());
            user.setHashedPassword(encodedPassword);
            user.setPasswdChangedOn(new Date());
            user.setMustChangePasswd(false); // 重置后不需要强制修改密码
            user.setUpdatedOn(new Date());
            userMapper.updateById(user);

            // 6. 删除已使用的Token
            tokenMapper.deleteById(token.getId());

            log.info("密码重置确认成功，用户ID: {}", token.getUserId());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("密码重置确认处理失败，Token: {}", requestDTO.getToken(), e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "密码重置确认处理失败");
        } finally {
            // 清理 MDC
            MDC.clear();
        }
    }
}
