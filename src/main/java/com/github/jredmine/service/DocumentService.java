package com.github.jredmine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.jredmine.dto.request.document.DocumentCreateRequestDTO;
import com.github.jredmine.dto.response.document.DocumentDetailResponseDTO;
import com.github.jredmine.entity.Document;
import com.github.jredmine.entity.EnabledModule;
import com.github.jredmine.entity.Enumeration;
import com.github.jredmine.entity.Project;
import com.github.jredmine.enums.ProjectModule;
import com.github.jredmine.enums.ResultCode;
import com.github.jredmine.exception.BusinessException;
import com.github.jredmine.mapper.DocumentMapper;
import com.github.jredmine.mapper.project.EnabledModuleMapper;
import com.github.jredmine.mapper.project.ProjectMapper;
import com.github.jredmine.mapper.workflow.EnumerationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * 文档服务：创建、列表、详情、更新、删除（本期仅实现创建）
 *
 * @author panfeng
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private static final String ENUM_TYPE_DOCUMENT_CATEGORY = "DocumentCategory";

    private final DocumentMapper documentMapper;
    private final EnabledModuleMapper enabledModuleMapper;
    private final ProjectMapper projectMapper;
    private final EnumerationMapper enumerationMapper;

    /**
     * 检查项目是否启用了文档模块
     */
    public boolean isDocumentsEnabledForProject(Long projectId) {
        LambdaQueryWrapper<EnabledModule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EnabledModule::getProjectId, projectId)
                .eq(EnabledModule::getName, ProjectModule.DOCUMENTS.getCode());
        return enabledModuleMapper.selectCount(wrapper) > 0;
    }

    /**
     * 创建文档：在项目下新增一条 documents 记录。
     * 要求项目已启用 documents 模块；title 必填，description、categoryId 可选，categoryId=0 或 null 表示未分类。
     */
    @Transactional(rollbackFor = Exception.class)
    public DocumentDetailResponseDTO create(Long projectId, DocumentCreateRequestDTO dto) {
        if (dto == null || dto.getTitle() == null || dto.getTitle().isBlank()) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "文档标题不能为空");
        }
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException(ResultCode.PROJECT_NOT_FOUND);
        }
        if (!isDocumentsEnabledForProject(projectId)) {
            throw new BusinessException(ResultCode.DOCUMENTS_NOT_ENABLED);
        }
        String title = dto.getTitle().trim();
        Integer categoryId = dto.getCategoryId() != null ? dto.getCategoryId() : 0;
        if (categoryId < 0) {
            categoryId = 0;
        }
        if (categoryId > 0) {
            validateDocumentCategory(categoryId, projectId);
        }
        Date now = new Date();
        Document doc = new Document();
        doc.setProjectId(projectId.intValue());
        doc.setCategoryId(categoryId);
        doc.setTitle(title);
        doc.setDescription(dto.getDescription());
        doc.setCreatedOn(now);
        documentMapper.insert(doc);
        log.info("文档创建成功: projectId={}, documentId={}, title={}", projectId, doc.getId(), title);
        return toDetailResponse(doc, projectId.longValue());
    }

    /**
     * 校验文档分类有效：存在、类型为 DocumentCategory、且为全局分类或属于当前项目。
     */
    private void validateDocumentCategory(Integer categoryId, Long projectId) {
        Enumeration cat = enumerationMapper.selectById(categoryId);
        if (cat == null) {
            throw new BusinessException(ResultCode.DOCUMENT_CATEGORY_NOT_FOUND);
        }
        if (!ENUM_TYPE_DOCUMENT_CATEGORY.equals(cat.getType())) {
            throw new BusinessException(ResultCode.DOCUMENT_CATEGORY_NOT_FOUND);
        }
        if (Boolean.FALSE.equals(cat.getActive())) {
            throw new BusinessException(ResultCode.DOCUMENT_CATEGORY_NOT_FOUND);
        }
        // 全局分类 project_id 为 null；项目级分类 project_id 须等于当前项目
        if (cat.getProjectId() != null && !cat.getProjectId().equals(projectId.intValue())) {
            throw new BusinessException(ResultCode.DOCUMENT_CATEGORY_NOT_FOUND);
        }
    }

    private DocumentDetailResponseDTO toDetailResponse(Document doc, Long projectId) {
        String categoryName = null;
        if (doc.getCategoryId() != null && doc.getCategoryId() > 0) {
            Enumeration cat = enumerationMapper.selectById(doc.getCategoryId());
            if (cat != null && ENUM_TYPE_DOCUMENT_CATEGORY.equals(cat.getType())) {
                categoryName = cat.getName();
            }
        }
        return DocumentDetailResponseDTO.builder()
                .id(doc.getId())
                .projectId(doc.getProjectId())
                .title(doc.getTitle())
                .description(doc.getDescription())
                .categoryId(doc.getCategoryId())
                .categoryName(categoryName)
                .createdOn(doc.getCreatedOn())
                .build();
    }
}
