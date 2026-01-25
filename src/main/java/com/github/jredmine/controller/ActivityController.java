package com.github.jredmine.controller;

import com.github.jredmine.dto.request.activity.*;
import com.github.jredmine.dto.response.ApiResponse;
import com.github.jredmine.dto.response.PageResponse;
import com.github.jredmine.dto.response.activity.ActivityItemResponseDTO;
import com.github.jredmine.dto.response.activity.ActivityStatsResponseDTO;
import com.github.jredmine.dto.response.activity.CommentResponseDTO;
import jakarta.validation.Valid;
import com.github.jredmine.service.ActivityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * 活动流控制器
 *
 * @author panfeng
 */
@Slf4j
@RestController
@RequestMapping("/api/activities")
@RequiredArgsConstructor
@Tag(name = "活动流管理", description = "活动流查看和统计功能")
public class ActivityController {

    private final ActivityService activityService;

    @Operation(summary = "获取全局活动流", description = "获取系统全局活动流，支持多种筛选条件")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResponse<ActivityItemResponseDTO>>> getActivities(
            ActivityQueryRequestDTO requestDTO,
            @Parameter(description = "活动类型，多个用逗号分隔（comment,field_change,create,delete）")
            @RequestParam(required = false) String activityTypes) {

        // 解析活动类型
        if (StringUtils.hasText(activityTypes)) {
            List<String> typeList = Arrays.asList(activityTypes.trim().split(","));
            requestDTO.setActivityTypes(typeList);
        }

        log.debug("查询全局活动流: {}", requestDTO);

        PageResponse<ActivityItemResponseDTO> result = activityService.getActivities(requestDTO);
        
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @Operation(summary = "获取项目活动流", description = "获取指定项目的活动流")
    @GetMapping("/project/{projectId}")
    @PreAuthorize("hasRole('ADMIN') or authentication.principal.hasPermission('view_issues')")
    public ResponseEntity<ApiResponse<PageResponse<ActivityItemResponseDTO>>> getProjectActivities(
            @Parameter(description = "项目ID", required = true)
            @PathVariable Long projectId,
            ProjectActivityRequestDTO requestDTO,
            @Parameter(description = "活动类型，多个用逗号分隔")
            @RequestParam(required = false) String activityTypes) {

        // 解析活动类型
        if (StringUtils.hasText(activityTypes)) {
            List<String> typeList = Arrays.asList(activityTypes.trim().split(","));
            requestDTO.setActivityTypes(typeList);
        }

        log.debug("查询项目活动流: projectId={}, request={}", projectId, requestDTO);

        // 转换为通用查询DTO
        ActivityQueryRequestDTO queryDTO = new ActivityQueryRequestDTO();
        queryDTO.setCurrent(requestDTO.getCurrent());
        queryDTO.setSize(requestDTO.getSize());
        queryDTO.setStartDate(requestDTO.getStartDate());
        queryDTO.setEndDate(requestDTO.getEndDate());
        queryDTO.setActivityTypes(requestDTO.getActivityTypes());
        queryDTO.setUserId(requestDTO.getUserId());
        queryDTO.setObjectType(requestDTO.getObjectType());
        queryDTO.setKeyword(requestDTO.getKeyword());

        PageResponse<ActivityItemResponseDTO> result = activityService.getProjectActivities(projectId, queryDTO);
        
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @Operation(summary = "获取用户活动流", description = "获取指定用户的活动流")
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or authentication.principal.id == #userId")
    public ResponseEntity<ApiResponse<PageResponse<ActivityItemResponseDTO>>> getUserActivities(
            @Parameter(description = "用户ID", required = true)
            @PathVariable Long userId,
            UserActivityRequestDTO requestDTO,
            @Parameter(description = "活动类型，多个用逗号分隔")
            @RequestParam(required = false) String activityTypes) {

        // 解析活动类型
        if (StringUtils.hasText(activityTypes)) {
            List<String> typeList = Arrays.asList(activityTypes.trim().split(","));
            requestDTO.setActivityTypes(typeList);
        }

        log.debug("查询用户活动流: userId={}, request={}", userId, requestDTO);

        // 转换为通用查询DTO
        ActivityQueryRequestDTO queryDTO = new ActivityQueryRequestDTO();
        queryDTO.setCurrent(requestDTO.getCurrent());
        queryDTO.setSize(requestDTO.getSize());
        queryDTO.setStartDate(requestDTO.getStartDate());
        queryDTO.setEndDate(requestDTO.getEndDate());
        queryDTO.setActivityTypes(requestDTO.getActivityTypes());
        queryDTO.setProjectId(requestDTO.getProjectId());
        queryDTO.setObjectType(requestDTO.getObjectType());

        PageResponse<ActivityItemResponseDTO> result = activityService.getUserActivities(userId, queryDTO);
        
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @Operation(summary = "获取对象活动流", description = "获取指定对象（如任务、项目）的活动历史")
    @GetMapping("/object/{objectType}/{objectId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResponse<ActivityItemResponseDTO>>> getObjectActivities(
            @Parameter(description = "对象类型（Issue,Project,Wiki等）", required = true)
            @PathVariable String objectType,
            
            @Parameter(description = "对象ID", required = true)
            @PathVariable Long objectId,
            ObjectActivityRequestDTO requestDTO,
            @Parameter(description = "活动类型，多个用逗号分隔")
            @RequestParam(required = false) String activityTypes) {

        // 解析活动类型
        if (StringUtils.hasText(activityTypes)) {
            List<String> typeList = Arrays.asList(activityTypes.trim().split(","));
            requestDTO.setActivityTypes(typeList);
        }

        log.debug("查询对象活动流: type={}, id={}, request={}", objectType, objectId, requestDTO);

        // 转换为通用查询DTO
        ActivityQueryRequestDTO queryDTO = new ActivityQueryRequestDTO();
        queryDTO.setCurrent(requestDTO.getCurrent());
        queryDTO.setSize(requestDTO.getSize());
        queryDTO.setStartDate(requestDTO.getStartDate());
        queryDTO.setEndDate(requestDTO.getEndDate());
        queryDTO.setActivityTypes(requestDTO.getActivityTypes());
        queryDTO.setUserId(requestDTO.getUserId());

        PageResponse<ActivityItemResponseDTO> result = activityService.getObjectActivities(objectType, objectId, queryDTO);
        
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @Operation(summary = "获取活动详情", description = "获取单个活动的详细信息")
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ActivityItemResponseDTO>> getActivityById(
            @Parameter(description = "活动ID", required = true)
            @PathVariable Long id) {

        log.debug("查询活动详情: id={}", id);

        ActivityItemResponseDTO activity = activityService.getActivityById(id);
        
        if (activity == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(ApiResponse.success(activity));
    }

    @Operation(summary = "获取活动统计", description = "获取活动流的统计信息")
    @GetMapping("/stats")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ActivityStatsResponseDTO>> getActivityStats(
            ActivityStatsRequestDTO requestDTO) {

        log.debug("查询活动统计: request={}", requestDTO);

        // 转换为通用查询DTO
        ActivityQueryRequestDTO queryDTO = new ActivityQueryRequestDTO();
        queryDTO.setStartDate(requestDTO.getStartDate());
        queryDTO.setEndDate(requestDTO.getEndDate());
        queryDTO.setProjectId(requestDTO.getProjectId());
        queryDTO.setUserId(requestDTO.getUserId());

        ActivityStatsResponseDTO stats = activityService.getActivityStats(queryDTO);
        
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @Operation(summary = "获取活动类型列表", description = "获取系统支持的活动类型")
    @GetMapping("/types")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<String>>> getActivityTypes() {
        
        List<String> types = Arrays.asList(
            "comment",      // 评论
            "field_change", // 字段变更
            "create",       // 创建
            "update",       // 更新
            "delete"        // 删除
        );
        
        return ResponseEntity.ok(ApiResponse.success(types));
    }

    @Operation(summary = "添加评论", description = "为指定对象添加评论")
    @PostMapping("/comment")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CommentResponseDTO>> createComment(
            @Valid @RequestBody CommentCreateRequestDTO requestDTO) {

        log.debug("添加评论请求: {}", requestDTO);

        CommentResponseDTO comment = activityService.createComment(requestDTO);
        
        return ResponseEntity.ok(ApiResponse.success(comment));
    }

    @Operation(summary = "更新评论", description = "更新指定的评论内容")
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CommentResponseDTO>> updateComment(
            @Parameter(description = "评论ID", required = true)
            @PathVariable Long id,
            @Valid @RequestBody CommentUpdateRequestDTO requestDTO) {

        log.debug("更新评论请求: id={}, request={}", id, requestDTO);

        CommentResponseDTO comment = activityService.updateComment(id, requestDTO);
        
        return ResponseEntity.ok(ApiResponse.success(comment));
    }

    @Operation(summary = "删除评论", description = "删除指定的评论")
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @Parameter(description = "评论ID", required = true)
            @PathVariable Long id) {

        log.debug("删除评论请求: id={}", id);

        activityService.deleteComment(id);
        
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}