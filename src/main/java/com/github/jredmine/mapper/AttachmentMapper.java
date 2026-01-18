package com.github.jredmine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.jredmine.entity.Attachment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 附件Mapper接口
 *
 * @author panfeng
 * @since 2026-01-18
 */
@Mapper
public interface AttachmentMapper extends BaseMapper<Attachment> {
}
