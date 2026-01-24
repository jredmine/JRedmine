package com.github.jredmine.controller;

import com.github.jredmine.dto.request.search.GlobalSearchRequestDTO;
import com.github.jredmine.dto.response.search.GlobalSearchResponseDTO;
import com.github.jredmine.dto.response.search.SearchSuggestionResponseDTO;
import com.github.jredmine.dto.response.search.SearchHistoryResponseDTO;
import com.github.jredmine.entity.SearchHistory;
import com.github.jredmine.mapper.project.ProjectMapper;
import com.github.jredmine.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.Arrays;
import java.util.List;

/**
 * 搜索控制器
 *
 * @author panfeng
 */
@Slf4j
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Tag(name = "搜索管理", description = "全局搜索功能")
public class SearchController {

    private final SearchService searchService;
    private final ProjectMapper projectMapper;

    @Operation(summary = "全局搜索", description = "在任务、项目、Wiki等内容中进行全局搜索")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GlobalSearchResponseDTO> globalSearch(
            @Parameter(description = "搜索关键词", required = true)
            @RequestParam String keyword,
            
            @Parameter(description = "搜索类型，多个类型用逗号分隔（如：issue,project,wiki）")
            @RequestParam(required = false) String types,
            
            @Parameter(description = "项目ID，指定项目范围搜索")
            @RequestParam(required = false) Long projectId,
            
            @Parameter(description = "是否包含私有内容")
            @RequestParam(required = false, defaultValue = "false") Boolean includePrivate,
            
            @Parameter(description = "页码，从1开始")
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            
            @Parameter(description = "每页数量")
            @RequestParam(required = false, defaultValue = "20") Integer pageSize) {

        // 构建请求DTO
        GlobalSearchRequestDTO requestDTO = new GlobalSearchRequestDTO();
        requestDTO.setKeyword(keyword);
        requestDTO.setProjectId(projectId);
        requestDTO.setIncludePrivate(includePrivate);
        requestDTO.setCurrent(pageNum);
        requestDTO.setSize(pageSize);
        
        // 解析搜索类型
        if (types != null && !types.trim().isEmpty()) {
            List<String> typeList = Arrays.asList(types.trim().split(","));
            requestDTO.setTypes(typeList);
        }

        log.debug("全局搜索请求: keyword={}, types={}, projectId={}, pageNum={}, pageSize={}", 
                keyword, types, projectId, pageNum, pageSize);

        GlobalSearchResponseDTO response = searchService.globalSearch(requestDTO);
        
        log.debug("全局搜索完成: totalCount={}, typeCounts={}", 
                response.getTotalCount(), response.getTypeCounts());

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "全局搜索（POST方式）", description = "使用POST方式进行全局搜索，支持更复杂的搜索条件")
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GlobalSearchResponseDTO> globalSearchPost(
            @Valid @RequestBody GlobalSearchRequestDTO requestDTO) {
        
        log.debug("全局搜索请求(POST): {}", requestDTO);

        GlobalSearchResponseDTO response = searchService.globalSearch(requestDTO);
        
        log.debug("全局搜索完成: totalCount={}, typeCounts={}", 
                response.getTotalCount(), response.getTypeCounts());

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "搜索建议", description = "根据输入提供搜索建议，包含历史记录和热门搜索")
    @GetMapping("/suggestions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SearchSuggestionResponseDTO> getSearchSuggestions(
            @Parameter(description = "搜索关键词前缀（可选）")
            @RequestParam(required = false) String prefix,
            
            @Parameter(description = "建议数量限制")
            @RequestParam(required = false, defaultValue = "10") Integer limit) {
        
        log.debug("搜索建议请求: prefix={}, limit={}", prefix, limit);

        // 获取搜索建议
        List<String> suggestions = searchService.getSearchSuggestions(prefix, limit);
        
        // 获取热门搜索关键词
        List<String> hotKeywords = searchService.getHotKeywords(Math.min(limit, 5));
        
        // 获取用户搜索历史关键词
        List<String> historyKeywords = searchService.getUserSearchHistories(Math.min(limit, 10))
                .stream()
                .map(SearchHistory::getKeyword)
                .distinct()
                .collect(java.util.stream.Collectors.toList());

        SearchSuggestionResponseDTO response = SearchSuggestionResponseDTO.builder()
                .suggestions(suggestions)
                .hotKeywords(hotKeywords)
                .historyKeywords(historyKeywords)
                .build();

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "获取用户搜索历史", description = "获取当前用户的搜索历史记录")
    @GetMapping("/history")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SearchHistoryResponseDTO>> getUserSearchHistory(
            @Parameter(description = "历史记录数量限制")
            @RequestParam(required = false, defaultValue = "20") Integer limit) {
        
        log.debug("获取用户搜索历史: limit={}", limit);

        List<SearchHistory> histories = searchService.getUserSearchHistories(limit);
        
        List<SearchHistoryResponseDTO> response = histories.stream().map(history -> {
            String projectName = null;
            if (history.getProjectId() != null) {
                try {
                    var project = projectMapper.selectById(history.getProjectId());
                    projectName = project != null ? project.getName() : null;
                } catch (Exception e) {
                    log.warn("获取项目名称失败: projectId={}", history.getProjectId());
                }
            }
            
            return SearchHistoryResponseDTO.builder()
                    .id(history.getId())
                    .keyword(history.getKeyword())
                    .searchTypes(history.getSearchTypes())
                    .projectId(history.getProjectId())
                    .projectName(projectName)
                    .resultCount(history.getResultCount())
                    .updatedOn(history.getUpdatedOn())
                    .build();
        }).collect(java.util.stream.Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "获取热门搜索关键词", description = "获取全站热门搜索关键词")
    @GetMapping("/hot-keywords")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<String>> getHotKeywords(
            @Parameter(description = "关键词数量限制")
            @RequestParam(required = false, defaultValue = "10") Integer limit) {
        
        log.debug("获取热门搜索关键词: limit={}", limit);

        List<String> hotKeywords = searchService.getHotKeywords(limit);
        
        return ResponseEntity.ok(hotKeywords);
    }
}