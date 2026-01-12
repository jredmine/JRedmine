package com.github.jredmine.dto.response.timeentry;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.github.jredmine.dto.response.project.ProjectSimpleResponseDTO;
import com.github.jredmine.dto.response.user.UserSimpleResponseDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 工时记录响应DTO
 *
 * @author panfeng
 * @since 2026-01-12
 */
@Data
@Schema(description = "工时记录响应")
public class TimeEntryResponseDTO {
    
    @Schema(description = "工时记录ID")
    private Long id;
    
    @Schema(description = "项目信息")
    private ProjectSimpleResponseDTO project;
    
    @Schema(description = "任务ID")
    private Long issueId;
    
    @Schema(description = "任务标题")
    private String issueSubject;
    
    @Schema(description = "工作人员")
    private UserSimpleResponseDTO user;
    
    @Schema(description = "创建者")
    private UserSimpleResponseDTO author;
    
    @Schema(description = "工时（小时）")
    private Float hours;
    
    @Schema(description = "工作日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate spentOn;
    
    @Schema(description = "活动类型ID")
    private Long activityId;
    
    @Schema(description = "活动类型名称")
    private String activityName;
    
    @Schema(description = "备注说明")
    private String comments;
    
    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdOn;
    
    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedOn;
}
