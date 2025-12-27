package com.github.jredmine.mapper.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.jredmine.entity.Role;
import org.apache.ibatis.annotations.Mapper;

/**
 * 角色Mapper
 *
 * @author panfeng
 */
@Mapper
public interface RoleMapper extends BaseMapper<Role> {
}

