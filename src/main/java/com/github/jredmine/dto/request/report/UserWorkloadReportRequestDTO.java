package com.github.jredmine.dto.request.report;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 用户工作量统计报表请求 DTO
 *
 * @author panfeng
 */
@Data
@Schema(description = "用户工作量统计报表请求")
public class UserWorkloadReportRequestDTO {

    @Schema(description = "项目ID（可选，不传则统计所有项目）")
    private Long projectId;

    @Schema(description = "开始日期，格式：yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @Schema(description = "结束日期，格式：yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    @Schema(description = "年份，如：2026")
    private Integer year;

    @Schema(description = "月份，1-12")
    private Integer month;
}
