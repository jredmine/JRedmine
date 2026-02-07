package com.github.jredmine.controller;

import com.github.jredmine.dto.response.ApiResponse;
import com.github.jredmine.dto.response.document.DocumentCategoryResponseDTO;
import com.github.jredmine.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 文档分类控制器
 * 提供项目下可用的文档分类列表，供下拉/筛选使用。
 *
 * @author panfeng
 */
@Tag(name = "文档分类", description = "文档分类列表（供下拉/筛选）")
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
}
