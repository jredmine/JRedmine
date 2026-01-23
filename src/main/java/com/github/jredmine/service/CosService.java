package com.github.jredmine.service;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.region.Region;
import com.github.jredmine.enums.SettingKey;
import com.github.jredmine.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;

/**
 * 腾讯云COS服务
 * 封装COS的上传、下载、删除等操作
 *
 * @author panfeng
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CosService {

    private final SettingService settingService;
    
    private COSClient cosClient;
    private String bucketName;
    private String pathPrefix;

    /**
     * 获取COS客户端（懒加载，单例模式）
     */
    private COSClient getCosClient() {
        if (cosClient == null) {
            synchronized (this) {
                if (cosClient == null) {
                    String region = getSettingValue(SettingKey.COS_REGION);
                    String secretId = getSettingValue(SettingKey.COS_SECRET_ID);
                    String secretKey = getSettingValue(SettingKey.COS_SECRET_KEY);
                    bucketName = getSettingValue(SettingKey.COS_BUCKET_NAME);
                    pathPrefix = getSettingValue(SettingKey.COS_PATH_PREFIX);
                    
                    if (region == null || region.isEmpty() ||
                        secretId == null || secretId.isEmpty() ||
                        secretKey == null || secretKey.isEmpty() ||
                        bucketName == null || bucketName.isEmpty()) {
                        throw new BusinessException("COS配置不完整，请检查系统设置");
                    }
                    
                    // 如果pathPrefix为空，使用默认值
                    if (pathPrefix == null || pathPrefix.isEmpty()) {
                        pathPrefix = "attachments";
                    }
                    
                    // 初始化COS凭证
                    COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
                    
                    // 设置存储桶区域
                    Region regionObj = new Region(region);
                    ClientConfig clientConfig = new ClientConfig(regionObj);
                    
                    // 使用HTTPS协议
                    clientConfig.setHttpProtocol(HttpProtocol.https);
                    
                    // 创建COS客户端
                    cosClient = new COSClient(cred, clientConfig);
                    log.info("COS客户端初始化成功: region={}, bucket={}", region, bucketName);
                }
            }
        }
        return cosClient;
    }

    /**
     * 上传文件到COS
     *
     * @param inputStream 文件输入流
     * @param objectKey   COS对象键（文件路径）
     * @param contentType 文件类型
     * @param contentLength 文件大小
     * @return COS对象键
     */
    public String uploadFile(InputStream inputStream, String objectKey, String contentType, long contentLength) {
        try {
            COSClient client = getCosClient();
            
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
            
            log.info("文件上传到COS成功: objectKey={}, size={}", fullObjectKey, contentLength);
            return fullObjectKey;
            
        } catch (Exception e) {
            log.error("上传文件到COS失败: objectKey={}, error={}", objectKey, e.getMessage(), e);
            throw new BusinessException("上传文件到COS失败: " + e.getMessage());
        }
    }

    /**
     * 从COS下载文件
     *
     * @param objectKey COS对象键
     * @return 文件输入流
     */
    public InputStream downloadFile(String objectKey) {
        try {
            COSClient client = getCosClient();
            
            // 如果objectKey已经包含路径前缀，直接使用；否则构建完整路径
            String fullObjectKey = objectKey.startsWith(pathPrefix) ? objectKey : buildObjectKey(objectKey);
            
            if (!client.doesObjectExist(bucketName, fullObjectKey)) {
                throw new BusinessException("COS文件不存在: " + fullObjectKey);
            }
            
            return client.getObject(bucketName, fullObjectKey).getObjectContent();
            
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("从COS下载文件失败: objectKey={}, error={}", objectKey, e.getMessage(), e);
            throw new BusinessException("从COS下载文件失败: " + e.getMessage());
        }
    }

    /**
     * 从COS删除文件
     *
     * @param objectKey COS对象键
     */
    public void deleteFile(String objectKey) {
        try {
            COSClient client = getCosClient();
            
            // 如果objectKey已经包含路径前缀，直接使用；否则构建完整路径
            String fullObjectKey = objectKey.startsWith(pathPrefix) ? objectKey : buildObjectKey(objectKey);
            
            if (!client.doesObjectExist(bucketName, fullObjectKey)) {
                log.warn("COS文件不存在，跳过删除: objectKey={}", fullObjectKey);
                return;
            }
            
            client.deleteObject(bucketName, fullObjectKey);
            log.info("从COS删除文件成功: objectKey={}", fullObjectKey);
            
        } catch (Exception e) {
            log.error("从COS删除文件失败: objectKey={}, error={}", objectKey, e.getMessage(), e);
            // 删除失败不影响主流程，只记录错误
        }
    }

    /**
     * 检查COS文件是否存在
     *
     * @param objectKey COS对象键
     * @return 是否存在
     */
    public boolean fileExists(String objectKey) {
        try {
            COSClient client = getCosClient();
            String fullObjectKey = objectKey.startsWith(pathPrefix) ? objectKey : buildObjectKey(objectKey);
            return client.doesObjectExist(bucketName, fullObjectKey);
        } catch (Exception e) {
            log.error("检查COS文件是否存在失败: objectKey={}, error={}", objectKey, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 生成COS文件的访问URL（带签名，有效期1小时）
     *
     * @param objectKey COS对象键
     * @param expiresInSeconds 过期时间（秒），默认3600（1小时）
     * @return 访问URL
     */
    public String generatePresignedUrl(String objectKey, long expiresInSeconds) {
        try {
            COSClient client = getCosClient();
            String fullObjectKey = objectKey.startsWith(pathPrefix) ? objectKey : buildObjectKey(objectKey);
            
            // 生成预签名URL，默认有效期1小时
            java.util.Date expiration = new java.util.Date(System.currentTimeMillis() + expiresInSeconds * 1000);
            java.net.URL url = client.generatePresignedUrl(bucketName, fullObjectKey, expiration);
            
            return url.toString();
            
        } catch (Exception e) {
            log.error("生成COS预签名URL失败: objectKey={}, error={}", objectKey, e.getMessage(), e);
            throw new BusinessException("生成COS访问URL失败: " + e.getMessage());
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
            log.warn("获取COS配置失败: key={}, error={}", settingKey.getKey(), e.getMessage());
            return null;
        }
    }

    /**
     * 清除COS客户端（用于配置更新后重新初始化）
     */
    public void clearClient() {
        synchronized (this) {
            if (cosClient != null) {
                cosClient.shutdown();
                cosClient = null;
                bucketName = null;
                pathPrefix = null;
                log.info("COS客户端已清除");
            }
        }
    }
}
