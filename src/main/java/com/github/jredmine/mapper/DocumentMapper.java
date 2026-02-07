package com.github.jredmine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.jredmine.entity.Document;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文档 Mapper 接口
 *
 * @author panfeng
 */
@Mapper
public interface DocumentMapper extends BaseMapper<Document> {
}
