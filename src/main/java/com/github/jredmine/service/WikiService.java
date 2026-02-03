package com.github.jredmine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.jredmine.dto.request.wiki.WikiPageCreateRequestDTO;
import com.github.jredmine.dto.request.wiki.WikiPageUpdateRequestDTO;
import com.github.jredmine.dto.response.PageResponse;
import com.github.jredmine.dto.response.wiki.WikiInfoResponseDTO;
import com.github.jredmine.dto.response.wiki.WikiPageDetailResponseDTO;
import com.github.jredmine.dto.response.wiki.WikiPageListItemResponseDTO;
import com.github.jredmine.entity.EnabledModule;
import com.github.jredmine.entity.Project;
import com.github.jredmine.entity.User;
import com.github.jredmine.entity.Wiki;
import com.github.jredmine.entity.WikiContent;
import com.github.jredmine.entity.WikiPage;
import com.github.jredmine.enums.ProjectModule;
import com.github.jredmine.enums.ResultCode;
import com.github.jredmine.exception.BusinessException;
import com.github.jredmine.mapper.project.EnabledModuleMapper;
import com.github.jredmine.mapper.project.ProjectMapper;
import com.github.jredmine.mapper.user.UserMapper;
import com.github.jredmine.mapper.wiki.WikiContentMapper;
import com.github.jredmine.mapper.wiki.WikiMapper;
import com.github.jredmine.mapper.wiki.WikiPageMapper;
import com.github.jredmine.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Wiki 服务：项目启用模块时创建/获取 wikis，以及获取 Wiki 信息
 *
 * @author panfeng
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WikiService {

    private static final String DEFAULT_START_PAGE = "Wiki";
    private static final int WIKI_STATUS_ACTIVE = 1;

    private final WikiMapper wikiMapper;
    private final WikiPageMapper wikiPageMapper;
    private final WikiContentMapper wikiContentMapper;
    private final EnabledModuleMapper enabledModuleMapper;
    private final ProjectMapper projectMapper;
    private final UserMapper userMapper;
    private final SecurityUtils securityUtils;

    /**
     * 检查项目是否启用了 Wiki 模块
     */
    public boolean isWikiEnabledForProject(Long projectId) {
        LambdaQueryWrapper<EnabledModule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EnabledModule::getProjectId, projectId)
                .eq(EnabledModule::getName, ProjectModule.WIKI.getCode());
        return enabledModuleMapper.selectCount(wrapper) > 0;
    }

    /**
     * 项目启用 Wiki 模块时，确保该项目有一条 wikis 记录；若不存在则创建。
     * 在项目创建/更新且 enabledModules 包含 "wiki" 时调用。
     */
    @Transactional(rollbackFor = Exception.class)
    public void ensureWikiForProject(Long projectId) {
        if (projectId == null) {
            return;
        }
        LambdaQueryWrapper<Wiki> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Wiki::getProjectId, projectId);
        Wiki existing = wikiMapper.selectOne(wrapper);
        if (existing != null) {
            log.debug("项目已存在 Wiki 记录，projectId={}, wikiId={}", projectId, existing.getId());
            return;
        }
        Wiki wiki = new Wiki();
        wiki.setProjectId(projectId);
        wiki.setStartPage(DEFAULT_START_PAGE);
        wiki.setStatus(WIKI_STATUS_ACTIVE);
        wikiMapper.insert(wiki);
        log.info("已为项目创建 Wiki 记录，projectId={}, wikiId={}", projectId, wiki.getId());
    }

    /**
     * 获取项目 Wiki 主表记录；若项目已启用 Wiki 但尚无 wikis 记录则先创建再返回。
     * 若项目未启用 Wiki 模块则抛出业务异常。
     */
    public Wiki getOrCreateWiki(Long projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException(ResultCode.PROJECT_NOT_FOUND);
        }
        if (!isWikiEnabledForProject(projectId)) {
            throw new BusinessException(ResultCode.WIKI_NOT_ENABLED);
        }
        LambdaQueryWrapper<Wiki> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Wiki::getProjectId, projectId);
        Wiki wiki = wikiMapper.selectOne(wrapper);
        if (wiki == null) {
            ensureWikiForProject(projectId);
            wiki = wikiMapper.selectOne(wrapper);
        }
        return wiki;
    }

    /**
     * 获取项目 Wiki 信息（用于 GET /api/projects/{id}/wiki）。
     * 项目必须启用 Wiki 模块；若尚无 wikis 记录则先创建再返回 DTO。
     */
    public WikiInfoResponseDTO getWikiInfo(Long projectId) {
        Wiki wiki = getOrCreateWiki(projectId);
        Project project = projectMapper.selectById(projectId);
        return WikiInfoResponseDTO.builder()
                .id(wiki.getId())
                .projectId(wiki.getProjectId())
                .projectName(project != null ? project.getName() : null)
                .startPage(wiki.getStartPage())
                .status(wiki.getStatus())
                .build();
    }

    // ==================== Wiki 页面 CRUD ====================

    /**
     * 根据 titleOrId 解析为 WikiPage：纯数字则按 id 查，否则按 title 查（同一 wiki 下）
     */
    public WikiPage getPageByProjectAndTitleOrId(Long projectId, String titleOrId) {
        Wiki wiki = getOrCreateWiki(projectId);
        if (titleOrId == null || titleOrId.isBlank()) {
            throw new BusinessException(ResultCode.WIKI_PAGE_NOT_FOUND);
        }
        WikiPage page;
        if (titleOrId.matches("\\d+")) {
            page = wikiPageMapper.selectById(Long.parseLong(titleOrId));
            if (page == null || !page.getWikiId().equals(wiki.getId())) {
                throw new BusinessException(ResultCode.WIKI_PAGE_NOT_FOUND);
            }
        } else {
            LambdaQueryWrapper<WikiPage> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(WikiPage::getWikiId, wiki.getId()).eq(WikiPage::getTitle, titleOrId.trim());
            page = wikiPageMapper.selectOne(wrapper);
            if (page == null) {
                throw new BusinessException(ResultCode.WIKI_PAGE_NOT_FOUND);
            }
        }
        return page;
    }

    /**
     * 获取某页面的最新内容
     */
    private WikiContent getLatestContent(Long pageId) {
        LambdaQueryWrapper<WikiContent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WikiContent::getPageId, pageId).orderByDesc(WikiContent::getVersion).last("LIMIT 1");
        return wikiContentMapper.selectOne(wrapper);
    }

    private String getAuthorDisplayName(Long authorId) {
        if (authorId == null)
            return null;
        User user = userMapper.selectById(authorId);
        if (user == null)
            return null;
        String name = ((user.getFirstname() != null ? user.getFirstname() : "") + " "
                + (user.getLastname() != null ? user.getLastname() : "")).trim();
        return name.isEmpty() ? user.getLogin() : name;
    }

    /**
     * 分页列出 Wiki 页面（平铺，可选按 parentId 筛选）
     */
    public PageResponse<WikiPageListItemResponseDTO> listPages(Long projectId, Integer current, Integer size,
            Long parentId) {
        Wiki wiki = getOrCreateWiki(projectId);
        LambdaQueryWrapper<WikiPage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WikiPage::getWikiId, wiki.getId());
        if (parentId != null) {
            wrapper.eq(WikiPage::getParentId, parentId);
        }
        wrapper.orderByAsc(WikiPage::getTitle);
        Page<WikiPage> page = new Page<>(current != null ? current : 1, size != null && size > 0 ? size : 20);
        Page<WikiPage> result = wikiPageMapper.selectPage(page, wrapper);
        List<WikiPageListItemResponseDTO> list = new ArrayList<>();
        for (WikiPage p : result.getRecords()) {
            WikiContent content = getLatestContent(p.getId());
            list.add(WikiPageListItemResponseDTO.builder()
                    .id(p.getId())
                    .title(p.getTitle())
                    .parentId(p.getParentId())
                    .isProtected(p.getIsProtected())
                    .createdOn(p.getCreatedOn())
                    .updatedOn(content != null ? content.getUpdatedOn() : p.getCreatedOn())
                    .version(content != null ? content.getVersion() : 0)
                    .authorName(content != null ? getAuthorDisplayName(content.getAuthorId()) : null)
                    .build());
        }
        return PageResponse.of(list, result.getTotal(), result.getCurrent(), result.getSize());
    }

    /**
     * 创建 Wiki 页面（可选带初始内容）
     */
    @Transactional(rollbackFor = Exception.class)
    public WikiPageDetailResponseDTO createPage(Long projectId, WikiPageCreateRequestDTO dto) {
        if (dto == null || dto.getTitle() == null || dto.getTitle().isBlank()) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "页面标题不能为空");
        }
        Wiki wiki = getOrCreateWiki(projectId);
        String title = dto.getTitle().trim();
        LambdaQueryWrapper<WikiPage> existWrapper = new LambdaQueryWrapper<>();
        existWrapper.eq(WikiPage::getWikiId, wiki.getId()).eq(WikiPage::getTitle, title);
        if (wikiPageMapper.selectCount(existWrapper) > 0) {
            throw new BusinessException(ResultCode.WIKI_PAGE_TITLE_EXISTS);
        }
        Date now = new Date();
        WikiPage page = new WikiPage();
        page.setWikiId(wiki.getId());
        page.setTitle(title);
        page.setCreatedOn(now);
        page.setParentId(dto.getParentId());
        page.setIsProtected(dto.getIsProtected() != null && dto.getIsProtected());
        wikiPageMapper.insert(page);

        int version = 1;
        WikiContent content = new WikiContent();
        content.setPageId(page.getId());
        content.setAuthorId(securityUtils.getCurrentUserId());
        content.setText(dto.getText() != null ? dto.getText() : "");
        content.setComments(dto.getComments());
        content.setUpdatedOn(now);
        content.setVersion(version);
        wikiContentMapper.insert(content);

        log.info("Wiki 页面创建成功: projectId={}, pageId={}, title={}", projectId, page.getId(), title);
        return toDetailResponse(projectId, page, content);
    }

    /**
     * 获取页面详情（含最新内容）
     */
    public WikiPageDetailResponseDTO getPageWithLatestContent(Long projectId, String titleOrId) {
        WikiPage page = getPageByProjectAndTitleOrId(projectId, titleOrId);
        WikiContent content = getLatestContent(page.getId());
        Wiki wiki = wikiMapper.selectById(page.getWikiId());
        Long projectIdResolved = wiki != null ? wiki.getProjectId() : projectId;
        return toDetailResponse(projectIdResolved, page, content);
    }

    /**
     * 更新页面（元数据及/或新增内容版本）
     */
    @Transactional(rollbackFor = Exception.class)
    public WikiPageDetailResponseDTO updatePage(Long projectId, String titleOrId, WikiPageUpdateRequestDTO dto) {
        WikiPage page = getPageByProjectAndTitleOrId(projectId, titleOrId);
        if (dto == null) {
            return getPageWithLatestContent(projectId, titleOrId);
        }
        boolean updateMeta = dto.getParentId() != null || dto.getIsProtected() != null;
        if (updateMeta) {
            if (dto.getParentId() != null)
                page.setParentId(dto.getParentId());
            if (dto.getIsProtected() != null)
                page.setIsProtected(dto.getIsProtected());
            wikiPageMapper.updateById(page);
        }
        boolean hasNewContent = (dto.getText() != null) || (dto.getComments() != null);
        if (hasNewContent) {
            WikiContent latest = getLatestContent(page.getId());
            int nextVersion = (latest != null ? latest.getVersion() : 0) + 1;
            Date now = new Date();
            WikiContent content = new WikiContent();
            content.setPageId(page.getId());
            content.setAuthorId(securityUtils.getCurrentUserId());
            content.setText(dto.getText() != null ? dto.getText() : (latest != null ? latest.getText() : ""));
            content.setComments(dto.getComments());
            content.setUpdatedOn(now);
            content.setVersion(nextVersion);
            wikiContentMapper.insert(content);
            log.info("Wiki 页面内容更新: projectId={}, pageId={}, version={}", projectId, page.getId(), nextVersion);
        }
        return getPageWithLatestContent(projectId, titleOrId);
    }

    /**
     * 删除页面（级联删除该页所有内容）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deletePage(Long projectId, String titleOrId) {
        WikiPage page = getPageByProjectAndTitleOrId(projectId, titleOrId);
        LambdaQueryWrapper<WikiContent> contentWrapper = new LambdaQueryWrapper<>();
        contentWrapper.eq(WikiContent::getPageId, page.getId());
        wikiContentMapper.delete(contentWrapper);
        wikiPageMapper.deleteById(page.getId());
        log.info("Wiki 页面已删除: projectId={}, pageId={}, title={}", projectId, page.getId(), page.getTitle());
    }

    private WikiPageDetailResponseDTO toDetailResponse(Long projectId, WikiPage page, WikiContent content) {
        return WikiPageDetailResponseDTO.builder()
                .id(page.getId())
                .wikiId(page.getWikiId())
                .projectId(projectId)
                .title(page.getTitle())
                .parentId(page.getParentId())
                .isProtected(page.getIsProtected())
                .createdOn(page.getCreatedOn())
                .text(content != null ? content.getText() : null)
                .comments(content != null ? content.getComments() : null)
                .version(content != null ? content.getVersion() : 0)
                .updatedOn(content != null ? content.getUpdatedOn() : page.getCreatedOn())
                .authorId(content != null ? content.getAuthorId() : null)
                .authorName(content != null ? getAuthorDisplayName(content.getAuthorId()) : null)
                .build();
    }
}
