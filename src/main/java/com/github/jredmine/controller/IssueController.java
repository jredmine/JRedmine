package com.github.jredmine.controller;

import com.github.jredmine.dto.request.issue.IssueCreateRequestDTO;
import com.github.jredmine.dto.request.issue.IssueListRequestDTO;
import com.github.jredmine.dto.request.issue.IssueStatusUpdateRequestDTO;
import com.github.jredmine.dto.request.issue.IssueUpdateRequestDTO;
import com.github.jredmine.dto.response.ApiResponse;
import com.github.jredmine.dto.response.PageResponse;
import com.github.jredmine.dto.response.issue.IssueDetailResponseDTO;
import com.github.jredmine.dto.response.issue.IssueListItemResponseDTO;
import com.github.jredmine.service.IssueService;
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
import org.springframework.web.bind.annotation.RestController;

/**
 * 任务管理控制器
 * 负责任务的查询、创建、更新等管理功能
 *
 * @author panfeng
 */
@Tag(name = "任务管理", description = "任务信息查询、创建、更新等管理接口")
@RestController
@RequestMapping("/api/issues")
public class IssueController {

    private final IssueService issueService;

    public IssueController(IssueService issueService) {
        this.issueService = issueService;
    }

    @Operation(summary = "获取任务列表", description = "分页查询任务列表，支持按项目、状态、跟踪器、优先级、指派人、创建者等条件筛选，支持关键词搜索（标题、描述），支持排序。需要认证，需要 view_issues 权限或系统管理员。私有任务仅项目成员可见。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN') or authentication.principal.hasPermission('view_issues')")
    @GetMapping
    public ApiResponse<PageResponse<IssueListItemResponseDTO>> listIssues(
            IssueListRequestDTO requestDTO) {
        PageResponse<IssueListItemResponseDTO> result = issueService.listIssues(requestDTO);
        return ApiResponse.success(result);
    }

    @Operation(summary = "创建任务", description = "创建新任务。需要认证，需要 add_issues 权限或系统管理员。创建者自动设置为当前用户。如果未指定状态，将使用跟踪器的默认状态。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN') or authentication.principal.hasPermission('add_issues')")
    @PostMapping
    public ApiResponse<IssueDetailResponseDTO> createIssue(
            @Valid @RequestBody IssueCreateRequestDTO requestDTO) {
        IssueDetailResponseDTO result = issueService.createIssue(requestDTO);
        return ApiResponse.success("任务创建成功", result);
    }

    @Operation(summary = "获取任务详情", description = "根据任务ID查询任务详细信息。需要认证，需要 view_issues 权限或系统管理员。私有任务仅项目成员可见。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN') or authentication.principal.hasPermission('view_issues')")
    @GetMapping("/{id}")
    public ApiResponse<IssueDetailResponseDTO> getIssueById(@PathVariable Long id) {
        IssueDetailResponseDTO result = issueService.getIssueDetailById(id);
        return ApiResponse.success(result);
    }

    @Operation(summary = "更新任务", description = "更新任务信息。支持部分更新（只更新提供的字段）。需要认证，需要 edit_issues 权限或系统管理员。如果状态改变，需要遵循工作流规则。使用乐观锁防止并发更新冲突。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN') or authentication.principal.hasPermission('edit_issues')")
    @PutMapping("/{id}")
    public ApiResponse<IssueDetailResponseDTO> updateIssue(
            @PathVariable Long id,
            @Valid @RequestBody IssueUpdateRequestDTO requestDTO) {
        IssueDetailResponseDTO result = issueService.updateIssue(id, requestDTO);
        return ApiResponse.success("任务更新成功", result);
    }

    @Operation(summary = "更新任务状态", description = "更新任务状态，完整支持工作流验证。需要认证，需要 edit_issues 权限或系统管理员。会验证工作流规则（状态转换是否允许）、用户角色权限、指派人/创建者限制。如果状态是关闭状态，自动设置 closed_on 和 done_ratio = 100。支持添加备注/评论。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN') or authentication.principal.hasPermission('edit_issues')")
    @PutMapping("/{id}/status")
    public ApiResponse<IssueDetailResponseDTO> updateIssueStatus(
            @PathVariable Long id,
            @Valid @RequestBody IssueStatusUpdateRequestDTO requestDTO) {
        IssueDetailResponseDTO result = issueService.updateIssueStatus(id, requestDTO);
        return ApiResponse.success("任务状态更新成功", result);
    }

    @Operation(summary = "删除任务", description = "删除任务（物理删除）。需要认证，需要 delete_issues 权限或系统管理员。如果任务存在子任务，则不能删除，需要先删除子任务。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN') or authentication.principal.hasPermission('delete_issues')")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteIssue(@PathVariable Long id) {
        issueService.deleteIssue(id);
        return ApiResponse.success("任务删除成功", null);
    }
}
