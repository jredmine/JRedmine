package com.github.jredmine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.jredmine.dto.request.timeentry.TimeEntryCreateRequestDTO;
import com.github.jredmine.dto.request.timeentry.TimeEntryQueryRequestDTO;
import com.github.jredmine.dto.request.timeentry.TimeEntryUpdateRequestDTO;
import com.github.jredmine.dto.response.PageResponse;
import com.github.jredmine.dto.response.project.ProjectSimpleResponseDTO;
import com.github.jredmine.dto.response.timeentry.TimeEntryResponseDTO;
import com.github.jredmine.dto.response.user.UserSimpleResponseDTO;
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
import java.util.List;
import java.util.Locale;

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
}
