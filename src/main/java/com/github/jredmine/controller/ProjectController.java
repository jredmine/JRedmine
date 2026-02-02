package com.github.jredmine.controller;

import com.github.jredmine.dto.request.issue.IssueCategoryCreateRequestDTO;
import com.github.jredmine.dto.request.issue.IssueCategoryListRequestDTO;
import com.github.jredmine.dto.request.issue.IssueCategoryUpdateRequestDTO;
import com.github.jredmine.dto.request.project.MemberRoleAssignRequestDTO;
import com.github.jredmine.dto.request.project.VersionCreateRequestDTO;
import com.github.jredmine.dto.request.project.VersionListRequestDTO;
import com.github.jredmine.dto.request.project.VersionUpdateRequestDTO;
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
import com.github.jredmine.dto.response.project.VersionResponseDTO;
import com.github.jredmine.dto.response.project.ProjectListItemResponseDTO;
import com.github.jredmine.dto.response.project.ProjectMemberResponseDTO;
import com.github.jredmine.dto.response.project.ProjectStatisticsResponseDTO;
import com.github.jredmine.dto.response.project.ProjectTemplateResponseDTO;
import com.github.jredmine.dto.response.project.ProjectTreeNodeResponseDTO;
import com.github.jredmine.dto.response.project.VersionStatisticsResponseDTO;
import com.github.jredmine.dto.response.issue.IssueListItemResponseDTO;
import com.github.jredmine.dto.request.project.VersionIssuesRequestDTO;
import com.github.jredmine.dto.request.project.VersionIssuesBatchAssignRequestDTO;
import com.github.jredmine.dto.request.project.VersionIssuesBatchUnassignRequestDTO;
import com.github.jredmine.dto.request.project.VersionStatusUpdateRequestDTO;
import com.github.jredmine.dto.request.project.VersionSharingUpdateRequestDTO;
import com.github.jredmine.dto.request.project.VersionReleaseRequestDTO;
import com.github.jredmine.dto.response.project.VersionIssuesBatchAssignResponseDTO;
import com.github.jredmine.dto.response.project.VersionIssuesBatchUnassignResponseDTO;
import com.github.jredmine.dto.response.project.VersionProgressResponseDTO;
import com.github.jredmine.dto.response.project.VersionStatusUpdateResponseDTO;
import com.github.jredmine.dto.response.project.VersionSharingUpdateResponseDTO;
import com.github.jredmine.dto.response.project.VersionSharedProjectsResponseDTO;
import com.github.jredmine.dto.response.project.VersionRoadmapResponseDTO;
import com.github.jredmine.dto.response.project.VersionReleaseResponseDTO;
import com.github.jredmine.dto.response.wiki.WikiInfoResponseDTO;
import com.github.jredmine.service.IssueService;
import com.github.jredmine.service.ProjectService;
import com.github.jredmine.service.WikiService;
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
    private final WikiService wikiService;

    public ProjectController(ProjectService projectService, IssueService issueService, WikiService wikiService) {
        this.projectService = projectService;
        this.issueService = issueService;
        this.wikiService = wikiService;
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

    @Operation(summary = "获取项目 Wiki 信息", description = "获取项目 Wiki 信息（含首页标题等）。项目必须已启用 Wiki 模块；若尚无 wikis 记录则自动创建后返回。需要认证，需要 view_wiki_pages 权限或系统管理员。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN') or hasPermission(#id, 'Project', 'view_wiki_pages')")
    @GetMapping("/{id}/wiki")
    public ApiResponse<WikiInfoResponseDTO> getProjectWiki(@PathVariable Long id) {
        WikiInfoResponseDTO result = wikiService.getWikiInfo(id);
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

    @Operation(summary = "获取项目统计信息", description = "获取项目统计报表，包含任务数、完成率、待处理/进行中/已完成分布、按状态/跟踪器统计、工时统计（总工时、本月、本周）等。需要认证，项目成员或系统管理员可访问。", security = @SecurityRequirement(name = "bearerAuth"))
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

    @Operation(summary = "获取任务分类列表", description = "分页查询项目任务分类列表，支持按名称模糊查询。需要认证，需要 view_issues 权限或系统管理员。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN') or hasPermission(#projectId, 'Project', 'view_issues')")
    @GetMapping("/{projectId}/issue-categories")
    public ApiResponse<PageResponse<IssueCategoryResponseDTO>> listIssueCategories(
            @PathVariable Long projectId,
            @Valid IssueCategoryListRequestDTO requestDTO) {
        PageResponse<IssueCategoryResponseDTO> result = issueService.listIssueCategories(projectId, requestDTO);
        return ApiResponse.success(result);
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

    // ==================== 版本管理接口 ====================

    @Operation(summary = "获取版本列表", description = "分页查询项目版本列表，支持按名称、状态筛选。需要认证，需要 view_versions 权限或系统管理员。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN') or hasPermission(#projectId, 'Project', 'view_versions')")
    @GetMapping("/{projectId}/versions")
    public ApiResponse<PageResponse<VersionResponseDTO>> listVersions(
            @PathVariable Long projectId,
            @Valid VersionListRequestDTO requestDTO) {
        PageResponse<VersionResponseDTO> result = projectService.listVersions(projectId, requestDTO);
        return ApiResponse.success(result);
    }

    @Operation(summary = "获取版本详情", description = "根据版本ID获取版本详细信息。需要认证，需要 view_versions 权限或系统管理员。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN') or hasPermission(#projectId, 'Project', 'view_versions')")
    @GetMapping("/{projectId}/versions/{id}")
    public ApiResponse<VersionResponseDTO> getVersionById(
            @PathVariable Long projectId,
            @PathVariable Integer id) {
        VersionResponseDTO result = projectService.getVersionById(projectId, id);
        return ApiResponse.success(result);
    }

    @Operation(summary = "创建版本", description = "创建项目版本/里程碑。版本是项目级别的，每个项目可以有自己的版本。需要认证，需要 manage_versions 权限或系统管理员。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN') or hasPermission(#projectId, 'Project', 'manage_versions')")
    @PostMapping("/{projectId}/versions")
    public ApiResponse<VersionResponseDTO> createVersion(
            @PathVariable Long projectId,
            @Valid @RequestBody VersionCreateRequestDTO requestDTO) {
        VersionResponseDTO result = projectService.createVersion(projectId, requestDTO);
        return ApiResponse.success("版本创建成功", result);
    }

    @Operation(summary = "更新版本", description = "更新项目版本。支持部分更新（只更新提供的字段）。需要认证，需要 manage_versions 权限或系统管理员。版本名称在同一项目内必须唯一。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN') or hasPermission(#projectId, 'Project', 'manage_versions')")
    @PutMapping("/{projectId}/versions/{id}")
    public ApiResponse<VersionResponseDTO> updateVersion(
            @PathVariable Long projectId,
            @PathVariable Integer id,
            @Valid @RequestBody VersionUpdateRequestDTO requestDTO) {
        VersionResponseDTO result = projectService.updateVersion(projectId, id, requestDTO);
        return ApiResponse.success("版本更新成功", result);
    }

    @Operation(summary = "删除版本", description = "删除项目版本。如果版本正在被任务使用，则不能删除。需要认证，需要 manage_versions 权限或系统管理员。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN') or hasPermission(#projectId, 'Project', 'manage_versions')")
    @DeleteMapping("/{projectId}/versions/{id}")
    public ApiResponse<Void> deleteVersion(
            @PathVariable Long projectId,
            @PathVariable Integer id) {
        projectService.deleteVersion(projectId, id);
        return ApiResponse.success("版本删除成功", null);
    }

    @Operation(summary = "获取版本统计", description = "获取版本统计信息，包括任务数量、完成度、工时统计等。需要认证，需要 view_versions 权限或系统管理员。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN') or hasPermission(#projectId, 'Project', 'view_versions')")
    @GetMapping("/{projectId}/versions/{id}/statistics")
    public ApiResponse<VersionStatisticsResponseDTO> getVersionStatistics(
            @PathVariable Long projectId,
            @PathVariable Integer id) {
        VersionStatisticsResponseDTO result = projectService.getVersionStatistics(projectId, id);
        return ApiResponse.success(result);
    }

    @Operation(summary = "获取版本关联任务列表", description = "获取指定版本关联的所有任务列表，支持多种筛选条件和分页。需要认证，需要 view_issues 权限或系统管理员。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN') or hasPermission(#projectId, 'Project', 'view_issues')")
    @GetMapping("/{projectId}/versions/{id}/issues")
    public ApiResponse<PageResponse<IssueListItemResponseDTO>> getVersionIssues(
            @PathVariable Long projectId,
            @PathVariable Integer id,
            VersionIssuesRequestDTO requestDTO) {
        PageResponse<IssueListItemResponseDTO> result = projectService.getVersionIssues(projectId, id, requestDTO);
        return ApiResponse.success(result);
    }

    @Operation(summary = "批量关联任务到版本", description = "批量将任务关联到指定版本。需要认证，需要 manage_versions 和 edit_issues 权限或系统管理员。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN') or (hasPermission(#projectId, 'Project', 'manage_versions') and hasPermission(#projectId, 'Project', 'edit_issues'))")
    @PostMapping("/{projectId}/versions/{id}/issues")
    public ApiResponse<VersionIssuesBatchAssignResponseDTO> batchAssignIssuesToVersion(
            @PathVariable Long projectId,
            @PathVariable Integer id,
            @Valid @RequestBody VersionIssuesBatchAssignRequestDTO requestDTO) {
        VersionIssuesBatchAssignResponseDTO result = projectService.batchAssignIssuesToVersion(projectId, id,
                requestDTO);
        return ApiResponse.success("批量关联任务完成", result);
    }

    @Operation(summary = "批量取消任务与版本关联", description = "批量取消任务与指定版本的关联。需要认证，需要 manage_versions 和 edit_issues 权限或系统管理员。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN') or (hasPermission(#projectId, 'Project', 'manage_versions') and hasPermission(#projectId, 'Project', 'edit_issues'))")
    @DeleteMapping("/{projectId}/versions/{id}/issues")
    public ApiResponse<VersionIssuesBatchUnassignResponseDTO> batchUnassignIssuesFromVersion(
            @PathVariable Long projectId,
            @PathVariable Integer id,
            @Valid @RequestBody VersionIssuesBatchUnassignRequestDTO requestDTO) {
        VersionIssuesBatchUnassignResponseDTO result = projectService.batchUnassignIssuesFromVersion(projectId, id,
                requestDTO);
        return ApiResponse.success("批量取消关联完成", result);
    }

    @Operation(summary = "获取版本进度跟踪", description = "获取版本进度跟踪信息，包括任务完成情况、里程碑节点、进度图表数据等。需要认证，需要 view_versions 权限或系统管理员。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN') or hasPermission(#projectId, 'Project', 'view_versions')")
    @GetMapping("/{projectId}/versions/{id}/progress")
    public ApiResponse<VersionProgressResponseDTO> getVersionProgress(
            @PathVariable Long projectId,
            @PathVariable Integer id) {
        VersionProgressResponseDTO result = projectService.getVersionProgress(projectId, id);
        return ApiResponse.success(result);
    }

    @Operation(summary = "更新版本状态", description = "更新版本状态（open/locked/closed），并记录状态变更历史。需要认证，需要 manage_versions 权限或系统管理员。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN') or hasPermission(#projectId, 'Project', 'manage_versions')")
    @PutMapping("/{projectId}/versions/{id}/status")
    public ApiResponse<VersionStatusUpdateResponseDTO> updateVersionStatus(
            @PathVariable Long projectId,
            @PathVariable Integer id,
            @Valid @RequestBody VersionStatusUpdateRequestDTO requestDTO) {
        VersionStatusUpdateResponseDTO result = projectService.updateVersionStatus(projectId, id, requestDTO);
        return ApiResponse.success("版本状态更新成功", result);
    }

    @Operation(summary = "获取共享版本的项目列表", description = "根据版本的sharing配置，获取可以访问该版本的项目列表。需要认证，需要 view_versions 权限或系统管理员。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN') or hasPermission(#projectId, 'Project', 'view_versions')")
    @GetMapping("/{projectId}/versions/{id}/shared-projects")
    public ApiResponse<VersionSharedProjectsResponseDTO> getVersionSharedProjects(
            @PathVariable Long projectId,
            @PathVariable Integer id) {
        VersionSharedProjectsResponseDTO result = projectService.getVersionSharedProjects(projectId, id);
        return ApiResponse.success(result);
    }

    @Operation(summary = "更新版本共享方式", description = "更新版本共享方式（none/descendants/hierarchy/tree/system），并记录共享方式变更历史。需要认证，需要 manage_versions 权限或系统管理员。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN') or hasPermission(#projectId, 'Project', 'manage_versions')")
    @PutMapping("/{projectId}/versions/{id}/sharing")
    public ApiResponse<VersionSharingUpdateResponseDTO> updateVersionSharing(
            @PathVariable Long projectId,
            @PathVariable Integer id,
            @Valid @RequestBody VersionSharingUpdateRequestDTO requestDTO) {
        VersionSharingUpdateResponseDTO result = projectService.updateVersionSharing(projectId, id, requestDTO);
        return ApiResponse.success("版本共享方式更新成功", result);
    }

    @Operation(summary = "获取项目版本路线图", description = "获取项目版本路线图，按时间线展示版本计划，包括版本状态、任务统计等信息。需要认证，需要 view_versions 权限或系统管理员。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN') or hasPermission(#projectId, 'Project', 'view_versions')")
    @GetMapping("/{projectId}/versions/roadmap")
    public ApiResponse<VersionRoadmapResponseDTO> getVersionRoadmap(
            @PathVariable Long projectId) {
        VersionRoadmapResponseDTO result = projectService.getVersionRoadmap(projectId);
        return ApiResponse.success(result);
    }

    @Operation(summary = "发布版本", description = "发布版本，将版本状态标记为已发布（closed），并记录发布历史。需要认证，需要 manage_versions 权限或系统管理员。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN') or hasPermission(#projectId, 'Project', 'manage_versions')")
    @PostMapping("/{projectId}/versions/{id}/release")
    public ApiResponse<VersionReleaseResponseDTO> releaseVersion(
            @PathVariable Long projectId,
            @PathVariable Integer id,
            @Valid @RequestBody(required = false) VersionReleaseRequestDTO requestDTO) {
        // 如果请求体为空，创建默认的请求DTO
        if (requestDTO == null) {
            requestDTO = new VersionReleaseRequestDTO();
        }
        VersionReleaseResponseDTO result = projectService.releaseVersion(projectId, id, requestDTO);
        return ApiResponse.success("版本发布成功", result);
    }
}
