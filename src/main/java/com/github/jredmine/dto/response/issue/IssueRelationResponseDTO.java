package com.github.jredmine.dto.response.issue;

import lombok.Data;

/**
 * 任务关联响应DTO
 *
 * @author panfeng
 */
@Data
public class IssueRelationResponseDTO {
    /**
     * 关联ID
     */
    private Integer id;

    /**
     * 源任务ID
     */
    private Long issueFromId;

    /**
     * 源任务标题
     */
    private String issueFromSubject;

    /**
     * 目标任务ID
     */
    private Long issueToId;

    /**
     * 目标任务标题
     */
    private String issueToSubject;

    /**
     * 关联类型
     */
    private String relationType;

    /**
     * 延迟天数
     */
    private Integer delay;
}
