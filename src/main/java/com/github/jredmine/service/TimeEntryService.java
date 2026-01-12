package com.github.jredmine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.jredmine.dto.request.timeentry.TimeEntryCreateRequestDTO;
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
