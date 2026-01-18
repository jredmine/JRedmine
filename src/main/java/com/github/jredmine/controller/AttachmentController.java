package com.github.jredmine.controller;

import com.github.jredmine.dto.request.attachment.AttachmentQueryRequestDTO;
import com.github.jredmine.dto.request.attachment.AttachmentUpdateRequestDTO;
import com.github.jredmine.dto.request.attachment.AttachmentUploadRequestDTO;
import com.github.jredmine.dto.response.ApiResponse;
import com.github.jredmine.dto.response.PageResponse;
import com.github.jredmine.dto.response.attachment.AttachmentBatchUploadResponseDTO;
import com.github.jredmine.dto.response.attachment.AttachmentResponseDTO;
import com.github.jredmine.service.AttachmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 附件管理控制器
 *
 * @author panfeng
 * @since 2026-01-18
 */
@Tag(name = "附件管理", description = "附件上传、下载、查询和删除等接口")
@RestController
@RequestMapping("/api/attachments")
@RequiredArgsConstructor
public class AttachmentController {
    
    private final AttachmentService attachmentService;

    /**
     * 上传附件
     */
    @Operation(summary = "上传附件", description = "上传文件并创建附件记录")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<AttachmentResponseDTO> uploadAttachment(
            @Parameter(description = "上传的文件", required = true) @RequestParam("file") MultipartFile file,
            @Parameter(description = "容器类型（如：Issue、Project、Document、WikiPage等）") @RequestParam(value = "containerType", required = false) String containerType,
            @Parameter(description = "容器ID") @RequestParam(value = "containerId", required = false) Long containerId,
            @Parameter(description = "文件描述") @RequestParam(value = "description", required = false) String description) {

        // 构建请求DTO
        AttachmentUploadRequestDTO request = new AttachmentUploadRequestDTO();
        request.setContainerType(containerType);
        request.setContainerId(containerId);
        request.setDescription(description);

        AttachmentResponseDTO result = attachmentService.uploadAttachment(file, request);
        return ApiResponse.success("附件上传成功", result);
    }
    
    /**
     * 批量上传附件
     */
    @Operation(summary = "批量上传附件", description = "批量上传多个文件并创建附件记录，所有文件共享相同的容器类型、容器ID和描述")
    @PostMapping(value = "/batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<AttachmentBatchUploadResponseDTO> batchUploadAttachments(
            @Parameter(description = "上传的文件列表", required = true)
            @RequestParam("files") List<MultipartFile> files,
            @Parameter(description = "容器类型（如：Issue、Project、Document、WikiPage等）")
            @RequestParam(value = "containerType", required = false) String containerType,
            @Parameter(description = "容器ID")
            @RequestParam(value = "containerId", required = false) Long containerId,
            @Parameter(description = "文件描述")
            @RequestParam(value = "description", required = false) String description) {
        
        AttachmentBatchUploadResponseDTO result = attachmentService.batchUploadAttachments(
                files, containerType, containerId, description);
        
        if (result.getFailureCount() == 0) {
            return ApiResponse.success("批量上传成功", result);
        } else if (result.getSuccessCount() == 0) {
            return ApiResponse.error(400, "批量上传失败，所有文件均未成功上传", result);
        } else {
            return ApiResponse.success("批量上传部分成功", result);
        }
    }

    /**
     * 获取附件详情
     */
    @Operation(summary = "获取附件详情", description = "根据ID获取附件详细信息")
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<AttachmentResponseDTO> getAttachment(
            @Parameter(description = "附件ID", required = true) @PathVariable Long id) {
        AttachmentResponseDTO result = attachmentService.getAttachmentById(id);
        return ApiResponse.success(result);
    }

    /**
     * 查询附件列表
     */
    @Operation(summary = "查询附件列表", description = "支持多条件筛选和分页查询附件，可通过containerType和containerId查询指定容器的附件")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResponse<AttachmentResponseDTO>> queryAttachments(
            @Valid AttachmentQueryRequestDTO request) {
        PageResponse<AttachmentResponseDTO> result = attachmentService.queryAttachments(request);
        return ApiResponse.success(result);
    }

    /**
     * 更新附件信息
     */
    @Operation(summary = "更新附件信息", description = "更新附件的描述和文件名等信息（不更新文件内容）")
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<AttachmentResponseDTO> updateAttachment(
            @Parameter(description = "附件ID", required = true) @PathVariable Long id,
            @Valid @RequestBody AttachmentUpdateRequestDTO request) {
        AttachmentResponseDTO result = attachmentService.updateAttachment(id, request);
        return ApiResponse.success("附件更新成功", result);
    }

    /**
     * 删除附件
     */
    @Operation(summary = "删除附件", description = "删除附件记录和文件")
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> deleteAttachment(
            @Parameter(description = "附件ID", required = true) @PathVariable Long id) {
        attachmentService.deleteAttachment(id);
        return ApiResponse.success("附件删除成功", null);
    }

    /**
     * 下载附件
     */
    @Operation(summary = "下载附件", description = "下载附件文件")
    @GetMapping("/{id}/download")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> downloadAttachment(
            @Parameter(description = "附件ID", required = true) @PathVariable Long id) {

        // 获取附件信息
        AttachmentResponseDTO attachment = attachmentService.getAttachmentById(id);

        // 获取文件
        File file = attachmentService.downloadAttachment(id);
        Resource resource = new FileSystemResource(file);

        // 处理文件名编码
        String filename = attachment.getFilename();
        String encodedFilename;
        try {
            encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8.toString())
                    .replaceAll("\\+", "%20");
        } catch (UnsupportedEncodingException e) {
            encodedFilename = filename;
        }

        // 设置响应头
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename*=UTF-8''" + encodedFilename);
        headers.setContentType(MediaType.parseMediaType(
                attachment.getContentType() != null ? attachment.getContentType()
                        : MediaType.APPLICATION_OCTET_STREAM_VALUE));

        return ResponseEntity.ok()
                .headers(headers)
                .body(resource);
    }
    
    /**
     * 批量下载附件（打包为ZIP）
     */
    @Operation(summary = "批量下载附件", description = "批量下载多个附件并打包为ZIP文件")
    @PostMapping("/batch/download")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> batchDownloadAttachments(
            @Parameter(description = "附件ID列表", required = true)
            @RequestBody List<Long> attachmentIds) {
        
        // 打包为ZIP
        File zipFile = attachmentService.batchDownloadAttachments(attachmentIds);
        Resource resource = new FileSystemResource(zipFile);
        
        // 处理文件名编码
        String filename = zipFile.getName();
        String encodedFilename;
        try {
            encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8.toString())
                    .replaceAll("\\+", "%20");
        } catch (UnsupportedEncodingException e) {
            encodedFilename = filename;
        }
        
        // 设置响应头
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename*=UTF-8''" + encodedFilename);
        headers.setContentType(MediaType.parseMediaType("application/zip"));
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(resource);
    }
}
