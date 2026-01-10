package com.github.jredmine.dto.response.issue;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 任务导入结果响应DTO
 *
 * @author panfeng
 */
@Data
public class IssueImportResultDTO {
    /**
     * 总数
     */
    private Integer total = 0;

    /**
     * 成功数
     */
    private Integer success = 0;

    /**
     * 失败数
     */
    private Integer failed = 0;

    /**
     * 跳过数
     */
    private Integer skipped = 0;

    /**
     * 错误信息列表
     */
    private List<ImportError> errors = new ArrayList<>();

    /**
     * 导入错误详情
     */
    @Data
    public static class ImportError {
        /**
         * 行号
         */
        private Integer row;

        /**
         * 任务标题
         */
        private String subject;

        /**
         * 错误信息
         */
        private String message;

        public ImportError(Integer row, String subject, String message) {
            this.row = row;
            this.subject = subject;
            this.message = message;
        }
    }

    /**
     * 添加错误
     */
    public void addError(Integer row, String subject, String message) {
        errors.add(new ImportError(row, subject, message));
        failed++;
    }

    /**
     * 增加成功计数
     */
    public void incrementSuccess() {
        success++;
    }

    /**
     * 增加跳过计数
     */
    public void incrementSkipped() {
        skipped++;
    }
}
