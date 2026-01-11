package com.github.jredmine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.jredmine.entity.Setting;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统设置 Mapper
 *
 * @author panfeng
 */
@Mapper
public interface SettingMapper extends BaseMapper<Setting> {
}
