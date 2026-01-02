package com.github.jredmine.dto.response.issue;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务评论响应DTO
 *
 * @author panfeng
 */
@Data
public class IssueJournalResponseDTO {
    /**
     * 评论ID
     */
    private Integer id;

    /**
     * 任务ID
     */
    private Long issueId;

    /**
     * 评论内容
     */
    private String notes;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdOn;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedOn;

    /**
     * 是否私有备注
     */
    private Boolean privateNotes;

    /**
     * 评论作者ID
     */
    private Long userId;

    /**
     * 评论作者登录名
     */
    private String userName;

    /**
     * 更新者ID
     */
    private Long updatedById;

    /**
     * 更新者登录名
     */
    private String updatedByName;
}
