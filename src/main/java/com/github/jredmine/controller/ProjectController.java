package com.github.jredmine.controller;

import com.github.jredmine.dto.request.project.MemberRoleAssignRequestDTO;
import com.github.jredmine.dto.request.project.ProjectCreateRequestDTO;
import com.github.jredmine.dto.request.project.ProjectMemberCreateRequestDTO;
import com.github.jredmine.dto.request.project.ProjectMemberUpdateRequestDTO;
import com.github.jredmine.dto.request.project.ProjectUpdateRequestDTO;
import com.github.jredmine.dto.response.ApiResponse;
import com.github.jredmine.dto.response.PageResponse;
import com.github.jredmine.dto.response.project.ProjectDetailResponseDTO;
import com.github.jredmine.dto.response.project.ProjectListItemResponseDTO;
import com.github.jredmine.dto.response.project.ProjectMemberResponseDTO;
import com.github.jredmine.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    @Operation(summary = "删除项目", description = "删除项目（软删除，更新状态为归档）。需要认证，需要 delete_projects 权限或系统管理员。如果项目存在子项目，则不能删除。", security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteProject(@PathVariable Long id) {
        projectService.deleteProject(id);
        return ApiResponse.success("项目删除成功", null);
    }

    @Operation(summary = "获取项目成员列表", description = "获取项目的所有成员列表，支持分页和按用户名称模糊查询。需要认证，项目成员或系统管理员可访问。", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/{id}/members")
    public ApiResponse<PageResponse<ProjectMemberResponseDTO>> listProjectMembers(
            @PathVariable Long id,
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "name", required = false) String name) {
        PageResponse<ProjectMemberResponseDTO> result = projectService.listProjectMembers(id, current, size, name);
        return ApiResponse.success(result);
    }

    @Operation(summary = "新增项目成员", description = "添加用户到项目（成为项目成员）。需要认证，需要 manage_projects 权限或系统管理员。", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/{id}/members")
    public ApiResponse<ProjectMemberResponseDTO> createProjectMember(
            @PathVariable Long id,
            @Valid @RequestBody ProjectMemberCreateRequestDTO requestDTO) {
        ProjectMemberResponseDTO result = projectService.createProjectMember(id, requestDTO);
        return ApiResponse.success("项目成员添加成功", result);
    }

    @Operation(summary = "更新项目成员", description = "更新项目成员信息（如邮件通知设置、角色等）。需要认证，需要 manage_projects 权限或系统管理员。", security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/{id}/members/{memberId}")
    public ApiResponse<ProjectMemberResponseDTO> updateProjectMember(
            @PathVariable Long id,
            @PathVariable Long memberId,
            @Valid @RequestBody ProjectMemberUpdateRequestDTO requestDTO) {
        ProjectMemberResponseDTO result = projectService.updateProjectMember(id, memberId, requestDTO);
        return ApiResponse.success("项目成员更新成功", result);
    }

    @Operation(summary = "移除项目成员", description = "从项目中移除成员。需要认证，需要 manage_projects 权限或系统管理员。", security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/{id}/members/{memberId}")
    public ApiResponse<Void> removeProjectMember(
            @PathVariable Long id,
            @PathVariable Long memberId) {
        projectService.removeProjectMember(id, memberId);
        return ApiResponse.success("项目成员移除成功", null);
    }

    @Operation(summary = "分配角色给项目成员", description = "为项目成员分配角色。如果成员已有该角色，则跳过。需要认证，需要 manage_projects 权限或系统管理员。", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/{id}/members/{memberId}/roles")
    public ApiResponse<Void> assignRolesToMember(
            @PathVariable Long id,
            @PathVariable Long memberId,
            @Valid @RequestBody MemberRoleAssignRequestDTO requestDTO) {
        projectService.assignRolesToMember(id, memberId, requestDTO);
        return ApiResponse.success("角色分配成功", null);
    }

    @Operation(summary = "更新项目成员角色", description = "更新项目成员的角色（替换现有直接分配的角色，保留继承的角色）。需要认证，需要 manage_projects 权限或系统管理员。", security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/{id}/members/{memberId}/roles")
    public ApiResponse<Void> updateMemberRoles(
            @PathVariable Long id,
            @PathVariable Long memberId,
            @Valid @RequestBody MemberRoleAssignRequestDTO requestDTO) {
        projectService.updateMemberRoles(id, memberId, requestDTO);
        return ApiResponse.success("角色更新成功", null);
    }

    @Operation(summary = "获取项目子项目列表", description = "获取项目的所有子项目。需要认证，项目成员或系统管理员可访问。", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/{id}/children")
    public ApiResponse<List<ProjectListItemResponseDTO>> getProjectChildren(@PathVariable Long id) {
        List<ProjectListItemResponseDTO> result = projectService.getProjectChildren(id);
        return ApiResponse.success(result);
    }
}
