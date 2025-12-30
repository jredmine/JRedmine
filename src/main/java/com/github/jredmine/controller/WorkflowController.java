package com.github.jredmine.controller;

import com.github.jredmine.dto.request.workflow.WorkflowCreateRequestDTO;
import com.github.jredmine.dto.request.workflow.WorkflowUpdateRequestDTO;
import com.github.jredmine.dto.response.ApiResponse;
import com.github.jredmine.dto.response.PageResponse;
import com.github.jredmine.dto.response.workflow.IssueStatusResponseDTO;
import com.github.jredmine.dto.response.workflow.WorkflowResponseDTO;
import com.github.jredmine.dto.response.workflow.WorkflowTransitionResponseDTO;
import com.github.jredmine.service.IssueStatusService;
import com.github.jredmine.service.WorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 工作流管理控制器
 * 负责工作流规则的创建、查询、更新等管理功能
 *
 * @author panfeng
 */
@Tag(name = "工作流管理", description = "工作流规则查询、创建、更新等管理接口")
@Slf4j
@RestController
@RequestMapping("/api/workflows")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;
    private final IssueStatusService issueStatusService;

    @Operation(
            summary = "获取工作流列表",
            description = "分页查询工作流列表，支持按跟踪器、状态、角色、类型等条件筛选。需要认证，仅管理员可访问。",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PageResponse<WorkflowResponseDTO>> listWorkflows(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer trackerId,
            @RequestParam(required = false) Integer oldStatusId,
            @RequestParam(required = false) Integer newStatusId,
            @RequestParam(required = false) Integer roleId,
            @RequestParam(required = false) String type) {
        PageResponse<WorkflowResponseDTO> response = workflowService.listWorkflows(
                current, size, trackerId, oldStatusId, newStatusId, roleId, type);
        return ApiResponse.success(response);
    }

    @Operation(
            summary = "获取工作流详情",
            description = "根据工作流ID查询工作流详细信息。需要认证，仅管理员可访问。",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<WorkflowResponseDTO> getWorkflowById(@PathVariable Integer id) {
        WorkflowResponseDTO response = workflowService.getWorkflowById(id);
        return ApiResponse.success(response);
    }

    @Operation(
            summary = "创建工作流规则",
            description = "创建工作流规则，包括状态转换规则和字段规则。需要认证，仅管理员可访问。",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<WorkflowResponseDTO> createWorkflow(
            @Valid @RequestBody WorkflowCreateRequestDTO requestDTO) {
        WorkflowResponseDTO response = workflowService.createWorkflow(requestDTO);
        return ApiResponse.success("工作流规则创建成功", response);
    }

    @Operation(
            summary = "更新工作流规则",
            description = "更新工作流规则信息。需要认证，仅管理员可访问。",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<WorkflowResponseDTO> updateWorkflow(
            @PathVariable Integer id,
            @Valid @RequestBody WorkflowUpdateRequestDTO requestDTO) {
        WorkflowResponseDTO response = workflowService.updateWorkflow(id, requestDTO);
        return ApiResponse.success("工作流规则更新成功", response);
    }

    @Operation(
            summary = "删除工作流规则",
            description = "删除工作流规则。需要认证，仅管理员可访问。",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteWorkflow(@PathVariable Integer id) {
        workflowService.deleteWorkflow(id);
        return ApiResponse.success("工作流规则删除成功", null);
    }

    @Operation(
            summary = "获取可用的状态转换",
            description = "根据跟踪器、当前状态和用户角色，返回可以转换到的目标状态列表。需要认证。",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/transitions")
    public ApiResponse<WorkflowTransitionResponseDTO> getAvailableTransitions(
            @RequestParam Integer trackerId,
            @RequestParam Integer currentStatusId,
            @RequestParam(required = false) String roleIds) {
        // 解析角色ID列表
        List<Integer> roleIdList = null;
        if (roleIds != null && !roleIds.trim().isEmpty()) {
            List<Integer> parsed = List.of(roleIds.split(","))
                    .stream()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Integer::parseInt)
                    .toList();
            // 如果解析后不为空，才设置 roleIdList
            if (!parsed.isEmpty()) {
                roleIdList = parsed;
            }
        }

        WorkflowTransitionResponseDTO response = workflowService.getAvailableTransitions(
                trackerId, currentStatusId, roleIdList);
        return ApiResponse.success(response);
    }

    @Operation(
            summary = "获取所有任务状态列表",
            description = "查询系统中所有可用的任务状态列表。需要认证。",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/statuses")
    public ApiResponse<List<IssueStatusResponseDTO>> listAllStatuses() {
        List<IssueStatusResponseDTO> response = issueStatusService.listAllStatuses();
        return ApiResponse.success(response);
    }

    @Operation(
            summary = "获取任务状态详情",
            description = "根据状态ID查询任务状态详细信息。需要认证。",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/statuses/{id}")
    public ApiResponse<IssueStatusResponseDTO> getStatusById(@PathVariable Integer id) {
        IssueStatusResponseDTO response = issueStatusService.getStatusById(id);
        return ApiResponse.success(response);
    }
}

