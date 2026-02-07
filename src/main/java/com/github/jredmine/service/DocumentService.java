package com.github.jredmine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.jredmine.dto.request.attachment.AttachmentQueryRequestDTO;
import com.github.jredmine.dto.request.document.DocumentCategoryCreateRequestDTO;
import com.github.jredmine.dto.request.document.DocumentCategoryUpdateRequestDTO;
import com.github.jredmine.dto.request.document.DocumentCreateRequestDTO;
import com.github.jredmine.dto.request.document.DocumentUpdateRequestDTO;
import com.github.jredmine.dto.response.PageResponse;
import com.github.jredmine.dto.response.attachment.AttachmentResponseDTO;
import com.github.jredmine.dto.response.document.DocumentCategoryResponseDTO;
import com.github.jredmine.dto.response.document.DocumentDetailResponseDTO;
import com.github.jredmine.dto.response.document.DocumentListItemResponseDTO;
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
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 文档服务：创建、列表、详情、更新、删除
 *
 * @author panfeng
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private static final String ENUM_TYPE_DOCUMENT_CATEGORY = "DocumentCategory";

    private static final String CONTAINER_TYPE_DOCUMENT = "Document";
    private static final int DETAIL_ATTACHMENT_PAGE_SIZE = 200;

    private final DocumentMapper documentMapper;
    private final EnabledModuleMapper enabledModuleMapper;
    private final ProjectMapper projectMapper;
    private final EnumerationMapper enumerationMapper;
    private final AttachmentService attachmentService;

    /**
     * 文档分类列表：某项目可用的文档分类（全局 + 当前项目），按 position、name 排序，供下拉/筛选。
     * 要求项目存在且已启用文档模块。
     */
    public List<DocumentCategoryResponseDTO> listDocumentCategories(Long projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException(ResultCode.PROJECT_NOT_FOUND);
        }
        if (!isDocumentsEnabledForProject(projectId)) {
            throw new BusinessException(ResultCode.DOCUMENTS_NOT_ENABLED);
        }
        LambdaQueryWrapper<Enumeration> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Enumeration::getType, ENUM_TYPE_DOCUMENT_CATEGORY)
                .eq(Enumeration::getActive, true)
                .and(w -> w.isNull(Enumeration::getProjectId).or().eq(Enumeration::getProjectId, projectId.intValue()))
                .orderByAsc(Enumeration::getPosition)
                .orderByAsc(Enumeration::getName);
        List<Enumeration> list = enumerationMapper.selectList(wrapper);
        return list.stream()
                .map(e -> DocumentCategoryResponseDTO.builder()
                        .id(e.getId())
                        .name(e.getName())
                        .position(e.getPosition())
                        .projectId(e.getProjectId())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 创建项目级文档分类（写入 enumerations，type=DocumentCategory，project_id=项目ID）。
     * 同一项目下分类名称不可重复。
     */
    @Transactional(rollbackFor = Exception.class)
    public DocumentCategoryResponseDTO createDocumentCategory(Long projectId, DocumentCategoryCreateRequestDTO dto) {
        if (dto == null || dto.getName() == null || dto.getName().isBlank()) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "分类名称不能为空");
        }
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException(ResultCode.PROJECT_NOT_FOUND);
        }
        if (!isDocumentsEnabledForProject(projectId)) {
            throw new BusinessException(ResultCode.DOCUMENTS_NOT_ENABLED);
        }
        String name = dto.getName().trim();
        LambdaQueryWrapper<Enumeration> existWrapper = new LambdaQueryWrapper<>();
        existWrapper.eq(Enumeration::getType, ENUM_TYPE_DOCUMENT_CATEGORY)
                .eq(Enumeration::getProjectId, projectId.intValue())
                .eq(Enumeration::getName, name);
        if (enumerationMapper.selectCount(existWrapper) > 0) {
            throw new BusinessException(ResultCode.DOCUMENT_CATEGORY_NAME_EXISTS);
        }
        Integer position = dto.getPosition() != null ? dto.getPosition() : 0;
        Enumeration enumeration = new Enumeration();
        enumeration.setType(ENUM_TYPE_DOCUMENT_CATEGORY);
        enumeration.setProjectId(projectId.intValue());
        enumeration.setName(name);
        enumeration.setPosition(position);
        enumeration.setActive(true);
        enumerationMapper.insert(enumeration);
        log.info("项目级文档分类已创建: projectId={}, categoryId={}, name={}", projectId, enumeration.getId(), name);
        return DocumentCategoryResponseDTO.builder()
                .id(enumeration.getId())
                .name(enumeration.getName())
                .position(enumeration.getPosition())
                .projectId(enumeration.getProjectId())
                .build();
    }

    /**
     * 更新项目级文档分类（仅可更新本项目下的分类，不可更新全局分类）。
     */
    @Transactional(rollbackFor = Exception.class)
    public DocumentCategoryResponseDTO updateDocumentCategory(Long projectId, Integer categoryId,
            DocumentCategoryUpdateRequestDTO dto) {
        if (dto == null) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "更新内容不能为空");
        }
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException(ResultCode.PROJECT_NOT_FOUND);
        }
        if (!isDocumentsEnabledForProject(projectId)) {
            throw new BusinessException(ResultCode.DOCUMENTS_NOT_ENABLED);
        }
        Enumeration cat = getProjectDocumentCategoryOrThrow(projectId, categoryId);
        if (dto.getName() != null) {
            String name = dto.getName().trim();
            if (name.isEmpty()) {
                throw new BusinessException(ResultCode.PARAM_INVALID, "分类名称不能为空");
            }
            LambdaQueryWrapper<Enumeration> sameName = new LambdaQueryWrapper<>();
            sameName.eq(Enumeration::getType, ENUM_TYPE_DOCUMENT_CATEGORY)
                    .eq(Enumeration::getProjectId, projectId.intValue())
                    .eq(Enumeration::getName, name)
                    .ne(Enumeration::getId, categoryId);
            if (enumerationMapper.selectCount(sameName) > 0) {
                throw new BusinessException(ResultCode.DOCUMENT_CATEGORY_NAME_EXISTS);
            }
            cat.setName(name);
        }
        if (dto.getPosition() != null) {
            cat.setPosition(dto.getPosition());
        }
        enumerationMapper.updateById(cat);
        log.info("项目级文档分类已更新: projectId={}, categoryId={}", projectId, categoryId);
        return DocumentCategoryResponseDTO.builder()
                .id(cat.getId())
                .name(cat.getName())
                .position(cat.getPosition())
                .projectId(cat.getProjectId())
                .build();
    }

    /**
     * 删除项目级文档分类。仅可删除本项目下的分类；若该分类下仍有文档则不允许删除。
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteDocumentCategory(Long projectId, Integer categoryId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException(ResultCode.PROJECT_NOT_FOUND);
        }
        if (!isDocumentsEnabledForProject(projectId)) {
            throw new BusinessException(ResultCode.DOCUMENTS_NOT_ENABLED);
        }
        Enumeration cat = getProjectDocumentCategoryOrThrow(projectId, categoryId);
        LambdaQueryWrapper<Document> docWrapper = new LambdaQueryWrapper<>();
        docWrapper.eq(Document::getProjectId, projectId.intValue()).eq(Document::getCategoryId, categoryId);
        if (documentMapper.selectCount(docWrapper) > 0) {
            throw new BusinessException(ResultCode.DOCUMENT_CATEGORY_IN_USE);
        }
        enumerationMapper.deleteById(categoryId);
        log.info("项目级文档分类已删除: projectId={}, categoryId={}, name={}", projectId, categoryId, cat.getName());
    }

    /**
     * 获取项目级文档分类，若不存在或为全局分类则抛异常（用于增删改时校验）。
     */
    private Enumeration getProjectDocumentCategoryOrThrow(Long projectId, Integer categoryId) {
        Enumeration cat = enumerationMapper.selectById(categoryId);
        if (cat == null) {
            throw new BusinessException(ResultCode.DOCUMENT_CATEGORY_NOT_FOUND);
        }
        if (!ENUM_TYPE_DOCUMENT_CATEGORY.equals(cat.getType())) {
            throw new BusinessException(ResultCode.DOCUMENT_CATEGORY_NOT_FOUND);
        }
        if (cat.getProjectId() == null) {
            throw new BusinessException(ResultCode.DOCUMENT_CATEGORY_NOT_EDITABLE);
        }
        if (!cat.getProjectId().equals(projectId.intValue())) {
            throw new BusinessException(ResultCode.DOCUMENT_CATEGORY_NOT_FOUND);
        }
        return cat;
    }

    /**
     * 文档分页列表：支持按 categoryId、keyword（title/description 模糊）筛选，按 created_on 倒序。
     * 要求项目存在且已启用文档模块。
     */
    public PageResponse<DocumentListItemResponseDTO> listDocuments(Long projectId, Integer categoryId,
            String keyword, Integer current, Integer size) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException(ResultCode.PROJECT_NOT_FOUND);
        }
        if (!isDocumentsEnabledForProject(projectId)) {
            throw new BusinessException(ResultCode.DOCUMENTS_NOT_ENABLED);
        }
        LambdaQueryWrapper<Document> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Document::getProjectId, projectId.intValue());
        if (categoryId != null && categoryId > 0) {
            wrapper.eq(Document::getCategoryId, categoryId);
        }
        if (keyword != null && !keyword.isBlank()) {
            String k = keyword.trim();
            wrapper.and(w -> w.like(Document::getTitle, k).or().like(Document::getDescription, k));
        }
        wrapper.orderByDesc(Document::getCreatedOn);
        int pageNum = (current != null && current > 0) ? current : 1;
        int pageSize = (size != null && size > 0) ? size : 20;
        Page<Document> page = new Page<>(pageNum, pageSize);
        Page<Document> result = documentMapper.selectPage(page, wrapper);
        List<DocumentListItemResponseDTO> list = new ArrayList<>();
        for (Document doc : result.getRecords()) {
            list.add(toListItemResponse(doc));
        }
        return PageResponse.of(list, result.getTotal(), result.getCurrent(), result.getSize());
    }

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
     * 更新文档：仅更新请求中非空字段（title、description、categoryId）。
     * 要求文档属于当前项目且项目已启用文档模块；若传入 categoryId&gt;0 会做分类校验。
     */
    @Transactional(rollbackFor = Exception.class)
    public DocumentDetailResponseDTO update(Long projectId, Integer documentId, DocumentUpdateRequestDTO dto) {
        if (dto == null) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "更新内容不能为空");
        }
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException(ResultCode.PROJECT_NOT_FOUND);
        }
        if (!isDocumentsEnabledForProject(projectId)) {
            throw new BusinessException(ResultCode.DOCUMENTS_NOT_ENABLED);
        }
        Document doc = getDocumentByProjectAndId(projectId, documentId);
        if (dto.getTitle() != null) {
            String title = dto.getTitle().trim();
            if (title.isEmpty()) {
                throw new BusinessException(ResultCode.PARAM_INVALID, "文档标题不能为空");
            }
            doc.setTitle(title);
        }
        if (dto.getDescription() != null) {
            doc.setDescription(dto.getDescription());
        }
        if (dto.getCategoryId() != null) {
            int categoryId = dto.getCategoryId() < 0 ? 0 : dto.getCategoryId();
            if (categoryId > 0) {
                validateDocumentCategory(categoryId, projectId);
            }
            doc.setCategoryId(categoryId);
        }
        documentMapper.updateById(doc);
        log.info("文档已更新: projectId={}, documentId={}", projectId, documentId);
        return toDetailResponse(doc, projectId.longValue());
    }

    /**
     * 文档详情：按 documentId 返回文档信息（含分类名称）。
     * 要求文档属于当前项目且项目已启用文档模块。
     */
    public DocumentDetailResponseDTO getDetail(Long projectId, Integer documentId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException(ResultCode.PROJECT_NOT_FOUND);
        }
        if (!isDocumentsEnabledForProject(projectId)) {
            throw new BusinessException(ResultCode.DOCUMENTS_NOT_ENABLED);
        }
        Document doc = getDocumentByProjectAndId(projectId, documentId);
        DocumentDetailResponseDTO detail = toDetailResponse(doc, projectId.longValue());
        fillAttachments(detail, documentId);
        return detail;
    }

    /**
     * 删除文档：同时级联删除该文档下的所有附件（数据库记录与物理文件）。
     * 要求文档属于当前项目且项目已启用文档模块。
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long projectId, Integer documentId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException(ResultCode.PROJECT_NOT_FOUND);
        }
        if (!isDocumentsEnabledForProject(projectId)) {
            throw new BusinessException(ResultCode.DOCUMENTS_NOT_ENABLED);
        }
        Document doc = getDocumentByProjectAndId(projectId, documentId);
        attachmentService.deleteAttachmentsByContainer(CONTAINER_TYPE_DOCUMENT, documentId.longValue());
        documentMapper.deleteById(documentId);
        log.info("文档已删除（含附件）: projectId={}, documentId={}, title={}", projectId, documentId, doc.getTitle());
    }

    /**
     * 为文档详情填充附件数量与附件列表（仅详情接口使用）。
     */
    private void fillAttachments(DocumentDetailResponseDTO detail, Integer documentId) {
        AttachmentQueryRequestDTO req = new AttachmentQueryRequestDTO();
        req.setContainerType(CONTAINER_TYPE_DOCUMENT);
        req.setContainerId(documentId.longValue());
        req.setCurrent(1);
        req.setSize(DETAIL_ATTACHMENT_PAGE_SIZE);
        PageResponse<AttachmentResponseDTO> page = attachmentService.queryAttachments(req);
        detail.setAttachmentCount(page.getTotal());
        detail.setAttachments(page.getRecords());
    }

    /**
     * 按项目与文档 ID 获取文档，不存在或不属于该项目则抛 DOCUMENT_NOT_FOUND。
     */
    private Document getDocumentByProjectAndId(Long projectId, Integer documentId) {
        Document doc = documentMapper.selectById(documentId);
        if (doc == null || !doc.getProjectId().equals(projectId.intValue())) {
            throw new BusinessException(ResultCode.DOCUMENT_NOT_FOUND);
        }
        return doc;
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

    private String resolveCategoryName(Integer categoryId) {
        if (categoryId == null || categoryId <= 0) {
            return null;
        }
        Enumeration cat = enumerationMapper.selectById(categoryId);
        return (cat != null && ENUM_TYPE_DOCUMENT_CATEGORY.equals(cat.getType())) ? cat.getName() : null;
    }

    private DocumentListItemResponseDTO toListItemResponse(Document doc) {
        return DocumentListItemResponseDTO.builder()
                .id(doc.getId())
                .projectId(doc.getProjectId())
                .title(doc.getTitle())
                .description(doc.getDescription())
                .categoryId(doc.getCategoryId())
                .categoryName(resolveCategoryName(doc.getCategoryId()))
                .createdOn(doc.getCreatedOn())
                .build();
    }

    private DocumentDetailResponseDTO toDetailResponse(Document doc, Long projectId) {
        return DocumentDetailResponseDTO.builder()
                .id(doc.getId())
                .projectId(doc.getProjectId())
                .title(doc.getTitle())
                .description(doc.getDescription())
                .categoryId(doc.getCategoryId())
                .categoryName(resolveCategoryName(doc.getCategoryId()))
                .createdOn(doc.getCreatedOn())
                .build();
    }
}
