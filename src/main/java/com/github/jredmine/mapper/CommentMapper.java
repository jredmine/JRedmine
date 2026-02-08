package com.github.jredmine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.jredmine.entity.Comment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 评论 Mapper 接口（comments 表）
 *
 * @author panfeng
 */
@Mapper
public interface CommentMapper extends BaseMapper<Comment> {
}
