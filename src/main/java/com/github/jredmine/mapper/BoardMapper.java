package com.github.jredmine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.jredmine.entity.Board;
import org.apache.ibatis.annotations.Mapper;

/**
 * 论坛板块 Mapper 接口
 *
 * @author panfeng
 */
@Mapper
public interface BoardMapper extends BaseMapper<Board> {
}
