package com.github.jredmine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.jredmine.dto.request.issue.IssueAssignRequestDTO;
import com.github.jredmine.dto.request.issue.IssueCategoryCreateRequestDTO;
import com.github.jredmine.dto.request.issue.IssueCategoryUpdateRequestDTO;
import com.github.jredmine.dto.request.issue.IssueCreateRequestDTO;
import com.github.jredmine.dto.request.issue.IssueListRequestDTO;
import com.github.jredmine.dto.request.issue.IssueRelationCreateRequestDTO;
import com.github.jredmine.dto.request.issue.IssueStatusUpdateRequestDTO;
import com.github.jredmine.dto.request.issue.IssueUpdateRequestDTO;
import com.github.jredmine.dto.request.issue.IssueJournalCreateRequestDTO;
import com.github.jredmine.dto.request.issue.IssueWatcherCreateRequestDTO;
import com.github.jredmine.dto.response.workflow.AvailableTransitionDTO;
import com.github.jredmine.dto.response.workflow.WorkflowTransitionResponseDTO;
import com.github.jredmine.dto.response.PageResponse;
import com.github.jredmine.dto.response.issue.IssueCategoryResponseDTO;
import com.github.jredmine.dto.response.issue.IssueDetailResponseDTO;
import com.github.jredmine.dto.response.issue.IssueJournalResponseDTO;
import com.github.jredmine.dto.response.issue.IssueListItemResponseDTO;
import com.github.jredmine.dto.response.issue.IssueRelationResponseDTO;
import com.github.jredmine.entity.Issue;
import com.github.jredmine.entity.IssueCategory;
import com.github.jredmine.entity.IssueRelation;
import com.github.jredmine.entity.IssueStatus;
import com.github.jredmine.entity.Journal;
import com.github.jredmine.entity.Project;
import com.github.jredmine.entity.Tracker;
import com.github.jredmine.entity.User;
import com.github.jredmine.entity.Watcher;
import com.github.jredmine.enums.ResultCode;
import com.github.jredmine.exception.BusinessException;
import com.github.jredmine.mapper.issue.IssueCategoryMapper;
import com.github.jredmine.mapper.issue.IssueMapper;
import com.github.jredmine.mapper.issue.IssueRelationMapper;
import com.github.jredmine.mapper.issue.JournalMapper;
import com.github.jredmine.mapper.issue.WatcherMapper;
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

import com.github.jredmine.entity.Member;
import com.github.jredmine.entity.MemberRole;
import com.github.jredmine.mapper.project.MemberMapper;
import com.github.jredmine.mapper.user.MemberRoleMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final IssueRelationMapper issueRelationMapper;
    private final IssueCategoryMapper issueCategoryMapper;
    private final JournalMapper journalMapper;
    private final WatcherMapper watcherMapper;
    private final ProjectMapper projectMapper;
    private final TrackerMapper trackerMapper;
    private final IssueStatusMapper issueStatusMapper;
    private final UserMapper userMapper;
    private final MemberMapper memberMapper;
    private final MemberRoleMapper memberRoleMapper;
    private final SecurityUtils securityUtils;
    private final ProjectPermissionService projectPermissionService;
    private final WorkflowService workflowService;

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
                                    .last("LIMIT 1"));
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
                // 验证分类是否存在
                IssueCategory category = issueCategoryMapper.selectById(categoryId);
                if (category == null) {
                    log.warn("任务分类不存在，分类ID: {}", categoryId);
                    throw new BusinessException(ResultCode.SYSTEM_ERROR, "任务分类不存在");
                }
                // 验证分类是否属于该项目
                if (!category.getProjectId().equals(requestDTO.getProjectId().intValue())) {
                    log.warn("任务分类不属于该项目，项目ID: {}, 分类ID: {}, 分类所属项目ID: {}",
                            requestDTO.getProjectId(), categoryId, category.getProjectId());
                    throw new BusinessException(ResultCode.PARAM_INVALID, "任务分类不属于该项目");
                }
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

            // 处理指派人：如果未指定或为0，则自动设置为当前用户
            Long assignedToId = requestDTO.getAssignedToId();
            if (assignedToId == null || assignedToId == 0) {
                // 未指定指派人，自动设置为当前用户
                assignedToId = currentUserId;
                log.info("未指定指派人，自动设置为当前用户，用户ID: {}", currentUserId);
            } else {
                // 验证指定的指派人是否存在
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
            // 设置指派人（已处理：未指定时自动设置为当前用户）
            issue.setAssignedToId(assignedToId);
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
     * 分页查询任务列表
     *
     * @param requestDTO 查询请求参数
     * @return 分页响应
     */
    public PageResponse<IssueListItemResponseDTO> listIssues(IssueListRequestDTO requestDTO) {
        MDC.put("operation", "list_issues");

        try {
            // 设置默认值
            Integer current = requestDTO.getCurrent() != null ? requestDTO.getCurrent() : 1;
            Integer size = requestDTO.getSize() != null ? requestDTO.getSize() : 10;
            String sortOrder = requestDTO.getSortOrder() != null ? requestDTO.getSortOrder() : "desc";

            log.debug("开始查询任务列表，页码: {}, 每页数量: {}", current, size);

            // 获取当前用户信息
            User currentUser = securityUtils.getCurrentUser();
            boolean isAdmin = Boolean.TRUE.equals(currentUser.getAdmin());
            Long currentUserId = currentUser.getId();

            // 获取当前用户是成员的项目ID集合（如果不是管理员）
            final Set<Long> memberProjectIds;
            if (!isAdmin) {
                LambdaQueryWrapper<Member> memberQuery = new LambdaQueryWrapper<>();
                memberQuery.eq(Member::getUserId, currentUserId);
                List<Member> members = memberMapper.selectList(memberQuery);
                memberProjectIds = members.stream()
                        .map(Member::getProjectId)
                        .collect(Collectors.toSet());
            } else {
                memberProjectIds = null;
            }

            // 创建分页对象
            Page<Issue> page = new Page<>(current, size);

            // 构建查询条件
            LambdaQueryWrapper<Issue> queryWrapper = new LambdaQueryWrapper<>();

            // 项目筛选
            if (requestDTO.getProjectId() != null) {
                queryWrapper.eq(Issue::getProjectId, requestDTO.getProjectId());
            }

            // 状态筛选
            if (requestDTO.getStatusId() != null) {
                queryWrapper.eq(Issue::getStatusId, requestDTO.getStatusId());
            }

            // 跟踪器筛选
            if (requestDTO.getTrackerId() != null) {
                queryWrapper.eq(Issue::getTrackerId, requestDTO.getTrackerId());
            }

            // 优先级筛选
            if (requestDTO.getPriorityId() != null) {
                queryWrapper.eq(Issue::getPriorityId, requestDTO.getPriorityId());
            }

            // 指派人筛选
            if (requestDTO.getAssignedToId() != null) {
                if (requestDTO.getAssignedToId() == 0) {
                    // 查询未分配的任务
                    queryWrapper.isNull(Issue::getAssignedToId);
                } else {
                    queryWrapper.eq(Issue::getAssignedToId, requestDTO.getAssignedToId());
                }
            }

            // 创建者筛选
            if (requestDTO.getAuthorId() != null) {
                queryWrapper.eq(Issue::getAuthorId, requestDTO.getAuthorId());
            }

            // 分类筛选
            if (requestDTO.getCategoryId() != null) {
                if (requestDTO.getCategoryId() == 0) {
                    // 查询无分类的任务
                    queryWrapper.isNull(Issue::getCategoryId);
                } else {
                    queryWrapper.eq(Issue::getCategoryId, requestDTO.getCategoryId());
                }
            }

            // 版本筛选
            if (requestDTO.getFixedVersionId() != null) {
                if (requestDTO.getFixedVersionId() == 0) {
                    // 查询无版本的任务
                    queryWrapper.isNull(Issue::getFixedVersionId);
                } else {
                    queryWrapper.eq(Issue::getFixedVersionId, requestDTO.getFixedVersionId());
                }
            }

            // 关键词搜索（在标题和描述中搜索）
            if (requestDTO.getKeyword() != null && !requestDTO.getKeyword().trim().isEmpty()) {
                queryWrapper.and(wrapper -> {
                    wrapper.like(Issue::getSubject, requestDTO.getKeyword().trim())
                            .or()
                            .like(Issue::getDescription, requestDTO.getKeyword().trim());
                });
            }

            // 是否私有筛选
            if (requestDTO.getIsPrivate() != null) {
                queryWrapper.eq(Issue::getIsPrivate, requestDTO.getIsPrivate());
            }

            // 权限过滤：如果不是管理员，只显示用户有权限查看的任务
            if (!isAdmin) {
                final Set<Long> finalMemberProjectIds = memberProjectIds;
                queryWrapper.and(wrapper -> {
                    // 私有任务：必须是项目成员
                    wrapper.and(w -> w.eq(Issue::getIsPrivate, true)
                            .in(finalMemberProjectIds != null && !finalMemberProjectIds.isEmpty(),
                                    Issue::getProjectId, finalMemberProjectIds))
                            .or()
                            // 非私有任务：项目成员可见
                            .and(w -> w.eq(Issue::getIsPrivate, false)
                                    .in(finalMemberProjectIds != null && !finalMemberProjectIds.isEmpty(),
                                            Issue::getProjectId, finalMemberProjectIds));
                });
            }

            // 排序
            if (requestDTO.getSortBy() != null && !requestDTO.getSortBy().trim().isEmpty()) {
                String sortField = requestDTO.getSortBy().trim().toLowerCase();
                boolean ascending = "asc".equalsIgnoreCase(sortOrder);
                switch (sortField) {
                    case "created_on":
                    case "updated_on":
                        // 统一使用 id 排序
                        if (ascending) {
                            queryWrapper.orderByAsc(Issue::getId);
                        } else {
                            queryWrapper.orderByDesc(Issue::getId);
                        }
                        break;
                    case "priority":
                    case "priority_id":
                        if (ascending) {
                            queryWrapper.orderByAsc(Issue::getPriorityId);
                        } else {
                            queryWrapper.orderByDesc(Issue::getPriorityId);
                        }
                        break;
                    case "due_date":
                        if (ascending) {
                            queryWrapper.orderByAsc(Issue::getDueDate);
                        } else {
                            queryWrapper.orderByDesc(Issue::getDueDate);
                        }
                        break;
                    default:
                        // 默认按 ID 倒序
                        queryWrapper.orderByDesc(Issue::getId);
                        break;
                }
            } else {
                // 默认按 ID 倒序
                queryWrapper.orderByDesc(Issue::getId);
            }

            // 执行分页查询
            Page<Issue> result = issueMapper.selectPage(page, queryWrapper);

            MDC.put("total", String.valueOf(result.getTotal()));
            log.info("任务列表查询成功，共查询到 {} 条记录", result.getTotal());

            // 转换为响应 DTO
            List<IssueListItemResponseDTO> dtoList = result.getRecords().stream()
                    .map(this::toIssueListItemResponseDTO)
                    .toList();

            return PageResponse.of(
                    dtoList,
                    (int) result.getTotal(),
                    (int) result.getCurrent(),
                    (int) result.getSize());
        } catch (Exception e) {
            log.error("任务列表查询失败", e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "任务列表查询失败");
        } finally {
            MDC.clear();
        }
    }

    /**
     * 将 Issue 实体转换为 IssueListItemResponseDTO
     *
     * @param issue 任务实体
     * @return 响应 DTO
     */
    private IssueListItemResponseDTO toIssueListItemResponseDTO(Issue issue) {
        IssueListItemResponseDTO dto = new IssueListItemResponseDTO();
        dto.setId(issue.getId());
        dto.setTrackerId(issue.getTrackerId());
        dto.setProjectId(issue.getProjectId());
        dto.setSubject(issue.getSubject());
        dto.setStatusId(issue.getStatusId());
        dto.setPriorityId(issue.getPriorityId());
        dto.setAssignedToId(issue.getAssignedToId());
        dto.setAuthorId(issue.getAuthorId());
        dto.setCreatedOn(issue.getCreatedOn());
        dto.setUpdatedOn(issue.getUpdatedOn());
        dto.setDueDate(issue.getDueDate());
        dto.setDoneRatio(issue.getDoneRatio());
        dto.setIsPrivate(issue.getIsPrivate());

        // 填充关联信息
        fillListItemRelatedInfo(dto, issue);

        return dto;
    }

    /**
     * 填充列表项关联信息（项目名称、跟踪器名称、状态名称等）
     */
    private void fillListItemRelatedInfo(IssueListItemResponseDTO dto, Issue issue) {
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

        // TODO: 填充优先级名称
        // 需要创建相应的实体和 Mapper
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

    /**
     * 更新任务
     *
     * @param id         任务ID
     * @param requestDTO 更新任务请求
     * @return 任务详情
     */
    @Transactional(rollbackFor = Exception.class)
    public IssueDetailResponseDTO updateIssue(Long id, IssueUpdateRequestDTO requestDTO) {
        MDC.put("operation", "update_issue");
        MDC.put("issueId", String.valueOf(id));

        try {
            log.info("开始更新任务，任务ID: {}", id);

            // 查询任务是否存在
            Issue issue = issueMapper.selectById(id);
            if (issue == null) {
                log.warn("任务不存在，任务ID: {}", id);
                throw new BusinessException(ResultCode.SYSTEM_ERROR, "任务不存在");
            }

            // 权限验证：需要 edit_issues 权限或系统管理员
            User currentUser = securityUtils.getCurrentUser();
            Long currentUserId = currentUser.getId();
            boolean isAdmin = Boolean.TRUE.equals(currentUser.getAdmin());
            if (!isAdmin) {
                if (!projectPermissionService.hasPermission(currentUserId, issue.getProjectId(), "edit_issues")) {
                    log.warn("用户无权限更新任务，任务ID: {}, 用户ID: {}", id, currentUserId);
                    throw new BusinessException(ResultCode.FORBIDDEN, "无权限更新任务，需要 edit_issues 权限");
                }
            }

            // 乐观锁检查
            if (requestDTO.getLockVersion() != null && !requestDTO.getLockVersion().equals(issue.getLockVersion())) {
                log.warn("任务已被其他用户修改，任务ID: {}, 当前版本: {}, 请求版本: {}",
                        id, issue.getLockVersion(), requestDTO.getLockVersion());
                throw new BusinessException(ResultCode.SYSTEM_ERROR, "任务已被其他用户修改，请刷新后重试");
            }

            // 处理状态更新（如果提供）
            if (requestDTO.getStatusId() != null && !requestDTO.getStatusId().equals(issue.getStatusId())) {
                Integer newStatusId = requestDTO.getStatusId();

                // 验证新状态是否存在
                IssueStatus newStatus = issueStatusMapper.selectById(newStatusId);
                if (newStatus == null) {
                    log.warn("任务状态不存在，状态ID: {}", newStatusId);
                    throw new BusinessException(ResultCode.SYSTEM_ERROR, "任务状态不存在");
                }

                // TODO: 验证工作流规则（状态转换是否允许）
                // 需要检查用户角色、指派人限制、创建者限制等
                // 暂时跳过，后续实现完整的工作流验证

                // 如果新状态是关闭状态，自动设置关闭时间和完成度
                if (Boolean.TRUE.equals(newStatus.getIsClosed())) {
                    issue.setClosedOn(LocalDateTime.now());
                    if (issue.getDoneRatio() == null || issue.getDoneRatio() < 100) {
                        issue.setDoneRatio(100);
                    }
                } else {
                    // 如果从关闭状态转换到非关闭状态，清除关闭时间
                    if (Boolean.TRUE.equals(issue.getStatusId() != null)) {
                        IssueStatus oldStatus = issueStatusMapper.selectById(issue.getStatusId());
                        if (oldStatus != null && Boolean.TRUE.equals(oldStatus.getIsClosed())) {
                            issue.setClosedOn(null);
                        }
                    }
                }

                issue.setStatusId(newStatusId);
            }

            // 处理指派人更新（如果提供）
            Long assignedToId = requestDTO.getAssignedToId();
            if (assignedToId != null) {
                if (assignedToId == 0) {
                    // 取消分配
                    issue.setAssignedToId(null);
                } else {
                    // 验证指派人是否存在
                    User assignedUser = userMapper.selectById(assignedToId);
                    if (assignedUser == null) {
                        log.warn("指派人不存在，用户ID: {}", assignedToId);
                        throw new BusinessException(ResultCode.USER_NOT_FOUND);
                    }
                    issue.setAssignedToId(assignedToId);
                }
            }

            // 处理父任务更新（如果提供）
            Long parentId = requestDTO.getParentId();
            if (parentId != null) {
                if (parentId == 0) {
                    // 取消父任务
                    issue.setParentId(null);
                } else {
                    // 验证父任务是否存在
                    Issue parentIssue = issueMapper.selectById(parentId);
                    if (parentIssue == null) {
                        log.warn("父任务不存在，父任务ID: {}", parentId);
                        throw new BusinessException(ResultCode.SYSTEM_ERROR, "父任务不存在");
                    }
                    // 验证父任务是否属于同一项目
                    if (!parentIssue.getProjectId().equals(issue.getProjectId())) {
                        log.warn("父任务不属于同一项目，父任务ID: {}, 项目ID: {}", parentId, issue.getProjectId());
                        throw new BusinessException(ResultCode.SYSTEM_ERROR, "父任务必须属于同一项目");
                    }
                    // 不能将任务设置为自己的父任务
                    if (parentId.equals(id)) {
                        log.warn("不能将任务设置为自己的父任务，任务ID: {}", id);
                        throw new BusinessException(ResultCode.SYSTEM_ERROR, "不能将任务设置为自己的父任务");
                    }
                    issue.setParentId(parentId);
                }
            }

            // 处理分类更新（如果提供）
            Integer categoryId = requestDTO.getCategoryId();
            if (categoryId != null) {
                if (categoryId == 0) {
                    // 取消分类
                    issue.setCategoryId(null);
                } else {
                    // 验证分类是否存在
                    IssueCategory category = issueCategoryMapper.selectById(categoryId);
                    if (category == null) {
                        log.warn("任务分类不存在，分类ID: {}", categoryId);
                        throw new BusinessException(ResultCode.SYSTEM_ERROR, "任务分类不存在");
                    }
                    // 验证分类是否属于该项目
                    if (!category.getProjectId().equals(issue.getProjectId().intValue())) {
                        log.warn("任务分类不属于该项目，项目ID: {}, 分类ID: {}, 分类所属项目ID: {}",
                                issue.getProjectId(), categoryId, category.getProjectId());
                        throw new BusinessException(ResultCode.PARAM_INVALID, "任务分类不属于该项目");
                    }
                    issue.setCategoryId(categoryId);
                }
            }

            // 处理版本更新（如果提供）
            Long fixedVersionId = requestDTO.getFixedVersionId();
            if (fixedVersionId != null) {
                issue.setFixedVersionId((fixedVersionId == 0) ? null : fixedVersionId);
            }

            // 更新其他字段
            if (requestDTO.getSubject() != null) {
                issue.setSubject(requestDTO.getSubject());
            }
            if (requestDTO.getDescription() != null) {
                issue.setDescription(requestDTO.getDescription());
            }
            if (requestDTO.getPriorityId() != null) {
                issue.setPriorityId(requestDTO.getPriorityId());
            }
            if (requestDTO.getStartDate() != null) {
                issue.setStartDate(requestDTO.getStartDate());
            }
            if (requestDTO.getDueDate() != null) {
                issue.setDueDate(requestDTO.getDueDate());
            }
            if (requestDTO.getEstimatedHours() != null) {
                issue.setEstimatedHours(requestDTO.getEstimatedHours());
            }
            if (requestDTO.getDoneRatio() != null) {
                if (requestDTO.getDoneRatio() < 0 || requestDTO.getDoneRatio() > 100) {
                    log.warn("完成度无效，完成度: {}", requestDTO.getDoneRatio());
                    throw new BusinessException(ResultCode.PARAM_INVALID, "完成度必须在 0-100 之间");
                }
                issue.setDoneRatio(requestDTO.getDoneRatio());
            }
            if (requestDTO.getIsPrivate() != null) {
                issue.setIsPrivate(requestDTO.getIsPrivate());
            }

            // 更新乐观锁版本号和更新时间
            issue.setLockVersion(issue.getLockVersion() + 1);
            issue.setUpdatedOn(LocalDateTime.now());

            // 保存任务
            int updateResult = issueMapper.updateById(issue);
            if (updateResult <= 0) {
                log.error("任务更新失败，更新数据库失败");
                throw new BusinessException(ResultCode.SYSTEM_ERROR, "任务更新失败");
            }

            log.info("任务更新成功，任务ID: {}", id);

            // TODO: 记录变更历史到 journals 表
            // 暂时跳过，后续实现

            // 查询更新后的任务（包含关联信息）
            return getIssueDetailById(id);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("任务更新失败，任务ID: {}", id, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "任务更新失败");
        } finally {
            MDC.clear();
        }
    }

    /**
     * 删除任务
     *
     * @param id 任务ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteIssue(Long id) {
        MDC.put("operation", "delete_issue");
        MDC.put("issueId", String.valueOf(id));

        try {
            log.info("开始删除任务，任务ID: {}", id);

            // 查询任务是否存在
            Issue issue = issueMapper.selectById(id);
            if (issue == null) {
                log.warn("任务不存在，任务ID: {}", id);
                throw new BusinessException(ResultCode.SYSTEM_ERROR, "任务不存在");
            }

            // 获取当前用户信息
            User currentUser = securityUtils.getCurrentUser();
            Long currentUserId = currentUser.getId();
            boolean isAdmin = Boolean.TRUE.equals(currentUser.getAdmin());

            // 权限验证：需要 delete_issues 权限或系统管理员
            if (!isAdmin) {
                // 检查用户在项目中是否拥有 delete_issues 权限
                if (!projectPermissionService.hasPermission(currentUserId, issue.getProjectId(), "delete_issues")) {
                    log.warn("用户无权限删除任务，任务ID: {}, 项目ID: {}, 用户ID: {}", id, issue.getProjectId(), currentUserId);
                    throw new BusinessException(ResultCode.FORBIDDEN, "无权限删除任务，需要 delete_issues 权限");
                }
            }

            // 检查是否有子任务
            LambdaQueryWrapper<Issue> childrenQuery = new LambdaQueryWrapper<>();
            childrenQuery.eq(Issue::getParentId, id);
            Long childrenCount = issueMapper.selectCount(childrenQuery);
            if (childrenCount > 0) {
                log.warn("任务存在子任务，不能删除，任务ID: {}, 子任务数量: {}", id, childrenCount);
                throw new BusinessException(ResultCode.PARAM_INVALID,
                        "任务存在 " + childrenCount + " 个子任务，请先删除子任务");
            }

            // TODO: 删除任务关联关系（issue_relations 表）
            // 暂时跳过，后续实现任务关联功能时再处理

            // 物理删除任务
            int deleteResult = issueMapper.deleteById(id);
            if (deleteResult <= 0) {
                log.error("任务删除失败，删除数据库失败，任务ID: {}", id);
                throw new BusinessException(ResultCode.SYSTEM_ERROR, "任务删除失败");
            }

            log.info("任务删除成功，任务ID: {}, 任务标题: {}", id, issue.getSubject());

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("任务删除失败，任务ID: {}", id, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "任务删除失败");
        } finally {
            MDC.clear();
        }
    }

    /**
     * 获取用户在项目中的角色ID列表
     *
     * @param userId    用户ID
     * @param projectId 项目ID
     * @return 角色ID列表
     */
    private List<Integer> getUserProjectRoleIds(Long userId, Long projectId) {
        // 查询用户是否是项目成员
        LambdaQueryWrapper<Member> memberQuery = new LambdaQueryWrapper<>();
        memberQuery.eq(Member::getUserId, userId)
                .eq(Member::getProjectId, projectId);
        Member member = memberMapper.selectOne(memberQuery);

        if (member == null) {
            return new ArrayList<>();
        }

        // 查询成员的所有角色
        LambdaQueryWrapper<MemberRole> memberRoleQuery = new LambdaQueryWrapper<>();
        memberRoleQuery.eq(MemberRole::getMemberId, member.getId().intValue());
        List<MemberRole> memberRoles = memberRoleMapper.selectList(memberRoleQuery);

        if (memberRoles.isEmpty()) {
            return new ArrayList<>();
        }

        // 获取所有角色ID
        return memberRoles.stream()
                .map(MemberRole::getRoleId)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 更新任务状态（完整支持工作流验证和备注功能）
     *
     * @param id         任务ID
     * @param requestDTO 更新状态请求
     * @return 任务详情
     */
    @Transactional(rollbackFor = Exception.class)
    public IssueDetailResponseDTO updateIssueStatus(Long id, IssueStatusUpdateRequestDTO requestDTO) {
        MDC.put("operation", "update_issue_status");
        MDC.put("issueId", String.valueOf(id));
        MDC.put("newStatusId", String.valueOf(requestDTO.getStatusId()));

        try {
            log.info("开始更新任务状态，任务ID: {}, 新状态ID: {}", id, requestDTO.getStatusId());

            // 查询任务是否存在
            Issue issue = issueMapper.selectById(id);
            if (issue == null) {
                log.warn("任务不存在，任务ID: {}", id);
                throw new BusinessException(ResultCode.SYSTEM_ERROR, "任务不存在");
            }

            // 获取当前用户信息
            User currentUser = securityUtils.getCurrentUser();
            Long currentUserId = currentUser.getId();
            boolean isAdmin = Boolean.TRUE.equals(currentUser.getAdmin());

            // 权限验证：需要 edit_issues 权限或系统管理员
            if (!isAdmin) {
                if (!projectPermissionService.hasPermission(currentUserId, issue.getProjectId(), "edit_issues")) {
                    log.warn("用户无权限更新任务状态，任务ID: {}, 项目ID: {}, 用户ID: {}", id, issue.getProjectId(), currentUserId);
                    throw new BusinessException(ResultCode.FORBIDDEN, "无权限更新任务状态，需要 edit_issues 权限");
                }
            }

            // 乐观锁检查
            if (requestDTO.getLockVersion() != null && !requestDTO.getLockVersion().equals(issue.getLockVersion())) {
                log.warn("任务已被其他用户修改，任务ID: {}, 当前版本: {}, 请求版本: {}",
                        id, issue.getLockVersion(), requestDTO.getLockVersion());
                throw new BusinessException(ResultCode.SYSTEM_ERROR, "任务已被其他用户修改，请刷新后重试");
            }

            // 验证新状态是否存在
            Integer newStatusId = requestDTO.getStatusId();
            IssueStatus newStatus = issueStatusMapper.selectById(newStatusId);
            if (newStatus == null) {
                log.warn("任务状态不存在，状态ID: {}", newStatusId);
                throw new BusinessException(ResultCode.SYSTEM_ERROR, "任务状态不存在");
            }

            // 如果状态没有变化，直接返回
            if (newStatusId.equals(issue.getStatusId())) {
                log.info("任务状态未变化，任务ID: {}, 状态ID: {}", id, newStatusId);
                return getIssueDetailById(id);
            }

            // 工作流验证：检查状态转换是否允许
            // 获取用户在项目中的角色ID列表
            List<Integer> userRoleIds = getUserProjectRoleIds(currentUserId, issue.getProjectId());

            // 获取可用的状态转换
            WorkflowTransitionResponseDTO availableTransitions = workflowService.getAvailableTransitions(
                    issue.getTrackerId(),
                    issue.getStatusId(),
                    userRoleIds.isEmpty() ? null : userRoleIds);

            // 检查新状态是否在可用转换列表中
            boolean isTransitionAllowed = false;
            AvailableTransitionDTO targetTransition = null;
            for (AvailableTransitionDTO transition : availableTransitions.getAvailableTransitions()) {
                if (transition.getStatusId().equals(newStatusId)) {
                    isTransitionAllowed = true;
                    targetTransition = transition;
                    break;
                }
            }

            if (!isTransitionAllowed) {
                log.warn("状态转换不允许，任务ID: {}, 当前状态ID: {}, 目标状态ID: {}, 用户角色IDs: {}",
                        id, issue.getStatusId(), newStatusId, userRoleIds);
                throw new BusinessException(ResultCode.PARAM_INVALID,
                        "不允许从状态 \"" + availableTransitions.getCurrentStatusName() + "\" 转换到状态 \"" + newStatus.getName()
                                + "\"，请检查工作流规则");
            }

            // 验证指派人限制
            if (Boolean.TRUE.equals(targetTransition.getAssignee())) {
                if (issue.getAssignedToId() == null || !issue.getAssignedToId().equals(currentUserId)) {
                    log.warn("状态转换需要指派人权限，任务ID: {}, 指派人ID: {}, 当前用户ID: {}",
                            id, issue.getAssignedToId(), currentUserId);
                    throw new BusinessException(ResultCode.FORBIDDEN, "只有任务的指派人可以执行此状态转换");
                }
            }

            // 验证创建者限制
            if (Boolean.TRUE.equals(targetTransition.getAuthor())) {
                if (issue.getAuthorId() == null || !issue.getAuthorId().equals(currentUserId)) {
                    log.warn("状态转换需要创建者权限，任务ID: {}, 创建者ID: {}, 当前用户ID: {}",
                            id, issue.getAuthorId(), currentUserId);
                    throw new BusinessException(ResultCode.FORBIDDEN, "只有任务的创建者可以执行此状态转换");
                }
            }

            // TODO: 验证字段规则（必填、只读等）
            // 暂时跳过，后续实现完整的字段规则验证

            // 更新状态
            Integer oldStatusId = issue.getStatusId();
            issue.setStatusId(newStatusId);

            // 如果新状态是关闭状态，自动设置关闭时间和完成度
            if (Boolean.TRUE.equals(newStatus.getIsClosed())) {
                issue.setClosedOn(LocalDateTime.now());
                if (issue.getDoneRatio() == null || issue.getDoneRatio() < 100) {
                    issue.setDoneRatio(100);
                }
            } else {
                // 如果从关闭状态转换到非关闭状态，清除关闭时间
                if (oldStatusId != null) {
                    IssueStatus oldStatus = issueStatusMapper.selectById(oldStatusId);
                    if (oldStatus != null && Boolean.TRUE.equals(oldStatus.getIsClosed())) {
                        issue.setClosedOn(null);
                    }
                }
            }

            // 更新乐观锁版本号和更新时间
            issue.setLockVersion(issue.getLockVersion() + 1);
            issue.setUpdatedOn(LocalDateTime.now());

            // 保存任务
            int updateResult = issueMapper.updateById(issue);
            if (updateResult <= 0) {
                log.error("任务状态更新失败，更新数据库失败，任务ID: {}", id);
                throw new BusinessException(ResultCode.SYSTEM_ERROR, "任务状态更新失败");
            }

            log.info("任务状态更新成功，任务ID: {}, 旧状态ID: {}, 新状态ID: {}", id, oldStatusId, newStatusId);

            // TODO: 记录状态变更历史到 journals 表
            // 包括备注信息（requestDTO.getNotes()）
            // 暂时跳过，后续实现

            // 查询更新后的任务（包含关联信息）
            return getIssueDetailById(id);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("任务状态更新失败，任务ID: {}", id, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "任务状态更新失败");
        } finally {
            MDC.clear();
        }
    }

    /**
     * 分配任务
     *
     * @param id         任务ID
     * @param requestDTO 分配任务请求
     * @return 任务详情
     */
    @Transactional(rollbackFor = Exception.class)
    public IssueDetailResponseDTO assignIssue(Long id, IssueAssignRequestDTO requestDTO) {
        MDC.put("operation", "assign_issue");
        MDC.put("issueId", String.valueOf(id));
        if (requestDTO.getAssignedToId() != null) {
            MDC.put("assignedToId", String.valueOf(requestDTO.getAssignedToId()));
        }

        try {
            log.info("开始分配任务，任务ID: {}, 指派人ID: {}", id, requestDTO.getAssignedToId());

            // 查询任务是否存在
            Issue issue = issueMapper.selectById(id);
            if (issue == null) {
                log.warn("任务不存在，任务ID: {}", id);
                throw new BusinessException(ResultCode.SYSTEM_ERROR, "任务不存在");
            }

            // 获取当前用户信息
            User currentUser = securityUtils.getCurrentUser();
            Long currentUserId = currentUser.getId();
            boolean isAdmin = Boolean.TRUE.equals(currentUser.getAdmin());

            // 权限验证：需要 edit_issues 权限或系统管理员
            if (!isAdmin) {
                if (!projectPermissionService.hasPermission(currentUserId, issue.getProjectId(), "edit_issues")) {
                    log.warn("用户无权限分配任务，任务ID: {}, 项目ID: {}, 用户ID: {}", id, issue.getProjectId(), currentUserId);
                    throw new BusinessException(ResultCode.FORBIDDEN, "无权限分配任务，需要 edit_issues 权限");
                }
            }

            // 乐观锁检查
            if (requestDTO.getLockVersion() != null && !requestDTO.getLockVersion().equals(issue.getLockVersion())) {
                log.warn("任务已被其他用户修改，任务ID: {}, 当前版本: {}, 请求版本: {}",
                        id, issue.getLockVersion(), requestDTO.getLockVersion());
                throw new BusinessException(ResultCode.SYSTEM_ERROR, "任务已被其他用户修改，请刷新后重试");
            }

            // 处理指派人更新
            Long assignedToId = requestDTO.getAssignedToId();
            Long oldAssignedToId = issue.getAssignedToId();

            if (assignedToId == null || assignedToId == 0) {
                // 取消分配
                if (oldAssignedToId == null) {
                    log.info("任务已经是未分配状态，任务ID: {}", id);
                    return getIssueDetailById(id);
                }
                issue.setAssignedToId(null);
                log.info("取消任务分配，任务ID: {}, 原指派人ID: {}", id, oldAssignedToId);
            } else {
                // 验证指派人是否存在
                User assignedUser = userMapper.selectById(assignedToId);
                if (assignedUser == null) {
                    log.warn("指派人不存在，用户ID: {}", assignedToId);
                    throw new BusinessException(ResultCode.USER_NOT_FOUND, "指派人不存在");
                }

                // 如果指派人没有变化，直接返回
                if (assignedToId.equals(oldAssignedToId)) {
                    log.info("任务指派人未变化，任务ID: {}, 指派人ID: {}", id, assignedToId);
                    return getIssueDetailById(id);
                }

                issue.setAssignedToId(assignedToId);
                log.info("分配任务，任务ID: {}, 原指派人ID: {}, 新指派人ID: {}", id, oldAssignedToId, assignedToId);
            }

            // 更新乐观锁版本号和更新时间
            issue.setLockVersion(issue.getLockVersion() + 1);
            issue.setUpdatedOn(LocalDateTime.now());

            // 保存任务
            int updateResult = issueMapper.updateById(issue);
            if (updateResult <= 0) {
                log.error("任务分配失败，更新数据库失败，任务ID: {}", id);
                throw new BusinessException(ResultCode.SYSTEM_ERROR, "任务分配失败");
            }

            log.info("任务分配成功，任务ID: {}", id);

            // TODO: 记录分配变更历史到 journals 表
            // 包括原指派人、新指派人信息
            // 暂时跳过，后续实现

            // TODO: 发送通知给新指派人
            // 如果指派人发生变化，发送邮件通知
            // 暂时跳过，后续实现

            // 查询更新后的任务（包含关联信息）
            return getIssueDetailById(id);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("任务分配失败，任务ID: {}", id, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "任务分配失败");
        } finally {
            MDC.clear();
        }
    }

    /**
     * 获取任务的子任务列表
     *
     * @param id        任务ID
     * @param recursive 是否递归查询（包含子任务的子任务）
     * @return 子任务列表
     */
    public List<IssueListItemResponseDTO> getIssueChildren(Long id, Boolean recursive) {
        MDC.put("operation", "get_issue_children");
        MDC.put("issueId", String.valueOf(id));
        MDC.put("recursive", String.valueOf(recursive != null && recursive));

        try {
            log.info("开始查询任务子任务列表，任务ID: {}, 递归查询: {}", id, recursive);

            // 查询父任务是否存在
            Issue parentIssue = issueMapper.selectById(id);
            if (parentIssue == null) {
                log.warn("任务不存在，任务ID: {}", id);
                throw new BusinessException(ResultCode.SYSTEM_ERROR, "任务不存在");
            }

            // 权限验证：需要 view_issues 权限或系统管理员
            User currentUser = securityUtils.getCurrentUser();
            Long currentUserId = currentUser.getId();
            boolean isAdmin = Boolean.TRUE.equals(currentUser.getAdmin());
            if (!isAdmin) {
                if (!projectPermissionService.hasPermission(currentUserId, parentIssue.getProjectId(), "view_issues")) {
                    log.warn("用户无权限查看任务，任务ID: {}, 用户ID: {}", id, currentUserId);
                    throw new BusinessException(ResultCode.FORBIDDEN, "无权限查看任务，需要 view_issues 权限");
                }
            }

            // 查询子任务
            List<Issue> children = getChildrenRecursive(id, recursive != null && recursive);

            // 权限过滤：私有任务仅项目成员可见
            List<Issue> filteredChildren = filterPrivateIssues(children, currentUserId, parentIssue.getProjectId(),
                    isAdmin);

            // 转换为响应 DTO
            List<IssueListItemResponseDTO> dtoList = filteredChildren.stream()
                    .map(this::toIssueListItemResponseDTO)
                    .toList();

            log.info("任务子任务列表查询成功，任务ID: {}, 子任务数量: {}", id, dtoList.size());
            return dtoList;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("任务子任务列表查询失败，任务ID: {}", id, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "任务子任务列表查询失败");
        } finally {
            MDC.clear();
        }
    }

    /**
     * 递归获取子任务
     *
     * @param parentId  父任务ID
     * @param recursive 是否递归查询
     * @return 子任务列表
     */
    private List<Issue> getChildrenRecursive(Long parentId, boolean recursive) {
        List<Issue> allChildren = new ArrayList<>();

        // 查询直接子任务
        LambdaQueryWrapper<Issue> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Issue::getParentId, parentId);
        queryWrapper.orderByAsc(Issue::getId);
        List<Issue> directChildren = issueMapper.selectList(queryWrapper);

        allChildren.addAll(directChildren);

        // 如果递归查询，继续查询子任务的子任务
        if (recursive) {
            for (Issue child : directChildren) {
                List<Issue> grandchildren = getChildrenRecursive(child.getId(), true);
                allChildren.addAll(grandchildren);
            }
        }

        return allChildren;
    }

    /**
     * 过滤私有任务（私有任务仅项目成员可见）
     *
     * @param issues    任务列表
     * @param userId    当前用户ID
     * @param projectId 项目ID
     * @param isAdmin   是否是管理员
     * @return 过滤后的任务列表
     */
    private List<Issue> filterPrivateIssues(List<Issue> issues, Long userId, Long projectId, boolean isAdmin) {
        if (isAdmin) {
            // 管理员可以看到所有任务
            return issues;
        }

        // 检查用户是否是项目成员
        LambdaQueryWrapper<Member> memberQuery = new LambdaQueryWrapper<>();
        memberQuery.eq(Member::getUserId, userId)
                .eq(Member::getProjectId, projectId);
        Member member = memberMapper.selectOne(memberQuery);
        boolean isProjectMember = member != null;

        // 过滤私有任务
        return issues.stream()
                .filter(issue -> {
                    // 公开任务或项目成员可以看到私有任务
                    if (Boolean.TRUE.equals(issue.getIsPrivate())) {
                        return isProjectMember;
                    }
                    return true;
                })
                .toList();
    }

    /**
     * 创建任务关联
     *
     * @param issueId    源任务ID
     * @param requestDTO 创建关联请求
     * @return 任务关联响应
     */
    @Transactional(rollbackFor = Exception.class)
    public IssueRelationResponseDTO createIssueRelation(Long issueId, IssueRelationCreateRequestDTO requestDTO) {
        MDC.put("operation", "create_issue_relation");
        MDC.put("issueId", String.valueOf(issueId));
        MDC.put("targetIssueId", String.valueOf(requestDTO.getTargetIssueId()));
        MDC.put("relationType", requestDTO.getRelationType());

        try {
            log.info("开始创建任务关联，源任务ID: {}, 目标任务ID: {}, 关联类型: {}",
                    issueId, requestDTO.getTargetIssueId(), requestDTO.getRelationType());

            // 查询源任务是否存在
            Issue sourceIssue = issueMapper.selectById(issueId);
            if (sourceIssue == null) {
                log.warn("源任务不存在，任务ID: {}", issueId);
                throw new BusinessException(ResultCode.SYSTEM_ERROR, "源任务不存在");
            }

            // 查询目标任务是否存在
            Issue targetIssue = issueMapper.selectById(requestDTO.getTargetIssueId());
            if (targetIssue == null) {
                log.warn("目标任务不存在，任务ID: {}", requestDTO.getTargetIssueId());
                throw new BusinessException(ResultCode.SYSTEM_ERROR, "目标任务不存在");
            }

            // 不能关联自己
            if (issueId.equals(requestDTO.getTargetIssueId())) {
                log.warn("不能将任务关联到自己，任务ID: {}", issueId);
                throw new BusinessException(ResultCode.PARAM_INVALID, "不能将任务关联到自己");
            }

            // 获取当前用户信息
            User currentUser = securityUtils.getCurrentUser();
            Long currentUserId = currentUser.getId();
            boolean isAdmin = Boolean.TRUE.equals(currentUser.getAdmin());

            // 权限验证：需要 edit_issues 权限或系统管理员
            // 检查源任务的权限
            if (!isAdmin) {
                if (!projectPermissionService.hasPermission(currentUserId, sourceIssue.getProjectId(), "edit_issues")) {
                    log.warn("用户无权限编辑源任务，任务ID: {}, 项目ID: {}, 用户ID: {}",
                            issueId, sourceIssue.getProjectId(), currentUserId);
                    throw new BusinessException(ResultCode.FORBIDDEN, "无权限编辑源任务，需要 edit_issues 权限");
                }
            }

            // 验证关联类型
            String relationType = requestDTO.getRelationType();
            if (relationType == null || relationType.trim().isEmpty()) {
                log.warn("关联类型不能为空");
                throw new BusinessException(ResultCode.PARAM_INVALID, "关联类型不能为空");
            }

            // 验证延迟天数（仅用于 precedes/follows 类型）
            // delay 为 null 或 0 时允许，只有当 delay > 0 且关联类型不是 precedes/follows 时才报错
            Integer delay = requestDTO.getDelay();
            if (delay != null && delay > 0 && !relationType.equals("precedes") && !relationType.equals("follows")) {
                log.warn("延迟天数仅用于 precedes/follows 类型，关联类型: {}, 延迟天数: {}", relationType, delay);
                throw new BusinessException(ResultCode.PARAM_INVALID, "延迟天数仅用于 precedes/follows 类型");
            }

            // 检查是否已存在相同的关联
            LambdaQueryWrapper<IssueRelation> checkQuery = new LambdaQueryWrapper<>();
            checkQuery.eq(IssueRelation::getIssueFromId, issueId.intValue())
                    .eq(IssueRelation::getIssueToId, requestDTO.getTargetIssueId().intValue())
                    .eq(IssueRelation::getRelationType, relationType);
            IssueRelation existingRelation = issueRelationMapper.selectOne(checkQuery);
            if (existingRelation != null) {
                log.warn("任务关联已存在，源任务ID: {}, 目标任务ID: {}, 关联类型: {}",
                        issueId, requestDTO.getTargetIssueId(), relationType);
                throw new BusinessException(ResultCode.PARAM_INVALID, "该任务关联已存在");
            }

            // 创建任务关联
            IssueRelation relation = new IssueRelation();
            relation.setIssueFromId(issueId.intValue());
            relation.setIssueToId(requestDTO.getTargetIssueId().intValue());
            relation.setRelationType(relationType);
            relation.setDelay(delay);

            int insertResult = issueRelationMapper.insert(relation);
            if (insertResult <= 0) {
                log.error("任务关联创建失败，插入数据库失败");
                throw new BusinessException(ResultCode.SYSTEM_ERROR, "任务关联创建失败");
            }

            log.info("任务关联创建成功，关联ID: {}", relation.getId());

            // 构建响应 DTO
            IssueRelationResponseDTO responseDTO = new IssueRelationResponseDTO();
            responseDTO.setId(relation.getId());
            responseDTO.setIssueFromId(issueId);
            responseDTO.setIssueFromSubject(sourceIssue.getSubject());
            responseDTO.setIssueToId(requestDTO.getTargetIssueId());
            responseDTO.setIssueToSubject(targetIssue.getSubject());
            responseDTO.setRelationType(relationType);
            responseDTO.setDelay(delay);

            return responseDTO;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("任务关联创建失败，源任务ID: {}", issueId, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "任务关联创建失败");
        } finally {
            MDC.clear();
        }
    }

    /**
     * 删除任务关联
     *
     * @param issueId    任务ID
     * @param relationId 关联ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteIssueRelation(Long issueId, Integer relationId) {
        MDC.put("operation", "delete_issue_relation");
        MDC.put("issueId", String.valueOf(issueId));
        MDC.put("relationId", String.valueOf(relationId));

        try {
            log.info("开始删除任务关联，任务ID: {}, 关联ID: {}", issueId, relationId);

            // 查询任务是否存在
            Issue issue = issueMapper.selectById(issueId);
            if (issue == null) {
                log.warn("任务不存在，任务ID: {}", issueId);
                throw new BusinessException(ResultCode.SYSTEM_ERROR, "任务不存在");
            }

            // 查询关联是否存在
            IssueRelation relation = issueRelationMapper.selectById(relationId);
            if (relation == null) {
                log.warn("任务关联不存在，关联ID: {}", relationId);
                throw new BusinessException(ResultCode.SYSTEM_ERROR, "任务关联不存在");
            }

            // 验证关联是否属于该任务（源任务或目标任务）
            if (!relation.getIssueFromId().equals(issueId.intValue())
                    && !relation.getIssueToId().equals(issueId.intValue())) {
                log.warn("任务关联不属于该任务，任务ID: {}, 关联ID: {}, 源任务ID: {}, 目标任务ID: {}",
                        issueId, relationId, relation.getIssueFromId(), relation.getIssueToId());
                throw new BusinessException(ResultCode.PARAM_INVALID, "任务关联不属于该任务");
            }

            // 获取当前用户信息
            User currentUser = securityUtils.getCurrentUser();
            Long currentUserId = currentUser.getId();
            boolean isAdmin = Boolean.TRUE.equals(currentUser.getAdmin());

            // 权限验证：需要 edit_issues 权限或系统管理员
            // 检查任务的权限（无论是源任务还是目标任务，都使用当前任务的项目ID）
            if (!isAdmin) {
                if (!projectPermissionService.hasPermission(currentUserId, issue.getProjectId(), "edit_issues")) {
                    log.warn("用户无权限删除任务关联，任务ID: {}, 项目ID: {}, 用户ID: {}",
                            issueId, issue.getProjectId(), currentUserId);
                    throw new BusinessException(ResultCode.FORBIDDEN, "无权限删除任务关联，需要 edit_issues 权限");
                }
            }

            // 删除关联
            int deleteResult = issueRelationMapper.deleteById(relationId);
            if (deleteResult <= 0) {
                log.error("任务关联删除失败，删除数据库失败，关联ID: {}", relationId);
                throw new BusinessException(ResultCode.SYSTEM_ERROR, "任务关联删除失败");
            }

            log.info("任务关联删除成功，任务ID: {}, 关联ID: {}", issueId, relationId);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("任务关联删除失败，任务ID: {}, 关联ID: {}", issueId, relationId, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "任务关联删除失败");
        } finally {
            MDC.clear();
        }
    }

    /**
     * 添加任务关注者
     *
     * @param issueId    任务ID
     * @param requestDTO 添加关注者请求
     */
    @Transactional(rollbackFor = Exception.class)
    public void addIssueWatcher(Long issueId, IssueWatcherCreateRequestDTO requestDTO) {
        MDC.put("operation", "add_issue_watcher");
        MDC.put("issueId", String.valueOf(issueId));
        MDC.put("userId", String.valueOf(requestDTO.getUserId()));

        try {
            log.info("开始添加任务关注者，任务ID: {}, 用户ID: {}", issueId, requestDTO.getUserId());

            // 查询任务是否存在
            Issue issue = issueMapper.selectById(issueId);
            if (issue == null) {
                log.warn("任务不存在，任务ID: {}", issueId);
                throw new BusinessException(ResultCode.SYSTEM_ERROR, "任务不存在");
            }

            // 获取当前用户信息
            User currentUser = securityUtils.getCurrentUser();
            Long currentUserId = currentUser.getId();
            boolean isAdmin = Boolean.TRUE.equals(currentUser.getAdmin());

            // 权限验证：需要 view_issues 权限或系统管理员
            if (!isAdmin) {
                if (!projectPermissionService.hasPermission(currentUserId, issue.getProjectId(), "view_issues")) {
                    log.warn("用户无权限查看任务，任务ID: {}, 项目ID: {}, 用户ID: {}",
                            issueId, issue.getProjectId(), currentUserId);
                    throw new BusinessException(ResultCode.FORBIDDEN, "无权限查看任务，需要 view_issues 权限");
                }
            }

            // 验证要添加的用户是否存在
            User watcherUser = userMapper.selectById(requestDTO.getUserId());
            if (watcherUser == null) {
                log.warn("用户不存在，用户ID: {}", requestDTO.getUserId());
                throw new BusinessException(ResultCode.USER_NOT_FOUND);
            }

            // 检查是否已经是关注者
            LambdaQueryWrapper<Watcher> checkQuery = new LambdaQueryWrapper<>();
            checkQuery.eq(Watcher::getWatchableType, "Issue")
                    .eq(Watcher::getWatchableId, issueId.intValue())
                    .eq(Watcher::getUserId, requestDTO.getUserId().intValue());
            Watcher existingWatcher = watcherMapper.selectOne(checkQuery);
            if (existingWatcher != null) {
                log.info("用户已经是任务关注者，任务ID: {}, 用户ID: {}", issueId, requestDTO.getUserId());
                return; // 已经是关注者，直接返回成功
            }

            // 创建关注者记录
            Watcher watcher = new Watcher();
            watcher.setWatchableType("Issue");
            watcher.setWatchableId(issueId.intValue());
            watcher.setUserId(requestDTO.getUserId().intValue());

            int insertResult = watcherMapper.insert(watcher);
            if (insertResult <= 0) {
                log.error("任务关注者添加失败，插入数据库失败");
                throw new BusinessException(ResultCode.SYSTEM_ERROR, "任务关注者添加失败");
            }

            log.info("任务关注者添加成功，任务ID: {}, 用户ID: {}, 关注者ID: {}",
                    issueId, requestDTO.getUserId(), watcher.getId());

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("任务关注者添加失败，任务ID: {}", issueId, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "任务关注者添加失败");
        } finally {
            MDC.clear();
        }
    }

    /**
     * 删除任务关注者
     *
     * @param issueId 任务ID
     * @param userId  用户ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteIssueWatcher(Long issueId, Long userId) {
        MDC.put("operation", "delete_issue_watcher");
        MDC.put("issueId", String.valueOf(issueId));
        MDC.put("userId", String.valueOf(userId));

        try {
            log.info("开始删除任务关注者，任务ID: {}, 用户ID: {}", issueId, userId);

            // 查询任务是否存在
            Issue issue = issueMapper.selectById(issueId);
            if (issue == null) {
                log.warn("任务不存在，任务ID: {}", issueId);
                throw new BusinessException(ResultCode.SYSTEM_ERROR, "任务不存在");
            }

            // 获取当前用户信息
            User currentUser = securityUtils.getCurrentUser();
            Long currentUserId = currentUser.getId();
            boolean isAdmin = Boolean.TRUE.equals(currentUser.getAdmin());

            // 权限验证：需要 view_issues 权限或系统管理员
            // 如果删除的不是自己的关注，需要 edit_issues 权限
            boolean isDeletingSelf = currentUserId.equals(userId);
            if (!isAdmin) {
                if (!projectPermissionService.hasPermission(currentUserId, issue.getProjectId(), "view_issues")) {
                    log.warn("用户无权限查看任务，任务ID: {}, 项目ID: {}, 用户ID: {}",
                            issueId, issue.getProjectId(), currentUserId);
                    throw new BusinessException(ResultCode.FORBIDDEN, "无权限查看任务，需要 view_issues 权限");
                }

                // 如果删除的不是自己的关注，需要 edit_issues 权限
                if (!isDeletingSelf) {
                    if (!projectPermissionService.hasPermission(currentUserId, issue.getProjectId(), "edit_issues")) {
                        log.warn("用户无权限删除其他用户的关注，任务ID: {}, 项目ID: {}, 用户ID: {}, 目标用户ID: {}",
                                issueId, issue.getProjectId(), currentUserId, userId);
                        throw new BusinessException(ResultCode.FORBIDDEN, "只能删除自己的关注，或需要 edit_issues 权限");
                    }
                }
            }

            // 查询关注者记录是否存在
            LambdaQueryWrapper<Watcher> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Watcher::getWatchableType, "Issue")
                    .eq(Watcher::getWatchableId, issueId.intValue())
                    .eq(Watcher::getUserId, userId.intValue());
            Watcher watcher = watcherMapper.selectOne(queryWrapper);
            if (watcher == null) {
                log.warn("任务关注者不存在，任务ID: {}, 用户ID: {}", issueId, userId);
                throw new BusinessException(ResultCode.SYSTEM_ERROR, "任务关注者不存在");
            }

            // 删除关注者记录
            int deleteResult = watcherMapper.deleteById(watcher.getId());
            if (deleteResult <= 0) {
                log.error("任务关注者删除失败，删除数据库失败，关注者ID: {}", watcher.getId());
                throw new BusinessException(ResultCode.SYSTEM_ERROR, "任务关注者删除失败");
            }

            log.info("任务关注者删除成功，任务ID: {}, 用户ID: {}", issueId, userId);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("任务关注者删除失败，任务ID: {}, 用户ID: {}", issueId, userId, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "任务关注者删除失败");
        } finally {
            MDC.clear();
        }
    }

    /**
     * 创建任务评论
     *
     * @param issueId    任务ID
     * @param requestDTO 创建评论请求
     * @return 评论响应
     */
    @Transactional(rollbackFor = Exception.class)
    public IssueJournalResponseDTO createIssueJournal(Long issueId, IssueJournalCreateRequestDTO requestDTO) {
        MDC.put("operation", "create_issue_journal");
        MDC.put("issueId", String.valueOf(issueId));

        try {
            log.info("开始创建任务评论，任务ID: {}", issueId);

            // 查询任务是否存在
            Issue issue = issueMapper.selectById(issueId);
            if (issue == null) {
                log.warn("任务不存在，任务ID: {}", issueId);
                throw new BusinessException(ResultCode.SYSTEM_ERROR, "任务不存在");
            }

            // 获取当前用户信息
            User currentUser = securityUtils.getCurrentUser();
            Long currentUserId = currentUser.getId();
            boolean isAdmin = Boolean.TRUE.equals(currentUser.getAdmin());

            // 权限验证：需要 add_notes 权限或系统管理员
            // 如果没有 add_notes 权限，可以使用 edit_issues 权限（兼容处理）
            if (!isAdmin) {
                if (!projectPermissionService.hasPermission(currentUserId, issue.getProjectId(), "add_notes")) {
                    // 如果没有 add_notes 权限，尝试使用 edit_issues 权限
                    if (!projectPermissionService.hasPermission(currentUserId, issue.getProjectId(), "edit_issues")) {
                        log.warn("用户无权限添加评论，任务ID: {}, 项目ID: {}, 用户ID: {}",
                                issueId, issue.getProjectId(), currentUserId);
                        throw new BusinessException(ResultCode.FORBIDDEN, "无权限添加评论，需要 add_notes 或 edit_issues 权限");
                    }
                }
            }

            // 创建评论实体
            Journal journal = new Journal();
            journal.setJournalizedId(issueId.intValue());
            journal.setJournalizedType("Issue");
            journal.setUserId(currentUserId.intValue());
            journal.setNotes(requestDTO.getNotes());
            journal.setPrivateNotes(requestDTO.getPrivateNotes() != null ? requestDTO.getPrivateNotes() : false);
            journal.setCreatedOn(LocalDateTime.now());
            journal.setUpdatedOn(LocalDateTime.now());

            // 保存评论
            int insertResult = journalMapper.insert(journal);
            if (insertResult <= 0) {
                log.error("任务评论创建失败，插入数据库失败");
                throw new BusinessException(ResultCode.SYSTEM_ERROR, "任务评论创建失败");
            }

            log.info("任务评论创建成功，任务ID: {}, 评论ID: {}", issueId, journal.getId());

            // 构建响应 DTO
            IssueJournalResponseDTO responseDTO = new IssueJournalResponseDTO();
            responseDTO.setId(journal.getId());
            responseDTO.setIssueId(issueId);
            responseDTO.setNotes(journal.getNotes());
            responseDTO.setCreatedOn(journal.getCreatedOn());
            responseDTO.setUpdatedOn(journal.getUpdatedOn());
            responseDTO.setPrivateNotes(journal.getPrivateNotes());
            responseDTO.setUserId(currentUserId);
            responseDTO.setUserName(currentUser.getLogin());

            // 填充更新者信息（如果有）
            if (journal.getUpdatedById() != null) {
                responseDTO.setUpdatedById(journal.getUpdatedById().longValue());
                User updatedBy = userMapper.selectById(journal.getUpdatedById().longValue());
                if (updatedBy != null) {
                    responseDTO.setUpdatedByName(updatedBy.getLogin());
                }
            }

            return responseDTO;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("任务评论创建失败，任务ID: {}", issueId, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "任务评论创建失败");
        } finally {
            MDC.clear();
        }
    }

    /**
     * 创建任务分类
     *
     * @param projectId  项目ID
     * @param requestDTO 创建分类请求
     * @return 任务分类响应
     */
    @Transactional(rollbackFor = Exception.class)
    public IssueCategoryResponseDTO createIssueCategory(Long projectId, IssueCategoryCreateRequestDTO requestDTO) {
        MDC.put("operation", "create_issue_category");
        MDC.put("projectId", String.valueOf(projectId));
        MDC.put("categoryName", requestDTO.getName());

        try {
            log.info("开始创建任务分类，项目ID: {}, 分类名称: {}", projectId, requestDTO.getName());

            // 查询项目是否存在
            Project project = projectMapper.selectById(projectId);
            if (project == null) {
                log.warn("项目不存在，项目ID: {}", projectId);
                throw new BusinessException(ResultCode.PROJECT_NOT_FOUND);
            }

            // 获取当前用户信息
            User currentUser = securityUtils.getCurrentUser();
            Long currentUserId = currentUser.getId();
            boolean isAdmin = Boolean.TRUE.equals(currentUser.getAdmin());

            // 权限验证：需要 manage_categories 权限或系统管理员
            // 注意：manage_categories 权限可能不在 Permission 枚举中，可以使用 manage_projects 权限
            if (!isAdmin) {
                if (!projectPermissionService.hasPermission(currentUserId, projectId, "manage_categories")) {
                    // 如果没有 manage_categories 权限，尝试使用 manage_projects 权限
                    if (!projectPermissionService.hasPermission(currentUserId, projectId, "manage_projects")) {
                        log.warn("用户无权限创建任务分类，项目ID: {}, 用户ID: {}", projectId, currentUserId);
                        throw new BusinessException(ResultCode.FORBIDDEN,
                                "无权限创建任务分类，需要 manage_categories 或 manage_projects 权限");
                    }
                }
            }

            // 验证分类名称在项目中是否已存在
            LambdaQueryWrapper<IssueCategory> checkQuery = new LambdaQueryWrapper<>();
            checkQuery.eq(IssueCategory::getProjectId, projectId.intValue())
                    .eq(IssueCategory::getName, requestDTO.getName().trim());
            IssueCategory existingCategory = issueCategoryMapper.selectOne(checkQuery);
            if (existingCategory != null) {
                log.warn("任务分类名称已存在，项目ID: {}, 分类名称: {}", projectId, requestDTO.getName());
                throw new BusinessException(ResultCode.PARAM_INVALID, "该分类名称已存在");
            }

            // 验证默认指派人是否存在（如果提供且不为0）
            Long assignedToId = requestDTO.getAssignedToId();
            if (assignedToId != null && assignedToId != 0) {
                User assignedUser = userMapper.selectById(assignedToId);
                if (assignedUser == null) {
                    log.warn("默认指派人不存在，用户ID: {}", assignedToId);
                    throw new BusinessException(ResultCode.USER_NOT_FOUND, "默认指派人不存在");
                }
            }

            // 创建任务分类
            IssueCategory category = new IssueCategory();
            category.setProjectId(projectId.intValue());
            category.setName(requestDTO.getName().trim());
            category.setAssignedToId((assignedToId != null && assignedToId != 0) ? assignedToId.intValue() : null);

            int insertResult = issueCategoryMapper.insert(category);
            if (insertResult <= 0) {
                log.error("任务分类创建失败，插入数据库失败");
                throw new BusinessException(ResultCode.SYSTEM_ERROR, "任务分类创建失败");
            }

            log.info("任务分类创建成功，分类ID: {}, 分类名称: {}", category.getId(), category.getName());

            // 构建响应 DTO
            IssueCategoryResponseDTO responseDTO = new IssueCategoryResponseDTO();
            responseDTO.setId(category.getId());
            responseDTO.setProjectId(projectId);
            responseDTO.setName(category.getName());
            responseDTO.setAssignedToId(
                    (category.getAssignedToId() != null) ? category.getAssignedToId().longValue() : null);

            // 填充默认指派人名称
            if (category.getAssignedToId() != null) {
                User assignedUser = userMapper.selectById(category.getAssignedToId().longValue());
                if (assignedUser != null) {
                    responseDTO.setAssignedToName(assignedUser.getLogin());
                }
            }

            return responseDTO;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("任务分类创建失败，项目ID: {}", projectId, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "任务分类创建失败");
        } finally {
            MDC.clear();
        }
    }

    /**
     * 更新任务分类
     *
     * @param projectId  项目ID
     * @param categoryId 分类ID
     * @param requestDTO 更新分类请求
     * @return 任务分类响应
     */
    @Transactional(rollbackFor = Exception.class)
    public IssueCategoryResponseDTO updateIssueCategory(Long projectId, Integer categoryId,
            IssueCategoryUpdateRequestDTO requestDTO) {
        MDC.put("operation", "update_issue_category");
        MDC.put("projectId", String.valueOf(projectId));
        MDC.put("categoryId", String.valueOf(categoryId));

        try {
            log.info("开始更新任务分类，项目ID: {}, 分类ID: {}", projectId, categoryId);

            // 查询项目是否存在
            Project project = projectMapper.selectById(projectId);
            if (project == null) {
                log.warn("项目不存在，项目ID: {}", projectId);
                throw new BusinessException(ResultCode.PROJECT_NOT_FOUND);
            }

            // 查询分类是否存在
            IssueCategory category = issueCategoryMapper.selectById(categoryId);
            if (category == null) {
                log.warn("任务分类不存在，分类ID: {}", categoryId);
                throw new BusinessException(ResultCode.SYSTEM_ERROR, "任务分类不存在");
            }

            // 验证分类是否属于该项目
            if (!category.getProjectId().equals(projectId.intValue())) {
                log.warn("任务分类不属于该项目，项目ID: {}, 分类ID: {}, 分类所属项目ID: {}",
                        projectId, categoryId, category.getProjectId());
                throw new BusinessException(ResultCode.PARAM_INVALID, "任务分类不属于该项目");
            }

            // 获取当前用户信息
            User currentUser = securityUtils.getCurrentUser();
            Long currentUserId = currentUser.getId();
            boolean isAdmin = Boolean.TRUE.equals(currentUser.getAdmin());

            // 权限验证：需要 manage_categories 权限或系统管理员
            // 注意：manage_categories 权限可能不在 Permission 枚举中，可以使用 manage_projects 权限
            if (!isAdmin) {
                if (!projectPermissionService.hasPermission(currentUserId, projectId, "manage_categories")) {
                    // 如果没有 manage_categories 权限，尝试使用 manage_projects 权限
                    if (!projectPermissionService.hasPermission(currentUserId, projectId, "manage_projects")) {
                        log.warn("用户无权限更新任务分类，项目ID: {}, 用户ID: {}", projectId, currentUserId);
                        throw new BusinessException(ResultCode.FORBIDDEN,
                                "无权限更新任务分类，需要 manage_categories 或 manage_projects 权限");
                    }
                }
            }

            // 处理分类名称更新（如果提供）
            boolean needUpdateName = false;
            String newName = null;
            if (requestDTO.getName() != null && !requestDTO.getName().trim().isEmpty()) {
                newName = requestDTO.getName().trim();
                // 如果名称有变化，验证新名称在项目中是否已存在
                if (!newName.equals(category.getName())) {
                    LambdaQueryWrapper<IssueCategory> checkQuery = new LambdaQueryWrapper<>();
                    checkQuery.eq(IssueCategory::getProjectId, projectId.intValue())
                            .eq(IssueCategory::getName, newName)
                            .ne(IssueCategory::getId, categoryId);
                    IssueCategory existingCategory = issueCategoryMapper.selectOne(checkQuery);
                    if (existingCategory != null) {
                        log.warn("任务分类名称已存在，项目ID: {}, 分类名称: {}", projectId, newName);
                        throw new BusinessException(ResultCode.PARAM_INVALID, "该分类名称已存在");
                    }
                }
                category.setName(newName);
                needUpdateName = true;
            }

            // 处理默认指派人更新（如果提供）
            Long assignedToId = requestDTO.getAssignedToId();
            boolean needSetAssignedToIdNull = false;
            if (assignedToId != null) {
                if (assignedToId == 0) {
                    // 取消默认指派人，需要显式设置为 null
                    category.setAssignedToId(null);
                    needSetAssignedToIdNull = true;
                } else {
                    // 验证默认指派人是否存在
                    User assignedUser = userMapper.selectById(assignedToId);
                    if (assignedUser == null) {
                        log.warn("默认指派人不存在，用户ID: {}", assignedToId);
                        throw new BusinessException(ResultCode.USER_NOT_FOUND, "默认指派人不存在");
                    }
                    category.setAssignedToId(assignedToId.intValue());
                }
            }

            // 更新分类
            // 如果需要将 assignedToId 更新为 null，必须使用 LambdaUpdateWrapper 显式设置
            int updateResult;
            if (needSetAssignedToIdNull) {
                // 使用 LambdaUpdateWrapper 显式设置 null 值
                LambdaUpdateWrapper<IssueCategory> updateWrapper = new LambdaUpdateWrapper<>();
                updateWrapper.eq(IssueCategory::getId, categoryId);
                // 更新名称（如果提供）
                if (needUpdateName) {
                    updateWrapper.set(IssueCategory::getName, newName);
                }
                // 显式设置 assignedToId 为 null
                updateWrapper.set(IssueCategory::getAssignedToId, null);
                updateResult = issueCategoryMapper.update(null, updateWrapper);
            } else {
                // 正常更新（非 null 值可以直接使用 updateById）
                updateResult = issueCategoryMapper.updateById(category);
            }

            if (updateResult <= 0) {
                log.error("任务分类更新失败，更新数据库失败，分类ID: {}", categoryId);
                throw new BusinessException(ResultCode.SYSTEM_ERROR, "任务分类更新失败");
            }

            log.info("任务分类更新成功，分类ID: {}, 分类名称: {}", categoryId, category.getName());

            // 构建响应 DTO
            IssueCategoryResponseDTO responseDTO = new IssueCategoryResponseDTO();
            responseDTO.setId(category.getId());
            responseDTO.setProjectId(projectId);
            responseDTO.setName(category.getName());
            responseDTO.setAssignedToId(
                    (category.getAssignedToId() != null) ? category.getAssignedToId().longValue() : null);

            // 填充默认指派人名称
            if (category.getAssignedToId() != null) {
                User assignedUser = userMapper.selectById(category.getAssignedToId().longValue());
                if (assignedUser != null) {
                    responseDTO.setAssignedToName(assignedUser.getLogin());
                }
            }

            return responseDTO;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("任务分类更新失败，项目ID: {}, 分类ID: {}", projectId, categoryId, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "任务分类更新失败");
        } finally {
            MDC.clear();
        }
    }

    /**
     * 删除任务分类
     *
     * @param projectId  项目ID
     * @param categoryId 分类ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteIssueCategory(Long projectId, Integer categoryId) {
        MDC.put("operation", "delete_issue_category");
        MDC.put("projectId", String.valueOf(projectId));
        MDC.put("categoryId", String.valueOf(categoryId));

        try {
            log.info("开始删除任务分类，项目ID: {}, 分类ID: {}", projectId, categoryId);

            // 查询项目是否存在
            Project project = projectMapper.selectById(projectId);
            if (project == null) {
                log.warn("项目不存在，项目ID: {}", projectId);
                throw new BusinessException(ResultCode.PROJECT_NOT_FOUND);
            }

            // 查询分类是否存在
            IssueCategory category = issueCategoryMapper.selectById(categoryId);
            if (category == null) {
                log.warn("任务分类不存在，分类ID: {}", categoryId);
                throw new BusinessException(ResultCode.SYSTEM_ERROR, "任务分类不存在");
            }

            // 验证分类是否属于该项目
            if (!category.getProjectId().equals(projectId.intValue())) {
                log.warn("任务分类不属于该项目，项目ID: {}, 分类ID: {}, 分类所属项目ID: {}",
                        projectId, categoryId, category.getProjectId());
                throw new BusinessException(ResultCode.PARAM_INVALID, "任务分类不属于该项目");
            }

            // 获取当前用户信息
            User currentUser = securityUtils.getCurrentUser();
            Long currentUserId = currentUser.getId();
            boolean isAdmin = Boolean.TRUE.equals(currentUser.getAdmin());

            // 权限验证：需要 manage_categories 权限或系统管理员
            // 注意：manage_categories 权限可能不在 Permission 枚举中，可以使用 manage_projects 权限
            if (!isAdmin) {
                if (!projectPermissionService.hasPermission(currentUserId, projectId, "manage_categories")) {
                    // 如果没有 manage_categories 权限，尝试使用 manage_projects 权限
                    if (!projectPermissionService.hasPermission(currentUserId, projectId, "manage_projects")) {
                        log.warn("用户无权限删除任务分类，项目ID: {}, 用户ID: {}", projectId, currentUserId);
                        throw new BusinessException(ResultCode.FORBIDDEN,
                                "无权限删除任务分类，需要 manage_categories 或 manage_projects 权限");
                    }
                }
            }

            // 检查是否有任务使用该分类
            LambdaQueryWrapper<Issue> issueQuery = new LambdaQueryWrapper<>();
            issueQuery.eq(Issue::getCategoryId, categoryId);
            Long issueCount = issueMapper.selectCount(issueQuery);
            if (issueCount > 0) {
                log.warn("任务分类正在被任务使用，不能删除，分类ID: {}, 使用数量: {}", categoryId, issueCount);
                throw new BusinessException(ResultCode.PARAM_INVALID,
                        "任务分类正在被 " + issueCount + " 个任务使用，请先将这些任务的分类移除后再删除");
            }

            // 删除分类
            int deleteResult = issueCategoryMapper.deleteById(categoryId);
            if (deleteResult <= 0) {
                log.error("任务分类删除失败，删除数据库失败，分类ID: {}", categoryId);
                throw new BusinessException(ResultCode.SYSTEM_ERROR, "任务分类删除失败");
            }

            log.info("任务分类删除成功，分类ID: {}, 分类名称: {}", categoryId, category.getName());

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("任务分类删除失败，项目ID: {}, 分类ID: {}", projectId, categoryId, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "任务分类删除失败");
        } finally {
            MDC.clear();
        }
    }
}
