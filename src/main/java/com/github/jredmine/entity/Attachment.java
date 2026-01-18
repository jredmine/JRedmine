package com.github.jredmine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 附件实体
 *
 * @author panfeng
 * @since 2026-01-18
 */
@Data
@TableName("attachments")
public class Attachment {
    
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 容器ID（多态关联）
     */
    @TableField("container_id")
    private Long containerId;
    
    /**
     * 容器类型（多态关联，如：Issue、Project、Document、WikiPage等）
     */
    @TableField("container_type")
    private String containerType;
    
    /**
     * 原始文件名
     */
    @TableField("filename")
    private String filename;
    
    /**
     * 磁盘存储文件名（UUID等）
     */
    @TableField("disk_filename")
    private String diskFilename;
    
    /**
     * 文件大小（字节）
     */
    @TableField("filesize")
    private Long filesize;
    
    /**
     * 文件类型（MIME类型）
     */
    @TableField("content_type")
    private String contentType;
    
    /**
     * 文件摘要（用于验证文件完整性，如：SHA256）
     */
    @TableField("digest")
    private String digest;
    
    /**
     * 下载次数
     */
    @TableField("downloads")
    private Integer downloads;
    
    /**
     * 上传者ID
     */
    @TableField("author_id")
    private Long authorId;
    
    /**
     * 创建时间
     */
    @TableField("created_on")
    private LocalDateTime createdOn;
    
    /**
     * 文件描述
     */
    @TableField("description")
    private String description;
    
    /**
     * 磁盘存储目录
     */
    @TableField("disk_directory")
    private String diskDirectory;
}
