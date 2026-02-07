package com.github.jredmine.controller;

import com.github.jredmine.dto.request.document.DocumentCreateRequestDTO;
import com.github.jredmine.dto.request.document.DocumentUpdateRequestDTO;
import com.github.jredmine.dto.response.ApiResponse;
import com.github.jredmine.dto.response.document.DocumentDetailResponseDTO;
import com.github.jredmine.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 文档管理控制器
 * 文档为“元数据+附件”：创建文档即写一条 documents 记录；上传文件走现有附件接口，containerType=Document、containerId=文档ID。
 *
 * @author panfeng
 */
@Tag(name = "文档管理", description = "项目文档的创建、列表、详情、更新、删除")
@RestController
@RequestMapping("/api/projects/{projectId}/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @Operation(summary = "创建文档", description = "在项目下新增一条文档记录（title 必填，description、categoryId 可选；categoryId=0 表示未分类）。需项目已启用文档模块。需要 add_documents 权限或系统管理员。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN') or hasPermission(#projectId, 'Project', 'add_documents')")
    @PostMapping
    public ApiResponse<DocumentDetailResponseDTO> create(
            @PathVariable Long projectId,
            @Valid @RequestBody DocumentCreateRequestDTO request) {
        DocumentDetailResponseDTO result = documentService.create(projectId, request);
        return ApiResponse.success("文档创建成功", result);
    }

    @Operation(summary = "更新文档", description = "更新文档的 title、description、categoryId；仅更新请求体中提供的非空字段。需要 edit_documents 权限或系统管理员。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN') or hasPermission(#projectId, 'Project', 'edit_documents')")
    @PutMapping("/{documentId}")
    public ApiResponse<DocumentDetailResponseDTO> update(
            @PathVariable Long projectId,
            @PathVariable Integer documentId,
            @RequestBody(required = false) DocumentUpdateRequestDTO request) {
        DocumentDetailResponseDTO result = documentService.update(projectId, documentId, request);
        return ApiResponse.success("文档更新成功", result);
    }
}
