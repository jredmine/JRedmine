package com.github.jredmine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.jredmine.entity.TimeEntry;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工时记录 Mapper 接口
 *
 * @author panfeng
 * @since 2026-01-12
 */
@Mapper
public interface TimeEntryMapper extends BaseMapper<TimeEntry> {
}
