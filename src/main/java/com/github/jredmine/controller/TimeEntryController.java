package com.github.jredmine.controller;

import com.github.jredmine.dto.request.timeentry.TimeEntryCreateRequestDTO;
import com.github.jredmine.dto.request.timeentry.TimeEntryQueryRequestDTO;
import com.github.jredmine.dto.request.timeentry.TimeEntryUpdateRequestDTO;
import com.github.jredmine.dto.response.ApiResponse;
import com.github.jredmine.dto.response.PageResponse;
import com.github.jredmine.dto.response.timeentry.TimeEntryResponseDTO;
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
}
