package com.github.jredmine.mapper.issue;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.jredmine.entity.IssueCategory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 任务分类 Mapper 接口
 *
 * @author panfeng
 */
@Mapper
public interface IssueCategoryMapper extends BaseMapper<IssueCategory> {
}
