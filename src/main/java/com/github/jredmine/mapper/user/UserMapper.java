package com.github.jredmine.mapper.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.jredmine.entity.User;

/**
 * 用户 Mapper 接口
 * 注意：不需要 @Mapper 注解，因为已经在 JRedmineApplication 中使用 @MapperScan 扫描
 */
public interface UserMapper extends BaseMapper<User> {
}

