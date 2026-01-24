package com.github.jredmine.mapper.search;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.jredmine.entity.SearchHistory;

/**
 * 搜索历史 Mapper 接口
 *
 * @author panfeng
 */
public interface SearchHistoryMapper extends BaseMapper<SearchHistory> {
    // 所有查询都通过 LambdaQueryWrapper 在 Service 层实现
}