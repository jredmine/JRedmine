package com.github.jredmine.mapper.issue;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.jredmine.entity.IssueRelation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 任务关联 Mapper 接口
 *
 * @author panfeng
 */
@Mapper
public interface IssueRelationMapper extends BaseMapper<IssueRelation> {
}
