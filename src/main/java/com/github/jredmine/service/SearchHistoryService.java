package com.github.jredmine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.jredmine.entity.SearchHistory;
import com.github.jredmine.mapper.search.SearchHistoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 搜索历史服务
 *
 * @author panfeng
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchHistoryService extends ServiceImpl<SearchHistoryMapper, SearchHistory> {

    private static final int MAX_USER_HISTORIES = 50;

    /**
     * 记录搜索历史
     */
    @Transactional
    public void recordSearchHistory(Long userId, String keyword, List<String> searchTypes, 
                                  Long projectId, Long resultCount) {
        if (userId == null || !StringUtils.hasText(keyword)) {
            return;
        }

        keyword = keyword.trim();
        String typesStr = searchTypes != null ? String.join(",", searchTypes) : null;

        // 查找是否已存在相同的搜索记录（用户+关键词+项目范围）
        LambdaQueryWrapper<SearchHistory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SearchHistory::getUserId, userId)
                   .eq(SearchHistory::getKeyword, keyword);
        
        if (projectId != null) {
            queryWrapper.eq(SearchHistory::getProjectId, projectId);
        } else {
            queryWrapper.isNull(SearchHistory::getProjectId);
        }

        SearchHistory existing = getOne(queryWrapper);
        
        if (existing != null) {
            // 更新已存在的记录
            existing.setSearchTypes(typesStr);
            existing.setResultCount(resultCount != null ? resultCount.intValue() : 0);
            existing.setUpdatedOn(LocalDateTime.now());
            updateById(existing);
            log.debug("更新搜索历史: userId={}, keyword={}, resultCount={}", 
                     userId, keyword, resultCount);
        } else {
            // 创建新记录
            SearchHistory history = new SearchHistory();
            history.setUserId(userId);
            history.setKeyword(keyword);
            history.setSearchTypes(typesStr);
            history.setProjectId(projectId);
            history.setResultCount(resultCount != null ? resultCount.intValue() : 0);
            history.setCreatedOn(LocalDateTime.now());
            history.setUpdatedOn(LocalDateTime.now());
            
            save(history);
            log.debug("新增搜索历史: userId={}, keyword={}, resultCount={}", 
                     userId, keyword, resultCount);
            
            // 清理旧记录，保持用户搜索历史不超过50条
            cleanupOldHistories(userId);
        }
    }

    /**
     * 获取用户搜索历史
     */
    public List<SearchHistory> getUserHistories(Long userId, Integer limit) {
        if (userId == null) {
            return new ArrayList<>();
        }
        
        if (limit == null || limit <= 0) {
            limit = 20;
        }
        
        LambdaQueryWrapper<SearchHistory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SearchHistory::getUserId, userId)
                   .orderByDesc(SearchHistory::getUpdatedOn)
                   .last("LIMIT " + limit);
        
        return list(queryWrapper);
    }

    /**
     * 获取热门搜索关键词
     */
    public List<String> getHotKeywords(Integer limit) {
        if (limit == null || limit <= 0) {
            limit = 10;
        }
        
        // 获取最近30天的搜索记录
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        
        LambdaQueryWrapper<SearchHistory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ge(SearchHistory::getCreatedOn, thirtyDaysAgo)
                   .groupBy(SearchHistory::getKeyword)
                   .orderByDesc(SearchHistory::getUpdatedOn)
                   .last("LIMIT " + limit);
        
        List<SearchHistory> histories = list(queryWrapper);
        
        // 提取关键词并按搜索频次排序
        return histories.stream()
                .collect(Collectors.groupingBy(SearchHistory::getKeyword, Collectors.counting()))
                .entrySet()
                .stream()
                .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
                .map(Map.Entry::getKey)
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * 获取搜索建议
     */
    public List<String> getSearchSuggestions(String prefix, Integer limit) {
        if (!StringUtils.hasText(prefix)) {
            return getHotKeywords(limit);
        }
        
        if (limit == null || limit <= 0) {
            limit = 10;
        }
        
        // 获取最近90天的搜索记录，按前缀匹配
        LocalDateTime ninetyDaysAgo = LocalDateTime.now().minusDays(90);
        
        LambdaQueryWrapper<SearchHistory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.likeRight(SearchHistory::getKeyword, prefix.trim())
                   .ge(SearchHistory::getCreatedOn, ninetyDaysAgo)
                   .groupBy(SearchHistory::getKeyword)
                   .orderByDesc(SearchHistory::getUpdatedOn)
                   .last("LIMIT " + limit * 2); // 多查一些用于去重和排序
        
        List<SearchHistory> histories = list(queryWrapper);
        
        // 按搜索频次和最近更新时间排序，去重
        return histories.stream()
                .collect(Collectors.groupingBy(
                    SearchHistory::getKeyword,
                    Collectors.maxBy(Comparator.comparing(SearchHistory::getUpdatedOn))
                ))
                .values()
                .stream()
                .filter(Optional::isPresent)
                .map(Optional::get)
                .sorted((h1, h2) -> h2.getUpdatedOn().compareTo(h1.getUpdatedOn()))
                .map(SearchHistory::getKeyword)
                .distinct()
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * 清理用户旧的搜索记录
     */
    private void cleanupOldHistories(Long userId) {
        try {
            // 先查询用户的搜索记录数量
            LambdaQueryWrapper<SearchHistory> countWrapper = new LambdaQueryWrapper<>();
            countWrapper.eq(SearchHistory::getUserId, userId);
            long totalCount = count(countWrapper);
            
            if (totalCount <= MAX_USER_HISTORIES) {
                return; // 不需要清理
            }
            
            // 获取需要保留的记录ID列表（最新的50条）
            LambdaQueryWrapper<SearchHistory> keepWrapper = new LambdaQueryWrapper<>();
            keepWrapper.eq(SearchHistory::getUserId, userId)
                      .orderByDesc(SearchHistory::getUpdatedOn)
                      .last("LIMIT " + MAX_USER_HISTORIES);
            
            List<SearchHistory> keepRecords = list(keepWrapper);
            List<Long> keepIds = keepRecords.stream()
                    .map(SearchHistory::getId)
                    .collect(Collectors.toList());
            
            if (!keepIds.isEmpty()) {
                // 删除不在保留列表中的记录
                LambdaQueryWrapper<SearchHistory> deleteWrapper = new LambdaQueryWrapper<>();
                deleteWrapper.eq(SearchHistory::getUserId, userId)
                           .notIn(SearchHistory::getId, keepIds);
                
                int deleted = baseMapper.delete(deleteWrapper);
                if (deleted > 0) {
                    log.debug("清理用户 {} 的 {} 条旧搜索记录", userId, deleted);
                }
            }
        } catch (Exception e) {
            log.warn("清理用户搜索历史失败: userId={}, error={}", userId, e.getMessage());
        }
    }
}