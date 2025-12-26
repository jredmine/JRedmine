package com.github.jredmine.mapper.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.jredmine.entity.EmailAddress;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户邮箱地址Mapper
 *
 * @author panfeng
 */
@Mapper
public interface EmailAddressMapper extends BaseMapper<EmailAddress> {
}

