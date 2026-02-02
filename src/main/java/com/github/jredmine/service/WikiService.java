package com.github.jredmine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.jredmine.dto.response.wiki.WikiInfoResponseDTO;
import com.github.jredmine.entity.EnabledModule;
import com.github.jredmine.entity.Project;
import com.github.jredmine.entity.Wiki;
import com.github.jredmine.enums.ProjectModule;
import com.github.jredmine.enums.ResultCode;
import com.github.jredmine.exception.BusinessException;
import com.github.jredmine.mapper.project.EnabledModuleMapper;
import com.github.jredmine.mapper.project.ProjectMapper;
import com.github.jredmine.mapper.wiki.WikiMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final EnabledModuleMapper enabledModuleMapper;
    private final ProjectMapper projectMapper;

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
}
