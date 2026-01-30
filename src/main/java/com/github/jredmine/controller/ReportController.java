package com.github.jredmine.controller;

import com.github.jredmine.dto.response.ApiResponse;
import com.github.jredmine.dto.response.project.ProjectStatisticsResponseDTO;
import com.github.jredmine.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
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

    public ReportController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @Operation(summary = "获取项目统计报表", description = "获取指定项目的统计报表，包含任务数、完成率、工时等。需要认证，项目成员或系统管理员可访问。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/projects/{projectId}")
    public ApiResponse<ProjectStatisticsResponseDTO> getProjectReport(@PathVariable Long projectId) {
        ProjectStatisticsResponseDTO result = projectService.getProjectStatistics(projectId);
        return ApiResponse.success(result);
    }
}
