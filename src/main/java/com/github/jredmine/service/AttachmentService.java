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
import com.github.jredmine.enums.SettingKey;
import com.github.jredmine.mapper.AttachmentMapper;
import com.github.jredmine.mapper.user.UserMapper;
import com.github.jredmine.service.SettingService;
import com.github.jredmine.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import net.coobird.thumbnailator.Thumbnails;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    private final SettingService settingService;
    private final OssService ossService;
    private final CosService cosService;

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

    @Value("${attachment.thumbnail.enabled:true}")
    private Boolean thumbnailEnabled;

    @Value("${attachment.thumbnail.width:200}")
    private Integer thumbnailWidth;

    @Value("${attachment.thumbnail.height:200}")
    private Integer thumbnailHeight;

    /**
     * 获取存储类型（local/oss/cos）
     */
    private String getStorageType() {
        try {
            String type = settingService.getSetting(SettingKey.ATTACHMENT_STORAGE_TYPE.getKey());
            return (type != null && !type.isEmpty()) ? type : "local";
        } catch (Exception e) {
            log.warn("获取存储类型失败，使用默认值local: {}", e.getMessage());
            return "local";
        }
    }

    /**
     * 判断是否使用OSS存储（根据配置）
     */
    private boolean isOssStorage() {
        return "oss".equalsIgnoreCase(getStorageType());
    }
    
    /**
     * 判断是否使用COS存储（根据配置）
     */
    private boolean isCosStorage() {
        return "cos".equalsIgnoreCase(getStorageType());
    }
    
    /**
     * 判断附件是否使用OSS存储（根据记录的存储类型）
     */
    private boolean isOssStorage(Attachment attachment) {
        String storageType = attachment.getStorageType();
        // 兼容历史数据：如果storageType为空，默认使用配置判断
        if (storageType == null || storageType.isEmpty()) {
            return isOssStorage();
        }
        return "oss".equalsIgnoreCase(storageType);
    }
    
    /**
     * 判断附件是否使用COS存储（根据记录的存储类型）
     */
    private boolean isCosStorage(Attachment attachment) {
        String storageType = attachment.getStorageType();
        // 兼容历史数据：如果storageType为空，默认使用配置判断
        if (storageType == null || storageType.isEmpty()) {
            return isCosStorage();
        }
        return "cos".equalsIgnoreCase(storageType);
    }
    
    /**
     * 判断附件是否使用云存储（OSS或COS）
     */
    private boolean isCloudStorage(Attachment attachment) {
        return isOssStorage(attachment) || isCosStorage(attachment);
    }
    
    /**
     * 从云存储下载文件到临时文件（通用方法）
     */
    private File downloadFromCloud(Attachment attachment, String cloudType) {
        try {
            String objectKey = attachment.getDiskDirectory() + "/" + attachment.getDiskFilename();
            InputStream inputStream;
            
            if ("oss".equals(cloudType)) {
                inputStream = ossService.downloadFile(objectKey);
            } else if ("cos".equals(cloudType)) {
                inputStream = cosService.downloadFile(objectKey);
            } else {
                throw new BusinessException("不支持的云存储类型: " + cloudType);
            }
            
            // 创建临时文件
            File tempFile = File.createTempFile(cloudType + "_download_", "_" + attachment.getDiskFilename());
            tempFile.deleteOnExit();
            
            // 将云存储文件流写入临时文件
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
            } finally {
                inputStream.close();
            }
            
            return tempFile;
        } catch (Exception e) {
            log.error("从{}下载文件失败: attachmentId={}, error={}", cloudType.toUpperCase(), 
                    attachment.getId(), e.getMessage(), e);
            throw new BusinessException("下载文件失败: " + e.getMessage());
        }
    }

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

        // 4. 保存文件（根据存储类型选择本地、OSS或COS）
        String objectKey = diskDirectory + "/" + diskFilename;
        
        if (isOssStorage()) {
            // OSS存储
            try {
                uploadToOss(file, objectKey, originalFilename, diskDirectory, diskFilename);
                log.debug("文件上传到OSS成功: objectKey={}", objectKey);
            } catch (Exception e) {
                log.error("上传文件到OSS失败: objectKey={}, error={}", objectKey, e.getMessage(), e);
                throw new BusinessException("上传文件到OSS失败: " + e.getMessage());
            }
        } else if (isCosStorage()) {
            // COS存储
            try {
                uploadToCos(file, objectKey, originalFilename, diskDirectory, diskFilename);
                log.debug("文件上传到COS成功: objectKey={}", objectKey);
            } catch (Exception e) {
                log.error("上传文件到COS失败: objectKey={}, error={}", objectKey, e.getMessage(), e);
                throw new BusinessException("上传文件到COS失败: " + e.getMessage());
            }
        } else {
            // 本地存储
            Path baseStoragePath = getStoragePath();
            Path fullPath = baseStoragePath.resolve(diskDirectory);
            try {
                // 确保目录存在
                Files.createDirectories(fullPath);
                Path filePath = fullPath.resolve(diskFilename);
                // 写入文件
                file.transferTo(filePath.toFile());
                log.debug("文件保存成功: {}", filePath.toAbsolutePath());

                // 如果是图片文件，处理图片（生成缩略图、添加水印）
                if (isImageFile(file.getContentType(), originalFilename)) {
                    // 生成缩略图
                    if (thumbnailEnabled) {
                        try {
                            generateThumbnail(filePath.toFile(), fullPath, diskFilename);
                        } catch (Exception e) {
                            log.warn("生成缩略图失败: filename={}, error={}", originalFilename, e.getMessage());
                        }
                    }
                    
                    // 添加水印
                    if (isWatermarkEnabled()) {
                        try {
                            addWatermark(filePath.toFile());
                        } catch (Exception e) {
                            log.warn("添加水印失败: filename={}, error={}", originalFilename, e.getMessage());
                        }
                    }
                }
            } catch (IOException e) {
                log.error("保存文件失败: path={}, error={}", fullPath.toAbsolutePath(), e.getMessage(), e);
                throw new BusinessException("文件保存失败: " + e.getMessage());
            }
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
        // 记录存储类型（快照，避免配置变更后无法找到文件）
        String storageType = getStorageType();
        attachment.setStorageType(storageType != null ? storageType : "local");

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

                AttachmentBatchUploadResponseDTO.FailureDetail failure = AttachmentBatchUploadResponseDTO.FailureDetail
                        .builder()
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

        attachmentMapper.deleteById(id);
        deletePhysicalFile(attachment);
        log.info("删除附件成功: id={}", id);
    }

    /**
     * 按容器删除所有附件（含物理文件）。用于删除文档/任务等时级联删除附件，不做按作者的权限校验，由调用方保证权限。
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteAttachmentsByContainer(String containerType, Long containerId) {
        LambdaQueryWrapper<Attachment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Attachment::getContainerType, containerType).eq(Attachment::getContainerId, containerId);
        List<Attachment> list = attachmentMapper.selectList(wrapper);
        for (Attachment att : list) {
            attachmentMapper.deleteById(att.getId());
            deletePhysicalFile(att);
        }
        if (!list.isEmpty()) {
            log.info("按容器删除附件: containerType={}, containerId={}, 数量={}", containerType, containerId, list.size());
        }
    }

    /**
     * 删除附件的物理文件（OSS/COS/本地），不删数据库记录。
     */
    private void deletePhysicalFile(Attachment attachment) {
        try {
            if (isOssStorage(attachment)) {
                String objectKey = attachment.getDiskDirectory() + "/" + attachment.getDiskFilename();
                ossService.deleteFile(objectKey);
                if (isImageFile(attachment.getContentType(), attachment.getFilename())) {
                    String thumbnailKey = attachment.getDiskDirectory() + "/thumb_" + attachment.getDiskFilename();
                    ossService.deleteFile(thumbnailKey);
                }
            } else if (isCosStorage(attachment)) {
                String objectKey = attachment.getDiskDirectory() + "/" + attachment.getDiskFilename();
                cosService.deleteFile(objectKey);
                if (isImageFile(attachment.getContentType(), attachment.getFilename())) {
                    String thumbnailKey = attachment.getDiskDirectory() + "/thumb_" + attachment.getDiskFilename();
                    cosService.deleteFile(thumbnailKey);
                }
            } else {
                Path baseStoragePath = getStoragePath();
                Path filePath = baseStoragePath.resolve(attachment.getDiskDirectory())
                        .resolve(attachment.getDiskFilename());
                Files.deleteIfExists(filePath);
                if (isImageFile(attachment.getContentType(), attachment.getFilename())) {
                    Path thumbnailPath = baseStoragePath.resolve(attachment.getDiskDirectory())
                            .resolve("thumb_" + attachment.getDiskFilename());
                    Files.deleteIfExists(thumbnailPath);
                }
            }
        } catch (Exception e) {
            log.error("删除文件失败: attachmentId={}, {}", attachment.getId(), e.getMessage(), e);
        }
    }

    /**
     * 下载附件（获取文件）
     * 注意：OSS存储时，会先下载到临时文件
     */
    public File downloadAttachment(Long id) {
        Attachment attachment = attachmentMapper.selectById(id);
        if (attachment == null) {
            throw new BusinessException("附件不存在");
        }

        // 增加下载次数
        attachment.setDownloads(attachment.getDownloads() + 1);
        attachmentMapper.updateById(attachment);

        if (isOssStorage(attachment)) {
            // OSS存储：下载到临时文件
            return downloadFromCloud(attachment, "oss");
        } else if (isCosStorage(attachment)) {
            // COS存储：下载到临时文件
            return downloadFromCloud(attachment, "cos");
        } else {
            // 本地存储
            Path baseStoragePath = getStoragePath();
            Path filePath = baseStoragePath.resolve(attachment.getDiskDirectory())
                    .resolve(attachment.getDiskFilename());
            File file = filePath.toFile();

            if (!file.exists()) {
                log.error("文件不存在: {}", filePath);
                throw new BusinessException("文件不存在");
            }

            return file;
        }
    }

    /**
     * 批量下载附件（打包为ZIP）
     */
    public File batchDownloadAttachments(List<Long> attachmentIds) {
        if (attachmentIds == null || attachmentIds.isEmpty()) {
            throw new BusinessException("附件ID列表不能为空");
        }

        // 查询所有附件
        List<Attachment> attachments = new ArrayList<>();
        for (Long id : attachmentIds) {
            Attachment attachment = attachmentMapper.selectById(id);
            if (attachment != null) {
                attachments.add(attachment);
            }
        }

        if (attachments.isEmpty()) {
            throw new BusinessException("未找到任何附件");
        }

        // 创建临时ZIP文件（使用系统临时目录）
        Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"));
        // 确保临时目录存在
        try {
            Files.createDirectories(tempDir);
        } catch (IOException e) {
            log.error("创建临时目录失败: {}", tempDir, e);
            throw new BusinessException("创建临时目录失败: " + e.getMessage());
        }

        String zipFilename = "attachments_" + System.currentTimeMillis() + "_"
                + UUID.randomUUID().toString().substring(0, 8) + ".zip";
        Path zipPath = tempDir.resolve(zipFilename);

        int successCount = 0;
        try {
            // 创建ZIP输出流
            try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipPath.toFile()))) {
                byte[] buffer = new byte[8192];

                // 用于处理同名文件
                Set<String> usedNames = new HashSet<>();

                for (Attachment attachment : attachments) {
                    InputStream fileInputStream = null;
                    File tempFile = null;
                    
                    try {
                        // 根据存储类型获取文件流
                        if (isOssStorage(attachment) || isCosStorage(attachment)) {
                            // 云存储（OSS或COS）：下载到临时文件
                            String cloudType = isOssStorage(attachment) ? "oss" : "cos";
                            try {
                                tempFile = downloadFromCloud(attachment, cloudType);
                                fileInputStream = new FileInputStream(tempFile);
                            } catch (Exception e) {
                                log.warn("从{}下载文件失败，跳过: attachmentId={}, error={}", 
                                        cloudType.toUpperCase(), attachment.getId(), e.getMessage());
                                continue;
                            }
                        } else {
                            // 本地存储
                            Path baseStoragePath = getStoragePath();
                            Path filePath = baseStoragePath.resolve(attachment.getDiskDirectory())
                                    .resolve(attachment.getDiskFilename());
                            File file = filePath.toFile();

                            if (!file.exists()) {
                                log.warn("文件不存在，跳过: attachmentId={}, path={}", attachment.getId(), filePath);
                                continue;
                            }
                            
                            fileInputStream = new FileInputStream(file);
                        }

                        // 处理同名文件：如果ZIP内已有同名文件，添加序号
                        String entryName = attachment.getFilename();
                        if (entryName == null || entryName.isEmpty()) {
                            entryName = "unnamed_" + attachment.getId();
                        }

                        int counter = 1;
                        String originalEntryName = entryName;
                        while (usedNames.contains(entryName)) {
                            int lastDot = originalEntryName.lastIndexOf('.');
                            if (lastDot > 0) {
                                String nameWithoutExt = originalEntryName.substring(0, lastDot);
                                String ext = originalEntryName.substring(lastDot);
                                entryName = nameWithoutExt + "_" + counter + ext;
                            } else {
                                entryName = originalEntryName + "_" + counter;
                            }
                            counter++;
                        }
                        usedNames.add(entryName);

                        // 创建ZIP条目（使用处理后的文件名）
                        ZipEntry entry = new ZipEntry(entryName);
                        zos.putNextEntry(entry);

                        // 写入文件内容
                        try (InputStream is = fileInputStream) {
                            int len;
                            while ((len = is.read(buffer)) > 0) {
                                zos.write(buffer, 0, len);
                            }
                        }

                        zos.closeEntry();
                        successCount++;

                        // 清理临时文件
                        if (tempFile != null && tempFile.exists()) {
                            tempFile.delete();
                        }

                        // 增加下载次数
                        attachment.setDownloads(attachment.getDownloads() + 1);
                        attachmentMapper.updateById(attachment);

                    } catch (Exception e) {
                        log.error("处理附件失败: attachmentId={}, error={}", attachment.getId(), e.getMessage(), e);
                        // 继续处理下一个文件
                    } finally {
                        // 确保关闭流
                        if (fileInputStream != null) {
                            try {
                                fileInputStream.close();
                            } catch (IOException e) {
                                log.warn("关闭文件流失败: {}", e.getMessage());
                            }
                        }
                        // 清理临时文件
                        if (tempFile != null && tempFile.exists()) {
                            tempFile.delete();
                        }
                    }
                }
            }

            if (successCount == 0) {
                // 清理临时文件
                try {
                    Files.deleteIfExists(zipPath);
                } catch (IOException ex) {
                    log.error("清理临时文件失败: {}", ex.getMessage());
                }
                throw new BusinessException("没有成功打包任何文件");
            }

            log.info("批量下载附件打包完成: zipPath={}, 总文件数={}, 成功数={}", zipPath, attachments.size(), successCount);
            return zipPath.toFile();

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("批量下载附件打包失败: {}", e.getMessage(), e);
            // 清理临时文件
            try {
                Files.deleteIfExists(zipPath);
            } catch (IOException ex) {
                log.error("清理临时文件失败: {}", ex.getMessage());
            }
            throw new BusinessException("批量下载打包失败: " + e.getMessage());
        }
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

        // 判断是否为图片文件
        boolean isImage = isImageFile(attachment.getContentType(), attachment.getFilename());
        dto.setIsImage(isImage);

        // 如果是图片，设置缩略图URL
        if (isImage && thumbnailEnabled) {
            dto.setThumbnailUrl("/api/attachments/" + attachment.getId() + "/thumbnail");
        }

        // 判断是否可预览
        boolean isPreviewable = isPreviewable(attachment.getContentType(), attachment.getFilename());
        if (isPreviewable) {
            dto.setPreviewUrl("/api/attachments/" + attachment.getId() + "/preview");
        }

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

    /**
     * 判断是否为图片文件
     */
    private boolean isImageFile(String contentType, String filename) {
        if (contentType != null && contentType.startsWith("image/")) {
            return true;
        }
        if (filename != null) {
            String lowerName = filename.toLowerCase();
            return lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")
                    || lowerName.endsWith(".png") || lowerName.endsWith(".gif")
                    || lowerName.endsWith(".bmp") || lowerName.endsWith(".webp");
        }
        return false;
    }

    /**
     * 上传文件到OSS（处理图片的缩略图和水印）
     */
    private void uploadToOss(MultipartFile file, String objectKey, String originalFilename, 
                            String diskDirectory, String diskFilename) throws Exception {
        // 如果是图片文件，需要先处理（缩略图、水印）
        InputStream uploadStream = file.getInputStream();
        
        if (isImageFile(file.getContentType(), originalFilename)) {
            // 先保存到临时文件进行处理
            File tempFile = File.createTempFile("upload_", "_" + diskFilename);
            try {
                file.transferTo(tempFile);
                
                // 处理图片（缩略图、水印）
                if (thumbnailEnabled) {
                    try {
                        generateThumbnailForCloud(tempFile, diskDirectory, diskFilename, "oss");
                    } catch (Exception e) {
                        log.warn("生成缩略图失败: filename={}, error={}", originalFilename, e.getMessage());
                    }
                }
                
                if (isWatermarkEnabled()) {
                    try {
                        addWatermark(tempFile);
                    } catch (Exception e) {
                        log.warn("添加水印失败: filename={}, error={}", originalFilename, e.getMessage());
                    }
                }
                
                // 上传处理后的文件到OSS
                try (FileInputStream fis = new FileInputStream(tempFile)) {
                    ossService.uploadFile(fis, objectKey, file.getContentType(), tempFile.length());
                }
            } finally {
                // 清理临时文件
                tempFile.delete();
            }
        } else {
            // 非图片文件直接上传
            ossService.uploadFile(uploadStream, objectKey, file.getContentType(), file.getSize());
        }
    }
    
    /**
     * 上传文件到COS（处理图片的缩略图和水印）
     */
    private void uploadToCos(MultipartFile file, String objectKey, String originalFilename, 
                            String diskDirectory, String diskFilename) throws Exception {
        // 如果是图片文件，需要先处理（缩略图、水印）
        InputStream uploadStream = file.getInputStream();
        
        if (isImageFile(file.getContentType(), originalFilename)) {
            // 先保存到临时文件进行处理
            File tempFile = File.createTempFile("upload_", "_" + diskFilename);
            try {
                file.transferTo(tempFile);
                
                // 处理图片（缩略图、水印）
                if (thumbnailEnabled) {
                    try {
                        generateThumbnailForCloud(tempFile, diskDirectory, diskFilename, "cos");
                    } catch (Exception e) {
                        log.warn("生成缩略图失败: filename={}, error={}", originalFilename, e.getMessage());
                    }
                }
                
                if (isWatermarkEnabled()) {
                    try {
                        addWatermark(tempFile);
                    } catch (Exception e) {
                        log.warn("添加水印失败: filename={}, error={}", originalFilename, e.getMessage());
                    }
                }
                
                // 上传处理后的文件到COS
                try (FileInputStream fis = new FileInputStream(tempFile)) {
                    cosService.uploadFile(fis, objectKey, file.getContentType(), tempFile.length());
                }
            } finally {
                // 清理临时文件
                tempFile.delete();
            }
        } else {
            // 非图片文件直接上传
            cosService.uploadFile(uploadStream, objectKey, file.getContentType(), file.getSize());
        }
    }
    
    /**
     * 为云存储（OSS/COS）生成缩略图
     */
    private void generateThumbnailForCloud(File originalFile, String diskDirectory, String diskFilename, String cloudType) throws IOException {
        // 先保存到临时文件
        File tempThumbnail = File.createTempFile("thumb_", "_" + diskFilename);
        try {
            // 生成缩略图到临时文件
            BufferedImage originalImage = ImageIO.read(originalFile);
            if (originalImage == null) {
                throw new IOException("无法读取图片文件");
            }

            int originalWidth = originalImage.getWidth();
            int originalHeight = originalImage.getHeight();
            int targetWidth = thumbnailWidth;
            int targetHeight = thumbnailHeight;

            if (originalWidth <= targetWidth && originalHeight <= targetHeight) {
                log.debug("原图尺寸较小，不生成缩略图: {}x{}", originalWidth, originalHeight);
                return;
            }

            double widthRatio = (double) targetWidth / originalWidth;
            double heightRatio = (double) targetHeight / originalHeight;
            double ratio = Math.min(widthRatio, heightRatio);

            int scaledWidth = (int) (originalWidth * ratio);
            int scaledHeight = (int) (originalHeight * ratio);

            Thumbnails.of(originalFile)
                    .size(scaledWidth, scaledHeight)
                    .outputFormat("jpg")
                    .outputQuality(0.85f)
                    .toFile(tempThumbnail);

            // 上传缩略图到云存储
            String thumbnailKey = diskDirectory + "/thumb_" + diskFilename;
            try (FileInputStream fis = new FileInputStream(tempThumbnail)) {
                if ("oss".equals(cloudType)) {
                    ossService.uploadFile(fis, thumbnailKey, "image/jpeg", tempThumbnail.length());
                } else if ("cos".equals(cloudType)) {
                    cosService.uploadFile(fis, thumbnailKey, "image/jpeg", tempThumbnail.length());
                }
            }
            
            log.debug("{}缩略图生成成功: {}", cloudType.toUpperCase(), thumbnailKey);
        } finally {
            tempThumbnail.delete();
        }
    }

    /**
     * 生成缩略图（本地存储）
     */
    private void generateThumbnail(File originalFile, Path directory, String diskFilename) throws IOException {
        String thumbnailFilename = "thumb_" + diskFilename;
        Path thumbnailPath = directory.resolve(thumbnailFilename);

        // 读取原图
        BufferedImage originalImage = ImageIO.read(originalFile);
        if (originalImage == null) {
            throw new IOException("无法读取图片文件");
        }

        // 计算缩略图尺寸（保持宽高比）
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        int targetWidth = thumbnailWidth;
        int targetHeight = thumbnailHeight;

        // 如果原图比缩略图小，不生成缩略图
        if (originalWidth <= targetWidth && originalHeight <= targetHeight) {
            log.debug("原图尺寸较小，不生成缩略图: {}x{}", originalWidth, originalHeight);
            return;
        }

        // 计算缩放比例，保持宽高比
        double widthRatio = (double) targetWidth / originalWidth;
        double heightRatio = (double) targetHeight / originalHeight;
        double ratio = Math.min(widthRatio, heightRatio);

        int scaledWidth = (int) (originalWidth * ratio);
        int scaledHeight = (int) (originalHeight * ratio);

        // 生成缩略图
        Thumbnails.of(originalFile)
                .size(scaledWidth, scaledHeight)
                .outputFormat("jpg") // 统一输出为JPG格式，减小文件大小
                .outputQuality(0.85f) // 质量85%，平衡文件大小和图片质量
                .toFile(thumbnailPath.toFile());

        log.debug("缩略图生成成功: {}", thumbnailPath);
    }

    /**
     * 获取缩略图文件
     */
    public File getThumbnail(Long attachmentId) {
        Attachment attachment = attachmentMapper.selectById(attachmentId);
        if (attachment == null) {
            throw new BusinessException("附件不存在");
        }

        // 检查是否为图片
        if (!isImageFile(attachment.getContentType(), attachment.getFilename())) {
            throw new BusinessException("该附件不是图片文件，无法生成缩略图");
        }

        if (isOssStorage(attachment) || isCosStorage(attachment)) {
            // 云存储（OSS或COS）
            String cloudType = isOssStorage(attachment) ? "oss" : "cos";
            String thumbnailKey = attachment.getDiskDirectory() + "/thumb_" + attachment.getDiskFilename();
            
            // 检查缩略图是否存在
            boolean thumbnailExists;
            if ("oss".equals(cloudType)) {
                thumbnailExists = ossService.fileExists(thumbnailKey);
            } else {
                thumbnailExists = cosService.fileExists(thumbnailKey);
            }
            
            if (!thumbnailExists) {
                // 如果不存在，尝试生成
                String originalKey = attachment.getDiskDirectory() + "/" + attachment.getDiskFilename();
                boolean originalExists;
                if ("oss".equals(cloudType)) {
                    originalExists = ossService.fileExists(originalKey);
                } else {
                    originalExists = cosService.fileExists(originalKey);
                }
                
                if (!originalExists) {
                    throw new BusinessException("原文件不存在");
                }
                
                // 下载原文件到临时文件
                try {
                    File tempOriginal = downloadFromCloud(attachment, cloudType);
                    tempOriginal.deleteOnExit();
                    
                    // 生成缩略图并上传到云存储
                    generateThumbnailForCloud(tempOriginal, attachment.getDiskDirectory(), 
                            attachment.getDiskFilename(), cloudType);
                    tempOriginal.delete();
                } catch (Exception e) {
                    log.error("生成{}缩略图失败: attachmentId={}, error={}", cloudType.toUpperCase(), 
                            attachmentId, e.getMessage(), e);
                    throw new BusinessException("生成缩略图失败: " + e.getMessage());
                }
            }
            
            // 下载缩略图到临时文件
            try {
                InputStream thumbnailStream;
                if ("oss".equals(cloudType)) {
                    thumbnailStream = ossService.downloadFile(thumbnailKey);
                } else {
                    thumbnailStream = cosService.downloadFile(thumbnailKey);
                }
                
                File tempThumbnail = File.createTempFile(cloudType + "_thumb_", "_" + attachment.getDiskFilename());
                tempThumbnail.deleteOnExit();
                
                try (FileOutputStream fos = new FileOutputStream(tempThumbnail)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = thumbnailStream.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                    }
                } finally {
                    thumbnailStream.close();
                }
                
                return tempThumbnail;
            } catch (Exception e) {
                log.error("从{}下载缩略图失败: attachmentId={}, error={}", cloudType.toUpperCase(), 
                        attachmentId, e.getMessage(), e);
                throw new BusinessException("获取缩略图失败: " + e.getMessage());
            }
        } else {
            // 本地存储
            Path baseStoragePath = getStoragePath();
            String thumbnailFilename = "thumb_" + attachment.getDiskFilename();
            Path thumbnailPath = baseStoragePath.resolve(attachment.getDiskDirectory())
                    .resolve(thumbnailFilename);

            File thumbnailFile = thumbnailPath.toFile();

            // 如果缩略图不存在，尝试生成
            if (!thumbnailFile.exists()) {
                Path originalFilePath = baseStoragePath.resolve(attachment.getDiskDirectory())
                        .resolve(attachment.getDiskFilename());
                File originalFile = originalFilePath.toFile();

                if (!originalFile.exists()) {
                    throw new BusinessException("原文件不存在");
                }

                try {
                    generateThumbnail(originalFile, baseStoragePath.resolve(attachment.getDiskDirectory()),
                            attachment.getDiskFilename());
                    thumbnailFile = thumbnailPath.toFile();
                } catch (IOException e) {
                    log.error("生成缩略图失败: attachmentId={}, error={}", attachmentId, e.getMessage(), e);
                    throw new BusinessException("生成缩略图失败: " + e.getMessage());
                }
            }

            if (!thumbnailFile.exists()) {
                throw new BusinessException("缩略图不存在");
            }

            return thumbnailFile;
        }
    }

    /**
     * 判断文件是否可预览
     */
    public boolean isPreviewable(String contentType, String filename) {
        // 图片文件
        if (isImageFile(contentType, filename)) {
            return true;
        }

        // PDF文件
        if (contentType != null && contentType.equals("application/pdf")) {
            return true;
        }

        if (filename != null) {
            String lowerName = filename.toLowerCase();
            if (lowerName.endsWith(".pdf")) {
                return true;
            }
        }

        return false;
    }

    /**
     * 获取预览文件
     */
    public File getPreviewFile(Long attachmentId) {
        Attachment attachment = attachmentMapper.selectById(attachmentId);
        if (attachment == null) {
            throw new BusinessException("附件不存在");
        }

        if (!isPreviewable(attachment.getContentType(), attachment.getFilename())) {
            throw new BusinessException("该文件类型不支持预览");
        }

        if (isOssStorage(attachment) || isCosStorage(attachment)) {
            // 云存储（OSS或COS）：下载到临时文件
            String cloudType = isOssStorage(attachment) ? "oss" : "cos";
            return downloadFromCloud(attachment, cloudType);
        } else {
            // 本地存储
            Path baseStoragePath = getStoragePath();
            Path filePath = baseStoragePath.resolve(attachment.getDiskDirectory())
                    .resolve(attachment.getDiskFilename());
            File file = filePath.toFile();

            if (!file.exists()) {
                throw new BusinessException("文件不存在");
            }

            return file;
        }
    }
    
    /**
     * 判断是否启用水印
     */
    private boolean isWatermarkEnabled() {
        try {
            String value = settingService.getSetting(SettingKey.WATERMARK_ENABLED.getKey());
            return "true".equalsIgnoreCase(value);
        } catch (Exception e) {
            log.warn("获取水印配置失败，使用默认值false: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 获取水印配置
     */
    private String getWatermarkText() {
        try {
            String value = settingService.getSetting(SettingKey.WATERMARK_TEXT.getKey());
            return value != null ? value : "JRedmine";
        } catch (Exception e) {
            log.warn("获取水印文本失败，使用默认值: {}", e.getMessage());
            return "JRedmine";
        }
    }
    
    private String getWatermarkPosition() {
        try {
            String value = settingService.getSetting(SettingKey.WATERMARK_POSITION.getKey());
            return value != null ? value : "bottom-right";
        } catch (Exception e) {
            return "bottom-right";
        }
    }
    
    private float getWatermarkOpacity() {
        try {
            String value = settingService.getSetting(SettingKey.WATERMARK_OPACITY.getKey());
            return value != null ? Float.parseFloat(value) : 0.5f;
        } catch (Exception e) {
            return 0.5f;
        }
    }
    
    private int getWatermarkFontSize() {
        try {
            String value = settingService.getSetting(SettingKey.WATERMARK_FONT_SIZE.getKey());
            return value != null ? Integer.parseInt(value) : 24;
        } catch (Exception e) {
            return 24;
        }
    }
    
    /**
     * 添加水印
     */
    private void addWatermark(File imageFile) throws IOException {
        // 读取原图
        BufferedImage originalImage = ImageIO.read(imageFile);
        if (originalImage == null) {
            throw new IOException("无法读取图片文件");
        }
        
        // 创建Graphics2D对象
        Graphics2D g2d = (Graphics2D) originalImage.getGraphics();
        
        // 设置抗锯齿
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        // 设置透明度
        float opacity = getWatermarkOpacity();
        AlphaComposite alphaComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity);
        g2d.setComposite(alphaComposite);
        
        // 设置字体和颜色
        int fontSize = getWatermarkFontSize();
        Font font = new Font("Arial", Font.BOLD, fontSize);
        g2d.setFont(font);
        g2d.setColor(Color.WHITE);
        
        // 获取水印文本
        String watermarkText = getWatermarkText();
        
        // 计算文本尺寸
        FontMetrics fontMetrics = g2d.getFontMetrics();
        Rectangle2D textBounds = fontMetrics.getStringBounds(watermarkText, g2d);
        int textWidth = (int) textBounds.getWidth();
        int textHeight = (int) textBounds.getHeight();
        
        // 计算水印位置
        int imageWidth = originalImage.getWidth();
        int imageHeight = originalImage.getHeight();
        int x, y;
        
        String position = getWatermarkPosition();
        int padding = 20; // 边距
        
        switch (position.toLowerCase()) {
            case "top-left":
                x = padding;
                y = padding + textHeight;
                break;
            case "top-right":
                x = imageWidth - textWidth - padding;
                y = padding + textHeight;
                break;
            case "bottom-left":
                x = padding;
                y = imageHeight - padding;
                break;
            case "bottom-right":
                x = imageWidth - textWidth - padding;
                y = imageHeight - padding;
                break;
            case "center":
                x = (imageWidth - textWidth) / 2;
                y = (imageHeight + textHeight) / 2;
                break;
            default:
                x = imageWidth - textWidth - padding;
                y = imageHeight - padding;
        }
        
        // 绘制文本阴影（增强可读性）
        g2d.setColor(new Color(0, 0, 0, (int) (opacity * 128)));
        g2d.drawString(watermarkText, x + 2, y + 2);
        
        // 绘制水印文本
        g2d.setColor(Color.WHITE);
        g2d.drawString(watermarkText, x, y);
        
        // 释放资源
        g2d.dispose();
        
        // 保存图片
        String formatName = getImageFormat(imageFile);
        ImageIO.write(originalImage, formatName, imageFile);
        
        log.debug("水印添加成功: {}", imageFile.getName());
    }
    
    /**
     * 获取图片格式
     */
    private String getImageFormat(File imageFile) {
        String filename = imageFile.getName().toLowerCase();
        if (filename.endsWith(".png")) {
            return "png";
        } else if (filename.endsWith(".gif")) {
            return "gif";
        } else if (filename.endsWith(".bmp")) {
            return "bmp";
        } else {
            return "jpg"; // 默认JPG
        }
    }
}
