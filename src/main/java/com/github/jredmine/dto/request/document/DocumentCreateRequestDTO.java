package com.github.jredmine.dto.request.document;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 文档创建请求 DTO
 * 创建文档即写一条 documents 记录；上传文件走现有附件接口，containerType=Document、containerId=文档ID。
 *
 * @author panfeng
 */
@Data
@Schema(description = "文档创建请求")
public class DocumentCreateRequestDTO {

    @NotBlank(message = "文档标题不能为空")
    @Size(max = 255)
    @Schema(description = "文档标题", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @Schema(description = "文档描述")
    private String description;

    @Schema(description = "文档分类 ID，0 或 null 表示未分类")
    private Integer categoryId;
}
