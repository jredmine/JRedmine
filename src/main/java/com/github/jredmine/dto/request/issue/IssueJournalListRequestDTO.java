package com.github.jredmine.dto.request.issue;

import lombok.Data;

/**
 * 任务活动日志列表查询请求DTO
 *
 * @author panfeng
 */
@Data
public class IssueJournalListRequestDTO {
    /**
     * 当前页码
     */
    private Integer current = 1;

    /**
     * 每页数量
     */
    private Integer size = 10;
}
