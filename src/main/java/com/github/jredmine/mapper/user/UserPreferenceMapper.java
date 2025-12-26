package com.github.jredmine.mapper.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.jredmine.entity.UserPreference;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户偏好设置Mapper
 *
 * @author panfeng
 */
@Mapper
public interface UserPreferenceMapper extends BaseMapper<UserPreference> {
}

