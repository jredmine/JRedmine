package com.github.jredmine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.jredmine.dto.request.issue.IssueCreateRequestDTO;
import com.github.jredmine.dto.response.issue.IssueDetailResponseDTO;
import com.github.jredmine.entity.Issue;
import com.github.jredmine.entity.IssueStatus;
import com.github.jredmine.entity.Project;
import com.github.jredmine.entity.Tracker;
import com.github.jredmine.entity.User;
import com.github.jredmine.enums.ResultCode;
import com.github.jredmine.exception.BusinessException;
import com.github.jredmine.mapper.issue.IssueMapper;
import com.github.jredmine.mapper.project.ProjectMapper;
import com.github.jredmine.mapper.TrackerMapper;
import com.github.jredmine.mapper.workflow.IssueStatusMapper;
import com.github.jredmine.mapper.user.UserMapper;
import com.github.jredmine.security.ProjectPermissionService;
import com.github.jredmine.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务服务
 *
 * @author panfeng
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IssueService {

    private final IssueMapper issueMapper;
    private final ProjectMapper projectMapper;
    private final TrackerMapper trackerMapper;
    private final IssueStatusMapper issueStatusMapper;
    private final UserMapper userMapper;
    private final SecurityUtils securityUtils;
    private final ProjectPermissionService projectPermissionService;

    /**
     * 创建任务
     *
     * @param requestDTO 创建任务请求
     * @return 任务详情
     */
    @Transactional(rollbackFor = Exception.class)
    public IssueDetailResponseDTO createIssue(IssueCreateRequestDTO requestDTO) {
        MDC.put("operation", "create_issue");

        try {
            log.info("开始创建任务，任务标题: {}", requestDTO.getSubject());

            // 获取当前用户信息
            User currentUser = securityUtils.getCurrentUser();
            Long currentUserId = currentUser.getId();
            boolean isAdmin = Boolean.TRUE.equals(currentUser.getAdmin());

            // 权限验证：需要 add_issues 权限或系统管理员
            if (!isAdmin) {
                // 检查用户在项目中是否拥有 add_issues 权限
                if (!projectPermissionService.hasPermission(currentUserId, requestDTO.getProjectId(), "add_issues")) {
                    log.warn("用户无权限创建任务，项目ID: {}, 用户ID: {}", requestDTO.getProjectId(), currentUserId);
                    throw new BusinessException(ResultCode.FORBIDDEN, "无权限创建任务，需要 add_issues 权限");
                }
            }

            // 验证项目是否存在
            Project project = projectMapper.selectById(requestDTO.getProjectId());
            if (project == null) {
                log.warn("项目不存在，项目ID: {}", requestDTO.getProjectId());
                throw new BusinessException(ResultCode.PROJECT_NOT_FOUND);
            }

            // 验证跟踪器是否存在
            Tracker tracker = trackerMapper.selectById(requestDTO.getTrackerId());
            if (tracker == null) {
                log.warn("跟踪器不存在，跟踪器ID: {}", requestDTO.getTrackerId());
                throw new BusinessException(ResultCode.SYSTEM_ERROR, "跟踪器不存在");
            }

            // 确定状态ID：优先使用请求中的状态，否则使用跟踪器的默认状态，最后使用第一个可用状态
            Integer statusId = requestDTO.getStatusId();
            if (statusId == null) {
                if (tracker.getDefaultStatusId() != null) {
                    statusId = tracker.getDefaultStatusId().intValue();
                } else {
                    // 获取第一个可用状态
                    List<IssueStatus> statuses = issueStatusMapper.selectList(
                            new LambdaQueryWrapper<IssueStatus>()
                                    .orderByAsc(IssueStatus::getPosition)
                                    .orderByAsc(IssueStatus::getId)
                                    .last("LIMIT 1")
                    );
                    if (statuses.isEmpty()) {
                        log.error("系统中没有可用的任务状态");
                        throw new BusinessException(ResultCode.SYSTEM_ERROR, "系统中没有可用的任务状态");
                    }
                    statusId = statuses.get(0).getId();
                }
            } else {
                // 验证状态是否存在
                IssueStatus status = issueStatusMapper.selectById(statusId);
                if (status == null) {
                    log.warn("任务状态不存在，状态ID: {}", statusId);
                    throw new BusinessException(ResultCode.SYSTEM_ERROR, "任务状态不存在");
                }
            }

            // 验证分类是否存在（如果提供且不为0）
            // categoryId 为 0 或 null 表示没有分类
            Integer categoryId = requestDTO.getCategoryId();
            if (categoryId != null && categoryId != 0) {
                // TODO: 验证分类是否存在（需要创建 IssueCategory 实体和 Mapper）
                // 暂时跳过，后续实现
            }

            // 验证版本是否存在（如果提供且不为0）
            // fixedVersionId 为 0 或 null 表示没有修复版本
            Long fixedVersionId = requestDTO.getFixedVersionId();
            if (fixedVersionId != null && fixedVersionId != 0) {
                // TODO: 验证版本是否存在（需要创建 Version 实体和 Mapper）
                // 暂时跳过，后续实现
            }

            // 验证父任务是否存在（如果提供且不为0）
            // parentId 为 0 或 null 表示没有父任务
            Long parentId = requestDTO.getParentId();
            if (parentId != null && parentId != 0) {
                Issue parentIssue = issueMapper.selectById(parentId);
                if (parentIssue == null) {
                    log.warn("父任务不存在，父任务ID: {}", parentId);
                    throw new BusinessException(ResultCode.SYSTEM_ERROR, "父任务不存在");
                }
                // 验证父任务是否属于同一项目
                if (!parentIssue.getProjectId().equals(requestDTO.getProjectId())) {
                    log.warn("父任务不属于同一项目，父任务ID: {}, 项目ID: {}", parentId, requestDTO.getProjectId());
                    throw new BusinessException(ResultCode.SYSTEM_ERROR, "父任务必须属于同一项目");
                }
            }

            // 验证指派人是否存在（如果提供且不为0）
            // assignedToId 为 0 或 null 表示未分配
            Long assignedToId = requestDTO.getAssignedToId();
            if (assignedToId != null && assignedToId != 0) {
                User assignedUser = userMapper.selectById(assignedToId);
                if (assignedUser == null) {
                    log.warn("指派人不存在，用户ID: {}", assignedToId);
                    throw new BusinessException(ResultCode.USER_NOT_FOUND);
                }
            }

            // 创建任务实体
            Issue issue = new Issue();
            issue.setTrackerId(requestDTO.getTrackerId());
            issue.setProjectId(requestDTO.getProjectId());
            issue.setSubject(requestDTO.getSubject());
            issue.setDescription(requestDTO.getDescription());
            issue.setStatusId(statusId);
            issue.setPriorityId(requestDTO.getPriorityId());
            // assignedToId 为 0 时设置为 null，表示未分配
            issue.setAssignedToId((assignedToId != null && assignedToId != 0) ? assignedToId : null);
            // categoryId 为 0 时设置为 null，表示没有分类
            issue.setCategoryId((categoryId != null && categoryId != 0) ? categoryId : null);
            // fixedVersionId 为 0 时设置为 null，表示没有修复版本
            issue.setFixedVersionId((fixedVersionId != null && fixedVersionId != 0) ? fixedVersionId : null);
            issue.setStartDate(requestDTO.getStartDate());
            issue.setDueDate(requestDTO.getDueDate());
            issue.setEstimatedHours(requestDTO.getEstimatedHours());
            issue.setDoneRatio(requestDTO.getDoneRatio() != null ? requestDTO.getDoneRatio() : 0);
            // parentId 为 0 时设置为 null，表示没有父任务
            issue.setParentId((parentId != null && parentId != 0) ? parentId : null);
            issue.setIsPrivate(requestDTO.getIsPrivate() != null ? requestDTO.getIsPrivate() : false);
            issue.setAuthorId(currentUserId);
            issue.setLockVersion(0);
            LocalDateTime now = LocalDateTime.now();
            issue.setCreatedOn(now);
            issue.setUpdatedOn(now);

            // TODO: 处理树形结构（parent_id, root_id, lft, rgt）
            // 暂时设置为 null，后续实现树形结构逻辑

            // 保存任务
            int insertResult = issueMapper.insert(issue);
            if (insertResult <= 0) {
                log.error("任务创建失败，插入数据库失败");
                throw new BusinessException(ResultCode.SYSTEM_ERROR, "任务创建失败");
            }

            log.info("任务创建成功，任务ID: {}", issue.getId());

            // 查询创建的任务（包含关联信息）
            return getIssueDetailById(issue.getId());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("任务创建失败", e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "任务创建失败");
        } finally {
            MDC.clear();
        }
    }

    /**
     * 根据ID查询任务详情
     *
     * @param id 任务ID
     * @return 任务详情
     */
    public IssueDetailResponseDTO getIssueDetailById(Long id) {
        MDC.put("operation", "get_issue_detail");
        MDC.put("issueId", String.valueOf(id));

        try {
            log.debug("开始查询任务详情，任务ID: {}", id);

            // 查询任务
            Issue issue = issueMapper.selectById(id);
            if (issue == null) {
                log.warn("任务不存在，任务ID: {}", id);
                throw new BusinessException(ResultCode.SYSTEM_ERROR, "任务不存在");
            }

            // 权限验证：需要 view_issues 权限
            User currentUser = securityUtils.getCurrentUser();
            boolean isAdmin = Boolean.TRUE.equals(currentUser.getAdmin());
            if (!isAdmin) {
                // 检查用户在项目中是否拥有 view_issues 权限
                if (!projectPermissionService.hasPermission(currentUser.getId(), issue.getProjectId(), "view_issues")) {
                    log.warn("用户无权限查看任务，任务ID: {}, 用户ID: {}", id, currentUser.getId());
                    throw new BusinessException(ResultCode.FORBIDDEN, "无权限查看任务，需要 view_issues 权限");
                }
            }

            // 转换为响应 DTO
            IssueDetailResponseDTO dto = toIssueDetailResponseDTO(issue);

            // 填充关联信息
            fillRelatedInfo(dto, issue);

            log.info("任务详情查询成功，任务ID: {}", id);
            return dto;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("任务详情查询失败，任务ID: {}", id, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "任务详情查询失败");
        } finally {
            MDC.clear();
        }
    }

    /**
     * 转换为响应 DTO
     */
    private IssueDetailResponseDTO toIssueDetailResponseDTO(Issue issue) {
        IssueDetailResponseDTO dto = new IssueDetailResponseDTO();
        dto.setId(issue.getId());
        dto.setTrackerId(issue.getTrackerId());
        dto.setProjectId(issue.getProjectId());
        dto.setSubject(issue.getSubject());
        dto.setDescription(issue.getDescription());
        dto.setStatusId(issue.getStatusId());
        dto.setPriorityId(issue.getPriorityId());
        dto.setAssignedToId(issue.getAssignedToId());
        dto.setCategoryId(issue.getCategoryId());
        dto.setFixedVersionId(issue.getFixedVersionId());
        dto.setStartDate(issue.getStartDate());
        dto.setDueDate(issue.getDueDate());
        dto.setEstimatedHours(issue.getEstimatedHours());
        dto.setDoneRatio(issue.getDoneRatio());
        dto.setParentId(issue.getParentId());
        dto.setRootId(issue.getRootId());
        dto.setIsPrivate(issue.getIsPrivate());
        dto.setAuthorId(issue.getAuthorId());
        dto.setCreatedOn(issue.getCreatedOn());
        dto.setUpdatedOn(issue.getUpdatedOn());
        dto.setClosedOn(issue.getClosedOn());
        return dto;
    }

    /**
     * 填充关联信息（项目名称、跟踪器名称、状态名称等）
     */
    private void fillRelatedInfo(IssueDetailResponseDTO dto, Issue issue) {
        // 填充项目信息
        Project project = projectMapper.selectById(issue.getProjectId());
        if (project != null) {
            dto.setProjectName(project.getName());
        }

        // 填充跟踪器信息
        Tracker tracker = trackerMapper.selectById(issue.getTrackerId());
        if (tracker != null) {
            dto.setTrackerName(tracker.getName());
        }

        // 填充状态信息
        IssueStatus status = issueStatusMapper.selectById(issue.getStatusId());
        if (status != null) {
            dto.setStatusName(status.getName());
        }

        // 填充创建者信息
        User author = userMapper.selectById(issue.getAuthorId());
        if (author != null) {
            dto.setAuthorName(author.getLogin());
        }

        // 填充指派人信息
        if (issue.getAssignedToId() != null) {
            User assignedUser = userMapper.selectById(issue.getAssignedToId());
            if (assignedUser != null) {
                dto.setAssignedToName(assignedUser.getLogin());
            }
        }

        // TODO: 填充优先级名称、分类名称、版本名称等
        // 需要创建相应的实体和 Mapper
    }
}
