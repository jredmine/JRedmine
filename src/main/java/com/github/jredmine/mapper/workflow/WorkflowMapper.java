package com.github.jredmine.mapper.workflow;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.jredmine.entity.Workflow;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工作流 Mapper 接口
 *
 * @author panfeng
 */
@Mapper
public interface WorkflowMapper extends BaseMapper<Workflow> {
}

