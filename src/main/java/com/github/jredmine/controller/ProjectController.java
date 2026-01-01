package com.github.jredmine.controller;

import com.github.jredmine.dto.request.issue.IssueCategoryCreateRequestDTO;
import com.github.jredmine.dto.request.issue.IssueCategoryUpdateRequestDTO;
import com.github.jredmine.dto.request.project.MemberRoleAssignRequestDTO;
import com.github.jredmine.dto.request.project.ProjectArchiveRequestDTO;
import com.github.jredmine.dto.request.project.ProjectCopyRequestDTO;
import com.github.jredmine.dto.request.project.ProjectCreateRequestDTO;
import com.github.jredmine.dto.request.project.ProjectMemberCreateRequestDTO;
import com.github.jredmine.dto.request.project.ProjectMemberUpdateRequestDTO;
import com.github.jredmine.dto.request.project.ProjectFromTemplateRequestDTO;
import com.github.jredmine.dto.request.project.ProjectTemplateCreateRequestDTO;
import com.github.jredmine.dto.request.project.ProjectTemplateUpdateRequestDTO;
import com.github.jredmine.dto.request.project.ProjectUpdateRequestDTO;
import com.github.jredmine.dto.response.ApiResponse;
import com.github.jredmine.dto.response.PageResponse;
import com.github.jredmine.dto.response.issue.IssueCategoryResponseDTO;
import com.github.jredmine.dto.response.project.ProjectDetailResponseDTO;
import com.github.jredmine.dto.response.project.ProjectListItemResponseDTO;
import com.github.jredmine.dto.response.project.ProjectMemberResponseDTO;
import com.github.jredmine.dto.response.project.ProjectStatisticsResponseDTO;
import com.github.jredmine.dto.response.project.ProjectTemplateResponseDTO;
import com.github.jredmine.dto.response.project.ProjectTreeNodeResponseDTO;
import com.github.jredmine.service.IssueService;
import com.github.jredmine.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
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
    private final IssueService issueService;

    public ProjectController(ProjectService projectService, IssueService issueService) {
        this.projectService = projectService;
        this.issueService = issueService;
    }

    @Operation(summary = "获取项目列表", description = "分页查询项目列表，支持按关键词（在名称和描述中搜索）、名称（仅在名称中搜索）、状态、是否公开、父项目等条件筛选。需要认证。公开项目所有用户可见，私有项目仅项目成员可见，系统管理员可见所有项目。", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping
    public ApiResponse<PageResponse<ProjectListItemResponseDTO>> listProjects(
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "isPublic", required = false) Boolean isPublic,
            @RequestParam(value = "parentId", required = false) Long parentId) {
        PageResponse<ProjectListItemResponseDTO> result = projectService.listProjects(
                current, size, name, keyword, status, isPublic, parentId);
        return ApiResponse.success(result);
    }

    @Operation(summary = "获取项目详情", description = "根据项目ID查询项目详细信息。需要认证。公开项目所有用户可见，私有项目仅项目成员可见，系统管理员可见所有项目。", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/{id}")
    public ApiResponse<ProjectDetailResponseDTO> getProjectById(@PathVariable Long id) {
        ProjectDetailResponseDTO result = projectService.getProjectById(id);
        return ApiResponse.success(result);
    }

    @Operation(summary = "创建项目", description = "创建新项目。需要认证，需要 create_projects 权限或系统管理员。创建者自动成为项目成员。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN') or authentication.principal.hasPermission('create_projects')")
    @PostMapping
    public ApiResponse<ProjectDetailResponseDTO> createProject(
            @Valid @RequestBody ProjectCreateRequestDTO requestDTO) {
        ProjectDetailResponseDTO result = projectService.createProject(requestDTO);
        return ApiResponse.success("项目创建成功", result);
    }

    @Operation(summary = "更新项目", description = "更新项目信息。需要认证，需要 edit_projects 权限或系统管理员。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasPermission(#id, 'Project', 'edit_projects')")
    @PutMapping("/{id}")
    public ApiResponse<ProjectDetailResponseDTO> updateProject(
            @PathVariable Long id,
            @Valid @RequestBody ProjectUpdateRequestDTO requestDTO) {
        ProjectDetailResponseDTO result = projectService.updateProject(id, requestDTO);
        return ApiResponse.success("项目更新成功", result);
    }

    @Operation(summary = "删除项目", description = "删除项目（软删除，更新状态为归档）。需要认证，需要 delete_projects 权限或系统管理员。如果项目存在子项目，则不能删除。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasPermission(#id, 'Project', 'delete_projects')")
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
    @PreAuthorize("hasPermission(#id, 'Project', 'manage_projects')")
    @PostMapping("/{id}/members")
    public ApiResponse<ProjectMemberResponseDTO> createProjectMember(
            @PathVariable Long id,
            @Valid @RequestBody ProjectMemberCreateRequestDTO requestDTO) {
        ProjectMemberResponseDTO result = projectService.createProjectMember(id, requestDTO);
        return ApiResponse.success("项目成员添加成功", result);
    }

    @Operation(summary = "更新项目成员", description = "更新项目成员信息（如邮件通知设置、角色等）。需要认证，需要 manage_projects 权限或系统管理员。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasPermission(#id, 'Project', 'manage_projects')")
    @PutMapping("/{id}/members/{memberId}")
    public ApiResponse<ProjectMemberResponseDTO> updateProjectMember(
            @PathVariable Long id,
            @PathVariable Long memberId,
            @Valid @RequestBody ProjectMemberUpdateRequestDTO requestDTO) {
        ProjectMemberResponseDTO result = projectService.updateProjectMember(id, memberId, requestDTO);
        return ApiResponse.success("项目成员更新成功", result);
    }

    @Operation(summary = "移除项目成员", description = "从项目中移除成员。需要认证，需要 manage_projects 权限或系统管理员。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasPermission(#id, 'Project', 'manage_projects')")
    @DeleteMapping("/{id}/members/{memberId}")
    public ApiResponse<Void> removeProjectMember(
            @PathVariable Long id,
            @PathVariable Long memberId) {
        projectService.removeProjectMember(id, memberId);
        return ApiResponse.success("项目成员移除成功", null);
    }

    @Operation(summary = "分配角色给项目成员", description = "为项目成员分配角色。如果成员已有该角色，则跳过。需要认证，需要 manage_projects 权限或系统管理员。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasPermission(#id, 'Project', 'manage_projects')")
    @PostMapping("/{id}/members/{memberId}/roles")
    public ApiResponse<Void> assignRolesToMember(
            @PathVariable Long id,
            @PathVariable Long memberId,
            @Valid @RequestBody MemberRoleAssignRequestDTO requestDTO) {
        projectService.assignRolesToMember(id, memberId, requestDTO);
        return ApiResponse.success("角色分配成功", null);
    }

    @Operation(summary = "更新项目成员角色", description = "更新项目成员的角色（替换现有直接分配的角色，保留继承的角色）。需要认证，需要 manage_projects 权限或系统管理员。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasPermission(#id, 'Project', 'manage_projects')")
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

    @Operation(summary = "获取项目树", description = "获取项目树形结构（所有项目及其层级关系）。如果指定 rootId，返回以该根项目为根的子树；否则返回所有顶级项目。需要认证，根据项目可见性过滤。", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/tree")
    public ApiResponse<List<ProjectTreeNodeResponseDTO>> getProjectTree(
            @RequestParam(value = "rootId", required = false) Long rootId) {
        List<ProjectTreeNodeResponseDTO> result = projectService.getProjectTree(rootId);
        return ApiResponse.success(result);
    }

    @Operation(summary = "项目归档/取消归档", description = "归档或取消归档项目。归档时，如果项目存在未归档的子项目，则不能归档。需要认证，需要 delete_projects 权限或系统管理员。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasPermission(#id, 'Project', 'delete_projects')")
    @PutMapping("/{id}/archive")
    public ApiResponse<ProjectDetailResponseDTO> archiveProject(
            @PathVariable Long id,
            @Valid @RequestBody ProjectArchiveRequestDTO requestDTO) {
        ProjectDetailResponseDTO result = projectService.archiveProject(id, requestDTO);
        String message = Boolean.TRUE.equals(requestDTO.getArchived()) ? "项目归档成功" : "项目取消归档成功";
        return ApiResponse.success(message, result);
    }

    @Operation(summary = "复制项目", description = "复制项目（包括项目信息、成员、模块、跟踪器等）。需要认证，需要 create_projects 权限或系统管理员。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN') or authentication.principal.hasPermission('create_projects')")
    @PostMapping("/{id}/copy")
    public ApiResponse<ProjectDetailResponseDTO> copyProject(
            @PathVariable Long id,
            @Valid @RequestBody ProjectCopyRequestDTO requestDTO) {
        ProjectDetailResponseDTO result = projectService.copyProject(id, requestDTO);
        return ApiResponse.success("项目复制成功", result);
    }

    @Operation(summary = "获取项目统计信息", description = "获取项目统计信息（成员数、子项目数、模块数、跟踪器数等）。任务和工时统计等任务管理模块实现后再完善。需要认证，项目成员或系统管理员可访问。", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/{id}/statistics")
    public ApiResponse<ProjectStatisticsResponseDTO> getProjectStatistics(@PathVariable Long id) {
        ProjectStatisticsResponseDTO result = projectService.getProjectStatistics(id);
        return ApiResponse.success(result);
    }

    @Operation(summary = "创建项目模板", description = "创建项目模板。需要认证，系统管理员。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/templates")
    public ApiResponse<ProjectTemplateResponseDTO> createTemplate(
            @Valid @RequestBody ProjectTemplateCreateRequestDTO requestDTO) {
        ProjectTemplateResponseDTO result = projectService.createTemplate(requestDTO);
        return ApiResponse.success("项目模板创建成功", result);
    }

    @Operation(summary = "获取项目模板列表", description = "分页查询项目模板列表，支持按名称模糊查询。需要认证，系统管理员。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/templates")
    public ApiResponse<PageResponse<ProjectTemplateResponseDTO>> listTemplates(
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "name", required = false) String name) {
        PageResponse<ProjectTemplateResponseDTO> result = projectService.listTemplates(current, size, name);
        return ApiResponse.success(result);
    }

    @Operation(summary = "获取项目模板详情", description = "根据模板ID查询模板详细信息。需要认证，系统管理员。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/templates/{id}")
    public ApiResponse<ProjectTemplateResponseDTO> getTemplateById(@PathVariable Long id) {
        ProjectTemplateResponseDTO result = projectService.getTemplateById(id);
        return ApiResponse.success(result);
    }

    @Operation(summary = "更新项目模板", description = "更新项目模板信息（名称、描述、模块、跟踪器、默认角色等）。需要认证，系统管理员。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/templates/{id}")
    public ApiResponse<ProjectTemplateResponseDTO> updateTemplate(
            @PathVariable Long id,
            @Valid @RequestBody ProjectTemplateUpdateRequestDTO requestDTO) {
        ProjectTemplateResponseDTO result = projectService.updateTemplate(id, requestDTO);
        return ApiResponse.success("项目模板更新成功", result);
    }

    @Operation(summary = "从模板创建项目", description = "从项目模板创建新项目。需要认证，需要 create_projects 权限或系统管理员。如果模板有默认角色，会自动分配给项目创建者。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN') or authentication.principal.hasPermission('create_projects')")
    @PostMapping("/from-template/{templateId}")
    public ApiResponse<ProjectDetailResponseDTO> createProjectFromTemplate(
            @PathVariable Long templateId,
            @Valid @RequestBody ProjectFromTemplateRequestDTO requestDTO) {
        ProjectDetailResponseDTO result = projectService.createProjectFromTemplate(templateId, requestDTO);
        return ApiResponse.success("从模板创建项目成功", result);
    }

    @Operation(summary = "删除项目模板", description = "删除项目模板。需要认证，系统管理员。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/templates/{id}")
    public ApiResponse<Void> deleteTemplate(@PathVariable Long id) {
        projectService.deleteTemplate(id);
        return ApiResponse.success("项目模板删除成功", null);
    }

    @Operation(summary = "创建任务分类", description = "创建项目任务分类。分类是项目级别的，每个项目可以有自己的分类。分类可以设置默认指派人。需要认证，需要 manage_categories 或 manage_projects 权限或系统管理员。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN') or hasPermission(#projectId, 'Project', 'manage_categories') or hasPermission(#projectId, 'Project', 'manage_projects')")
    @PostMapping("/{projectId}/issue-categories")
    public ApiResponse<IssueCategoryResponseDTO> createIssueCategory(
            @PathVariable Long projectId,
            @Valid @RequestBody IssueCategoryCreateRequestDTO requestDTO) {
        IssueCategoryResponseDTO result = issueService.createIssueCategory(projectId, requestDTO);
        return ApiResponse.success("任务分类创建成功", result);
    }

    @Operation(summary = "更新任务分类", description = "更新项目任务分类。支持部分更新（只更新提供的字段）。需要认证，需要 manage_categories 或 manage_projects 权限或系统管理员。分类名称在同一项目内必须唯一。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN') or hasPermission(#projectId, 'Project', 'manage_categories') or hasPermission(#projectId, 'Project', 'manage_projects')")
    @PutMapping("/{projectId}/issue-categories/{id}")
    public ApiResponse<IssueCategoryResponseDTO> updateIssueCategory(
            @PathVariable Long projectId,
            @PathVariable Integer id,
            @Valid @RequestBody IssueCategoryUpdateRequestDTO requestDTO) {
        IssueCategoryResponseDTO result = issueService.updateIssueCategory(projectId, id, requestDTO);
        return ApiResponse.success("任务分类更新成功", result);
    }

    @Operation(summary = "删除任务分类", description = "删除项目任务分类。如果分类正在被任务使用，则不能删除。需要认证，需要 manage_categories 或 manage_projects 权限或系统管理员。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN') or hasPermission(#projectId, 'Project', 'manage_categories') or hasPermission(#projectId, 'Project', 'manage_projects')")
    @DeleteMapping("/{projectId}/issue-categories/{id}")
    public ApiResponse<Void> deleteIssueCategory(
            @PathVariable Long projectId,
            @PathVariable Integer id) {
        issueService.deleteIssueCategory(projectId, id);
        return ApiResponse.success("任务分类删除成功", null);
    }
}
