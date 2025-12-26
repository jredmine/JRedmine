package com.github.jredmine.mapper.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.jredmine.entity.Token;
import org.apache.ibatis.annotations.Mapper;

/**
 * Token Mapper
 *
 * @author panfeng
 */
@Mapper
public interface TokenMapper extends BaseMapper<Token> {
}

