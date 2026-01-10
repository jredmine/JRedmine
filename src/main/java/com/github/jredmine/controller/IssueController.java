package com.github.jredmine.controller;

import com.github.jredmine.dto.request.issue.IssueAssignRequestDTO;
import com.github.jredmine.dto.request.issue.IssueBatchUpdateRequestDTO;
import com.github.jredmine.dto.request.issue.IssueCopyRequestDTO;
import com.github.jredmine.dto.request.issue.IssueCreateRequestDTO;
import com.github.jredmine.dto.request.issue.IssueListRequestDTO;
import com.github.jredmine.dto.request.issue.IssueRelationCreateRequestDTO;
import com.github.jredmine.dto.request.issue.IssueStatusUpdateRequestDTO;
import com.github.jredmine.dto.request.issue.IssueUpdateRequestDTO;
import com.github.jredmine.dto.request.issue.IssueJournalCreateRequestDTO;
import com.github.jredmine.dto.request.issue.IssueJournalListRequestDTO;
import com.github.jredmine.dto.request.issue.IssueWatcherCreateRequestDTO;
import com.github.jredmine.dto.request.issue.IssueWatcherBatchAddRequestDTO;
import com.github.jredmine.dto.request.issue.IssueWatcherBatchDeleteRequestDTO;
import com.github.jredmine.dto.response.ApiResponse;
import com.github.jredmine.dto.response.PageResponse;
import com.github.jredmine.dto.response.issue.IssueDetailResponseDTO;
import com.github.jredmine.dto.response.issue.IssueJournalResponseDTO;
import com.github.jredmine.dto.response.issue.IssueListItemResponseDTO;
import com.github.jredmine.dto.response.issue.IssueRelationResponseDTO;
import com.github.jredmine.dto.response.issue.IssueStatisticsResponseDTO;
import com.github.jredmine.dto.response.issue.IssueTreeNodeResponseDTO;
import com.github.jredmine.dto.response.issue.IssueImportResultDTO;
import com.github.jredmine.dto.response.workflow.WorkflowTransitionResponseDTO;
import com.github.jredmine.enums.ResultCode;
import com.github.jredmine.exception.BusinessException;
import com.github.jredmine.service.IssueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

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
            @Valid IssueListRequestDTO requestDTO) {
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

    @Operation(summary = "复制任务", description = "复制任务到当前项目或指定项目。支持复制子任务、关联关系、关注者、评论等。需要认证，需要 view_issues 权限查看源任务，需要 add_issues 权限在目标项目创建任务或系统管理员。新任务的状态会重置为跟踪器的默认状态，完成度重置为0。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN') or (authentication.principal.hasPermission('view_issues') and authentication.principal.hasPermission('add_issues'))")
    @PostMapping("/{id}/copy")
    public ApiResponse<IssueDetailResponseDTO> copyIssue(
            @PathVariable Long id,
            @Valid @RequestBody IssueCopyRequestDTO requestDTO) {
        IssueDetailResponseDTO result = issueService.copyIssue(id, requestDTO);
        return ApiResponse.success("任务复制成功", result);
    }

    @Operation(summary = "获取任务详情", description = "根据任务ID查询任务详细信息。需要认证，需要 view_issues 权限或系统管理员。私有任务仅项目成员可见。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN') or authentication.principal.hasPermission('view_issues')")
    @GetMapping("/{id}")
    public ApiResponse<IssueDetailResponseDTO> getIssueById(@PathVariable Long id) {
        IssueDetailResponseDTO result = issueService.getIssueDetailById(id);
        return ApiResponse.success(result);
    }

    @Operation(summary = "获取任务可用的状态转换", description = "根据任务的工作流规则和用户权限，返回当前用户可以执行的状态转换列表。会过滤掉需要指派人或创建者权限但当前用户不满足条件的转换。需要认证，需要 view_issues 权限或系统管理员。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN') or authentication.principal.hasPermission('view_issues')")
    @GetMapping("/{id}/transitions")
    public ApiResponse<WorkflowTransitionResponseDTO> getIssueAvailableTransitions(@PathVariable Long id) {
        WorkflowTransitionResponseDTO result = issueService.getIssueAvailableTransitions(id);
        return ApiResponse.success(result);
    }

    @Operation(summary = "获取任务树", description = "获取任务的树形结构（所有任务及其层级关系）。如果指定 projectId，返回该项目的任务树；如果指定 rootId，返回以该根任务为根的子树；否则返回所有顶级任务。需要认证，需要 view_issues 权限或系统管理员。私有任务仅项目成员可见。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN') or authentication.principal.hasPermission('view_issues')")
    @GetMapping("/tree")
    public ApiResponse<List<IssueTreeNodeResponseDTO>> getIssueTree(
            @RequestParam(value = "projectId", required = false) Long projectId,
            @RequestParam(value = "rootId", required = false) Long rootId) {
        List<IssueTreeNodeResponseDTO> result = issueService.getIssueTree(projectId, rootId);
        return ApiResponse.success(result);
    }

    @Operation(summary = "获取任务统计信息", description = "获取任务的统计信息，按状态、优先级、跟踪器等维度统计。如果指定 projectId，统计该项目的任务；否则统计所有可访问项目的任务。需要认证，需要 view_issues 权限或系统管理员。私有任务仅项目成员可见。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN') or authentication.principal.hasPermission('view_issues')")
    @GetMapping("/statistics")
    public ApiResponse<IssueStatisticsResponseDTO> getIssueStatistics(
            @RequestParam(value = "projectId", required = false) Long projectId) {
        IssueStatisticsResponseDTO result = issueService.getIssueStatistics(projectId);
        return ApiResponse.success(result);
    }

    @Operation(summary = "导出任务列表", description = "导出任务列表为 CSV 格式。支持按筛选条件导出，参数同任务列表接口。需要认证，需要 view_issues 权限或系统管理员。私有任务仅项目成员可见。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN') or authentication.principal.hasPermission('view_issues')")
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportIssues(@Valid IssueListRequestDTO requestDTO) {
        // 导出为 CSV
        byte[] csvBytes = issueService.exportIssues(requestDTO);

        // 设置响应头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv;charset=UTF-8"));
        headers.setContentDispositionFormData("attachment", "issues_" + System.currentTimeMillis() + ".csv");
        headers.setContentLength(csvBytes.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(csvBytes);
    }

    @Operation(summary = "下载任务导入模板", description = "下载任务导入的 Excel 模板文件。需要认证。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/import/template")
    public ResponseEntity<byte[]> downloadImportTemplate() {
        // 生成导入模板
        byte[] templateBytes = issueService.generateImportTemplate();

        // 设置响应头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", "issues_import_template.xlsx");
        headers.setContentLength(templateBytes.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(templateBytes);
    }

    @Operation(summary = "导入任务（文件上传）", description = "通过上传 Excel 文件批量导入任务到指定项目。支持的字段包括：任务标题、跟踪器、状态、优先级、指派人、描述、开始日期、截止日期、预估工时、完成度。需要认证，需要 add_issues 权限或系统管理员。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN') or authentication.principal.hasPermission('add_issues')")
    @PostMapping("/import")
    public ApiResponse<IssueImportResultDTO> importIssues(
            @RequestParam("projectId") Long projectId,
            @RequestParam("file") MultipartFile file) {
        
        // 验证文件
        if (file.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "文件不能为空");
        }
        
        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "只支持 Excel 文件格式 (.xlsx, .xls)");
        }

        IssueImportResultDTO result = issueService.importIssuesFromExcel(projectId, file);
        return ApiResponse.success("任务导入完成", result);
    }

    @Operation(summary = "导入任务（文件路径）", description = "通过指定 Excel 文件路径批量导入任务到指定项目。支持的字段包括：任务标题、跟踪器、状态、优先级、指派人、描述、开始日期、截止日期、预估工时、完成度。需要认证，需要 add_issues 权限或系统管理员。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN') or authentication.principal.hasPermission('add_issues')")
    @PostMapping("/import-from-path")
    public ApiResponse<IssueImportResultDTO> importIssuesFromPath(
            @RequestParam("projectId") Long projectId,
            @RequestParam("filePath") String filePath) {
        
        IssueImportResultDTO result = issueService.importIssuesFromPath(projectId, filePath);
        return ApiResponse.success("任务导入完成", result);
    }

    @Operation(summary = "获取任务子任务列表", description = "查询任务的所有子任务。需要认证，需要 view_issues 权限或系统管理员。支持递归查询（包含子任务的子任务）。私有任务仅项目成员可见。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN') or authentication.principal.hasPermission('view_issues')")
    @GetMapping("/{id}/children")
    public ApiResponse<List<IssueListItemResponseDTO>> getIssueChildren(
            @PathVariable Long id,
            @RequestParam(value = "recursive", defaultValue = "false") Boolean recursive) {
        List<IssueListItemResponseDTO> result = issueService.getIssueChildren(id, recursive);
        return ApiResponse.success(result);
    }

    @Operation(summary = "添加任务关联", description = "添加任务关联关系。支持多种关联类型（相关、重复、阻塞、前置等）。支持设置延迟天数（用于甘特图）。需要认证，需要 edit_issues 权限或系统管理员。不能将任务关联到自己。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN') or authentication.principal.hasPermission('edit_issues')")
    @PostMapping("/{id}/relations")
    public ApiResponse<IssueRelationResponseDTO> createIssueRelation(
            @PathVariable Long id,
            @Valid @RequestBody IssueRelationCreateRequestDTO requestDTO) {
        IssueRelationResponseDTO result = issueService.createIssueRelation(id, requestDTO);
        return ApiResponse.success("任务关联创建成功", result);
    }

    @Operation(summary = "删除任务关联", description = "删除任务关联关系。需要认证，需要 edit_issues 权限或系统管理员。只能删除属于该任务的关联（无论是源任务还是目标任务）。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN') or authentication.principal.hasPermission('edit_issues')")
    @DeleteMapping("/{id}/relations/{relationId}")
    public ApiResponse<Void> deleteIssueRelation(
            @PathVariable Long id,
            @PathVariable Integer relationId) {
        issueService.deleteIssueRelation(id, relationId);
        return ApiResponse.success("任务关联删除成功", null);
    }

    @Operation(summary = "批量添加任务关注者", description = "批量添加任务关注者。关注者会收到任务更新通知。可以关注自己。需要认证，需要 view_issues 权限或系统管理员。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN') or authentication.principal.hasPermission('view_issues')")
    @PostMapping("/{id}/watchers/batch")
    public ApiResponse<Void> batchAddIssueWatchers(
            @PathVariable Long id,
            @Valid @RequestBody IssueWatcherBatchAddRequestDTO requestDTO) {
        issueService.batchAddIssueWatchers(id, requestDTO);
        return ApiResponse.success("任务关注者批量添加成功", null);
    }

    @Operation(summary = "批量删除任务关注者", description = "批量删除任务关注者。只能删除自己的关注，或需要 edit_issues 权限。需要认证，需要 view_issues 权限或系统管理员。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN') or authentication.principal.hasPermission('view_issues')")
    @DeleteMapping("/{id}/watchers/batch")
    public ApiResponse<Void> batchDeleteIssueWatchers(
            @PathVariable Long id,
            @Valid @RequestBody IssueWatcherBatchDeleteRequestDTO requestDTO) {
        issueService.batchDeleteIssueWatchers(id, requestDTO);
        return ApiResponse.success("任务关注者批量删除成功", null);
    }

    @Operation(summary = "添加任务关注者（单个）", description = "添加单个任务关注者。关注者会收到任务更新通知。可以关注自己。需要认证，需要 view_issues 权限或系统管理员。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN') or authentication.principal.hasPermission('view_issues')")
    @PostMapping("/{id}/watchers")
    public ApiResponse<Void> addIssueWatcher(
            @PathVariable Long id,
            @Valid @RequestBody IssueWatcherCreateRequestDTO requestDTO) {
        issueService.addIssueWatcher(id, requestDTO);
        return ApiResponse.success("任务关注者添加成功", null);
    }

    @Operation(summary = "删除任务关注者（单个）", description = "删除单个任务关注者。只能删除自己的关注，或需要 edit_issues 权限。需要认证，需要 view_issues 权限或系统管理员。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN') or authentication.principal.hasPermission('view_issues')")
    @DeleteMapping("/{id}/watchers/{userId}")
    public ApiResponse<Void> deleteIssueWatcher(
            @PathVariable Long id,
            @PathVariable Long userId) {
        issueService.deleteIssueWatcher(id, userId);
        return ApiResponse.success("任务关注者删除成功", null);
    }

    @Operation(summary = "创建任务评论", description = "为任务添加评论/备注。支持公开评论和私有备注（只有项目成员可见）。需要认证，需要 add_notes 或 edit_issues 权限或系统管理员。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN') or authentication.principal.hasPermission('add_notes') or authentication.principal.hasPermission('edit_issues')")
    @PostMapping("/{id}/journals")
    public ApiResponse<IssueJournalResponseDTO> createIssueJournal(
            @PathVariable Long id,
            @Valid @RequestBody IssueJournalCreateRequestDTO requestDTO) {
        IssueJournalResponseDTO result = issueService.createIssueJournal(id, requestDTO);
        return ApiResponse.success("任务评论创建成功", result);
    }

    @Operation(summary = "获取任务活动日志列表", description = "分页查询任务的活动日志（包括评论、状态变更等）。私有备注只有项目成员可见。需要认证，需要 view_issues 权限或系统管理员。按创建时间倒序排序（最新的在前）。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN') or authentication.principal.hasPermission('view_issues')")
    @GetMapping("/{id}/journals")
    public ApiResponse<PageResponse<IssueJournalResponseDTO>> listIssueJournals(
            @PathVariable Long id,
            @Valid IssueJournalListRequestDTO requestDTO) {
        PageResponse<IssueJournalResponseDTO> result = issueService.listIssueJournals(id, requestDTO);
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

    @Operation(summary = "批量更新任务", description = "批量更新多个任务的相同字段。支持部分更新（只更新提供的字段）。需要认证，需要 edit_issues 权限或系统管理员。所有任务必须全部更新成功，否则全部回滚。使用乐观锁防止并发更新冲突。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN') or authentication.principal.hasPermission('edit_issues')")
    @PutMapping("/batch")
    public ApiResponse<List<IssueDetailResponseDTO>> batchUpdateIssues(
            @Valid @RequestBody IssueBatchUpdateRequestDTO requestDTO) {
        List<IssueDetailResponseDTO> result = issueService.batchUpdateIssues(requestDTO);
        return ApiResponse.success("批量更新任务成功", result);
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

    @Operation(summary = "分配任务", description = "分配任务给指定用户或取消分配。需要认证，需要 edit_issues 权限或系统管理员。支持取消分配（assignedToId 为 null 或 0）。使用乐观锁防止并发更新冲突。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN') or authentication.principal.hasPermission('edit_issues')")
    @PutMapping("/{id}/assign")
    public ApiResponse<IssueDetailResponseDTO> assignIssue(
            @PathVariable Long id,
            @Valid @RequestBody IssueAssignRequestDTO requestDTO) {
        IssueDetailResponseDTO result = issueService.assignIssue(id, requestDTO);
        return ApiResponse.success("任务分配成功", result);
    }

    @Operation(summary = "删除任务", description = "删除任务（物理删除）。需要认证，需要 delete_issues 权限或系统管理员。如果任务存在子任务，则不能删除，需要先删除子任务。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN') or authentication.principal.hasPermission('delete_issues')")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteIssue(@PathVariable Long id) {
        issueService.deleteIssue(id);
        return ApiResponse.success("任务删除成功", null);
    }
}
