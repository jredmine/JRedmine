package com.github.jredmine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.jredmine.dto.request.search.GlobalSearchRequestDTO;
import com.github.jredmine.dto.response.search.GlobalSearchResponseDTO;
import com.github.jredmine.dto.response.search.SearchResultItemDTO;
import com.github.jredmine.entity.*;
import com.github.jredmine.mapper.issue.IssueMapper;
import com.github.jredmine.mapper.project.ProjectMapper;
import com.github.jredmine.mapper.user.UserMapper;
import com.github.jredmine.mapper.wiki.WikiPageMapper;
import com.github.jredmine.mapper.wiki.WikiContentMapper;
import com.github.jredmine.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 搜索服务
 *
 * @author panfeng
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private final IssueMapper issueMapper;
    private final ProjectMapper projectMapper;
    private final UserMapper userMapper;
    private final WikiPageMapper wikiPageMapper;
    private final WikiContentMapper wikiContentMapper;
    private final SearchHistoryService searchHistoryService;
    private final SecurityUtils securityUtils;

    /**
     * 全局搜索
     */
    public GlobalSearchResponseDTO globalSearch(GlobalSearchRequestDTO requestDTO) {
        String keyword = requestDTO.getKeyword();
        if (!StringUtils.hasText(keyword)) {
            return buildEmptyResponse(keyword);
        }

        keyword = keyword.trim();
        List<String> searchTypes = requestDTO.getTypes();
        
        // 如果未指定类型，搜索所有类型
        if (searchTypes == null || searchTypes.isEmpty()) {
            searchTypes = Arrays.asList("issue", "project", "wiki");
        }

        // 存储各类型搜索结果
        List<SearchResultItemDTO> allResults = new ArrayList<>();
        Map<String, Long> typeCounts = new HashMap<>();
        
        // 搜索任务
        if (searchTypes.contains("issue")) {
            List<SearchResultItemDTO> issueResults = searchIssues(keyword, requestDTO.getProjectId());
            allResults.addAll(issueResults);
            typeCounts.put("issue", (long) issueResults.size());
        } else {
            typeCounts.put("issue", 0L);
        }

        // 搜索项目
        if (searchTypes.contains("project")) {
            List<SearchResultItemDTO> projectResults = searchProjects(keyword);
            allResults.addAll(projectResults);
            typeCounts.put("project", (long) projectResults.size());
        } else {
            typeCounts.put("project", 0L);
        }

        // 搜索Wiki
        if (searchTypes.contains("wiki")) {
            List<SearchResultItemDTO> wikiResults = searchWikis(keyword, requestDTO.getProjectId());
            allResults.addAll(wikiResults);
            typeCounts.put("wiki", (long) wikiResults.size());
        } else {
            typeCounts.put("wiki", 0L);
        }

        // 按更新时间倒序排序
        allResults.sort((a, b) -> {
            if (a.getUpdatedOn() == null && b.getUpdatedOn() == null) return 0;
            if (a.getUpdatedOn() == null) return 1;
            if (b.getUpdatedOn() == null) return -1;
            return b.getUpdatedOn().compareTo(a.getUpdatedOn());
        });

        // 计算分页
        int pageNum = requestDTO.getCurrent() != null ? requestDTO.getCurrent() : 1;
        int pageSize = requestDTO.getSize() != null ? requestDTO.getSize() : 20;
        long total = allResults.size();
        
        int startIndex = (pageNum - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, allResults.size());
        
        List<SearchResultItemDTO> pagedResults = new ArrayList<>();
        if (startIndex < allResults.size()) {
            pagedResults = allResults.subList(startIndex, endIndex);
        }

        // 按类型分组（用于展示分类标签）
        Map<String, List<SearchResultItemDTO>> groupedResults = pagedResults.stream()
                .collect(Collectors.groupingBy(SearchResultItemDTO::getType));

        // 记录搜索历史
        recordSearchHistory(requestDTO, total);

        return GlobalSearchResponseDTO.builder()
                .keyword(requestDTO.getKeyword())
                .totalCount(total)
                .typeCounts(typeCounts)
                .results(com.github.jredmine.dto.response.PageResponse.of(
                        pagedResults, total, (long) pageNum, (long) pageSize))
                .groupedResults(groupedResults)
                .build();
    }

    /**
     * 搜索任务
     */
    private List<SearchResultItemDTO> searchIssues(String keyword, Long projectId) {
        LambdaQueryWrapper<Issue> queryWrapper = new LambdaQueryWrapper<>();
        
        // 权限过滤：只查询用户有权限查看的项目的任务
        List<Long> accessibleProjectIds = getAccessibleProjectIds();
        if (accessibleProjectIds.isEmpty()) {
            return new ArrayList<>();
        }
        queryWrapper.in(Issue::getProjectId, accessibleProjectIds);

        // 指定项目范围
        if (projectId != null) {
            queryWrapper.eq(Issue::getProjectId, projectId);
        }

        // 关键词搜索（在标题和描述中搜索）
        queryWrapper.and(wrapper -> {
            wrapper.like(Issue::getSubject, keyword)
                    .or()
                    .like(Issue::getDescription, keyword);
        });

        // 按更新时间倒序
        queryWrapper.orderByDesc(Issue::getUpdatedOn);

        List<Issue> issues = issueMapper.selectList(queryWrapper);
        
        return issues.stream().map(issue -> {
            Project project = projectMapper.selectById(issue.getProjectId());
            User author = issue.getAuthorId() != null ? userMapper.selectById(issue.getAuthorId()) : null;

            return SearchResultItemDTO.builder()
                    .type("issue")
                    .id(issue.getId())
                    .title(issue.getSubject())
                    .description(truncateDescription(issue.getDescription()))
                    .projectId(issue.getProjectId())
                    .projectName(project != null ? project.getName() : null)
                    .authorId(issue.getAuthorId())
                    .authorName(author != null ? getUserDisplayName(author) : null)
                    .createdOn(issue.getCreatedOn())
                    .updatedOn(issue.getUpdatedOn())
                    .url("/issues/" + issue.getId())
                    .isPrivate(issue.getIsPrivate() != null && issue.getIsPrivate())
                    .status(issue.getStatusId() != null ? issue.getStatusId().toString() : null)
                    .priority(issue.getPriorityId())
                    .build();
        }).collect(Collectors.toList());
    }

    /**
     * 搜索项目
     */
    private List<SearchResultItemDTO> searchProjects(String keyword) {
        LambdaQueryWrapper<Project> queryWrapper = new LambdaQueryWrapper<>();
        
        // 权限过滤：只查询用户有权限查看的项目
        List<Long> accessibleProjectIds = getAccessibleProjectIds();
        if (accessibleProjectIds.isEmpty()) {
            return new ArrayList<>();
        }
        queryWrapper.in(Project::getId, accessibleProjectIds);

        // 关键词搜索（在名称和描述中搜索）
        queryWrapper.and(wrapper -> {
            wrapper.like(Project::getName, keyword)
                    .or()
                    .like(Project::getDescription, keyword);
        });

        // 按更新时间倒序
        queryWrapper.orderByDesc(Project::getUpdatedOn);

        List<Project> projects = projectMapper.selectList(queryWrapper);
        
        return projects.stream().map(project -> {
            return SearchResultItemDTO.builder()
                    .type("project")
                    .id(project.getId())
                    .title(project.getName())
                    .description(truncateDescription(project.getDescription()))
                    .projectId(project.getId())
                    .projectName(project.getName())
                    .authorId(null) // 项目没有作者字段，可以用创建者
                    .authorName(null)
                    .createdOn(project.getCreatedOn() != null ? 
                            new java.sql.Timestamp(project.getCreatedOn().getTime()).toLocalDateTime() : null)
                    .updatedOn(project.getUpdatedOn() != null ? 
                            new java.sql.Timestamp(project.getUpdatedOn().getTime()).toLocalDateTime() : null)
                    .url("/projects/" + project.getIdentifier())
                    .isPrivate(project.getIsPublic() == null || !project.getIsPublic())
                    .status(project.getStatus() != null ? project.getStatus().toString() : null)
                    .statusName(getProjectStatusName(project.getStatus()))
                    .build();
        }).collect(Collectors.toList());
    }

    /**
     * 搜索Wiki
     */
    private List<SearchResultItemDTO> searchWikis(String keyword, Long projectId) {
        // 首先根据Wiki页面标题搜索
        LambdaQueryWrapper<WikiPage> pageQueryWrapper = new LambdaQueryWrapper<>();
        
        // 权限过滤：只查询用户有权限查看的项目的Wiki
        List<Long> accessibleProjectIds = getAccessibleProjectIds();
        if (accessibleProjectIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 通过wiki表关联项目
        pageQueryWrapper.exists(
                "SELECT 1 FROM wikis w WHERE w.id = wiki_pages.wiki_id AND w.project_id IN (" +
                        accessibleProjectIds.stream().map(String::valueOf).collect(Collectors.joining(",")) + ")"
        );

        // 指定项目范围
        if (projectId != null) {
            pageQueryWrapper.exists(
                    "SELECT 1 FROM wikis w WHERE w.id = wiki_pages.wiki_id AND w.project_id = " + projectId
            );
        }

        // 在Wiki页面标题中搜索
        pageQueryWrapper.like(WikiPage::getTitle, keyword);

        List<WikiPage> wikiPages = wikiPageMapper.selectList(pageQueryWrapper);
        
        List<SearchResultItemDTO> results = new ArrayList<>();
        
        // 为每个Wiki页面构建搜索结果
        for (WikiPage page : wikiPages) {
            // 获取最新的Wiki内容
            LambdaQueryWrapper<WikiContent> contentWrapper = new LambdaQueryWrapper<>();
            contentWrapper.eq(WikiContent::getPageId, page.getId())
                    .orderByDesc(WikiContent::getVersion)
                    .last("LIMIT 1");
            WikiContent content = wikiContentMapper.selectOne(contentWrapper);
            
            // 获取项目信息
            Project project = null;
            if (page.getWikiId() != null) {
                // 通过wiki_id查找项目
                LambdaQueryWrapper<Project> projectWrapper = new LambdaQueryWrapper<>();
                projectWrapper.exists(
                        "SELECT 1 FROM wikis w WHERE w.project_id = projects.id AND w.id = " + page.getWikiId()
                );
                project = projectMapper.selectOne(projectWrapper);
            }
            
            // 获取作者信息
            User author = null;
            if (content != null && content.getAuthorId() != null) {
                author = userMapper.selectById(content.getAuthorId());
            }
            
            SearchResultItemDTO result = SearchResultItemDTO.builder()
                    .type("wiki")
                    .id(page.getId())
                    .title(page.getTitle())
                    .description(content != null ? truncateDescription(content.getText()) : null)
                    .projectId(project != null ? project.getId() : null)
                    .projectName(project != null ? project.getName() : null)
                    .authorId(content != null ? content.getAuthorId() : null)
                    .authorName(author != null ? getUserDisplayName(author) : null)
                    .createdOn(page.getCreatedOn() != null ? 
                            new java.sql.Timestamp(page.getCreatedOn().getTime()).toLocalDateTime() : null)
                    .updatedOn(content != null && content.getUpdatedOn() != null ? 
                            new java.sql.Timestamp(content.getUpdatedOn().getTime()).toLocalDateTime() : null)
                    .url("/projects/" + (project != null ? project.getIdentifier() : "unknown") + "/wiki/" + page.getTitle())
                    .isPrivate(false) // Wiki通常是项目内公开的
                    .status("active")
                    .statusName("活动")
                    .build();
            
            results.add(result);
        }
        
        // 也在Wiki内容中搜索
        LambdaQueryWrapper<WikiContent> contentSearchWrapper = new LambdaQueryWrapper<>();
        contentSearchWrapper.like(WikiContent::getText, keyword);
        
        List<WikiContent> contents = wikiContentMapper.selectList(contentSearchWrapper);
        
        for (WikiContent content : contents) {
            // 检查是否已经通过标题搜索添加过了
            boolean alreadyAdded = results.stream()
                    .anyMatch(r -> r.getId().equals(content.getPageId()) && "wiki".equals(r.getType()));
            
            if (!alreadyAdded) {
                WikiPage page = wikiPageMapper.selectById(content.getPageId());
                if (page != null) {
                    // 检查项目权限
                    Project project = null;
                    if (page.getWikiId() != null) {
                        LambdaQueryWrapper<Project> projectWrapper = new LambdaQueryWrapper<>();
                        projectWrapper.exists(
                                "SELECT 1 FROM wikis w WHERE w.project_id = projects.id AND w.id = " + page.getWikiId()
                        );
                        project = projectMapper.selectOne(projectWrapper);
                        
                        // 权限检查
                        if (project == null || !accessibleProjectIds.contains(project.getId())) {
                            continue;
                        }
                        
                        // 项目范围检查
                        if (projectId != null && !projectId.equals(project.getId())) {
                            continue;
                        }
                    }
                    
                    User author = content.getAuthorId() != null ? userMapper.selectById(content.getAuthorId()) : null;
                    
                    SearchResultItemDTO result = SearchResultItemDTO.builder()
                            .type("wiki")
                            .id(page.getId())
                            .title(page.getTitle())
                            .description(truncateDescription(content.getText()))
                            .projectId(project != null ? project.getId() : null)
                            .projectName(project != null ? project.getName() : null)
                            .authorId(content.getAuthorId())
                            .authorName(author != null ? getUserDisplayName(author) : null)
                            .createdOn(page.getCreatedOn() != null ? 
                                    new java.sql.Timestamp(page.getCreatedOn().getTime()).toLocalDateTime() : null)
                            .updatedOn(content.getUpdatedOn() != null ? 
                                    new java.sql.Timestamp(content.getUpdatedOn().getTime()).toLocalDateTime() : null)
                            .url("/projects/" + (project != null ? project.getIdentifier() : "unknown") + "/wiki/" + page.getTitle())
                            .isPrivate(false)
                            .status("active")
                            .statusName("活动")
                            .build();
                    
                    results.add(result);
                }
            }
        }
        
        return results;
    }

    /**
     * 获取用户可访问的项目ID列表
     */
    private List<Long> getAccessibleProjectIds() {
        // 简化实现：直接查询用户可见的项目
        try {
            LambdaQueryWrapper<Project> queryWrapper = new LambdaQueryWrapper<>();
            
            // 添加基本的可见性过滤
            // 1. 公开项目
            queryWrapper.eq(Project::getIsPublic, true)
                    // 2. 或者活动状态的项目
                    .or(wrapper -> wrapper.eq(Project::getStatus, 1));
            
            List<Project> projects = projectMapper.selectList(queryWrapper);
            return projects.stream()
                    .map(Project::getId)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("获取用户可访问项目失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 截断描述内容
     */
    private String truncateDescription(String description) {
        if (!StringUtils.hasText(description)) {
            return null;
        }
        
        description = description.trim();
        if (description.length() <= 200) {
            return description;
        }
        
        return description.substring(0, 200) + "...";
    }

    /**
     * 获取用户显示名称
     */
    private String getUserDisplayName(User user) {
        if (user == null) {
            return null;
        }
        
        if (StringUtils.hasText(user.getFirstname()) || StringUtils.hasText(user.getLastname())) {
            return (StringUtils.hasText(user.getFirstname()) ? user.getFirstname() : "") +
                   " " +
                   (StringUtils.hasText(user.getLastname()) ? user.getLastname() : "");
        }
        
        return user.getLogin();
    }

    /**
     * 获取项目状态名称
     */
    private String getProjectStatusName(Integer status) {
        if (status == null) {
            return null;
        }
        
        switch (status) {
            case 1: return "活动";
            case 5: return "关闭";
            case 9: return "已归档";
            default: return "未知";
        }
    }

    /**
     * 记录搜索历史
     */
    private void recordSearchHistory(GlobalSearchRequestDTO requestDTO, Long totalCount) {
        try {
            Long currentUserId = securityUtils.getCurrentUserId();
            if (currentUserId != null && StringUtils.hasText(requestDTO.getKeyword())) {
                searchHistoryService.recordSearchHistory(
                    currentUserId,
                    requestDTO.getKeyword(),
                    requestDTO.getTypes(),
                    requestDTO.getProjectId(),
                    totalCount
                );
            }
        } catch (Exception e) {
            log.warn("记录搜索历史失败: {}", e.getMessage());
        }
    }

    /**
     * 获取搜索建议
     */
    public List<String> getSearchSuggestions(String prefix, Integer limit) {
        return searchHistoryService.getSearchSuggestions(prefix, limit);
    }

    /**
     * 获取用户搜索历史
     */
    public List<SearchHistory> getUserSearchHistories(Integer limit) {
        try {
            Long currentUserId = securityUtils.getCurrentUserId();
            if (currentUserId != null) {
                return searchHistoryService.getUserHistories(currentUserId, limit);
            }
        } catch (Exception e) {
            log.warn("获取用户搜索历史失败: {}", e.getMessage());
        }
        return new ArrayList<>();
    }

    /**
     * 获取热门搜索关键词
     */
    public List<String> getHotKeywords(Integer limit) {
        return searchHistoryService.getHotKeywords(limit);
    }

    /**
     * 构建空响应
     */
    private GlobalSearchResponseDTO buildEmptyResponse(String keyword) {
        Map<String, Long> typeCounts = new HashMap<>();
        typeCounts.put("issue", 0L);
        typeCounts.put("project", 0L);
        typeCounts.put("wiki", 0L);
        
        return GlobalSearchResponseDTO.builder()
                .keyword(keyword)
                .totalCount(0L)
                .typeCounts(typeCounts)
                .results(com.github.jredmine.dto.response.PageResponse.of(
                        new ArrayList<>(), 0L, 1L, 20L))
                .groupedResults(new HashMap<>())
                .build();
    }
}