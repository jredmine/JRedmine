package com.github.jredmine.mapper.project;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.jredmine.entity.Version;
import org.apache.ibatis.annotations.Mapper;

/**
 * 版本 Mapper 接口
 *
 * @author panfeng
 */
@Mapper
public interface VersionMapper extends BaseMapper<Version> {
}
