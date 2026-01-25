package com.github.jredmine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.jredmine.dto.request.activity.ActivityQueryRequestDTO;
import com.github.jredmine.dto.request.activity.CommentCreateRequestDTO;
import com.github.jredmine.dto.request.activity.CommentUpdateRequestDTO;
import com.github.jredmine.dto.response.PageResponse;
import com.github.jredmine.dto.response.activity.ActivityItemResponseDTO;
import com.github.jredmine.dto.response.activity.ActivityStatsResponseDTO;
import com.github.jredmine.dto.response.activity.CommentResponseDTO;
import com.github.jredmine.dto.response.activity.FieldChangeDTO;
import com.github.jredmine.exception.BusinessException;
import com.github.jredmine.util.SecurityUtils;
import com.github.jredmine.entity.*;
import com.github.jredmine.mapper.issue.JournalMapper;
import com.github.jredmine.mapper.issue.JournalDetailMapper;
import com.github.jredmine.mapper.issue.IssueMapper;
import com.github.jredmine.mapper.project.ProjectMapper;
import com.github.jredmine.mapper.user.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 活动流服务
 *
 * @author panfeng
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityService {

    private final JournalMapper journalMapper;
    private final JournalDetailMapper journalDetailMapper;
    private final IssueMapper issueMapper;
    private final ProjectMapper projectMapper;
    private final UserMapper userMapper;
    private final SecurityUtils securityUtils;

    /**
     * 查询活动流
     */
    public PageResponse<ActivityItemResponseDTO> getActivities(ActivityQueryRequestDTO requestDTO) {
        // 构建查询条件（不包含活动类型，因为需要动态判断）
        LambdaQueryWrapper<Journal> queryWrapper = buildQueryWrapper(requestDTO);
        
        // 如果有活动类型筛选，需要查询更多数据后再筛选，所以先不分页
        if (requestDTO.getActivityTypes() != null && !requestDTO.getActivityTypes().isEmpty()) {
            return getActivitiesWithTypeFilter(requestDTO, queryWrapper);
        }
        
        // 没有活动类型筛选，直接分页查询
        Page<Journal> page = new Page<>(requestDTO.getCurrent(), requestDTO.getSize());
        Page<Journal> result = journalMapper.selectPage(page, queryWrapper);
        
        // 转换为响应DTO
        List<ActivityItemResponseDTO> activities = convertToActivityItems(result.getRecords());
        
        return PageResponse.of(activities, result.getTotal(), result.getCurrent(), result.getSize());
    }

    /**
     * 带活动类型筛选的查询（需要先查询后筛选）
     */
    private PageResponse<ActivityItemResponseDTO> getActivitiesWithTypeFilter(
            ActivityQueryRequestDTO requestDTO, LambdaQueryWrapper<Journal> queryWrapper) {
        
        // 查询所有符合其他条件的记录
        List<Journal> allJournals = journalMapper.selectList(queryWrapper);
        
        // 转换为活动项并筛选活动类型
        List<ActivityItemResponseDTO> allActivities = convertToActivityItems(allJournals);
        List<ActivityItemResponseDTO> filteredActivities = allActivities.stream()
                .filter(activity -> requestDTO.getActivityTypes().contains(activity.getActivityType()))
                .collect(Collectors.toList());
        
        // 手动分页
        long total = filteredActivities.size();
        int current = requestDTO.getCurrent();
        int size = requestDTO.getSize();
        int startIndex = (current - 1) * size;
        int endIndex = Math.min(startIndex + size, filteredActivities.size());
        
        List<ActivityItemResponseDTO> pagedActivities = new ArrayList<>();
        if (startIndex < filteredActivities.size()) {
            pagedActivities = filteredActivities.subList(startIndex, endIndex);
        }
        
        return PageResponse.of(pagedActivities, total, (long) current, (long) size);
    }

    /**
     * 获取项目活动流
     */
    public PageResponse<ActivityItemResponseDTO> getProjectActivities(Long projectId, ActivityQueryRequestDTO requestDTO) {
        requestDTO.setProjectId(projectId);
        return getActivities(requestDTO);
    }

    /**
     * 获取用户活动流
     */
    public PageResponse<ActivityItemResponseDTO> getUserActivities(Long userId, ActivityQueryRequestDTO requestDTO) {
        requestDTO.setUserId(userId);
        return getActivities(requestDTO);
    }

    /**
     * 获取对象活动流
     */
    public PageResponse<ActivityItemResponseDTO> getObjectActivities(String objectType, Long objectId, ActivityQueryRequestDTO requestDTO) {
        requestDTO.setObjectType(objectType);
        requestDTO.setObjectId(objectId);
        return getActivities(requestDTO);
    }

    /**
     * 获取单个活动详情
     */
    public ActivityItemResponseDTO getActivityById(Long id) {
        Journal journal = journalMapper.selectById(id);
        if (journal == null) {
            return null;
        }
        
        List<ActivityItemResponseDTO> activities = convertToActivityItems(Arrays.asList(journal));
        return activities.isEmpty() ? null : activities.get(0);
    }

    /**
     * 获取活动统计
     */
    public ActivityStatsResponseDTO getActivityStats(ActivityQueryRequestDTO requestDTO) {
        LambdaQueryWrapper<Journal> queryWrapper = buildQueryWrapper(requestDTO);
        
        List<Journal> journals = journalMapper.selectList(queryWrapper);
        
        // 统计各种维度的数据
        Map<String, Long> typeCount = journals.stream()
                .collect(Collectors.groupingBy(this::determineActivityType, Collectors.counting()));
        
        Map<String, Long> userCount = journals.stream()
                .collect(Collectors.groupingBy(j -> getUserDisplayName(j.getUserId()), Collectors.counting()));
        
        Map<String, Long> projectCount = journals.stream()
                .filter(j -> getProjectIdByJournal(j) != null)
                .collect(Collectors.groupingBy(j -> getProjectName(getProjectIdByJournal(j)), Collectors.counting()));
        
        // 最近7天每日统计
        Map<String, Long> dailyCount = journals.stream()
                .filter(j -> j.getCreatedOn() != null && j.getCreatedOn().isAfter(LocalDateTime.now().minusDays(7)))
                .collect(Collectors.groupingBy(
                    j -> j.getCreatedOn().toLocalDate().toString(),
                    Collectors.counting()
                ));
        
        return ActivityStatsResponseDTO.builder()
                .totalCount((long) journals.size())
                .typeCount(typeCount)
                .userCount(userCount)
                .projectCount(projectCount)
                .dailyCount(dailyCount)
                .build();
    }

    /**
     * 构建查询条件
     */
    private LambdaQueryWrapper<Journal> buildQueryWrapper(ActivityQueryRequestDTO requestDTO) {
        LambdaQueryWrapper<Journal> queryWrapper = new LambdaQueryWrapper<>();
        
        // 时间范围
        if (requestDTO.getStartDate() != null) {
            queryWrapper.ge(Journal::getCreatedOn, requestDTO.getStartDate());
        }
        if (requestDTO.getEndDate() != null) {
            queryWrapper.le(Journal::getCreatedOn, requestDTO.getEndDate());
        }
        
        // 用户筛选
        if (requestDTO.getUserId() != null) {
            queryWrapper.eq(Journal::getUserId, requestDTO.getUserId());
        }
        
        // 对象类型和ID筛选
        if (StringUtils.hasText(requestDTO.getObjectType())) {
            queryWrapper.eq(Journal::getJournalizedType, requestDTO.getObjectType());
        }
        if (requestDTO.getObjectId() != null) {
            queryWrapper.eq(Journal::getJournalizedId, requestDTO.getObjectId());
        }
        
        // 项目筛选（通过关联对象）
        if (requestDTO.getProjectId() != null) {
            queryWrapper.and(wrapper -> {
                // Project类型：直接筛选项目ID
                wrapper.and(w -> w.eq(Journal::getJournalizedType, "Project")
                                 .eq(Journal::getJournalizedId, requestDTO.getProjectId()))
                       // 或Issue类型：通过issues表关联项目ID  
                       .or(w -> w.eq(Journal::getJournalizedType, "Issue")
                                 .exists("SELECT 1 FROM issues i WHERE i.id = journals.journalized_id AND i.project_id = " + requestDTO.getProjectId()));
                       // TODO: 添加其他类型的项目关联逻辑
            });
        }
        
        // 关键词搜索
        if (StringUtils.hasText(requestDTO.getKeyword())) {
            queryWrapper.like(Journal::getNotes, requestDTO.getKeyword().trim());
        }
        
        // 私有备注处理
        if (!Boolean.TRUE.equals(requestDTO.getIncludePrivate())) {
            queryWrapper.and(wrapper -> {
                wrapper.eq(Journal::getPrivateNotes, false)
                       .or()
                       .isNull(Journal::getPrivateNotes);
            });
        }
        
        // 按创建时间倒序
        queryWrapper.orderByDesc(Journal::getCreatedOn);
        
        return queryWrapper;
    }

    /**
     * 转换为活动项DTO
     */
    private List<ActivityItemResponseDTO> convertToActivityItems(List<Journal> journals) {
        return journals.stream().map(journal -> {
            // 获取用户信息
            User user = getUserById(journal.getUserId());
            User updatedByUser = journal.getUpdatedById() != null ? getUserById(journal.getUpdatedById()) : null;
            
            // 获取关联对象信息
            ObjectInfo objectInfo = getObjectInfo(journal.getJournalizedType(), journal.getJournalizedId());
            
            // 获取字段变更详情
            List<FieldChangeDTO> changes = getFieldChanges(journal.getId());
            
            // 确定活动类型
            String activityType = determineActivityType(journal);
            
            return ActivityItemResponseDTO.builder()
                    .id(journal.getId().longValue())
                    .activityType(activityType)
                    .userId(journal.getUserId().longValue())
                    .userName(user != null ? getUserDisplayName(user) : null)
                    .userLogin(user != null ? user.getLogin() : null)
                    .objectType(journal.getJournalizedType())
                    .objectId(journal.getJournalizedId().longValue())
                    .objectTitle(objectInfo.title)
                    .objectUrl(objectInfo.url)
                    .projectId(objectInfo.projectId)
                    .projectName(objectInfo.projectName)
                    .notes(journal.getNotes())
                    .isPrivate(journal.getPrivateNotes() != null && journal.getPrivateNotes())
                    .changes(changes)
                    .createdOn(journal.getCreatedOn())
                    .updatedOn(journal.getUpdatedOn())
                    .updatedById(updatedByUser != null ? updatedByUser.getId().longValue() : null)
                    .updatedByName(updatedByUser != null ? getUserDisplayName(updatedByUser) : null)
                    .build();
        }).collect(Collectors.toList());
    }

    /**
     * 获取字段变更详情
     */
    private List<FieldChangeDTO> getFieldChanges(Integer journalId) {
        LambdaQueryWrapper<JournalDetail> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(JournalDetail::getJournalId, journalId);
        
        List<JournalDetail> details = journalDetailMapper.selectList(queryWrapper);
        
        return details.stream().map(detail -> {
            String fieldLabel = getFieldLabel(detail.getPropKey());
            
            return FieldChangeDTO.builder()
                    .property(detail.getProperty())
                    .fieldName(detail.getPropKey())
                    .fieldLabel(fieldLabel)
                    .oldValue(detail.getOldValue())
                    .newValue(detail.getValue())
                    .oldValueDisplay(getValueDisplay(detail.getPropKey(), detail.getOldValue()))
                    .newValueDisplay(getValueDisplay(detail.getPropKey(), detail.getValue()))
                    .build();
        }).collect(Collectors.toList());
    }

    /**
     * 确定活动类型
     */
    private String determineActivityType(Journal journal) {
        if (StringUtils.hasText(journal.getNotes())) {
            return "comment";
        }
        
        // 检查是否有字段变更
        LambdaQueryWrapper<JournalDetail> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(JournalDetail::getJournalId, journal.getId());
        long changeCount = journalDetailMapper.selectCount(queryWrapper);
        
        if (changeCount > 0) {
            return "field_change";
        }
        
        return "update";
    }

    /**
     * 获取关联对象信息
     */
    private ObjectInfo getObjectInfo(String objectType, Integer objectId) {
        ObjectInfo info = new ObjectInfo();
        
        if ("Issue".equals(objectType)) {
            Issue issue = issueMapper.selectById(objectId);
            if (issue != null) {
                info.title = issue.getSubject();
                info.url = "/issues/" + objectId;
                info.projectId = issue.getProjectId().longValue();
                info.projectName = getProjectName(issue.getProjectId().longValue());
            }
        } else if ("Project".equals(objectType)) {
            Project project = projectMapper.selectById(objectId);
            if (project != null) {
                info.title = project.getName();
                info.url = "/projects/" + project.getIdentifier();
                info.projectId = project.getId().longValue();
                info.projectName = project.getName();
            }
        }
        // TODO: 添加其他对象类型的处理
        
        return info;
    }

    /**
     * 对象信息内部类
     */
    private static class ObjectInfo {
        String title;
        String url;
        Long projectId;
        String projectName;
    }

    /**
     * 获取用户信息
     */
    private User getUserById(Integer userId) {
        if (userId == null) return null;
        return userMapper.selectById(userId);
    }

    /**
     * 获取用户显示名
     */
    private String getUserDisplayName(User user) {
        if (user == null) return null;
        
        if (StringUtils.hasText(user.getFirstname()) || StringUtils.hasText(user.getLastname())) {
            return (StringUtils.hasText(user.getFirstname()) ? user.getFirstname() : "") +
                   " " +
                   (StringUtils.hasText(user.getLastname()) ? user.getLastname() : "");
        }
        
        return user.getLogin();
    }

    /**
     * 获取用户显示名（通过ID）
     */
    private String getUserDisplayName(Integer userId) {
        User user = getUserById(userId);
        return getUserDisplayName(user);
    }

    /**
     * 获取项目名称
     */
    private String getProjectName(Long projectId) {
        if (projectId == null) return null;
        Project project = projectMapper.selectById(projectId);
        return project != null ? project.getName() : null;
    }

    /**
     * 通过Journal获取项目ID
     */
    private Long getProjectIdByJournal(Journal journal) {
        if ("Issue".equals(journal.getJournalizedType())) {
            Issue issue = issueMapper.selectById(journal.getJournalizedId());
            return issue != null ? issue.getProjectId().longValue() : null;
        } else if ("Project".equals(journal.getJournalizedType())) {
            return journal.getJournalizedId().longValue();
        }
        return null;
    }

    /**
     * 获取字段标签
     */
    private String getFieldLabel(String fieldName) {
        // TODO: 实现字段名到显示名的映射
        switch (fieldName) {
            case "status_id": return "状态";
            case "assigned_to_id": return "分配给";
            case "priority_id": return "优先级";
            case "subject": return "标题";
            case "description": return "描述";
            case "start_date": return "开始日期";
            case "due_date": return "截止日期";
            case "done_ratio": return "完成度";
            case "estimated_hours": return "预估工时";
            default: return fieldName;
        }
    }

    /**
     * 获取值显示名
     */
    private String getValueDisplay(String fieldName, String value) {
        // TODO: 实现值到显示名的转换（如状态ID转状态名）
        return value;
    }

    /**
     * 添加评论
     */
    public CommentResponseDTO createComment(CommentCreateRequestDTO requestDTO) {
        // 获取当前用户
        Long currentUserId = securityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException("用户未登录");
        }

        // 验证对象是否存在
        validateObject(requestDTO.getObjectType(), requestDTO.getObjectId());

        // 创建Journal记录
        Journal journal = new Journal();
        journal.setJournalizedType(requestDTO.getObjectType());
        journal.setJournalizedId(requestDTO.getObjectId().intValue());
        journal.setUserId(currentUserId.intValue());
        journal.setNotes(requestDTO.getNotes().trim());
        journal.setCreatedOn(LocalDateTime.now());
        journal.setUpdatedOn(LocalDateTime.now());
        journal.setPrivateNotes(requestDTO.getIsPrivate() != null && requestDTO.getIsPrivate());

        // 保存到数据库
        int result = journalMapper.insert(journal);
        if (result <= 0) {
            throw new BusinessException("评论保存失败");
        }

        log.info("用户 {} 为 {} {} 添加了评论", currentUserId, requestDTO.getObjectType(), requestDTO.getObjectId());

        // 返回评论信息
        return convertToCommentResponse(journal);
    }

    /**
     * 更新评论
     */
    public CommentResponseDTO updateComment(Long commentId, CommentUpdateRequestDTO requestDTO) {
        // 获取当前用户
        Long currentUserId = securityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException("用户未登录");
        }

        // 查询评论记录
        Journal journal = journalMapper.selectById(commentId);
        if (journal == null) {
            throw new BusinessException("评论不存在");
        }

        // 权限检查：只有评论作者或管理员可以编辑
        if (!currentUserId.equals(journal.getUserId().longValue()) && !isAdmin()) {
            throw new BusinessException("无权限编辑此评论");
        }

        // 更新评论内容
        journal.setNotes(requestDTO.getNotes().trim());
        journal.setUpdatedOn(LocalDateTime.now());
        journal.setUpdatedById(currentUserId.intValue());
        
        if (requestDTO.getIsPrivate() != null) {
            journal.setPrivateNotes(requestDTO.getIsPrivate());
        }

        // 保存更新
        int result = journalMapper.updateById(journal);
        if (result <= 0) {
            throw new BusinessException("评论更新失败");
        }

        log.info("用户 {} 更新了评论 {}", currentUserId, commentId);

        // 返回更新后的评论信息
        return convertToCommentResponse(journal);
    }

    /**
     * 删除评论
     */
    public void deleteComment(Long commentId) {
        // 获取当前用户
        Long currentUserId = securityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException("用户未登录");
        }

        // 查询评论记录
        Journal journal = journalMapper.selectById(commentId);
        if (journal == null) {
            throw new BusinessException("评论不存在");
        }

        // 权限检查：只有评论作者或管理员可以删除
        if (!currentUserId.equals(journal.getUserId().longValue()) && !isAdmin()) {
            throw new BusinessException("无权限删除此评论");
        }

        // 检查是否有字段变更记录
        LambdaQueryWrapper<JournalDetail> detailWrapper = new LambdaQueryWrapper<>();
        detailWrapper.eq(JournalDetail::getJournalId, commentId);
        long detailCount = journalDetailMapper.selectCount(detailWrapper);
        
        if (detailCount > 0) {
            throw new BusinessException("包含字段变更记录的活动不能删除，只能编辑备注");
        }

        // 删除评论
        int result = journalMapper.deleteById(commentId);
        if (result <= 0) {
            throw new BusinessException("评论删除失败");
        }

        log.info("用户 {} 删除了评论 {}", currentUserId, commentId);
    }

    /**
     * 验证关联对象是否存在
     */
    private void validateObject(String objectType, Long objectId) {
        switch (objectType) {
            case "Issue":
                Issue issue = issueMapper.selectById(objectId);
                if (issue == null) {
                    throw new BusinessException("任务不存在");
                }
                break;
            case "Project":
                Project project = projectMapper.selectById(objectId);
                if (project == null) {
                    throw new BusinessException("项目不存在");
                }
                break;
            // TODO: 添加其他对象类型的验证
            default:
                throw new BusinessException("不支持的对象类型: " + objectType);
        }
    }

    /**
     * 检查当前用户是否为管理员
     */
    private boolean isAdmin() {
        try {
            User currentUser = securityUtils.getCurrentUser();
            return currentUser != null && Boolean.TRUE.equals(currentUser.getAdmin());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 转换为评论响应DTO
     */
    private CommentResponseDTO convertToCommentResponse(Journal journal) {
        User user = getUserById(journal.getUserId());
        User updatedByUser = journal.getUpdatedById() != null ? getUserById(journal.getUpdatedById()) : null;

        return CommentResponseDTO.builder()
                .id(journal.getId().longValue())
                .objectType(journal.getJournalizedType())
                .objectId(journal.getJournalizedId().longValue())
                .notes(journal.getNotes())
                .isPrivate(journal.getPrivateNotes() != null && journal.getPrivateNotes())
                .userId(journal.getUserId().longValue())
                .userName(user != null ? getUserDisplayName(user) : null)
                .userLogin(user != null ? user.getLogin() : null)
                .createdOn(journal.getCreatedOn())
                .updatedOn(journal.getUpdatedOn())
                .updatedById(updatedByUser != null ? updatedByUser.getId().longValue() : null)
                .updatedByName(updatedByUser != null ? getUserDisplayName(updatedByUser) : null)
                .build();
    }
}