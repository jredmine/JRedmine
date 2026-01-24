package com.github.jredmine.controller;

import com.github.jredmine.dto.request.search.GlobalSearchRequestDTO;
import com.github.jredmine.dto.response.search.GlobalSearchResponseDTO;
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

    @Operation(summary = "搜索建议", description = "根据输入提供搜索建议")
    @GetMapping("/suggestions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<String>> getSearchSuggestions(
            @Parameter(description = "搜索关键词前缀")
            @RequestParam String prefix,
            
            @Parameter(description = "建议数量限制")
            @RequestParam(required = false, defaultValue = "10") Integer limit) {
        
        // TODO: 实现搜索建议逻辑
        // 可以从历史搜索记录、常用词汇等生成建议
        log.debug("搜索建议请求: prefix={}, limit={}", prefix, limit);
        
        return ResponseEntity.ok(Arrays.asList(
                prefix + "相关建议1",
                prefix + "相关建议2"
        ));
    }

    @Operation(summary = "获取搜索统计", description = "获取各类型内容的数量统计")
    @GetMapping("/stats")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Object> getSearchStats() {
        
        // TODO: 实现搜索统计逻辑
        log.debug("搜索统计请求");
        
        return ResponseEntity.ok("搜索统计功能待实现");
    }
}