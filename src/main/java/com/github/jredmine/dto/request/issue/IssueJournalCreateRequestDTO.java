package com.github.jredmine.dto.request.issue;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建任务评论请求DTO
 *
 * @author panfeng
 */
@Data
public class IssueJournalCreateRequestDTO {
    /**
     * 评论内容（必填）
     */
    @NotBlank(message = "评论内容不能为空")
    private String notes;

    /**
     * 是否私有备注（只有项目成员可见）
     * 默认为 false（公开）
     */
    private Boolean privateNotes;
}
