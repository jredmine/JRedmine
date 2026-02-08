package com.github.jredmine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.jredmine.entity.Message;
import org.apache.ibatis.annotations.Mapper;

/**
 * 论坛消息 Mapper 接口
 *
 * @author panfeng
 */
@Mapper
public interface MessageMapper extends BaseMapper<Message> {
}
