package com.github.jredmine.mapper.project;

import com.github.jredmine.entity.Member;
import com.github.yulichang.base.MPJBaseMapper;

/**
 * 项目成员 Mapper 接口
 * 注意：不需要 @Mapper 注解，因为已经在 JRedmineApplication 中使用 @MapperScan 扫描
 * MPJBaseMapper 已经继承了 BaseMapper，无需重复继承
 *
 * @author panfeng
 */
public interface MemberMapper extends MPJBaseMapper<Member> {
}
