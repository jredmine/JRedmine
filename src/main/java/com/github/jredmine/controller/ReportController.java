package com.github.jredmine.controller;

import com.github.jredmine.dto.request.report.BurndownReportRequestDTO;
import com.github.jredmine.dto.request.report.UserWorkloadReportRequestDTO;
import com.github.jredmine.dto.request.timeentry.TimeEntryReportRequestDTO;
import com.github.jredmine.dto.response.ApiResponse;
import com.github.jredmine.dto.response.project.ProjectStatisticsResponseDTO;
import com.github.jredmine.dto.response.report.BurndownReportResponseDTO;
import com.github.jredmine.dto.response.report.UserWorkloadReportResponseDTO;
import com.github.jredmine.dto.response.timeentry.TimeEntryReportResponseDTO;
import com.github.jredmine.service.ProjectService;
import com.github.jredmine.service.ReportExportService;
import com.github.jredmine.service.ReportService;
import com.github.jredmine.service.TimeEntryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
    private final ReportExportService reportExportService;

    public ReportController(ProjectService projectService, TimeEntryService timeEntryService,
            ReportService reportService, ReportExportService reportExportService) {
        this.projectService = projectService;
        this.timeEntryService = timeEntryService;
        this.reportService = reportService;
        this.reportExportService = reportExportService;
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

    @Operation(summary = "获取燃尽图报表", description = "获取项目或版本的燃尽图数据，包含理想燃尽线和实际燃尽线（按剩余任务数）。支持按项目或按版本（fixed_version_id）统计。需要认证，需要 view_issues 权限或系统管理员。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN') or authentication.principal.hasPermission('view_issues')")
    @GetMapping("/burndown")
    public ApiResponse<BurndownReportResponseDTO> getBurndownReport(
            @ModelAttribute BurndownReportRequestDTO request) {
        BurndownReportResponseDTO result = reportService.getBurndownReport(request);
        return ApiResponse.success(result);
    }

    // ==================== 报表导出 ====================

    @Operation(summary = "导出项目统计报表(Excel)", description = "将项目统计报表导出为 Excel 文件。需要认证。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/projects/{projectId}/export/excel")
    public ResponseEntity<byte[]> exportProjectReportToExcel(@PathVariable Long projectId) {
        byte[] data = reportExportService.exportProjectReportToExcel(projectId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "project_statistics.xlsx");
        return ResponseEntity.ok().headers(headers).body(data);
    }

    @Operation(summary = "导出项目统计报表(CSV)", description = "将项目统计报表导出为 CSV 文件。需要认证。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/projects/{projectId}/export/csv")
    public ResponseEntity<byte[]> exportProjectReportToCSV(@PathVariable Long projectId) {
        byte[] data = reportExportService.exportProjectReportToCSV(projectId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv;charset=UTF-8"));
        headers.setContentDispositionFormData("attachment", "project_statistics.csv");
        return ResponseEntity.ok().headers(headers).body(data);
    }

    @Operation(summary = "导出工时统计报表(Excel)", description = "将工时统计报表导出为 Excel 文件。参数同工时统计接口。需要 view_time_entries 或管理员。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN') or authentication.principal.hasPermission('view_time_entries')")
    @GetMapping("/time-entries/export/excel")
    public ResponseEntity<byte[]> exportTimeEntryReportToExcel(@ModelAttribute TimeEntryReportRequestDTO request) {
        byte[] data = reportExportService.exportTimeEntryReportToExcel(request);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "time_entry_report.xlsx");
        return ResponseEntity.ok().headers(headers).body(data);
    }

    @Operation(summary = "导出工时统计报表(CSV)", description = "将工时统计报表导出为 CSV 文件。需要 view_time_entries 或管理员。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN') or authentication.principal.hasPermission('view_time_entries')")
    @GetMapping("/time-entries/export/csv")
    public ResponseEntity<byte[]> exportTimeEntryReportToCSV(@ModelAttribute TimeEntryReportRequestDTO request) {
        byte[] data = reportExportService.exportTimeEntryReportToCSV(request);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv;charset=UTF-8"));
        headers.setContentDispositionFormData("attachment", "time_entry_report.csv");
        return ResponseEntity.ok().headers(headers).body(data);
    }

    @Operation(summary = "导出用户工作量报表(Excel)", description = "将用户工作量统计报表导出为 Excel 文件。参数同用户工作量接口。需要 view_issues 或 view_time_entries 或管理员。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN') or authentication.principal.hasPermission('view_issues') or authentication.principal.hasPermission('view_time_entries')")
    @GetMapping("/users/workload/export/excel")
    public ResponseEntity<byte[]> exportUserWorkloadReportToExcel(@ModelAttribute UserWorkloadReportRequestDTO request) {
        byte[] data = reportExportService.exportUserWorkloadReportToExcel(request);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "user_workload_report.xlsx");
        return ResponseEntity.ok().headers(headers).body(data);
    }

    @Operation(summary = "导出用户工作量报表(CSV)", description = "将用户工作量统计报表导出为 CSV 文件。需要 view_issues 或 view_time_entries 或管理员。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN') or authentication.principal.hasPermission('view_issues') or authentication.principal.hasPermission('view_time_entries')")
    @GetMapping("/users/workload/export/csv")
    public ResponseEntity<byte[]> exportUserWorkloadReportToCSV(@ModelAttribute UserWorkloadReportRequestDTO request) {
        byte[] data = reportExportService.exportUserWorkloadReportToCSV(request);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv;charset=UTF-8"));
        headers.setContentDispositionFormData("attachment", "user_workload_report.csv");
        return ResponseEntity.ok().headers(headers).body(data);
    }

    @Operation(summary = "导出燃尽图报表(Excel)", description = "将燃尽图报表导出为 Excel 文件。参数 projectId 必填，versionId 可选。需要 view_issues 或管理员。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN') or authentication.principal.hasPermission('view_issues')")
    @GetMapping("/burndown/export/excel")
    public ResponseEntity<byte[]> exportBurndownReportToExcel(@ModelAttribute BurndownReportRequestDTO request) {
        byte[] data = reportExportService.exportBurndownReportToExcel(request);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "burndown_report.xlsx");
        return ResponseEntity.ok().headers(headers).body(data);
    }

    @Operation(summary = "导出燃尽图报表(CSV)", description = "将燃尽图报表导出为 CSV 文件。需要 view_issues 或管理员。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN') or authentication.principal.hasPermission('view_issues')")
    @GetMapping("/burndown/export/csv")
    public ResponseEntity<byte[]> exportBurndownReportToCSV(@ModelAttribute BurndownReportRequestDTO request) {
        byte[] data = reportExportService.exportBurndownReportToCSV(request);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv;charset=UTF-8"));
        headers.setContentDispositionFormData("attachment", "burndown_report.csv");
        return ResponseEntity.ok().headers(headers).body(data);
    }
}
