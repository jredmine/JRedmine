package com.github.jredmine.mapper.workflow;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.jredmine.entity.IssueStatus;
import org.apache.ibatis.annotations.Mapper;

/**
 * 任务状态 Mapper 接口
 *
 * @author panfeng
 */
@Mapper
public interface IssueStatusMapper extends BaseMapper<IssueStatus> {
}

