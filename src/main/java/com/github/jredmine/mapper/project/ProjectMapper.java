package com.github.jredmine.mapper.project;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.jredmine.entity.Project;

/**
 * 项目 Mapper 接口
 * 注意：不需要 @Mapper 注解，因为已经在 JRedmineApplication 中使用 @MapperScan 扫描
 *
 * @author panfeng
 */
public interface ProjectMapper extends BaseMapper<Project> {
}

