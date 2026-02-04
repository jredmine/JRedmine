package com.github.jredmine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.jredmine.dto.request.wiki.WikiPageCreateRequestDTO;
import com.github.jredmine.dto.request.wiki.WikiPageUpdateRequestDTO;
import com.github.jredmine.dto.response.PageResponse;
import com.github.jredmine.dto.response.wiki.WikiInfoResponseDTO;
import com.github.jredmine.dto.response.wiki.WikiPageDetailResponseDTO;
import com.github.jredmine.dto.response.wiki.WikiPageListItemResponseDTO;
import com.github.jredmine.dto.request.wiki.WikiRedirectCreateRequestDTO;
import com.github.jredmine.dto.response.wiki.WikiPageVersionDetailResponseDTO;
import com.github.jredmine.dto.response.wiki.WikiPageVersionListItemResponseDTO;
import com.github.jredmine.dto.response.wiki.WikiRedirectResponseDTO;
import com.github.jredmine.entity.EnabledModule;
import com.github.jredmine.entity.Project;
import com.github.jredmine.entity.User;
import com.github.jredmine.entity.Wiki;
import com.github.jredmine.entity.WikiContent;
import com.github.jredmine.entity.WikiContentVersion;
import com.github.jredmine.entity.WikiPage;
import com.github.jredmine.entity.WikiRedirect;
import com.github.jredmine.enums.ProjectModule;
import com.github.jredmine.enums.ResultCode;
import com.github.jredmine.exception.BusinessException;
import com.github.jredmine.mapper.project.EnabledModuleMapper;
import com.github.jredmine.mapper.project.ProjectMapper;
import com.github.jredmine.mapper.user.UserMapper;
import com.github.jredmine.mapper.wiki.WikiContentMapper;
import com.github.jredmine.mapper.wiki.WikiContentVersionMapper;
import com.github.jredmine.mapper.wiki.WikiMapper;
import com.github.jredmine.mapper.wiki.WikiPageMapper;
import com.github.jredmine.mapper.wiki.WikiRedirectMapper;
import com.github.jredmine.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
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
    private final WikiContentVersionMapper wikiContentVersionMapper;
    private final WikiRedirectMapper wikiRedirectMapper;
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
     * 根据 titleOrId 解析为 WikiPage：纯数字则按 id 查，否则按 title 查（同一 wiki 下）。
     * 按标题查时先解析重定向：若存在重定向则用目标标题查页面。
     */
    public WikiPage getPageByProjectAndTitleOrId(Long projectId, String titleOrId) {
        Wiki wiki = getOrCreateWiki(projectId);
        if (titleOrId == null || titleOrId.isBlank()) {
            throw new BusinessException(ResultCode.WIKI_PAGE_NOT_FOUND);
        }
        String effectiveTitle = titleOrId.trim();
        if (!titleOrId.matches("\\d+")) {
            String resolved = resolveRedirect(projectId, effectiveTitle);
            if (resolved != null) {
                effectiveTitle = resolved;
            }
        }
        WikiPage page;
        if (titleOrId.matches("\\d+")) {
            page = wikiPageMapper.selectById(Long.parseLong(titleOrId));
            if (page == null || !page.getWikiId().equals(wiki.getId())) {
                throw new BusinessException(ResultCode.WIKI_PAGE_NOT_FOUND);
            }
        } else {
            LambdaQueryWrapper<WikiPage> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(WikiPage::getWikiId, wiki.getId()).eq(WikiPage::getTitle, effectiveTitle);
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

    /**
     * 获取某页面的指定版本内容
     */
    private WikiContent getContentByVersion(Long pageId, Integer version) {
        LambdaQueryWrapper<WikiContent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WikiContent::getPageId, pageId).eq(WikiContent::getVersion, version);
        return wikiContentMapper.selectOne(wrapper);
    }

    /**
     * 将当前内容版本同步写入 wiki_content_versions 表，作为持久化快照。
     * 每次向 wiki_contents 插入新记录后调用（content 需已具备 id）。
     */
    private void saveContentVersionSnapshot(WikiContent content) {
        if (content == null || content.getId() == null) {
            return;
        }
        WikiContentVersion snapshot = new WikiContentVersion();
        snapshot.setWikiContentId(content.getId());
        snapshot.setPageId(content.getPageId());
        snapshot.setAuthorId(content.getAuthorId());
        String text = content.getText();
        snapshot.setData(text != null ? text.getBytes(StandardCharsets.UTF_8) : new byte[0]);
        snapshot.setCompression("");
        snapshot.setComments(content.getComments());
        snapshot.setUpdatedOn(content.getUpdatedOn());
        snapshot.setVersion(content.getVersion());
        wikiContentVersionMapper.insert(snapshot);
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
        saveContentVersionSnapshot(content);

        log.info("Wiki 页面创建成功: projectId={}, pageId={}, title={}", projectId, page.getId(), title);
        return toDetailResponse(projectId, page, content);
    }

    /**
     * 获取页面详情（含最新内容）。按标题访问时若存在重定向会在 getPageByProjectAndTitleOrId 中解析。
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
            saveContentVersionSnapshot(content);
            log.info("Wiki 页面内容更新: projectId={}, pageId={}, version={}", projectId, page.getId(), nextVersion);
        }
        return getPageWithLatestContent(projectId, titleOrId);
    }

    /**
     * 删除页面（级联删除该页所有内容及版本快照）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deletePage(Long projectId, String titleOrId) {
        WikiPage page = getPageByProjectAndTitleOrId(projectId, titleOrId);
        LambdaQueryWrapper<WikiContentVersion> versionWrapper = new LambdaQueryWrapper<>();
        versionWrapper.eq(WikiContentVersion::getPageId, page.getId());
        wikiContentVersionMapper.delete(versionWrapper);
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

    // ==================== Wiki 版本历史 ====================

    /**
     * 获取页面所有版本列表（按版本号倒序，最新在前）
     */
    public List<WikiPageVersionListItemResponseDTO> listVersions(Long projectId, String titleOrId) {
        WikiPage page = getPageByProjectAndTitleOrId(projectId, titleOrId);
        LambdaQueryWrapper<WikiContent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WikiContent::getPageId, page.getId()).orderByDesc(WikiContent::getVersion);
        List<WikiContent> contents = wikiContentMapper.selectList(wrapper);
        List<WikiPageVersionListItemResponseDTO> list = new ArrayList<>();
        for (WikiContent c : contents) {
            list.add(WikiPageVersionListItemResponseDTO.builder()
                    .version(c.getVersion())
                    .authorId(c.getAuthorId())
                    .authorName(getAuthorDisplayName(c.getAuthorId()))
                    .updatedOn(c.getUpdatedOn())
                    .comments(c.getComments())
                    .build());
        }
        return list;
    }

    /**
     * 获取指定版本内容详情
     */
    public WikiPageVersionDetailResponseDTO getVersionContent(Long projectId, String titleOrId, Integer version) {
        WikiPage page = getPageByProjectAndTitleOrId(projectId, titleOrId);
        WikiContent content = getContentByVersion(page.getId(), version);
        if (content == null) {
            throw new BusinessException(ResultCode.WIKI_VERSION_NOT_FOUND);
        }
        return WikiPageVersionDetailResponseDTO.builder()
                .pageId(page.getId())
                .pageTitle(page.getTitle())
                .version(content.getVersion())
                .text(content.getText())
                .comments(content.getComments())
                .authorId(content.getAuthorId())
                .authorName(getAuthorDisplayName(content.getAuthorId()))
                .updatedOn(content.getUpdatedOn())
                .build();
    }

    /**
     * 回滚到指定版本：将指定版本正文作为新版本插入，备注为「回滚到版本 N」
     */
    @Transactional(rollbackFor = Exception.class)
    public WikiPageDetailResponseDTO revertToVersion(Long projectId, String titleOrId, Integer version) {
        WikiPage page = getPageByProjectAndTitleOrId(projectId, titleOrId);
        WikiContent targetContent = getContentByVersion(page.getId(), version);
        if (targetContent == null) {
            throw new BusinessException(ResultCode.WIKI_VERSION_NOT_FOUND);
        }
        WikiContent latest = getLatestContent(page.getId());
        int nextVersion = (latest != null ? latest.getVersion() : 0) + 1;
        Date now = new Date();
        WikiContent newContent = new WikiContent();
        newContent.setPageId(page.getId());
        newContent.setAuthorId(securityUtils.getCurrentUserId());
        newContent.setText(targetContent.getText() != null ? targetContent.getText() : "");
        newContent.setComments("回滚到版本 " + version);
        newContent.setUpdatedOn(now);
        newContent.setVersion(nextVersion);
        wikiContentMapper.insert(newContent);
        saveContentVersionSnapshot(newContent);
        log.info("Wiki 页面回滚: projectId={}, pageId={}, 回滚到版本={}, 新版本={}", projectId, page.getId(), version, nextVersion);
        Wiki wiki = wikiMapper.selectById(page.getWikiId());
        Long projectIdResolved = wiki != null ? wiki.getProjectId() : projectId;
        return toDetailResponse(projectIdResolved, page, newContent);
    }

    // ==================== Wiki 重定向 ====================

    /**
     * 解析重定向：若标题存在重定向则返回目标标题，否则返回 null。
     */
    public String resolveRedirect(Long projectId, String title) {
        if (title == null || title.isBlank()) return null;
        Wiki wiki = getOrCreateWiki(projectId);
        LambdaQueryWrapper<WikiRedirect> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WikiRedirect::getWikiId, wiki.getId()).eq(WikiRedirect::getTitle, title.trim());
        WikiRedirect redirect = wikiRedirectMapper.selectOne(wrapper);
        return redirect != null ? redirect.getRedirectsTo() : null;
    }

    /**
     * 创建重定向：原标题 -> 目标标题（目标页须存在于本 Wiki）。
     */
    @Transactional(rollbackFor = Exception.class)
    public WikiRedirectResponseDTO createRedirect(Long projectId, WikiRedirectCreateRequestDTO dto) {
        if (dto == null || dto.getTitle() == null || dto.getTitle().isBlank()
                || dto.getRedirectsTo() == null || dto.getRedirectsTo().isBlank()) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "原标题与目标标题不能为空");
        }
        Wiki wiki = getOrCreateWiki(projectId);
        String title = dto.getTitle().trim();
        String redirectsTo = dto.getRedirectsTo().trim();
        if (title.equals(redirectsTo)) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "原标题不能与目标标题相同");
        }
        LambdaQueryWrapper<WikiPage> pageByTitle = new LambdaQueryWrapper<>();
        pageByTitle.eq(WikiPage::getWikiId, wiki.getId()).eq(WikiPage::getTitle, title);
        if (wikiPageMapper.selectCount(pageByTitle) > 0) {
            throw new BusinessException(ResultCode.WIKI_REDIRECT_TITLE_EXISTS);
        }
        LambdaQueryWrapper<WikiRedirect> redirectExist = new LambdaQueryWrapper<>();
        redirectExist.eq(WikiRedirect::getWikiId, wiki.getId()).eq(WikiRedirect::getTitle, title);
        if (wikiRedirectMapper.selectCount(redirectExist) > 0) {
            throw new BusinessException(ResultCode.WIKI_REDIRECT_TITLE_EXISTS);
        }
        LambdaQueryWrapper<WikiPage> targetPage = new LambdaQueryWrapper<>();
        targetPage.eq(WikiPage::getWikiId, wiki.getId()).eq(WikiPage::getTitle, redirectsTo);
        if (wikiPageMapper.selectCount(targetPage) == 0) {
            throw new BusinessException(ResultCode.WIKI_REDIRECT_TARGET_NOT_FOUND);
        }
        Date now = new Date();
        WikiRedirect redirect = new WikiRedirect();
        redirect.setWikiId(wiki.getId());
        redirect.setTitle(title);
        redirect.setRedirectsTo(redirectsTo);
        redirect.setRedirectsToWikiId(wiki.getId());
        redirect.setCreatedOn(now);
        wikiRedirectMapper.insert(redirect);
        log.info("Wiki 重定向创建: projectId={}, title={} -> {}", projectId, title, redirectsTo);
        return toRedirectResponse(redirect, projectId);
    }

    /**
     * 列出项目 Wiki 下所有重定向
     */
    public List<WikiRedirectResponseDTO> listRedirects(Long projectId) {
        Wiki wiki = getOrCreateWiki(projectId);
        LambdaQueryWrapper<WikiRedirect> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WikiRedirect::getWikiId, wiki.getId()).orderByAsc(WikiRedirect::getTitle);
        List<WikiRedirect> list = wikiRedirectMapper.selectList(wrapper);
        List<WikiRedirectResponseDTO> result = new ArrayList<>();
        for (WikiRedirect r : list) {
            result.add(toRedirectResponse(r, projectId));
        }
        return result;
    }

    /**
     * 删除重定向
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteRedirect(Long projectId, Long redirectId) {
        Wiki wiki = getOrCreateWiki(projectId);
        WikiRedirect redirect = wikiRedirectMapper.selectById(redirectId);
        if (redirect == null || !redirect.getWikiId().equals(wiki.getId())) {
            throw new BusinessException(ResultCode.WIKI_REDIRECT_NOT_FOUND);
        }
        wikiRedirectMapper.deleteById(redirectId);
        log.info("Wiki 重定向已删除: projectId={}, redirectId={}, title={}", projectId, redirectId, redirect.getTitle());
    }

    private WikiRedirectResponseDTO toRedirectResponse(WikiRedirect r, Long projectId) {
        return WikiRedirectResponseDTO.builder()
                .id(r.getId())
                .wikiId(r.getWikiId())
                .projectId(projectId)
                .title(r.getTitle())
                .redirectsTo(r.getRedirectsTo())
                .redirectsToWikiId(r.getRedirectsToWikiId())
                .createdOn(r.getCreatedOn())
                .build();
    }
}
