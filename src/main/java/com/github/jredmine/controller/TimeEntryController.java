package com.github.jredmine.controller;

import com.github.jredmine.dto.request.timeentry.*;
import com.github.jredmine.dto.response.ApiResponse;
import com.github.jredmine.dto.response.PageResponse;
import com.github.jredmine.dto.response.timeentry.*;
import com.github.jredmine.service.TimeEntryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 工时记录控制器
 *
 * @author panfeng
 * @since 2026-01-12
 */
@Tag(name = "工时记录管理", description = "工时记录相关接口")
@RestController
@RequestMapping("/api/time-entries")
@RequiredArgsConstructor
public class TimeEntryController {
    
    private final TimeEntryService timeEntryService;
    
    /**
     * 查询工时记录列表
     */
    @Operation(summary = "查询工时记录列表", description = "支持多条件筛选和分页")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or authentication.principal.hasPermission('view_time_entries')")
    public ApiResponse<PageResponse<TimeEntryResponseDTO>> queryTimeEntries(TimeEntryQueryRequestDTO request) {
        PageResponse<TimeEntryResponseDTO> result = timeEntryService.queryTimeEntries(request);
        return ApiResponse.success(result);
    }
    
    /**
     * 创建工时记录
     */
    @Operation(summary = "创建工时记录", description = "记录工作时间")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or authentication.principal.hasPermission('log_time')")
    public ApiResponse<TimeEntryResponseDTO> createTimeEntry(@Valid @RequestBody TimeEntryCreateRequestDTO request) {
        TimeEntryResponseDTO result = timeEntryService.createTimeEntry(request);
        return ApiResponse.success("工时记录创建成功", result);
    }
    
    /**
     * 根据ID获取工时记录
     */
    @Operation(summary = "获取工时记录详情", description = "根据ID获取工时记录详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or authentication.principal.hasPermission('view_time_entries')")
    public ApiResponse<TimeEntryResponseDTO> getTimeEntry(@PathVariable Long id) {
        TimeEntryResponseDTO result = timeEntryService.getTimeEntryById(id);
        return ApiResponse.success(result);
    }
    
    /**
     * 更新工时记录
     */
    @Operation(summary = "更新工时记录", description = "更新工时记录信息，只能更新自己创建的记录")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or authentication.principal.hasPermission('edit_time_entries')")
    public ApiResponse<TimeEntryResponseDTO> updateTimeEntry(
            @PathVariable Long id,
            @Valid @RequestBody TimeEntryUpdateRequestDTO request) {
        TimeEntryResponseDTO result = timeEntryService.updateTimeEntry(id, request);
        return ApiResponse.success("工时记录更新成功", result);
    }
    
    /**
     * 删除工时记录
     */
    @Operation(summary = "删除工时记录", description = "删除工时记录，只能删除自己创建的记录")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or authentication.principal.hasPermission('edit_time_entries')")
    public ApiResponse<Void> deleteTimeEntry(@PathVariable Long id) {
        timeEntryService.deleteTimeEntry(id);
        return ApiResponse.success("工时记录删除成功", null);
    }
    
    /**
     * 获取工时汇总统计
     */
    @Operation(summary = "获取工时汇总统计", description = "获取工时的总计、平均值、最大值、最小值等统计信息")
    @GetMapping("/summary")
    @PreAuthorize("hasRole('ADMIN') or authentication.principal.hasPermission('view_time_entries')")
    public ApiResponse<TimeEntrySummaryResponseDTO> getTimeEntrySummary(TimeEntryStatisticsRequestDTO request) {
        TimeEntrySummaryResponseDTO result = timeEntryService.getTimeEntrySummary(request);
        return ApiResponse.success("工时汇总统计获取成功", result);
    }
    
    /**
     * 获取工时分组统计
     */
    @Operation(summary = "获取工时分组统计", description = "按项目、用户、活动类型或日期分组统计工时")
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN') or authentication.principal.hasPermission('view_time_entries')")
    public ApiResponse<TimeEntryStatisticsResponseDTO> getTimeEntryStatistics(TimeEntryStatisticsRequestDTO request) {
        TimeEntryStatisticsResponseDTO result = timeEntryService.getTimeEntryStatistics(request);
        return ApiResponse.success("工时分组统计获取成功", result);
    }
    
    /**
     * 生成项目工时报表
     */
    @Operation(summary = "生成项目工时报表", description = "生成指定项目的详细工时报表，包含成员和活动类型分布")
    @GetMapping("/reports/project")
    @PreAuthorize("hasRole('ADMIN') or authentication.principal.hasPermission('view_time_entries')")
    public ApiResponse<TimeEntryProjectReportDTO> generateProjectReport(TimeEntryReportRequestDTO request) {
        TimeEntryProjectReportDTO result = timeEntryService.generateProjectReport(request);
        return ApiResponse.success("项目工时报表生成成功", result);
    }
    
    /**
     * 生成用户工时报表
     */
    @Operation(summary = "生成用户工时报表", description = "生成指定用户的详细工时报表，包含项目和活动类型分布")
    @GetMapping("/reports/user")
    @PreAuthorize("hasRole('ADMIN') or authentication.principal.hasPermission('view_time_entries')")
    public ApiResponse<TimeEntryUserReportDTO> generateUserReport(TimeEntryReportRequestDTO request) {
        TimeEntryUserReportDTO result = timeEntryService.generateUserReport(request);
        return ApiResponse.success("用户工时报表生成成功", result);
    }
    
    /**
     * 生成时间段工时报表
     */
    @Operation(summary = "生成时间段工时报表", description = "生成指定时间段的工时趋势报表，支持按日、周、月分组")
    @GetMapping("/reports/period")
    @PreAuthorize("hasRole('ADMIN') or authentication.principal.hasPermission('view_time_entries')")
    public ApiResponse<TimeEntryPeriodReportDTO> generatePeriodReport(TimeEntryReportRequestDTO request) {
        TimeEntryPeriodReportDTO result = timeEntryService.generatePeriodReport(request);
        return ApiResponse.success("时间段工时报表生成成功", result);
    }
}
