package com.github.jredmine.mapper.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.jredmine.entity.MemberRole;
import org.apache.ibatis.annotations.Mapper;

/**
 * 成员角色关联Mapper
 *
 * @author panfeng
 */
@Mapper
public interface MemberRoleMapper extends BaseMapper<MemberRole> {
}

