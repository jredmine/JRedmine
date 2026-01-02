package com.github.jredmine.dto.request.issue;

import com.github.jredmine.dto.request.PageRequestDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 任务活动日志列表查询请求DTO
 *
 * @author panfeng
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class IssueJournalListRequestDTO extends PageRequestDTO {
}
