package com.github.jredmine.controller;

import com.github.jredmine.dto.request.project.ProjectCreateRequestDTO;
import com.github.jredmine.dto.request.project.ProjectUpdateRequestDTO;
import com.github.jredmine.dto.response.ApiResponse;
import com.github.jredmine.dto.response.PageResponse;
import com.github.jredmine.dto.response.project.ProjectDetailResponseDTO;
import com.github.jredmine.dto.response.project.ProjectListItemResponseDTO;
import com.github.jredmine.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 项目管理控制器
 * 负责项目的查询、创建、更新等管理功能
 *
 * @author panfeng
 */
@Tag(name = "项目管理", description = "项目信息查询、创建、更新等管理接口")
@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @Operation(summary = "获取项目列表", description = "分页查询项目列表，支持按名称、状态、是否公开、父项目等条件筛选。需要认证。公开项目所有用户可见，私有项目仅项目成员可见，系统管理员可见所有项目。", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping
    public ApiResponse<PageResponse<ProjectListItemResponseDTO>> listProjects(
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "isPublic", required = false) Boolean isPublic,
            @RequestParam(value = "parentId", required = false) Long parentId) {
        PageResponse<ProjectListItemResponseDTO> result = projectService.listProjects(
                current, size, name, status, isPublic, parentId);
        return ApiResponse.success(result);
    }

    @Operation(summary = "获取项目详情", description = "根据项目ID查询项目详细信息。需要认证。公开项目所有用户可见，私有项目仅项目成员可见，系统管理员可见所有项目。", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/{id}")
    public ApiResponse<ProjectDetailResponseDTO> getProjectById(@PathVariable Long id) {
        ProjectDetailResponseDTO result = projectService.getProjectById(id);
        return ApiResponse.success(result);
    }

    @Operation(summary = "创建项目", description = "创建新项目。需要认证，需要 create_projects 权限或系统管理员。创建者自动成为项目成员。", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping
    public ApiResponse<ProjectDetailResponseDTO> createProject(
            @Valid @RequestBody ProjectCreateRequestDTO requestDTO) {
        ProjectDetailResponseDTO result = projectService.createProject(requestDTO);
        return ApiResponse.success("项目创建成功", result);
    }

    @Operation(summary = "更新项目", description = "更新项目信息。需要认证，需要 edit_projects 权限或系统管理员。", security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/{id}")
    public ApiResponse<ProjectDetailResponseDTO> updateProject(
            @PathVariable Long id,
            @Valid @RequestBody ProjectUpdateRequestDTO requestDTO) {
        ProjectDetailResponseDTO result = projectService.updateProject(id, requestDTO);
        return ApiResponse.success("项目更新成功", result);
    }
}
