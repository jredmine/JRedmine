package com.github.jredmine.mapper.issue;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.jredmine.entity.Watcher;
import org.apache.ibatis.annotations.Mapper;

/**
 * 关注者 Mapper 接口
 *
 * @author panfeng
 */
@Mapper
public interface WatcherMapper extends BaseMapper<Watcher> {
}
