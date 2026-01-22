package com.github.jredmine.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import com.github.jredmine.enums.SettingKey;
import com.github.jredmine.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;

/**
 * 阿里云OSS服务
 * 封装OSS的上传、下载、删除等操作
 *
 * @author panfeng
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OssService {

    private final SettingService settingService;
    
    private OSS ossClient;
    private String bucketName;
    private String pathPrefix;

    /**
     * 获取OSS客户端（懒加载，单例模式）
     */
    private OSS getOssClient() {
        if (ossClient == null) {
            synchronized (this) {
                if (ossClient == null) {
                    String endpoint = getSettingValue(SettingKey.OSS_ENDPOINT);
                    String accessKeyId = getSettingValue(SettingKey.OSS_ACCESS_KEY_ID);
                    String accessKeySecret = getSettingValue(SettingKey.OSS_ACCESS_KEY_SECRET);
                    bucketName = getSettingValue(SettingKey.OSS_BUCKET_NAME);
                    pathPrefix = getSettingValue(SettingKey.OSS_PATH_PREFIX);
                    
                    if (endpoint == null || endpoint.isEmpty() ||
                        accessKeyId == null || accessKeyId.isEmpty() ||
                        accessKeySecret == null || accessKeySecret.isEmpty() ||
                        bucketName == null || bucketName.isEmpty()) {
                        throw new BusinessException("OSS配置不完整，请检查系统设置");
                    }
                    
                    // 如果pathPrefix为空，使用默认值
                    if (pathPrefix == null || pathPrefix.isEmpty()) {
                        pathPrefix = "attachments";
                    }
                    
                    ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
                    log.info("OSS客户端初始化成功: endpoint={}, bucket={}", endpoint, bucketName);
                }
            }
        }
        return ossClient;
    }

    /**
     * 上传文件到OSS
     *
     * @param inputStream 文件输入流
     * @param objectKey   OSS对象键（文件路径）
     * @param contentType 文件类型
     * @param contentLength 文件大小
     * @return OSS对象键
     */
    public String uploadFile(InputStream inputStream, String objectKey, String contentType, long contentLength) {
        try {
            OSS client = getOssClient();
            
            // 构建完整的对象键（包含路径前缀）
            String fullObjectKey = buildObjectKey(objectKey);
            
            // 创建元数据
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(contentType);
            metadata.setContentLength(contentLength);
            
            // 创建上传请求
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, fullObjectKey, inputStream, metadata);
            
            // 上传文件
            client.putObject(putObjectRequest);
            
            log.info("文件上传到OSS成功: objectKey={}, size={}", fullObjectKey, contentLength);
            return fullObjectKey;
            
        } catch (Exception e) {
            log.error("上传文件到OSS失败: objectKey={}, error={}", objectKey, e.getMessage(), e);
            throw new BusinessException("上传文件到OSS失败: " + e.getMessage());
        }
    }

    /**
     * 从OSS下载文件
     *
     * @param objectKey OSS对象键
     * @return 文件输入流
     */
    public InputStream downloadFile(String objectKey) {
        try {
            OSS client = getOssClient();
            
            // 如果objectKey已经包含路径前缀，直接使用；否则构建完整路径
            String fullObjectKey = objectKey.startsWith(pathPrefix) ? objectKey : buildObjectKey(objectKey);
            
            if (!client.doesObjectExist(bucketName, fullObjectKey)) {
                throw new BusinessException("OSS文件不存在: " + fullObjectKey);
            }
            
            return client.getObject(bucketName, fullObjectKey).getObjectContent();
            
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("从OSS下载文件失败: objectKey={}, error={}", objectKey, e.getMessage(), e);
            throw new BusinessException("从OSS下载文件失败: " + e.getMessage());
        }
    }

    /**
     * 从OSS删除文件
     *
     * @param objectKey OSS对象键
     */
    public void deleteFile(String objectKey) {
        try {
            OSS client = getOssClient();
            
            // 如果objectKey已经包含路径前缀，直接使用；否则构建完整路径
            String fullObjectKey = objectKey.startsWith(pathPrefix) ? objectKey : buildObjectKey(objectKey);
            
            if (!client.doesObjectExist(bucketName, fullObjectKey)) {
                log.warn("OSS文件不存在，跳过删除: objectKey={}", fullObjectKey);
                return;
            }
            
            client.deleteObject(bucketName, fullObjectKey);
            log.info("从OSS删除文件成功: objectKey={}", fullObjectKey);
            
        } catch (Exception e) {
            log.error("从OSS删除文件失败: objectKey={}, error={}", objectKey, e.getMessage(), e);
            // 删除失败不影响主流程，只记录错误
        }
    }

    /**
     * 检查OSS文件是否存在
     *
     * @param objectKey OSS对象键
     * @return 是否存在
     */
    public boolean fileExists(String objectKey) {
        try {
            OSS client = getOssClient();
            String fullObjectKey = objectKey.startsWith(pathPrefix) ? objectKey : buildObjectKey(objectKey);
            return client.doesObjectExist(bucketName, fullObjectKey);
        } catch (Exception e) {
            log.error("检查OSS文件是否存在失败: objectKey={}, error={}", objectKey, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 生成OSS文件的访问URL（带签名，有效期1小时）
     *
     * @param objectKey OSS对象键
     * @param expiresInSeconds 过期时间（秒），默认3600（1小时）
     * @return 访问URL
     */
    public String generatePresignedUrl(String objectKey, long expiresInSeconds) {
        try {
            OSS client = getOssClient();
            String fullObjectKey = objectKey.startsWith(pathPrefix) ? objectKey : buildObjectKey(objectKey);
            
            // 生成预签名URL，默认有效期1小时
            java.util.Date expiration = new java.util.Date(System.currentTimeMillis() + expiresInSeconds * 1000);
            java.net.URL url = client.generatePresignedUrl(bucketName, fullObjectKey, expiration);
            
            return url.toString();
            
        } catch (Exception e) {
            log.error("生成OSS预签名URL失败: objectKey={}, error={}", objectKey, e.getMessage(), e);
            throw new BusinessException("生成OSS访问URL失败: " + e.getMessage());
        }
    }

    /**
     * 构建完整的对象键（包含路径前缀）
     *
     * @param objectKey 原始对象键
     * @return 完整的对象键
     */
    private String buildObjectKey(String objectKey) {
        if (pathPrefix == null || pathPrefix.isEmpty()) {
            return objectKey;
        }
        
        // 确保路径前缀不以/开头和结尾
        String prefix = pathPrefix.startsWith("/") ? pathPrefix.substring(1) : pathPrefix;
        prefix = prefix.endsWith("/") ? prefix.substring(0, prefix.length() - 1) : prefix;
        
        // 确保objectKey不以/开头
        String key = objectKey.startsWith("/") ? objectKey.substring(1) : objectKey;
        
        return prefix + "/" + key;
    }

    /**
     * 获取设置值
     */
    private String getSettingValue(SettingKey settingKey) {
        try {
            return settingService.getSetting(settingKey.getKey());
        } catch (Exception e) {
            log.warn("获取OSS配置失败: key={}, error={}", settingKey.getKey(), e.getMessage());
            return null;
        }
    }

    /**
     * 清除OSS客户端（用于配置更新后重新初始化）
     */
    public void clearClient() {
        synchronized (this) {
            if (ossClient != null) {
                ossClient.shutdown();
                ossClient = null;
                bucketName = null;
                pathPrefix = null;
                log.info("OSS客户端已清除");
            }
        }
    }
}
