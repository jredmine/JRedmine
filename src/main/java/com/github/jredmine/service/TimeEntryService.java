package com.github.jredmine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.jredmine.dto.request.timeentry.TimeEntryBatchDeleteRequestDTO;
import com.github.jredmine.dto.request.timeentry.TimeEntryBatchUpdateRequestDTO;
import com.github.jredmine.dto.request.timeentry.TimeEntryCreateRequestDTO;
import com.github.jredmine.dto.request.timeentry.TimeEntryQueryRequestDTO;
import com.github.jredmine.dto.request.timeentry.TimeEntryReportRequestDTO;
import com.github.jredmine.dto.request.timeentry.TimeEntryStatisticsRequestDTO;
import com.github.jredmine.dto.request.timeentry.TimeEntryUpdateRequestDTO;
import com.github.jredmine.dto.response.PageResponse;
import com.github.jredmine.dto.response.project.ProjectSimpleResponseDTO;
import com.github.jredmine.dto.response.timeentry.*;
import com.github.jredmine.dto.response.user.UserSimpleResponseDTO;
import com.github.jredmine.dto.response.timeentry.TimeEntryBatchUpdateResponseDTO;
import com.github.jredmine.entity.*;
import com.github.jredmine.exception.BusinessException;
import com.github.jredmine.mapper.TimeEntryMapper;
import com.github.jredmine.mapper.project.ProjectMapper;
import com.github.jredmine.mapper.issue.IssueMapper;
import com.github.jredmine.mapper.user.UserMapper;
import com.github.jredmine.mapper.workflow.EnumerationMapper;
import com.github.jredmine.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 工时记录服务
 *
 * @author panfeng
 * @since 2026-01-12
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TimeEntryService {
    
    private final TimeEntryMapper timeEntryMapper;
    private final ProjectMapper projectMapper;
    private final IssueMapper issueMapper;
    private final UserMapper userMapper;
    private final EnumerationMapper enumerationMapper;
    private final SecurityUtils securityUtils;
    
    /**
     * 创建工时记录
     */
    @Transactional(rollbackFor = Exception.class)
    public TimeEntryResponseDTO createTimeEntry(TimeEntryCreateRequestDTO request) {
        Long currentUserId = securityUtils.getCurrentUserId();
        MDC.put("userId", String.valueOf(currentUserId));
        
        // 1. 验证项目是否存在
        Project project = projectMapper.selectById(request.getProjectId());
        if (project == null) {
            throw new BusinessException("项目不存在");
        }
        
        // 2. 如果指定了任务，验证任务是否存在且属于该项目
        if (request.getIssueId() != null) {
            Issue issue = issueMapper.selectById(request.getIssueId());
            if (issue == null) {
                throw new BusinessException("任务不存在");
            }
            if (!issue.getProjectId().equals(request.getProjectId())) {
                throw new BusinessException("任务不属于指定的项目");
            }
        }
        
        // 3. 验证活动类型是否存在
        Enumeration activity = enumerationMapper.selectById(request.getActivityId());
        if (activity == null || !"TimeEntryActivity".equals(activity.getType())) {
            throw new BusinessException("活动类型不存在或类型不正确");
        }
        
        // 4. 确定工作人员ID（如果未指定，则为当前用户）
        Long userId = request.getUserId() != null ? request.getUserId() : currentUserId;
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("工作人员不存在");
        }
        
        // 5. 构建工时记录实体
        TimeEntry timeEntry = new TimeEntry();
        timeEntry.setProjectId(request.getProjectId());
        timeEntry.setIssueId(request.getIssueId());
        timeEntry.setUserId(userId);
        timeEntry.setAuthorId(currentUserId);
        timeEntry.setHours(request.getHours());
        timeEntry.setSpentOn(request.getSpentOn());
        timeEntry.setActivityId(request.getActivityId());
        timeEntry.setComments(request.getComments());
        
        // 6. 计算年月周（用于统计查询）
        LocalDate spentOn = request.getSpentOn();
        timeEntry.setTyear(spentOn.getYear());
        timeEntry.setTmonth(spentOn.getMonthValue());
        WeekFields weekFields = WeekFields.of(Locale.getDefault());
        timeEntry.setTweek(spentOn.get(weekFields.weekOfWeekBasedYear()));
        
        // 7. 设置时间戳
        LocalDateTime now = LocalDateTime.now();
        timeEntry.setCreatedOn(now);
        timeEntry.setUpdatedOn(now);
        
        // 8. 保存到数据库
        timeEntryMapper.insert(timeEntry);
        
        log.info("创建工时记录成功: id={}, projectId={}, userId={}, hours={}", 
                timeEntry.getId(), timeEntry.getProjectId(), timeEntry.getUserId(), timeEntry.getHours());
        
        // 9. 返回详细信息
        return getTimeEntryById(timeEntry.getId());
    }
    
    /**
     * 根据ID获取工时记录详情
     */
    public TimeEntryResponseDTO getTimeEntryById(Long id) {
        TimeEntry timeEntry = timeEntryMapper.selectById(id);
        if (timeEntry == null) {
            throw new BusinessException("工时记录不存在");
        }
        
        return convertToResponseDTO(timeEntry);
    }
    
    /**
     * 更新工时记录
     */
    @Transactional(rollbackFor = Exception.class)
    public TimeEntryResponseDTO updateTimeEntry(Long id, TimeEntryUpdateRequestDTO request) {
        Long currentUserId = securityUtils.getCurrentUserId();
        boolean isAdmin = securityUtils.isAdmin();
        MDC.put("userId", String.valueOf(currentUserId));
        
        // 1. 验证工时记录是否存在
        TimeEntry timeEntry = timeEntryMapper.selectById(id);
        if (timeEntry == null) {
            throw new BusinessException("工时记录不存在");
        }
        
        // 2. 权限检查：只能更新自己创建的记录，或者是管理员
        if (!isAdmin && !timeEntry.getAuthorId().equals(currentUserId)) {
            throw new BusinessException("无权限修改此工时记录");
        }
        
        // 3. 如果更新了项目ID，验证项目是否存在
        if (request.getProjectId() != null) {
            Project project = projectMapper.selectById(request.getProjectId());
            if (project == null) {
                throw new BusinessException("项目不存在");
            }
            timeEntry.setProjectId(request.getProjectId());
        }
        
        // 4. 如果更新了任务ID，验证任务是否存在且属于该项目
        if (request.getIssueId() != null) {
            Issue issue = issueMapper.selectById(request.getIssueId());
            if (issue == null) {
                throw new BusinessException("任务不存在");
            }
            if (!issue.getProjectId().equals(timeEntry.getProjectId())) {
                throw new BusinessException("任务不属于指定的项目");
            }
            timeEntry.setIssueId(request.getIssueId());
        }
        
        // 5. 如果更新了工作人员ID，验证用户是否存在
        if (request.getUserId() != null) {
            User user = userMapper.selectById(request.getUserId());
            if (user == null) {
                throw new BusinessException("工作人员不存在");
            }
            timeEntry.setUserId(request.getUserId());
        }
        
        // 6. 如果更新了活动类型，验证活动类型是否存在
        if (request.getActivityId() != null) {
            Enumeration activity = enumerationMapper.selectById(request.getActivityId());
            if (activity == null || !"TimeEntryActivity".equals(activity.getType())) {
                throw new BusinessException("活动类型不存在或类型不正确");
            }
            timeEntry.setActivityId(request.getActivityId());
        }
        
        // 7. 更新工时
        if (request.getHours() != null) {
            timeEntry.setHours(request.getHours());
        }
        
        // 8. 更新工作日期，需要重新计算年月周
        if (request.getSpentOn() != null) {
            LocalDate spentOn = request.getSpentOn();
            timeEntry.setSpentOn(spentOn);
            timeEntry.setTyear(spentOn.getYear());
            timeEntry.setTmonth(spentOn.getMonthValue());
            WeekFields weekFields = WeekFields.of(Locale.getDefault());
            timeEntry.setTweek(spentOn.get(weekFields.weekOfWeekBasedYear()));
        }
        
        // 9. 更新备注
        if (request.getComments() != null) {
            timeEntry.setComments(request.getComments());
        }
        
        // 10. 更新时间戳
        timeEntry.setUpdatedOn(LocalDateTime.now());
        
        // 11. 保存到数据库
        timeEntryMapper.updateById(timeEntry);
        
        log.info("更新工时记录成功: id={}, projectId={}, userId={}, hours={}", 
                timeEntry.getId(), timeEntry.getProjectId(), timeEntry.getUserId(), timeEntry.getHours());
        
        // 12. 返回详细信息
        return getTimeEntryById(id);
    }
    
    /**
     * 查询工时记录列表
     */
    public PageResponse<TimeEntryResponseDTO> queryTimeEntries(TimeEntryQueryRequestDTO request) {
        // 1. 构建查询条件
        LambdaQueryWrapper<TimeEntry> queryWrapper = new LambdaQueryWrapper<>();
        
        // 项目ID筛选
        if (request.getProjectId() != null) {
            queryWrapper.eq(TimeEntry::getProjectId, request.getProjectId());
        }
        
        // 任务ID筛选
        if (request.getIssueId() != null) {
            queryWrapper.eq(TimeEntry::getIssueId, request.getIssueId());
        }
        
        // 工作人员ID筛选
        if (request.getUserId() != null) {
            queryWrapper.eq(TimeEntry::getUserId, request.getUserId());
        }
        
        // 活动类型ID筛选
        if (request.getActivityId() != null) {
            queryWrapper.eq(TimeEntry::getActivityId, request.getActivityId());
        }
        
        // 日期范围筛选
        if (request.getStartDate() != null) {
            queryWrapper.ge(TimeEntry::getSpentOn, request.getStartDate());
        }
        if (request.getEndDate() != null) {
            queryWrapper.le(TimeEntry::getSpentOn, request.getEndDate());
        }
        
        // 年份筛选
        if (request.getYear() != null) {
            queryWrapper.eq(TimeEntry::getTyear, request.getYear());
        }
        
        // 月份筛选
        if (request.getMonth() != null) {
            queryWrapper.eq(TimeEntry::getTmonth, request.getMonth());
        }
        
        // 2. 排序
        String sortBy = request.getSortBy();
        String sortOrder = request.getSortOrder();
        
        boolean isAsc = "asc".equalsIgnoreCase(sortOrder);
        
        switch (sortBy) {
            case "spent_on":
                queryWrapper.orderBy(true, isAsc, TimeEntry::getSpentOn);
                break;
            case "created_on":
                queryWrapper.orderBy(true, isAsc, TimeEntry::getCreatedOn);
                break;
            case "hours":
                queryWrapper.orderBy(true, isAsc, TimeEntry::getHours);
                break;
            default:
                // 默认按工作日期降序
                queryWrapper.orderByDesc(TimeEntry::getSpentOn);
        }
        
        // 3. 分页查询
        Page<TimeEntry> page = new Page<>(request.getPageNum(), request.getPageSize());
        IPage<TimeEntry> pageResult = timeEntryMapper.selectPage(page, queryWrapper);
        
        // 4. 转换为响应DTO
        List<TimeEntryResponseDTO> records = pageResult.getRecords().stream()
                .map(this::convertToResponseDTO)
                .toList();
        
        // 5. 返回分页结果
        return PageResponse.of(
                records,
                pageResult.getTotal(),
                pageResult.getCurrent(),
                pageResult.getSize()
        );
    }
    
    /**
     * 删除工时记录
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteTimeEntry(Long id) {
        Long currentUserId = securityUtils.getCurrentUserId();
        boolean isAdmin = securityUtils.isAdmin();
        MDC.put("userId", String.valueOf(currentUserId));
        
        // 1. 验证工时记录是否存在
        TimeEntry timeEntry = timeEntryMapper.selectById(id);
        if (timeEntry == null) {
            throw new BusinessException("工时记录不存在");
        }
        
        // 2. 权限检查：只能删除自己创建的记录，或者是管理员
        if (!isAdmin && !timeEntry.getAuthorId().equals(currentUserId)) {
            throw new BusinessException("无权限删除此工时记录");
        }
        
        // 3. 删除工时记录
        timeEntryMapper.deleteById(id);
        
        log.info("删除工时记录成功: id={}, projectId={}, userId={}, hours={}", 
                id, timeEntry.getProjectId(), timeEntry.getUserId(), timeEntry.getHours());
    }
    
    /**
     * 批量删除工时记录
     */
    @Transactional(rollbackFor = Exception.class)
    public TimeEntryBatchDeleteResponseDTO batchDeleteTimeEntries(TimeEntryBatchDeleteRequestDTO request) {
        Long currentUserId = securityUtils.getCurrentUserId();
        boolean isAdmin = securityUtils.isAdmin();
        MDC.put("userId", String.valueOf(currentUserId));
        
        List<Long> ids = request.getIds();
        int successCount = 0;
        List<TimeEntryBatchDeleteResponseDTO.FailureDetail> failures = new ArrayList<>();
        
        log.info("开始批量删除工时记录，数量: {}, 用户: {}", ids.size(), currentUserId);
        
        for (Long id : ids) {
            try {
                // 1. 验证工时记录是否存在
                TimeEntry timeEntry = timeEntryMapper.selectById(id);
                if (timeEntry == null) {
                    failures.add(TimeEntryBatchDeleteResponseDTO.FailureDetail.builder()
                            .timeEntryId(id)
                            .reason("工时记录不存在")
                            .build());
                    continue;
                }
                
                // 2. 权限检查：只能删除自己创建的记录，或者是管理员
                if (!isAdmin && !timeEntry.getAuthorId().equals(currentUserId)) {
                    failures.add(TimeEntryBatchDeleteResponseDTO.FailureDetail.builder()
                            .timeEntryId(id)
                            .reason("无权限删除此工时记录")
                            .build());
                    continue;
                }
                
                // 3. 删除工时记录
                timeEntryMapper.deleteById(id);
                successCount++;
                
                log.debug("删除工时记录成功: id={}", id);
                
            } catch (Exception e) {
                log.warn("删除工时记录失败: id={}, 原因: {}", id, e.getMessage());
                failures.add(TimeEntryBatchDeleteResponseDTO.FailureDetail.builder()
                        .timeEntryId(id)
                        .reason(e.getMessage())
                        .build());
            }
        }
        
        log.info("批量删除工时记录完成: 总数={}, 成功={}, 失败={}", 
                ids.size(), successCount, failures.size());
        
        return TimeEntryBatchDeleteResponseDTO.builder()
                .totalCount(ids.size())
                .successCount(successCount)
                .failureCount(failures.size())
                .failures(failures)
                .build();
    }
    
    /**
     * 批量更新工时记录
     */
    @Transactional(rollbackFor = Exception.class)
    public TimeEntryBatchUpdateResponseDTO batchUpdateTimeEntries(TimeEntryBatchUpdateRequestDTO request) {
        Long currentUserId = securityUtils.getCurrentUserId();
        boolean isAdmin = securityUtils.isAdmin();
        MDC.put("userId", String.valueOf(currentUserId));
        
        List<TimeEntryBatchUpdateRequestDTO.TimeEntryUpdateItem> items = request.getItems();
        int successCount = 0;
        List<TimeEntryBatchUpdateResponseDTO.FailureDetail> failures = new ArrayList<>();
        List<TimeEntryResponseDTO> successRecords = new ArrayList<>();
        
        log.info("开始批量更新工时记录，数量: {}, 用户: {}", items.size(), currentUserId);
        
        for (TimeEntryBatchUpdateRequestDTO.TimeEntryUpdateItem item : items) {
            try {
                // 1. 验证工时记录是否存在
                TimeEntry timeEntry = timeEntryMapper.selectById(item.getId());
                if (timeEntry == null) {
                    failures.add(TimeEntryBatchUpdateResponseDTO.FailureDetail.builder()
                            .timeEntryId(item.getId())
                            .reason("工时记录不存在")
                            .build());
                    continue;
                }
                
                // 2. 权限检查：只能更新自己创建的记录，或者是管理员
                if (!isAdmin && !timeEntry.getAuthorId().equals(currentUserId)) {
                    failures.add(TimeEntryBatchUpdateResponseDTO.FailureDetail.builder()
                            .timeEntryId(item.getId())
                            .reason("无权限更新此工时记录")
                            .build());
                    continue;
                }
                
                // 3. 更新任务ID
                if (item.getIssueId() != null) {
                    if (item.getIssueId() > 0) {
                        Issue issue = issueMapper.selectById(item.getIssueId());
                        if (issue == null) {
                            failures.add(TimeEntryBatchUpdateResponseDTO.FailureDetail.builder()
                                    .timeEntryId(item.getId())
                                    .reason("任务不存在")
                                    .build());
                            continue;
                        }
                        // 验证任务是否属于同一项目
                        if (!issue.getProjectId().equals(timeEntry.getProjectId())) {
                            failures.add(TimeEntryBatchUpdateResponseDTO.FailureDetail.builder()
                                    .timeEntryId(item.getId())
                                    .reason("任务不属于该项目")
                                    .build());
                            continue;
                        }
                    }
                    timeEntry.setIssueId(item.getIssueId());
                }
                
                // 4. 更新工作人员ID
                if (item.getUserId() != null) {
                    User user = userMapper.selectById(item.getUserId());
                    if (user == null) {
                        failures.add(TimeEntryBatchUpdateResponseDTO.FailureDetail.builder()
                                .timeEntryId(item.getId())
                                .reason("工作人员不存在")
                                .build());
                        continue;
                    }
                    timeEntry.setUserId(item.getUserId());
                }
                
                // 5. 更新活动类型
                if (item.getActivityId() != null) {
                    com.github.jredmine.entity.Enumeration activity = enumerationMapper.selectById(item.getActivityId());
                    if (activity == null || !"TimeEntryActivity".equals(activity.getType())) {
                        failures.add(TimeEntryBatchUpdateResponseDTO.FailureDetail.builder()
                                .timeEntryId(item.getId())
                                .reason("活动类型不存在或类型不正确")
                                .build());
                        continue;
                    }
                    timeEntry.setActivityId(item.getActivityId());
                }
                
                // 6. 更新工时
                if (item.getHours() != null) {
                    timeEntry.setHours(item.getHours());
                }
                
                // 7. 更新工作日期，需要重新计算年月周
                if (item.getSpentOn() != null) {
                    LocalDate spentOn = item.getSpentOn();
                    timeEntry.setSpentOn(spentOn);
                    timeEntry.setTyear(spentOn.getYear());
                    timeEntry.setTmonth(spentOn.getMonthValue());
                    WeekFields weekFields = WeekFields.of(Locale.getDefault());
                    timeEntry.setTweek(spentOn.get(weekFields.weekOfWeekBasedYear()));
                }
                
                // 8. 更新备注
                if (item.getComments() != null) {
                    timeEntry.setComments(item.getComments());
                }
                
                // 9. 更新时间戳
                timeEntry.setUpdatedOn(LocalDateTime.now());
                
                // 10. 保存到数据库
                timeEntryMapper.updateById(timeEntry);
                successCount++;
                
                // 11. 添加到成功记录列表
                TimeEntryResponseDTO responseDTO = convertToResponseDTO(timeEntry);
                successRecords.add(responseDTO);
                
                log.debug("更新工时记录成功: id={}", item.getId());
                
            } catch (Exception e) {
                log.warn("更新工时记录失败: id={}, 原因: {}", item.getId(), e.getMessage());
                failures.add(TimeEntryBatchUpdateResponseDTO.FailureDetail.builder()
                        .timeEntryId(item.getId())
                        .reason(e.getMessage())
                        .build());
            }
        }
        
        log.info("批量更新工时记录完成: 总数={}, 成功={}, 失败={}", 
                items.size(), successCount, failures.size());
        
        return TimeEntryBatchUpdateResponseDTO.builder()
                .totalCount(items.size())
                .successCount(successCount)
                .failureCount(failures.size())
                .failures(failures)
                .successRecords(successRecords)
                .build();
    }
    
    /**
     * 转换为响应DTO
     */
    private TimeEntryResponseDTO convertToResponseDTO(TimeEntry timeEntry) {
        TimeEntryResponseDTO dto = new TimeEntryResponseDTO();
        dto.setId(timeEntry.getId());
        dto.setIssueId(timeEntry.getIssueId());
        dto.setHours(timeEntry.getHours());
        dto.setSpentOn(timeEntry.getSpentOn());
        dto.setActivityId(timeEntry.getActivityId());
        dto.setComments(timeEntry.getComments());
        dto.setCreatedOn(timeEntry.getCreatedOn());
        dto.setUpdatedOn(timeEntry.getUpdatedOn());
        
        // 填充项目信息
        Project project = projectMapper.selectById(timeEntry.getProjectId());
        if (project != null) {
            ProjectSimpleResponseDTO projectDTO = new ProjectSimpleResponseDTO();
            projectDTO.setId(project.getId());
            projectDTO.setName(project.getName());
            projectDTO.setIdentifier(project.getIdentifier());
            dto.setProject(projectDTO);
        }
        
        // 填充任务标题
        if (timeEntry.getIssueId() != null) {
            Issue issue = issueMapper.selectById(timeEntry.getIssueId());
            if (issue != null) {
                dto.setIssueSubject(issue.getSubject());
            }
        }
        
        // 填充工作人员信息
        User user = userMapper.selectById(timeEntry.getUserId());
        if (user != null) {
            UserSimpleResponseDTO userDTO = new UserSimpleResponseDTO();
            userDTO.setId(user.getId());
            userDTO.setLogin(user.getLogin());
            userDTO.setFirstname(user.getFirstname());
            userDTO.setLastname(user.getLastname());
            dto.setUser(userDTO);
        }
        
        // 填充创建者信息
        User author = userMapper.selectById(timeEntry.getAuthorId());
        if (author != null) {
            UserSimpleResponseDTO authorDTO = new UserSimpleResponseDTO();
            authorDTO.setId(author.getId());
            authorDTO.setLogin(author.getLogin());
            authorDTO.setFirstname(author.getFirstname());
            authorDTO.setLastname(author.getLastname());
            dto.setAuthor(authorDTO);
        }
        
        // 填充活动类型名称
        Enumeration activity = enumerationMapper.selectById(timeEntry.getActivityId());
        if (activity != null) {
            dto.setActivityName(activity.getName());
        }
        
        return dto;
    }
    
    /**
     * 获取工时汇总统计
     */
    public TimeEntrySummaryResponseDTO getTimeEntrySummary(TimeEntryStatisticsRequestDTO request) {
        MDC.put("method", "getTimeEntrySummary");
        
        // 1. 构建查询条件
        LambdaQueryWrapper<TimeEntry> wrapper = buildStatisticsQueryWrapper(request);
        
        // 2. 查询所有符合条件的记录
        List<TimeEntry> entries = timeEntryMapper.selectList(wrapper);
        
        if (entries.isEmpty()) {
            return TimeEntrySummaryResponseDTO.builder()
                    .totalHours(0f)
                    .totalCount(0L)
                    .averageHours(0f)
                    .minHours(0f)
                    .maxHours(0f)
                    .build();
        }
        
        // 3. 计算统计数据
        Float totalHours = 0f;
        Float minHours = Float.MAX_VALUE;
        Float maxHours = 0f;
        
        for (TimeEntry entry : entries) {
            Float hours = entry.getHours();
            totalHours += hours;
            if (hours < minHours) {
                minHours = hours;
            }
            if (hours > maxHours) {
                maxHours = hours;
            }
        }
        
        Long totalCount = (long) entries.size();
        Float averageHours = totalHours / totalCount;
        
        log.info("工时汇总统计完成: totalHours={}, totalCount={}, averageHours={}", 
                totalHours, totalCount, averageHours);
        
        return TimeEntrySummaryResponseDTO.builder()
                .totalHours(Math.round(totalHours * 100) / 100f)
                .totalCount(totalCount)
                .averageHours(Math.round(averageHours * 100) / 100f)
                .minHours(Math.round(minHours * 100) / 100f)
                .maxHours(Math.round(maxHours * 100) / 100f)
                .build();
    }
    
    /**
     * 获取工时分组统计
     */
    public TimeEntryStatisticsResponseDTO getTimeEntryStatistics(TimeEntryStatisticsRequestDTO request) {
        MDC.put("method", "getTimeEntryStatistics");
        MDC.put("groupBy", request.getGroupBy());
        
        // 1. 构建查询条件
        LambdaQueryWrapper<TimeEntry> wrapper = buildStatisticsQueryWrapper(request);
        
        // 2. 查询所有符合条件的记录
        List<TimeEntry> entries = timeEntryMapper.selectList(wrapper);
        
        if (entries.isEmpty()) {
            return TimeEntryStatisticsResponseDTO.builder()
                    .totalHours(0f)
                    .totalCount(0L)
                    .averageHours(0f)
                    .groups(new ArrayList<>())
                    .build();
        }
        
        // 3. 计算总计
        Float totalHours = entries.stream()
                .map(TimeEntry::getHours)
                .reduce(0f, Float::sum);
        Long totalCount = (long) entries.size();
        Float averageHours = totalHours / totalCount;
        
        // 4. 按指定维度分组统计
        List<TimeEntryStatisticsResponseDTO.GroupStatistics> groups = 
                groupStatistics(entries, request.getGroupBy(), totalHours);
        
        log.info("工时分组统计完成: groupBy={}, totalHours={}, groupCount={}", 
                request.getGroupBy(), totalHours, groups.size());
        
        return TimeEntryStatisticsResponseDTO.builder()
                .totalHours(Math.round(totalHours * 100) / 100f)
                .totalCount(totalCount)
                .averageHours(Math.round(averageHours * 100) / 100f)
                .groups(groups)
                .build();
    }
    
    /**
     * 构建统计查询条件
     */
    private LambdaQueryWrapper<TimeEntry> buildStatisticsQueryWrapper(TimeEntryStatisticsRequestDTO request) {
        LambdaQueryWrapper<TimeEntry> wrapper = new LambdaQueryWrapper<>();
        
        // 项目筛选
        if (request.getProjectId() != null) {
            wrapper.eq(TimeEntry::getProjectId, request.getProjectId());
        }
        
        // 任务筛选
        if (request.getIssueId() != null) {
            wrapper.eq(TimeEntry::getIssueId, request.getIssueId());
        }
        
        // 用户筛选
        if (request.getUserId() != null) {
            wrapper.eq(TimeEntry::getUserId, request.getUserId());
        }
        
        // 活动类型筛选
        if (request.getActivityId() != null) {
            wrapper.eq(TimeEntry::getActivityId, request.getActivityId());
        }
        
        // 日期范围筛选
        if (request.getStartDate() != null) {
            wrapper.ge(TimeEntry::getSpentOn, request.getStartDate());
        }
        if (request.getEndDate() != null) {
            wrapper.le(TimeEntry::getSpentOn, request.getEndDate());
        }
        
        // 年份筛选
        if (request.getYear() != null) {
            wrapper.eq(TimeEntry::getTyear, request.getYear());
        }
        
        // 月份筛选
        if (request.getMonth() != null) {
            wrapper.eq(TimeEntry::getTmonth, request.getMonth());
        }
        
        return wrapper;
    }
    
    /**
     * 分组统计
     */
    private List<TimeEntryStatisticsResponseDTO.GroupStatistics> groupStatistics(
            List<TimeEntry> entries, String groupBy, Float totalHours) {
        
        if (groupBy == null || groupBy.isEmpty()) {
            groupBy = "project"; // 默认按项目分组
        }
        
        Map<String, List<TimeEntry>> groupMap = new HashMap<>();
        
        // 根据不同维度分组
        switch (groupBy.toLowerCase()) {
            case "project":
                groupMap = entries.stream()
                        .collect(Collectors.groupingBy(e -> String.valueOf(e.getProjectId())));
                break;
                
            case "user":
                groupMap = entries.stream()
                        .collect(Collectors.groupingBy(e -> String.valueOf(e.getUserId())));
                break;
                
            case "activity":
                groupMap = entries.stream()
                        .collect(Collectors.groupingBy(e -> String.valueOf(e.getActivityId())));
                break;
                
            case "date":
                groupMap = entries.stream()
                        .collect(Collectors.groupingBy(e -> e.getSpentOn().toString()));
                break;
                
            default:
                // 默认按项目分组
                groupMap = entries.stream()
                        .collect(Collectors.groupingBy(e -> String.valueOf(e.getProjectId())));
                break;
        }
        
        // 计算每组的统计数据
        List<TimeEntryStatisticsResponseDTO.GroupStatistics> groups = new ArrayList<>();
        
        for (Map.Entry<String, List<TimeEntry>> entry : groupMap.entrySet()) {
            String key = entry.getKey();
            List<TimeEntry> groupEntries = entry.getValue();
            
            Float groupHours = groupEntries.stream()
                    .map(TimeEntry::getHours)
                    .reduce(0f, Float::sum);
            
            Long groupCount = (long) groupEntries.size();
            Float percentage = (groupHours / totalHours) * 100;
            
            // 获取分组名称
            String groupName = getGroupName(groupBy, key, groupEntries.get(0));
            
            groups.add(TimeEntryStatisticsResponseDTO.GroupStatistics.builder()
                    .groupKey(key)
                    .groupName(groupName)
                    .hours(Math.round(groupHours * 100) / 100f)
                    .count(groupCount)
                    .percentage(Math.round(percentage * 100) / 100f)
                    .build());
        }
        
        // 按工时降序排序
        groups.sort((a, b) -> Float.compare(b.getHours(), a.getHours()));
        
        return groups;
    }
    
    /**
     * 获取分组名称
     */
    private String getGroupName(String groupBy, String key, TimeEntry sampleEntry) {
        switch (groupBy.toLowerCase()) {
            case "project":
                Project project = projectMapper.selectById(Long.valueOf(key));
                return project != null ? project.getName() : "未知项目";
                
            case "user":
                User user = userMapper.selectById(Long.valueOf(key));
                return user != null ? user.getLogin() : "未知用户";
                
            case "activity":
                Enumeration activity = enumerationMapper.selectById(Long.valueOf(key));
                return activity != null ? activity.getName() : "未知活动类型";
                
            case "date":
                return key; // 日期直接返回
                
            default:
                return key;
        }
    }
    
    /**
     * 生成项目工时报表
     */
    public TimeEntryProjectReportDTO generateProjectReport(TimeEntryReportRequestDTO request) {
        MDC.put("method", "generateProjectReport");
        MDC.put("projectId", String.valueOf(request.getProjectId()));
        
        if (request.getProjectId() == null) {
            throw new BusinessException("项目ID不能为空");
        }
        
        // 1. 获取项目信息
        Project project = projectMapper.selectById(request.getProjectId());
        if (project == null) {
            throw new BusinessException("项目不存在");
        }
        
        // 2. 构建查询条件
        LambdaQueryWrapper<TimeEntry> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TimeEntry::getProjectId, request.getProjectId());
        addDateRangeConditions(wrapper, request);
        
        // 3. 查询所有工时记录
        List<TimeEntry> entries = timeEntryMapper.selectList(wrapper);
        
        if (entries.isEmpty()) {
            return buildEmptyProjectReport(project);
        }
        
        // 4. 计算基础统计数据
        Float totalHours = entries.stream().map(TimeEntry::getHours).reduce(0f, Float::sum);
        Long totalCount = (long) entries.size();
        Long userCount = entries.stream().map(TimeEntry::getUserId).distinct().count();
        Float averageHours = totalHours / totalCount;
        
        String earliestDate = entries.stream()
                .map(e -> e.getSpentOn().toString())
                .min(String::compareTo)
                .orElse("");
        String latestDate = entries.stream()
                .map(e -> e.getSpentOn().toString())
                .max(String::compareTo)
                .orElse("");
        
        // 5. 按用户分组统计
        Map<Long, List<TimeEntry>> userGroupMap = new HashMap<>();
        for (TimeEntry entry : entries) {
            userGroupMap.computeIfAbsent(entry.getUserId(), k -> new ArrayList<>()).add(entry);
        }
        
        List<TimeEntryProjectReportDTO.UserTimeDetail> userDetails = new ArrayList<>();
        for (Map.Entry<Long, List<TimeEntry>> entry : userGroupMap.entrySet()) {
            Long userId = entry.getKey();
            List<TimeEntry> userEntries = entry.getValue();
            Float userHours = userEntries.stream().map(TimeEntry::getHours).reduce(0f, Float::sum);
            
            User user = userMapper.selectById(userId);
            String userName = user != null ? user.getLogin() : "未知用户";
            
            userDetails.add(TimeEntryProjectReportDTO.UserTimeDetail.builder()
                    .userId(userId)
                    .userName(userName)
                    .hours(round(userHours))
                    .count((long) userEntries.size())
                    .percentage(round((userHours / totalHours) * 100))
                    .build());
        }
        userDetails.sort((a, b) -> Float.compare(b.getHours(), a.getHours()));
        
        // 6. 按活动类型分组统计
        Map<Long, List<TimeEntry>> activityGroupMap = new HashMap<>();
        for (TimeEntry entry : entries) {
            activityGroupMap.computeIfAbsent(entry.getActivityId(), k -> new ArrayList<>()).add(entry);
        }
        
        List<TimeEntryProjectReportDTO.ActivityTimeDetail> activityDetails = new ArrayList<>();
        for (Map.Entry<Long, List<TimeEntry>> entry : activityGroupMap.entrySet()) {
            Long activityId = entry.getKey();
            List<TimeEntry> activityEntries = entry.getValue();
            Float activityHours = activityEntries.stream().map(TimeEntry::getHours).reduce(0f, Float::sum);
            
            Enumeration activity = enumerationMapper.selectById(activityId);
            String activityName = activity != null ? activity.getName() : "未知活动类型";
            
            activityDetails.add(TimeEntryProjectReportDTO.ActivityTimeDetail.builder()
                    .activityId(activityId)
                    .activityName(activityName)
                    .hours(round(activityHours))
                    .count((long) activityEntries.size())
                    .percentage(round((activityHours / totalHours) * 100))
                    .build());
        }
        activityDetails.sort((a, b) -> Float.compare(b.getHours(), a.getHours()));
        
        // 7. 构建项目信息
        ProjectSimpleResponseDTO projectDTO = new ProjectSimpleResponseDTO();
        projectDTO.setId(project.getId());
        projectDTO.setName(project.getName());
        projectDTO.setIdentifier(project.getIdentifier());
        
        log.info("项目工时报表生成完成: projectId={}, totalHours={}, userCount={}", 
                request.getProjectId(), totalHours, userCount);
        
        return TimeEntryProjectReportDTO.builder()
                .project(projectDTO)
                .totalHours(round(totalHours))
                .totalCount(totalCount)
                .userCount(userCount)
                .averageHours(round(averageHours))
                .earliestDate(earliestDate)
                .latestDate(latestDate)
                .userDetails(userDetails)
                .activityDetails(activityDetails)
                .build();
    }
    
    /**
     * 生成用户工时报表
     */
    public TimeEntryUserReportDTO generateUserReport(TimeEntryReportRequestDTO request) {
        MDC.put("method", "generateUserReport");
        MDC.put("userId", String.valueOf(request.getUserId()));
        
        if (request.getUserId() == null) {
            throw new BusinessException("用户ID不能为空");
        }
        
        // 1. 获取用户信息
        User user = userMapper.selectById(request.getUserId());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        
        // 2. 构建查询条件
        LambdaQueryWrapper<TimeEntry> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TimeEntry::getUserId, request.getUserId());
        addDateRangeConditions(wrapper, request);
        
        // 3. 查询所有工时记录
        List<TimeEntry> entries = timeEntryMapper.selectList(wrapper);
        
        if (entries.isEmpty()) {
            return buildEmptyUserReport(user);
        }
        
        // 4. 计算基础统计数据
        Float totalHours = entries.stream().map(TimeEntry::getHours).reduce(0f, Float::sum);
        Long totalCount = (long) entries.size();
        Long projectCount = entries.stream().map(TimeEntry::getProjectId).distinct().count();
        Float averageHours = totalHours / totalCount;
        
        String earliestDate = entries.stream()
                .map(e -> e.getSpentOn().toString())
                .min(String::compareTo)
                .orElse("");
        String latestDate = entries.stream()
                .map(e -> e.getSpentOn().toString())
                .max(String::compareTo)
                .orElse("");
        
        // 5. 按项目分组统计
        Map<Long, List<TimeEntry>> projectGroupMap = new HashMap<>();
        for (TimeEntry entry : entries) {
            projectGroupMap.computeIfAbsent(entry.getProjectId(), k -> new ArrayList<>()).add(entry);
        }
        
        List<TimeEntryUserReportDTO.ProjectTimeDetail> projectDetails = new ArrayList<>();
        for (Map.Entry<Long, List<TimeEntry>> entry : projectGroupMap.entrySet()) {
            Long projectId = entry.getKey();
            List<TimeEntry> projectEntries = entry.getValue();
            Float projectHours = projectEntries.stream().map(TimeEntry::getHours).reduce(0f, Float::sum);
            
            Project project = projectMapper.selectById(projectId);
            String projectName = project != null ? project.getName() : "未知项目";
            
            projectDetails.add(TimeEntryUserReportDTO.ProjectTimeDetail.builder()
                    .projectId(projectId)
                    .projectName(projectName)
                    .hours(round(projectHours))
                    .count((long) projectEntries.size())
                    .percentage(round((projectHours / totalHours) * 100))
                    .build());
        }
        projectDetails.sort((a, b) -> Float.compare(b.getHours(), a.getHours()));
        
        // 6. 按活动类型分组统计
        Map<Long, List<TimeEntry>> activityGroupMap = new HashMap<>();
        for (TimeEntry entry : entries) {
            activityGroupMap.computeIfAbsent(entry.getActivityId(), k -> new ArrayList<>()).add(entry);
        }
        
        List<TimeEntryUserReportDTO.ActivityTimeDetail> activityDetails = new ArrayList<>();
        for (Map.Entry<Long, List<TimeEntry>> entry : activityGroupMap.entrySet()) {
            Long activityId = entry.getKey();
            List<TimeEntry> activityEntries = entry.getValue();
            Float activityHours = activityEntries.stream().map(TimeEntry::getHours).reduce(0f, Float::sum);
            
            Enumeration activity = enumerationMapper.selectById(activityId);
            String activityName = activity != null ? activity.getName() : "未知活动类型";
            
            activityDetails.add(TimeEntryUserReportDTO.ActivityTimeDetail.builder()
                    .activityId(activityId)
                    .activityName(activityName)
                    .hours(round(activityHours))
                    .count((long) activityEntries.size())
                    .percentage(round((activityHours / totalHours) * 100))
                    .build());
        }
        activityDetails.sort((a, b) -> Float.compare(b.getHours(), a.getHours()));
        
        // 7. 构建用户信息
        UserSimpleResponseDTO userDTO = new UserSimpleResponseDTO();
        userDTO.setId(user.getId());
        userDTO.setLogin(user.getLogin());
        userDTO.setFirstname(user.getFirstname());
        userDTO.setLastname(user.getLastname());
        
        log.info("用户工时报表生成完成: userId={}, totalHours={}, projectCount={}", 
                request.getUserId(), totalHours, projectCount);
        
        return TimeEntryUserReportDTO.builder()
                .user(userDTO)
                .totalHours(round(totalHours))
                .totalCount(totalCount)
                .projectCount(projectCount)
                .averageHours(round(averageHours))
                .earliestDate(earliestDate)
                .latestDate(latestDate)
                .projectDetails(projectDetails)
                .activityDetails(activityDetails)
                .build();
    }
    
    /**
     * 生成时间段工时报表
     */
    public TimeEntryPeriodReportDTO generatePeriodReport(TimeEntryReportRequestDTO request) {
        MDC.put("method", "generatePeriodReport");
        MDC.put("periodType", request.getPeriodType());
        
        // 1. 构建查询条件
        LambdaQueryWrapper<TimeEntry> wrapper = new LambdaQueryWrapper<>();
        if (request.getProjectId() != null) {
            wrapper.eq(TimeEntry::getProjectId, request.getProjectId());
        }
        if (request.getUserId() != null) {
            wrapper.eq(TimeEntry::getUserId, request.getUserId());
        }
        addDateRangeConditions(wrapper, request);
        
        // 2. 查询所有工时记录
        List<TimeEntry> entries = timeEntryMapper.selectList(wrapper);
        
        if (entries.isEmpty()) {
            return buildEmptyPeriodReport(request);
        }
        
        // 3. 确定时间段类型
        String periodType = request.getPeriodType();
        if (periodType == null || periodType.isEmpty()) {
            periodType = "day"; // 默认按日
        }
        
        // 4. 计算基础统计数据
        Float totalHours = entries.stream().map(TimeEntry::getHours).reduce(0f, Float::sum);
        Long totalCount = (long) entries.size();
        
        String startDate = entries.stream()
                .map(e -> e.getSpentOn().toString())
                .min(String::compareTo)
                .orElse("");
        String endDate = entries.stream()
                .map(e -> e.getSpentOn().toString())
                .max(String::compareTo)
                .orElse("");
        
        // 5. 按时间段分组统计
        Map<String, List<TimeEntry>> periodGroupMap = new HashMap<>();
        for (TimeEntry entry : entries) {
            String period = getPeriodKey(entry.getSpentOn(), periodType);
            periodGroupMap.computeIfAbsent(period, k -> new ArrayList<>()).add(entry);
        }
        
        List<TimeEntryPeriodReportDTO.PeriodTimeDetail> periodDetails = new ArrayList<>();
        for (Map.Entry<String, List<TimeEntry>> entry : periodGroupMap.entrySet()) {
            String period = entry.getKey();
            List<TimeEntry> periodEntries = entry.getValue();
            Float periodHours = periodEntries.stream().map(TimeEntry::getHours).reduce(0f, Float::sum);
            Long userCount = periodEntries.stream().map(TimeEntry::getUserId).distinct().count();
            
            periodDetails.add(TimeEntryPeriodReportDTO.PeriodTimeDetail.builder()
                    .period(period)
                    .periodName(getPeriodName(period, periodType))
                    .hours(round(periodHours))
                    .count((long) periodEntries.size())
                    .userCount(userCount)
                    .build());
        }
        periodDetails.sort((a, b) -> a.getPeriod().compareTo(b.getPeriod()));
        
        // 6. 计算每日平均工时
        long dayCount = periodDetails.size();
        Float averageDailyHours = dayCount > 0 ? totalHours / dayCount : 0f;
        
        Float maxDailyHours = periodDetails.stream()
                .map(TimeEntryPeriodReportDTO.PeriodTimeDetail::getHours)
                .max(Float::compare)
                .orElse(0f);
        
        Float minDailyHours = periodDetails.stream()
                .map(TimeEntryPeriodReportDTO.PeriodTimeDetail::getHours)
                .min(Float::compare)
                .orElse(0f);
        
        log.info("时间段工时报表生成完成: periodType={}, totalHours={}, periodCount={}", 
                periodType, totalHours, periodDetails.size());
        
        return TimeEntryPeriodReportDTO.builder()
                .periodType(periodType)
                .startDate(startDate)
                .endDate(endDate)
                .totalHours(round(totalHours))
                .totalCount(totalCount)
                .averageDailyHours(round(averageDailyHours))
                .maxDailyHours(round(maxDailyHours))
                .minDailyHours(round(minDailyHours))
                .periodDetails(periodDetails)
                .build();
    }
    
    /**
     * 添加日期范围条件
     */
    private void addDateRangeConditions(LambdaQueryWrapper<TimeEntry> wrapper, TimeEntryReportRequestDTO request) {
        if (request.getStartDate() != null) {
            wrapper.ge(TimeEntry::getSpentOn, request.getStartDate());
        }
        if (request.getEndDate() != null) {
            wrapper.le(TimeEntry::getSpentOn, request.getEndDate());
        }
        if (request.getYear() != null) {
            wrapper.eq(TimeEntry::getTyear, request.getYear());
        }
        if (request.getMonth() != null) {
            wrapper.eq(TimeEntry::getTmonth, request.getMonth());
        }
    }
    
    /**
     * 获取时间段键
     */
    private String getPeriodKey(LocalDate date, String periodType) {
        switch (periodType.toLowerCase()) {
            case "week":
                WeekFields weekFields = WeekFields.of(Locale.getDefault());
                int weekNumber = date.get(weekFields.weekOfWeekBasedYear());
                return String.format("%d-W%02d", date.getYear(), weekNumber);
            case "month":
                return String.format("%d-%02d", date.getYear(), date.getMonthValue());
            case "day":
            default:
                return date.toString();
        }
    }
    
    /**
     * 获取时间段名称
     */
    private String getPeriodName(String period, String periodType) {
        switch (periodType.toLowerCase()) {
            case "week":
                return period.replace("-W", "年第") + "周";
            case "month":
                return period + "月";
            case "day":
            default:
                return period;
        }
    }
    
    /**
     * 构建空的项目报表
     */
    private TimeEntryProjectReportDTO buildEmptyProjectReport(Project project) {
        ProjectSimpleResponseDTO projectDTO = new ProjectSimpleResponseDTO();
        projectDTO.setId(project.getId());
        projectDTO.setName(project.getName());
        projectDTO.setIdentifier(project.getIdentifier());
        
        return TimeEntryProjectReportDTO.builder()
                .project(projectDTO)
                .totalHours(0f)
                .totalCount(0L)
                .userCount(0L)
                .averageHours(0f)
                .earliestDate("")
                .latestDate("")
                .userDetails(new ArrayList<>())
                .activityDetails(new ArrayList<>())
                .build();
    }
    
    /**
     * 构建空的用户报表
     */
    private TimeEntryUserReportDTO buildEmptyUserReport(User user) {
        UserSimpleResponseDTO userDTO = new UserSimpleResponseDTO();
        userDTO.setId(user.getId());
        userDTO.setLogin(user.getLogin());
        userDTO.setFirstname(user.getFirstname());
        userDTO.setLastname(user.getLastname());
        
        return TimeEntryUserReportDTO.builder()
                .user(userDTO)
                .totalHours(0f)
                .totalCount(0L)
                .projectCount(0L)
                .averageHours(0f)
                .earliestDate("")
                .latestDate("")
                .projectDetails(new ArrayList<>())
                .activityDetails(new ArrayList<>())
                .build();
    }
    
    /**
     * 构建空的时间段报表
     */
    private TimeEntryPeriodReportDTO buildEmptyPeriodReport(TimeEntryReportRequestDTO request) {
        String periodType = request.getPeriodType();
        if (periodType == null || periodType.isEmpty()) {
            periodType = "day";
        }
        
        return TimeEntryPeriodReportDTO.builder()
                .periodType(periodType)
                .startDate("")
                .endDate("")
                .totalHours(0f)
                .totalCount(0L)
                .averageDailyHours(0f)
                .maxDailyHours(0f)
                .minDailyHours(0f)
                .periodDetails(new ArrayList<>())
                .build();
    }
    
    /**
     * 四舍五入保留2位小数
     */
    private Float round(Float value) {
        return Math.round(value * 100) / 100f;
    }
}
