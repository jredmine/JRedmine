package com.github.jredmine.controller;

import com.github.jredmine.dto.request.report.UserWorkloadReportRequestDTO;
import com.github.jredmine.dto.request.timeentry.TimeEntryReportRequestDTO;
import com.github.jredmine.dto.response.ApiResponse;
import com.github.jredmine.dto.response.project.ProjectStatisticsResponseDTO;
import com.github.jredmine.dto.response.report.UserWorkloadReportResponseDTO;
import com.github.jredmine.dto.response.timeentry.TimeEntryReportResponseDTO;
import com.github.jredmine.service.ProjectService;
import com.github.jredmine.service.ReportService;
import com.github.jredmine.service.TimeEntryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 统计报表模块控制器
 *
 * @author panfeng
 */
@Tag(name = "统计报表", description = "统计报表相关接口")
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ProjectService projectService;
    private final TimeEntryService timeEntryService;
    private final ReportService reportService;

    public ReportController(ProjectService projectService, TimeEntryService timeEntryService,
            ReportService reportService) {
        this.projectService = projectService;
        this.timeEntryService = timeEntryService;
        this.reportService = reportService;
    }

    @Operation(summary = "获取项目统计报表", description = "获取指定项目的统计报表，包含任务数、完成率、工时等。需要认证，项目成员或系统管理员可访问。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/projects/{projectId}")
    public ApiResponse<ProjectStatisticsResponseDTO> getProjectReport(@PathVariable Long projectId) {
        ProjectStatisticsResponseDTO result = projectService.getProjectStatistics(projectId);
        return ApiResponse.success(result);
    }

    @Operation(summary = "获取工时统计报表", description = "获取工时统计报表，支持按项目、用户、日期范围筛选，返回多维度汇总（按用户/项目/活动类型/日期趋势）。需要认证，需要 view_time_entries 权限或系统管理员。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN') or authentication.principal.hasPermission('view_time_entries')")
    @GetMapping("/time-entries")
    public ApiResponse<TimeEntryReportResponseDTO> getTimeEntryReport(
            @ModelAttribute TimeEntryReportRequestDTO request) {
        TimeEntryReportResponseDTO result = timeEntryService.getTimeEntryReport(request);
        return ApiResponse.success(result);
    }

    @Operation(summary = "获取用户工作量统计报表", description = "按用户统计分配任务数、已完成数、登记工时等，支持按项目、日期范围筛选。需要认证，需要 view_issues 或 view_time_entries 权限或系统管理员。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN') or authentication.principal.hasPermission('view_issues') or authentication.principal.hasPermission('view_time_entries')")
    @GetMapping("/users/workload")
    public ApiResponse<UserWorkloadReportResponseDTO> getUserWorkloadReport(
            @ModelAttribute UserWorkloadReportRequestDTO request) {
        UserWorkloadReportResponseDTO result = reportService.getUserWorkloadReport(request);
        return ApiResponse.success(result);
    }
}
