package com.github.jredmine.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.jredmine.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserRepository extends BaseMapper<User> {
}
