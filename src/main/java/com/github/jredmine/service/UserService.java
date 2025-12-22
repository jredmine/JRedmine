package com.github.jredmine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.jredmine.dto.request.user.UserRegisterRequestDTO;
import com.github.jredmine.dto.response.PageResponse;
import com.github.jredmine.dto.response.user.UserRegisterResponseDTO;
import com.github.jredmine.entity.User;
import com.github.jredmine.dto.converter.UserConverter;
import com.github.jredmine.enums.ResultCode;
import com.github.jredmine.exception.BusinessException;
import com.github.jredmine.mapper.user.UserMapper;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserMapper userMapper;

    public UserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public UserRegisterResponseDTO register(UserRegisterRequestDTO requestDTO) {
        // 使用 Lambda QueryWrapper 检查用户是否存在（类型安全）
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getLogin, requestDTO.getLogin());
        User existsUser = userMapper.selectOne(queryWrapper);
        
        if (existsUser != null) {
            throw new BusinessException(ResultCode.USER_ALREADY_EXISTS);
        }

        User user = new User();
        user.setLogin(requestDTO.getLogin());
        user.setHashedPassword(requestDTO.getPassword());
        user.setFirstname(requestDTO.getFirstname());
        user.setLastname(requestDTO.getLastname());
        userMapper.insert(user);

        return UserConverter.INSTANCE.toUserRegisterResponseDTO(user);
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
        
        // 转换为响应 DTO
        return PageResponse.of(
            result.getRecords().stream()
                .map(UserConverter.INSTANCE::toUserRegisterResponseDTO)
                .toList(),
            (int) result.getTotal(),
            (int) result.getCurrent(),
            (int) result.getSize()
        );
    }
}
