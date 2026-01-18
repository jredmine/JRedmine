package com.github.jredmine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.jredmine.dto.request.attachment.AttachmentQueryRequestDTO;
import com.github.jredmine.dto.request.attachment.AttachmentUpdateRequestDTO;
import com.github.jredmine.dto.request.attachment.AttachmentUploadRequestDTO;
import com.github.jredmine.dto.response.PageResponse;
import com.github.jredmine.dto.response.attachment.AttachmentBatchUploadResponseDTO;
import com.github.jredmine.dto.response.attachment.AttachmentResponseDTO;
import com.github.jredmine.dto.response.user.UserSimpleResponseDTO;
import com.github.jredmine.entity.Attachment;
import com.github.jredmine.entity.User;
import com.github.jredmine.exception.BusinessException;
import com.github.jredmine.mapper.AttachmentMapper;
import com.github.jredmine.mapper.user.UserMapper;
import com.github.jredmine.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 附件服务
 *
 * @author panfeng
 * @since 2026-01-18
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AttachmentService {

    private final AttachmentMapper attachmentMapper;
    private final UserMapper userMapper;
    private final SecurityUtils securityUtils;

    @Value("${attachment.storage.path:files}")
    private String storagePath;
    
    /**
     * 获取存储路径的绝对路径
     */
    private Path getStoragePath() {
        Path path = Paths.get(storagePath);
        // 如果是相对路径，则基于用户目录或项目目录
        if (!path.isAbsolute()) {
            // 优先使用系统属性指定的路径
            String basePath = System.getProperty("attachment.storage.base", 
                    System.getProperty("user.home", "."));
            path = Paths.get(basePath, storagePath);
        }
        return path;
    }

    @Value("${attachment.max-file-size:10485760}")
    private Long maxFileSize; // 默认10MB

    @Value("${attachment.allowed-extensions:}")
    private String allowedExtensions;

    /**
     * 上传附件
     */
    @Transactional(rollbackFor = Exception.class)
    public AttachmentResponseDTO uploadAttachment(MultipartFile file, AttachmentUploadRequestDTO request) {
        Long currentUserId = securityUtils.getCurrentUserId();

        // 1. 验证文件
        validateFile(file);

        // 2. 生成存储文件名和路径
        String originalFilename = file.getOriginalFilename();
        String diskFilename = generateDiskFilename(originalFilename);
        String diskDirectory = generateDiskDirectory();

        // 3. 计算文件摘要
        String digest = calculateFileDigest(file);

        // 4. 保存文件到磁盘
        Path baseStoragePath = getStoragePath();
        Path fullPath = baseStoragePath.resolve(diskDirectory);
        try {
            // 确保目录存在
            Files.createDirectories(fullPath);
            Path filePath = fullPath.resolve(diskFilename);
            // 写入文件
            file.transferTo(filePath.toFile());
            log.debug("文件保存成功: {}", filePath.toAbsolutePath());
        } catch (IOException e) {
            log.error("保存文件失败: path={}, error={}", fullPath.toAbsolutePath(), e.getMessage(), e);
            throw new BusinessException("文件保存失败: " + e.getMessage());
        }

        // 5. 创建附件记录
        Attachment attachment = new Attachment();
        attachment.setContainerId(request.getContainerId());
        attachment.setContainerType(request.getContainerType());
        attachment.setFilename(originalFilename);
        attachment.setDiskFilename(diskFilename);
        attachment.setDiskDirectory(diskDirectory);
        attachment.setFilesize(file.getSize());
        attachment.setContentType(file.getContentType());
        attachment.setDigest(digest);
        attachment.setDownloads(0);
        attachment.setAuthorId(currentUserId);
        attachment.setDescription(request.getDescription());
        attachment.setCreatedOn(LocalDateTime.now());

        attachmentMapper.insert(attachment);

        log.info("上传附件成功: id={}, filename={}, size={}",
                attachment.getId(), originalFilename, file.getSize());

        return convertToResponseDTO(attachment);
    }
    
    /**
     * 批量上传附件
     */
    @Transactional(rollbackFor = Exception.class)
    public AttachmentBatchUploadResponseDTO batchUploadAttachments(
            List<MultipartFile> files, 
            String containerType, 
            Long containerId, 
            String description) {
        
        List<AttachmentResponseDTO> successes = new ArrayList<>();
        List<AttachmentBatchUploadResponseDTO.FailureDetail> failures = new ArrayList<>();
        
        for (MultipartFile file : files) {
            try {
                // 构建请求DTO
                AttachmentUploadRequestDTO request = new AttachmentUploadRequestDTO();
                request.setContainerType(containerType);
                request.setContainerId(containerId);
                request.setDescription(description);
                
                // 上传单个文件
                AttachmentResponseDTO attachment = uploadAttachment(file, request);
                successes.add(attachment);
                
            } catch (Exception e) {
                log.error("批量上传文件失败: filename={}, error={}", 
                        file.getOriginalFilename(), e.getMessage(), e);
                
                AttachmentBatchUploadResponseDTO.FailureDetail failure = 
                        AttachmentBatchUploadResponseDTO.FailureDetail.builder()
                                .filename(file.getOriginalFilename())
                                .reason(e.getMessage())
                                .build();
                failures.add(failure);
            }
        }
        
        int totalCount = files.size();
        int successCount = successes.size();
        int failureCount = failures.size();
        
        log.info("批量上传附件完成: 总数={}, 成功={}, 失败={}", totalCount, successCount, failureCount);
        
        return AttachmentBatchUploadResponseDTO.builder()
                .totalCount(totalCount)
                .successCount(successCount)
                .failureCount(failureCount)
                .successes(successes)
                .failures(failures)
                .build();
    }

    /**
     * 根据ID获取附件详情
     */
    public AttachmentResponseDTO getAttachmentById(Long id) {
        Attachment attachment = attachmentMapper.selectById(id);
        if (attachment == null) {
            throw new BusinessException("附件不存在");
        }
        return convertToResponseDTO(attachment);
    }

    /**
     * 查询附件列表
     */
    public PageResponse<AttachmentResponseDTO> queryAttachments(AttachmentQueryRequestDTO request) {
        Page<Attachment> page = new Page<>(request.getCurrent(), request.getSize());

        LambdaQueryWrapper<Attachment> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(request.getContainerType())) {
            wrapper.eq(Attachment::getContainerType, request.getContainerType());
        }

        if (request.getContainerId() != null) {
            wrapper.eq(Attachment::getContainerId, request.getContainerId());
        }

        if (request.getAuthorId() != null) {
            wrapper.eq(Attachment::getAuthorId, request.getAuthorId());
        }

        if (StringUtils.hasText(request.getFilename())) {
            wrapper.like(Attachment::getFilename, request.getFilename());
        }

        if (StringUtils.hasText(request.getContentType())) {
            wrapper.like(Attachment::getContentType, request.getContentType());
        }

        wrapper.orderByDesc(Attachment::getCreatedOn);

        IPage<Attachment> result = attachmentMapper.selectPage(page, wrapper);

        List<AttachmentResponseDTO> records = result.getRecords().stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());

        return PageResponse.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    /**
     * 更新附件信息
     */
    @Transactional(rollbackFor = Exception.class)
    public AttachmentResponseDTO updateAttachment(Long id, AttachmentUpdateRequestDTO request) {
        Attachment attachment = attachmentMapper.selectById(id);
        if (attachment == null) {
            throw new BusinessException("附件不存在");
        }

        // 只允许作者或管理员更新
        Long currentUserId = securityUtils.getCurrentUserId();
        boolean isAdmin = securityUtils.isAdmin();
        if (!isAdmin && !attachment.getAuthorId().equals(currentUserId)) {
            throw new BusinessException("无权限修改此附件");
        }

        if (StringUtils.hasText(request.getDescription())) {
            attachment.setDescription(request.getDescription());
        }

        if (StringUtils.hasText(request.getFilename())) {
            attachment.setFilename(request.getFilename());
        }

        attachmentMapper.updateById(attachment);

        log.info("更新附件成功: id={}", id);

        return convertToResponseDTO(attachment);
    }

    /**
     * 删除附件
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteAttachment(Long id) {
        Attachment attachment = attachmentMapper.selectById(id);
        if (attachment == null) {
            throw new BusinessException("附件不存在");
        }

        // 只允许作者或管理员删除
        Long currentUserId = securityUtils.getCurrentUserId();
        boolean isAdmin = securityUtils.isAdmin();
        if (!isAdmin && !attachment.getAuthorId().equals(currentUserId)) {
            throw new BusinessException("无权限删除此附件");
        }

        // 删除数据库记录
        attachmentMapper.deleteById(id);

        // 删除物理文件
        try {
            Path baseStoragePath = getStoragePath();
            Path filePath = baseStoragePath.resolve(attachment.getDiskDirectory()).resolve(attachment.getDiskFilename());
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.error("删除文件失败: {}", e.getMessage(), e);
            // 继续执行，不抛出异常
        }

        log.info("删除附件成功: id={}", id);
    }

    /**
     * 下载附件（获取文件）
     */
    public File downloadAttachment(Long id) {
        Attachment attachment = attachmentMapper.selectById(id);
        if (attachment == null) {
            throw new BusinessException("附件不存在");
        }

        // 增加下载次数
        attachment.setDownloads(attachment.getDownloads() + 1);
        attachmentMapper.updateById(attachment);

        Path baseStoragePath = getStoragePath();
        Path filePath = baseStoragePath.resolve(attachment.getDiskDirectory()).resolve(attachment.getDiskFilename());
        File file = filePath.toFile();

        if (!file.exists()) {
            log.error("文件不存在: {}", filePath);
            throw new BusinessException("文件不存在");
        }

        return file;
    }

    /**
     * 验证文件
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }

        // 验证文件大小
        if (file.getSize() > maxFileSize) {
            throw new BusinessException("文件大小超过限制：" + (maxFileSize / 1024 / 1024) + "MB");
        }

        // 验证文件扩展名
        if (StringUtils.hasText(allowedExtensions)) {
            String filename = file.getOriginalFilename();
            if (filename != null) {
                String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
                String[] allowed = allowedExtensions.split(",");
                boolean valid = false;
                for (String ext : allowed) {
                    if (ext.trim().equalsIgnoreCase(extension)) {
                        valid = true;
                        break;
                    }
                }
                if (!valid) {
                    throw new BusinessException("不支持的文件类型：" + extension);
                }
            }
        }
    }

    /**
     * 生成磁盘文件名
     */
    private String generateDiskFilename(String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return UUID.randomUUID().toString() + extension;
    }

    /**
     * 生成磁盘目录（按日期分目录）
     */
    private String generateDiskDirectory() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM");
        return now.format(formatter);
    }

    /**
     * 计算文件摘要（SHA-256）
     */
    private String calculateFileDigest(MultipartFile file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(file.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException | IOException e) {
            log.error("计算文件摘要失败: {}", e.getMessage(), e);
            return "";
        }
    }

    /**
     * 转换为响应DTO
     */
    private AttachmentResponseDTO convertToResponseDTO(Attachment attachment) {
        AttachmentResponseDTO dto = new AttachmentResponseDTO();
        dto.setId(attachment.getId());
        dto.setContainerType(attachment.getContainerType());
        dto.setContainerId(attachment.getContainerId());
        dto.setFilename(attachment.getFilename());
        dto.setFilesize(attachment.getFilesize());
        dto.setContentType(attachment.getContentType());
        dto.setDownloads(attachment.getDownloads());
        dto.setCreatedOn(attachment.getCreatedOn());
        dto.setDescription(attachment.getDescription());
        dto.setDownloadUrl("/api/attachments/" + attachment.getId() + "/download");

        // 查询上传者信息
        User author = userMapper.selectById(attachment.getAuthorId());
        if (author != null) {
            UserSimpleResponseDTO authorDTO = new UserSimpleResponseDTO();
            authorDTO.setId(author.getId());
            authorDTO.setLogin(author.getLogin());
            authorDTO.setFirstname(author.getFirstname());
            authorDTO.setLastname(author.getLastname());
            dto.setAuthor(authorDTO);
        }

        return dto;
    }
}
