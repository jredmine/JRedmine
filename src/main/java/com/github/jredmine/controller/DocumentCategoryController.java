package com.github.jredmine.controller;

import com.github.jredmine.dto.request.document.DocumentCategoryCreateRequestDTO;
import com.github.jredmine.dto.request.document.DocumentCategoryUpdateRequestDTO;
import com.github.jredmine.dto.response.ApiResponse;
import com.github.jredmine.dto.response.document.DocumentCategoryResponseDTO;
import com.github.jredmine.service.DocumentService;
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

import java.util.List;

/**
 * 文档分类控制器
 * 提供项目下可用的文档分类列表及项目级分类的增删改。
 *
 * @author panfeng
 */
@Tag(name = "文档分类", description = "文档分类列表与项目级分类增删改")
@RestController
@RequestMapping("/api/projects/{projectId}/document-categories")
public class DocumentCategoryController {

    private final DocumentService documentService;

    public DocumentCategoryController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @Operation(summary = "文档分类列表", description = "查询项目可用的文档分类（全局分类 + 项目级分类），按 position、name 排序，供下拉与筛选。需项目已启用文档模块。需要 view_documents 权限或系统管理员。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN') or hasPermission(#projectId, 'Project', 'view_documents')")
    @GetMapping
    public ApiResponse<List<DocumentCategoryResponseDTO>> list(@PathVariable Long projectId) {
        List<DocumentCategoryResponseDTO> list = documentService.listDocumentCategories(projectId);
        return ApiResponse.success(list);
    }

    @Operation(summary = "创建项目级文档分类", description = "在当前项目下新增一个文档分类（仅项目级，非全局）。同一项目下分类名称不可重复。需要 edit_documents 权限或系统管理员。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN') or hasPermission(#projectId, 'Project', 'edit_documents')")
    @PostMapping
    public ApiResponse<DocumentCategoryResponseDTO> create(
            @PathVariable Long projectId,
            @Valid @RequestBody DocumentCategoryCreateRequestDTO request) {
        DocumentCategoryResponseDTO result = documentService.createDocumentCategory(projectId, request);
        return ApiResponse.success("文档分类创建成功", result);
    }

    @Operation(summary = "更新项目级文档分类", description = "仅可更新本项目下的文档分类（名称、排序）；不可更新全局分类。需要 edit_documents 权限或系统管理员。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN') or hasPermission(#projectId, 'Project', 'edit_documents')")
    @PutMapping("/{categoryId}")
    public ApiResponse<DocumentCategoryResponseDTO> update(
            @PathVariable Long projectId,
            @PathVariable Integer categoryId,
            @RequestBody(required = false) DocumentCategoryUpdateRequestDTO request) {
        DocumentCategoryResponseDTO result = documentService.updateDocumentCategory(projectId, categoryId, request);
        return ApiResponse.success("文档分类更新成功", result);
    }

    @Operation(summary = "删除项目级文档分类", description = "仅可删除本项目下的文档分类；若该分类下仍有文档则不允许删除。不可删除全局分类。需要 edit_documents 权限或系统管理员。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN') or hasPermission(#projectId, 'Project', 'edit_documents')")
    @DeleteMapping("/{categoryId}")
    public ApiResponse<Void> delete(
            @PathVariable Long projectId,
            @PathVariable Integer categoryId) {
        documentService.deleteDocumentCategory(projectId, categoryId);
        return ApiResponse.success();
    }
}
