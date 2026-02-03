package com.github.jredmine.controller;

import com.github.jredmine.dto.request.wiki.WikiPageCreateRequestDTO;
import com.github.jredmine.dto.request.wiki.WikiPageUpdateRequestDTO;
import com.github.jredmine.dto.response.ApiResponse;
import com.github.jredmine.dto.response.PageResponse;
import com.github.jredmine.dto.response.wiki.WikiPageDetailResponseDTO;
import com.github.jredmine.dto.response.wiki.WikiPageListItemResponseDTO;
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

/**
 * Wiki 页面控制器：列表、创建、获取（含最新内容）、更新、删除
 *
 * @author panfeng
 */
@Tag(name = "Wiki 页面", description = "Wiki 页面列表、创建、获取、更新、删除")
@RestController
@RequestMapping("/api/projects/{projectId}/wiki")
public class WikiController {

    private final WikiService wikiService;

    public WikiController(WikiService wikiService) {
        this.wikiService = wikiService;
    }

    @Operation(summary = "Wiki 页面列表", description = "分页列出项目 Wiki 页面，可选按父页面 ID 筛选。需要认证，需要 view_wiki_pages 权限或系统管理员。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN') or hasPermission(#projectId, 'Project', 'view_wiki_pages')")
    @GetMapping("/pages")
    public ApiResponse<PageResponse<WikiPageListItemResponseDTO>> listPages(
            @PathVariable Long projectId,
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam(value = "size", defaultValue = "20") Integer size,
            @RequestParam(value = "parentId", required = false) Long parentId) {
        PageResponse<WikiPageListItemResponseDTO> result = wikiService.listPages(projectId, current, size, parentId);
        return ApiResponse.success(result);
    }

    @Operation(summary = "创建 Wiki 页面", description = "在项目 Wiki 下创建新页面，可选初始正文与备注。同一 Wiki 下标题唯一。需要认证，需要 edit_wiki_pages 权限或系统管理员。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN') or hasPermission(#projectId, 'Project', 'edit_wiki_pages')")
    @PostMapping("/pages")
    public ApiResponse<WikiPageDetailResponseDTO> createPage(
            @PathVariable Long projectId,
            @Valid @RequestBody WikiPageCreateRequestDTO request) {
        WikiPageDetailResponseDTO result = wikiService.createPage(projectId, request);
        return ApiResponse.success("Wiki 页面创建成功", result);
    }

    @Operation(summary = "获取 Wiki 页面详情", description = "根据页面 ID 或标题获取页面详情（含最新内容）。titleOrId 为数字时按 ID 查，否则按标题查。需要认证，需要 view_wiki_pages 权限或系统管理员。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN') or hasPermission(#projectId, 'Project', 'view_wiki_pages')")
    @GetMapping("/pages/{titleOrId}")
    public ApiResponse<WikiPageDetailResponseDTO> getPage(
            @PathVariable Long projectId,
            @PathVariable String titleOrId) {
        WikiPageDetailResponseDTO result = wikiService.getPageWithLatestContent(projectId, titleOrId);
        return ApiResponse.success(result);
    }

    @Operation(summary = "更新 Wiki 页面", description = "更新页面元数据（父页面、保护）和/或新增一条内容版本（text、comments）。需要认证，需要 edit_wiki_pages 权限或系统管理员。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN') or hasPermission(#projectId, 'Project', 'edit_wiki_pages')")
    @PutMapping("/pages/{titleOrId}")
    public ApiResponse<WikiPageDetailResponseDTO> updatePage(
            @PathVariable Long projectId,
            @PathVariable String titleOrId,
            @RequestBody(required = false) WikiPageUpdateRequestDTO request) {
        WikiPageDetailResponseDTO result = wikiService.updatePage(projectId, titleOrId, request);
        return ApiResponse.success("Wiki 页面更新成功", result);
    }

    @Operation(summary = "删除 Wiki 页面", description = "删除指定页面及其所有内容版本。需要认证，需要 delete_wiki_pages 权限或系统管理员。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN') or hasPermission(#projectId, 'Project', 'delete_wiki_pages')")
    @DeleteMapping("/pages/{titleOrId}")
    public ApiResponse<Void> deletePage(
            @PathVariable Long projectId,
            @PathVariable String titleOrId) {
        wikiService.deletePage(projectId, titleOrId);
        return ApiResponse.success();
    }
}
