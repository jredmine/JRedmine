package com.github.jredmine.controller;

import com.github.jredmine.dto.request.tracker.TrackerCreateRequestDTO;
import com.github.jredmine.dto.request.tracker.TrackerUpdateRequestDTO;
import com.github.jredmine.dto.response.ApiResponse;
import com.github.jredmine.dto.response.PageResponse;
import com.github.jredmine.dto.response.tracker.TrackerDetailResponseDTO;
import com.github.jredmine.dto.response.tracker.TrackerListItemResponseDTO;
import com.github.jredmine.service.TrackerService;
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

/**
 * 跟踪器管理控制器
 * 负责跟踪器的创建、查询、更新等管理功能
 *
 * @author panfeng
 */
@Tag(name = "跟踪器管理", description = "跟踪器信息查询、创建、更新等管理接口")
@RestController
@RequestMapping("/api/trackers")
public class TrackerController {

    private final TrackerService trackerService;

    public TrackerController(TrackerService trackerService) {
        this.trackerService = trackerService;
    }

    @Operation(
            summary = "获取跟踪器列表",
            description = "分页查询跟踪器列表，支持按名称模糊查询。需要认证。",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping
    public ApiResponse<PageResponse<TrackerListItemResponseDTO>> listTrackers(
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "name", required = false) String name) {
        PageResponse<TrackerListItemResponseDTO> result = trackerService.listTrackers(current, size, name);
        return ApiResponse.success(result);
    }

    @Operation(
            summary = "获取跟踪器详情",
            description = "根据跟踪器ID查询跟踪器详细信息。需要认证。",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/{id}")
    public ApiResponse<TrackerDetailResponseDTO> getTrackerById(@PathVariable Long id) {
        TrackerDetailResponseDTO result = trackerService.getTrackerById(id);
        return ApiResponse.success(result);
    }

    @Operation(
            summary = "创建跟踪器",
            description = "创建新跟踪器。需要认证，仅管理员可访问。",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping
    public ApiResponse<TrackerDetailResponseDTO> createTracker(
            @Valid @RequestBody TrackerCreateRequestDTO requestDTO) {
        TrackerDetailResponseDTO result = trackerService.createTracker(requestDTO);
        return ApiResponse.success("跟踪器创建成功", result);
    }

    @Operation(
            summary = "更新跟踪器",
            description = "更新跟踪器信息。需要认证，仅管理员可访问。",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PutMapping("/{id}")
    public ApiResponse<TrackerDetailResponseDTO> updateTracker(
            @PathVariable Long id,
            @Valid @RequestBody TrackerUpdateRequestDTO requestDTO) {
        TrackerDetailResponseDTO result = trackerService.updateTracker(id, requestDTO);
        return ApiResponse.success("跟踪器更新成功", result);
    }

    @Operation(
            summary = "删除跟踪器",
            description = "删除跟踪器。需要认证，仅管理员可访问。如果跟踪器正在被项目使用，则不能删除。",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteTracker(@PathVariable Long id) {
        trackerService.deleteTracker(id);
        return ApiResponse.success("跟踪器删除成功", null);
    }
}

