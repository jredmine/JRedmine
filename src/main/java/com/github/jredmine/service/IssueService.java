package com.github.jredmine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.jredmine.dto.request.issue.IssueAssignRequestDTO;
import com.github.jredmine.dto.request.issue.IssueBatchUpdateRequestDTO;
import com.github.jredmine.dto.request.issue.IssueCategoryCreateRequestDTO;
import com.github.jredmine.dto.request.issue.IssueCopyRequestDTO;
import com.github.jredmine.dto.request.issue.IssueCategoryListRequestDTO;
import com.github.jredmine.dto.request.issue.IssueCategoryUpdateRequestDTO;
import com.github.jredmine.dto.request.issue.IssueCreateRequestDTO;
import com.github.jredmine.dto.request.issue.IssueListRequestDTO;
import com.github.jredmine.dto.request.issue.IssueRelationCreateRequestDTO;
import com.github.jredmine.dto.request.issue.IssueStatusUpdateRequestDTO;
import com.github.jredmine.dto.request.issue.IssueUpdateRequestDTO;
import com.github.jredmine.dto.request.issue.IssueJournalCreateRequestDTO;
import com.github.jredmine.dto.request.issue.IssueJournalListRequestDTO;
import com.github.jredmine.dto.request.issue.IssueWatcherCreateRequestDTO;
import com.github.jredmine.dto.response.workflow.AvailableTransitionDTO;
import com.github.jredmine.dto.response.workflow.WorkflowTransitionResponseDTO;
import com.github.jredmine.dto.response.PageResponse;
import com.github.jredmine.dto.response.issue.IssueCategoryResponseDTO;
import com.github.jredmine.dto.response.issue.IssueDetailResponseDTO;
import com.github.jredmine.dto.response.issue.IssueJournalResponseDTO;
import com.github.jredmine.dto.response.issue.IssueListItemResponseDTO;
import com.github.jredmine.dto.response.issue.IssueRelationResponseDTO;
import com.github.jredmine.dto.response.issue.IssueStatisticsResponseDTO;
import com.github.jredmine.dto.response.issue.IssueTreeNodeResponseDTO;
import com.github.jredmine.entity.EmailAddress;
import com.github.jredmine.entity.Issue;
import com.github.jredmine.entity.IssueCategory;
import com.github.jredmine.entity.IssueRelation;
import com.github.jredmine.entity.IssueStatus;
import com.github.jredmine.entity.Journal;
import com.github.jredmine.entity.JournalDetail;
import com.github.jredmine.entity.Project;
import com.github.jredmine.entity.Tracker;
import com.github.jredmine.entity.User;
import com.github.jredmine.entity.Version;
import com.github.jredmine.entity.Watcher;
import com.github.jredmine.entity.Workflow;
import com.github.jredmine.entity.Enumeration;
import com.github.jredmine.enums.ResultCode;
import com.github.jredmine.enums.WorkflowRule;
import com.github.jredmine.enums.WorkflowType;
import com.github.jredmine.exception.BusinessException;
import com.github.jredmine.mapper.issue.IssueCategoryMapper;
import com.github.jredmine.mapper.issue.IssueMapper;
import com.github.jredmine.mapper.issue.IssueRelationMapper;
import com.github.jredmine.mapper.issue.JournalDetailMapper;
import com.github.jredmine.mapper.issue.JournalMapper;
import com.github.jredmine.mapper.issue.WatcherMapper;
import com.github.jredmine.mapper.project.ProjectMapper;
import com.github.jredmine.mapper.project.VersionMapper;
import com.github.jredmine.mapper.TrackerMapper;
import com.github.jredmine.mapper.workflow.IssueStatusMapper;
import com.github.jredmine.mapper.workflow.WorkflowMapper;
import com.github.jredmine.mapper.workflow.EnumerationMapper;
import com.github.jredmine.mapper.user.EmailAddressMapper;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    private final JournalDetailMapper journalDetailMapper;
    private final WatcherMapper watcherMapper;
    private final ProjectMapper projectMapper;
    private final VersionMapper versionMapper;
    private final TrackerMapper trackerMapper;
    private final IssueStatusMapper issueStatusMapper;
    private final WorkflowMapper workflowMapper;
    private final EnumerationMapper enumerationMapper;
    private final UserMapper userMapper;
    private final MemberMapper memberMapper;
    private final MemberRoleMapper memberRoleMapper;
    private final SecurityUtils securityUtils;
    private final ProjectPermissionService projectPermissionService;
    private final WorkflowService workflowService;
    private final EmailService emailService;
    private final EmailAddressMapper emailAddressMapper;

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

            // 验证优先级是否存在且有效
            validatePriority(requestDTO.getPriorityId());

            // 验证版本是否存在（如果提供且不为0）
            // fixedVersionId 为 0 或 null 表示没有修复版本
            Long fixedVersionId = requestDTO.getFixedVersionId();
            if (fixedVersionId != null && fixedVersionId != 0) {
                validateVersion(fixedVersionId, requestDTO.getProjectId());
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
            // 分页参数已通过注解验证，null 值使用默认值
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
     * 导出任务列表为 CSV
     *
     * @param requestDTO 查询条件（复用任务列表查询参数）
     * @return CSV 文件内容（字节数组）
     */
    public byte[] exportIssues(IssueListRequestDTO requestDTO) {
        MDC.put("operation", "export_issues");

        try {
            log.info("开始导出任务列表");

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

            // 构建查询条件（复用任务列表查询逻辑，但不分页）
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
                    queryWrapper.isNull(Issue::getCategoryId);
                } else {
                    queryWrapper.eq(Issue::getCategoryId, requestDTO.getCategoryId());
                }
            }

            // 版本筛选
            if (requestDTO.getFixedVersionId() != null) {
                if (requestDTO.getFixedVersionId() == 0) {
                    queryWrapper.isNull(Issue::getFixedVersionId);
                } else {
                    queryWrapper.eq(Issue::getFixedVersionId, requestDTO.getFixedVersionId());
                }
            }

            // 关键词搜索
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
                    wrapper.and(w -> w.eq(Issue::getIsPrivate, true)
                            .in(finalMemberProjectIds != null && !finalMemberProjectIds.isEmpty(),
                                    Issue::getProjectId, finalMemberProjectIds))
                            .or()
                            .and(w -> w.eq(Issue::getIsPrivate, false)
                                    .in(finalMemberProjectIds != null && !finalMemberProjectIds.isEmpty(),
                                            Issue::getProjectId, finalMemberProjectIds));
                });
            }

            // 排序（复用任务列表排序逻辑）
            String sortOrder = requestDTO.getSortOrder() != null ? requestDTO.getSortOrder() : "desc";
            if (requestDTO.getSortBy() != null && !requestDTO.getSortBy().trim().isEmpty()) {
                String sortField = requestDTO.getSortBy().trim().toLowerCase();
                boolean ascending = "asc".equalsIgnoreCase(sortOrder);
                switch (sortField) {
                    case "created_on":
                    case "updated_on":
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
                        queryWrapper.orderByDesc(Issue::getId);
                        break;
                }
            } else {
                queryWrapper.orderByDesc(Issue::getId);
            }

            // 查询所有符合条件的任务（不分页）
            List<Issue> issues = issueMapper.selectList(queryWrapper);

            // 权限过滤：私有任务仅项目成员可见
            List<Issue> filteredIssues = new ArrayList<>();
            for (Issue issue : issues) {
                if (Boolean.TRUE.equals(issue.getIsPrivate()) && !isAdmin) {
                    LambdaQueryWrapper<Member> memberQuery = new LambdaQueryWrapper<>();
                    memberQuery.eq(Member::getUserId, currentUserId)
                            .eq(Member::getProjectId, issue.getProjectId());
                    Member member = memberMapper.selectOne(memberQuery);
                    if (member == null) {
                        continue;
                    }
                }
                filteredIssues.add(issue);
            }

            log.info("任务列表导出查询成功，共查询到 {} 条记录", filteredIssues.size());

            // 生成 CSV 内容
            StringBuilder csvContent = new StringBuilder();
            csvContent.append("\uFEFF"); // BOM，确保 Excel 正确识别 UTF-8

            // CSV 表头
            csvContent.append("任务ID,项目名称,跟踪器,任务标题,状态,优先级,指派人,创建者,创建时间,更新时间,截止日期,完成度,是否私有\n");

            // 获取所有状态、跟踪器、用户信息（用于填充名称）
            List<IssueStatus> allStatuses = issueStatusMapper.selectList(null);
            Map<Integer, IssueStatus> statusMap = allStatuses.stream()
                    .collect(Collectors.toMap(IssueStatus::getId, s -> s));

            List<Tracker> allTrackers = trackerMapper.selectList(null);
            Map<Integer, Tracker> trackerMap = allTrackers.stream()
                    .collect(Collectors.toMap(t -> t.getId().intValue(), t -> t));

            Set<Long> userIds = new java.util.HashSet<>();
            for (Issue issue : filteredIssues) {
                if (issue.getAssignedToId() != null) {
                    userIds.add(issue.getAssignedToId());
                }
                if (issue.getAuthorId() != null) {
                    userIds.add(issue.getAuthorId());
                }
            }
            Map<Long, User> userMap = new HashMap<>();
            if (!userIds.isEmpty()) {
                List<User> users = userMapper.selectBatchIds(userIds);
                userMap = users.stream()
                        .collect(Collectors.toMap(User::getId, u -> u));
            }

            Set<Long> projectIds = filteredIssues.stream()
                    .map(Issue::getProjectId)
                    .collect(Collectors.toSet());
            Map<Long, Project> projectMap = new HashMap<>();
            if (!projectIds.isEmpty()) {
                List<Project> projects = projectMapper.selectBatchIds(projectIds);
                projectMap = projects.stream()
                        .collect(Collectors.toMap(Project::getId, p -> p));
            }

            // 填充 CSV 数据行
            for (Issue issue : filteredIssues) {
                csvContent.append(escapeCsvField(String.valueOf(issue.getId()))).append(",");

                // 项目名称
                Project project = projectMap.get(issue.getProjectId());
                csvContent.append(escapeCsvField(project != null ? project.getName() : "")).append(",");

                // 跟踪器名称
                Tracker tracker = trackerMap.get(issue.getTrackerId());
                csvContent.append(escapeCsvField(tracker != null ? tracker.getName() : "")).append(",");

                // 任务标题
                csvContent.append(escapeCsvField(issue.getSubject() != null ? issue.getSubject() : "")).append(",");

                // 状态名称
                IssueStatus status = statusMap.get(issue.getStatusId());
                csvContent.append(escapeCsvField(status != null ? status.getName() : "")).append(",");

                // 优先级（暂时显示ID）
                csvContent.append(escapeCsvField("优先级 " + issue.getPriorityId())).append(",");

                // 指派人名称
                if (issue.getAssignedToId() != null) {
                    User assignedUser = userMap.get(issue.getAssignedToId());
                    csvContent.append(escapeCsvField(assignedUser != null ? assignedUser.getLogin() : ""));
                } else {
                    csvContent.append("");
                }
                csvContent.append(",");

                // 创建者名称
                User author = userMap.get(issue.getAuthorId());
                csvContent.append(escapeCsvField(author != null ? author.getLogin() : "")).append(",");

                // 创建时间
                csvContent.append(escapeCsvField(issue.getCreatedOn() != null
                        ? issue.getCreatedOn().toString().replace("T", " ")
                        : "")).append(",");

                // 更新时间
                csvContent.append(escapeCsvField(issue.getUpdatedOn() != null
                        ? issue.getUpdatedOn().toString().replace("T", " ")
                        : "")).append(",");

                // 截止日期
                csvContent.append(escapeCsvField(issue.getDueDate() != null
                        ? issue.getDueDate().toString()
                        : "")).append(",");

                // 完成度
                csvContent.append(escapeCsvField(issue.getDoneRatio() != null
                        ? String.valueOf(issue.getDoneRatio())
                        : "0")).append(",");

                // 是否私有
                csvContent.append(escapeCsvField(Boolean.TRUE.equals(issue.getIsPrivate()) ? "是" : "否"));

                csvContent.append("\n");
            }

            // 转换为字节数组（UTF-8 编码）
            byte[] csvBytes = csvContent.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);

            log.info("任务列表导出成功，共导出 {} 条记录", filteredIssues.size());
            return csvBytes;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("任务列表导出失败", e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "任务列表导出失败");
        } finally {
            MDC.clear();
        }
    }

    /**
     * 转义 CSV 字段（处理逗号、引号、换行符等特殊字符）
     *
     * @param field 字段值
     * @return 转义后的字段值
     */
    private String escapeCsvField(String field) {
        if (field == null) {
            return "";
        }
        // 如果字段包含逗号、引号或换行符，需要用引号包裹，并转义引号
        if (field.contains(",") || field.contains("\"") || field.contains("\n") || field.contains("\r")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
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

        // 填充优先级名称
        if (issue.getPriorityId() != null) {
            String priorityName = getPriorityName(issue.getPriorityId());
            if (priorityName != null) {
                dto.setPriorityName(priorityName);
            }
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

        // 填充优先级名称
        if (issue.getPriorityId() != null) {
            String priorityName = getPriorityName(issue.getPriorityId());
            if (priorityName != null) {
                dto.setPriorityName(priorityName);
            }
        }
        // TODO: 填充分类名称、版本名称等
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

            // 保存旧任务对象（用于记录变更历史）
            Issue oldIssue = new Issue();
            copyIssueProperties(issue, oldIssue);

            // 应用更新数据（复用公共逻辑）
            applyUpdateToIssue(issue, requestDTO);

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

            // 记录变更历史到 journals 表
            recordIssueChanges(oldIssue, issue, null);

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
     * 批量更新任务
     *
     * @param requestDTO 批量更新请求
     * @return 更新后的任务详情列表
     */
    @Transactional(rollbackFor = Exception.class)
    public List<IssueDetailResponseDTO> batchUpdateIssues(IssueBatchUpdateRequestDTO requestDTO) {
        MDC.put("operation", "batch_update_issues");
        MDC.put("issueCount", String.valueOf(requestDTO.getIssueIds().size()));

        try {
            log.info("开始批量更新任务，任务数量: {}", requestDTO.getIssueIds().size());

            // 验证任务ID列表不能为空
            if (requestDTO.getIssueIds() == null || requestDTO.getIssueIds().isEmpty()) {
                log.warn("任务ID列表为空");
                throw new BusinessException(ResultCode.PARAM_INVALID, "任务ID列表不能为空");
            }

            // 验证更新数据不能为空
            if (requestDTO.getUpdateData() == null) {
                log.warn("更新数据为空");
                throw new BusinessException(ResultCode.PARAM_INVALID, "更新数据不能为空");
            }

            // 获取当前用户信息
            User currentUser = securityUtils.getCurrentUser();
            Long currentUserId = currentUser.getId();
            boolean isAdmin = Boolean.TRUE.equals(currentUser.getAdmin());

            // 批量查询所有任务
            List<Issue> issues = issueMapper.selectBatchIds(requestDTO.getIssueIds());
            if (issues.size() != requestDTO.getIssueIds().size()) {
                log.warn("部分任务不存在，请求数量: {}, 实际查询到: {}",
                        requestDTO.getIssueIds().size(), issues.size());
                throw new BusinessException(ResultCode.SYSTEM_ERROR, "部分任务不存在");
            }

            // 验证用户对所有任务都有权限
            if (!isAdmin) {
                for (Issue issue : issues) {
                    if (!projectPermissionService.hasPermission(currentUserId, issue.getProjectId(), "edit_issues")) {
                        log.warn("用户无权限更新任务，任务ID: {}, 项目ID: {}, 用户ID: {}",
                                issue.getId(), issue.getProjectId(), currentUserId);
                        throw new BusinessException(ResultCode.FORBIDDEN,
                                "无权限更新任务 ID: " + issue.getId() + "，需要 edit_issues 权限");
                    }
                }
            }

            // 批量更新所有任务
            List<IssueDetailResponseDTO> resultList = new ArrayList<>();
            for (Issue issue : issues) {
                // 乐观锁检查（如果提供了 lockVersion）
                if (requestDTO.getUpdateData().getLockVersion() != null
                        && !requestDTO.getUpdateData().getLockVersion().equals(issue.getLockVersion())) {
                    log.warn("任务已被其他用户修改，任务ID: {}, 当前版本: {}, 请求版本: {}",
                            issue.getId(), issue.getLockVersion(), requestDTO.getUpdateData().getLockVersion());
                    throw new BusinessException(ResultCode.SYSTEM_ERROR,
                            "任务 ID: " + issue.getId() + " 已被其他用户修改，请刷新后重试");
                }

                // 应用更新数据（复用单个更新的逻辑）
                applyUpdateToIssue(issue, requestDTO.getUpdateData());

                // 更新乐观锁版本号和更新时间
                issue.setLockVersion(issue.getLockVersion() + 1);
                issue.setUpdatedOn(LocalDateTime.now());

                // 保存任务
                int updateResult = issueMapper.updateById(issue);
                if (updateResult <= 0) {
                    log.error("任务更新失败，更新数据库失败，任务ID: {}", issue.getId());
                    throw new BusinessException(ResultCode.SYSTEM_ERROR,
                            "任务 ID: " + issue.getId() + " 更新失败");
                }

                // 查询更新后的任务详情
                IssueDetailResponseDTO detailDTO = getIssueDetailById(issue.getId());
                resultList.add(detailDTO);
            }

            log.info("批量更新任务成功，任务数量: {}", resultList.size());
            return resultList;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("批量更新任务失败", e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "批量更新任务失败");
        } finally {
            MDC.clear();
        }
    }

    /**
     * 将更新数据应用到任务实体（内部辅助方法）
     * 提取自 updateIssue 方法的公共逻辑
     *
     * @param issue      任务实体
     * @param requestDTO 更新请求
     */
    private void applyUpdateToIssue(Issue issue, IssueUpdateRequestDTO requestDTO) {
        // 处理状态更新（如果提供）
        if (requestDTO.getStatusId() != null && !requestDTO.getStatusId().equals(issue.getStatusId())) {
            Integer newStatusId = requestDTO.getStatusId();

            // 验证新状态是否存在
            IssueStatus newStatus = issueStatusMapper.selectById(newStatusId);
            if (newStatus == null) {
                log.warn("任务状态不存在，状态ID: {}", newStatusId);
                throw new BusinessException(ResultCode.SYSTEM_ERROR, "任务状态不存在");
            }

            // 如果新状态是关闭状态，自动设置关闭时间和完成度
            if (Boolean.TRUE.equals(newStatus.getIsClosed())) {
                issue.setClosedOn(LocalDateTime.now());
                if (issue.getDoneRatio() == null || issue.getDoneRatio() < 100) {
                    issue.setDoneRatio(100);
                }
            } else {
                // 如果从关闭状态转换到非关闭状态，清除关闭时间
                if (issue.getStatusId() != null) {
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
                if (parentId.equals(issue.getId())) {
                    log.warn("不能将任务设置为自己的父任务，任务ID: {}", issue.getId());
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
            if (fixedVersionId == 0) {
                // 0 表示清除版本
                issue.setFixedVersionId(null);
            } else {
                // 验证版本是否存在且属于该项目
                validateVersion(fixedVersionId, issue.getProjectId());
                issue.setFixedVersionId(fixedVersionId);
            }
        }

        // 更新其他字段
        if (requestDTO.getSubject() != null) {
            issue.setSubject(requestDTO.getSubject());
        }
        if (requestDTO.getDescription() != null) {
            issue.setDescription(requestDTO.getDescription());
        }
        // 处理优先级更新（如果提供）
        if (requestDTO.getPriorityId() != null) {
            // 验证优先级是否存在且有效
            validatePriority(requestDTO.getPriorityId());
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

            // 删除任务关联关系（issue_relations 表）
            // 删除该任务作为源任务的关联关系（issue_from_id = 任务ID）
            LambdaQueryWrapper<IssueRelation> fromRelationQuery = new LambdaQueryWrapper<>();
            fromRelationQuery.eq(IssueRelation::getIssueFromId, id.intValue());
            int deletedFromCount = issueRelationMapper.delete(fromRelationQuery);
            if (deletedFromCount > 0) {
                log.info("删除任务关联关系（作为源任务），任务ID: {}, 删除数量: {}", id, deletedFromCount);
            }

            // 删除该任务作为目标任务的关联关系（issue_to_id = 任务ID）
            LambdaQueryWrapper<IssueRelation> toRelationQuery = new LambdaQueryWrapper<>();
            toRelationQuery.eq(IssueRelation::getIssueToId, id.intValue());
            int deletedToCount = issueRelationMapper.delete(toRelationQuery);
            if (deletedToCount > 0) {
                log.info("删除任务关联关系（作为目标任务），任务ID: {}, 删除数量: {}", id, deletedToCount);
            }

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
     * 获取任务树
     *
     * @param projectId 项目ID（可选，如果不指定，返回所有项目的任务树）
     * @param rootId    根任务ID（可选，如果不指定，返回所有顶级任务）
     * @return 任务树列表
     */
    public List<IssueTreeNodeResponseDTO> getIssueTree(Long projectId, Long rootId) {
        MDC.put("operation", "get_issue_tree");
        if (projectId != null) {
            MDC.put("projectId", String.valueOf(projectId));
        }
        if (rootId != null) {
            MDC.put("rootId", String.valueOf(rootId));
        }

        try {
            log.debug("开始查询任务树，项目ID: {}, 根任务ID: {}", projectId, rootId);

            // 获取当前用户信息
            User currentUser = securityUtils.getCurrentUser();
            Long currentUserId = currentUser.getId();
            boolean isAdmin = Boolean.TRUE.equals(currentUser.getAdmin());

            // 构建查询条件
            LambdaQueryWrapper<Issue> queryWrapper = new LambdaQueryWrapper<>();

            // 如果指定了项目ID，验证项目是否存在并过滤
            if (projectId != null) {
                Project project = projectMapper.selectById(projectId);
                if (project == null) {
                    log.warn("项目不存在，项目ID: {}", projectId);
                    throw new BusinessException(ResultCode.PROJECT_NOT_FOUND);
                }

                // 验证用户是否有权限访问项目
                if (!isAdmin) {
                    if (!projectPermissionService.hasPermission(currentUserId, projectId, "view_issues")) {
                        log.warn("用户无权限查看项目任务，项目ID: {}, 用户ID: {}", projectId, currentUserId);
                        throw new BusinessException(ResultCode.FORBIDDEN, "无权限查看项目任务，需要 view_issues 权限");
                    }
                }

                queryWrapper.eq(Issue::getProjectId, projectId);
            } else {
                // 如果没有指定项目ID，需要根据权限过滤项目
                if (!isAdmin) {
                    // 获取当前用户是成员的项目ID集合
                    LambdaQueryWrapper<Member> memberQuery = new LambdaQueryWrapper<>();
                    memberQuery.eq(Member::getUserId, currentUserId);
                    List<Member> members = memberMapper.selectList(memberQuery);
                    Set<Long> memberProjectIds = members.stream()
                            .map(Member::getProjectId)
                            .collect(Collectors.toSet());

                    // 获取公开项目ID集合
                    LambdaQueryWrapper<Project> projectQuery = new LambdaQueryWrapper<>();
                    projectQuery.eq(Project::getIsPublic, true);
                    List<Project> publicProjects = projectMapper.selectList(projectQuery);
                    Set<Long> publicProjectIds = publicProjects.stream()
                            .map(Project::getId)
                            .collect(Collectors.toSet());

                    // 合并项目ID集合
                    Set<Long> accessibleProjectIds = new java.util.HashSet<>();
                    accessibleProjectIds.addAll(memberProjectIds);
                    accessibleProjectIds.addAll(publicProjectIds);

                    if (accessibleProjectIds.isEmpty()) {
                        // 用户没有任何可访问的项目，返回空列表
                        log.info("用户无任何可访问的项目，用户ID: {}", currentUserId);
                        return List.of();
                    }

                    queryWrapper.in(Issue::getProjectId, accessibleProjectIds);
                }
            }

            // 如果指定了根任务ID，验证根任务是否存在
            if (rootId != null) {
                Issue rootIssue = issueMapper.selectById(rootId);
                if (rootIssue == null) {
                    log.warn("根任务不存在，根任务ID: {}", rootId);
                    throw new BusinessException(ResultCode.SYSTEM_ERROR, "根任务不存在");
                }

                // 验证用户是否有权限访问根任务
                if (!isAdmin) {
                    if (!projectPermissionService.hasPermission(currentUserId, rootIssue.getProjectId(),
                            "view_issues")) {
                        log.warn("用户无权限查看根任务，根任务ID: {}, 项目ID: {}, 用户ID: {}",
                                rootId, rootIssue.getProjectId(), currentUserId);
                        throw new BusinessException(ResultCode.FORBIDDEN, "无权限查看根任务，需要 view_issues 权限");
                    }
                }

                // 查询以根任务为根的子树（使用 lft 和 rgt，如果可用）
                // 如果 lft 和 rgt 为空，则使用 parent_id 递归查询
                if (rootIssue.getLft() != null && rootIssue.getRgt() != null) {
                    // 使用嵌套集合模型查询
                    queryWrapper.ge(Issue::getLft, rootIssue.getLft())
                            .le(Issue::getRgt, rootIssue.getRgt());
                } else {
                    // 使用 parent_id 递归查询（需要查询所有任务，然后在内存中构建树）
                    // 这里先查询所有任务，然后过滤
                }
            }

            // 按 ID 排序
            queryWrapper.orderByAsc(Issue::getId);

            // 执行查询
            List<Issue> allIssues = issueMapper.selectList(queryWrapper);

            // 如果指定了根任务ID且使用 parent_id，需要过滤出子树
            if (rootId != null) {
                // 检查根任务是否使用了 lft/rgt
                Issue rootIssue = issueMapper.selectById(rootId);
                if (rootIssue.getLft() == null || rootIssue.getRgt() == null) {
                    // 收集所有需要包含的任务ID（包括根任务及其所有子孙任务）
                    Set<Long> includeIssueIds = new java.util.HashSet<>();
                    includeIssueIds.add(rootId);
                    collectDescendantIssueIds(allIssues, rootId, includeIssueIds);

                    // 过滤出子树任务
                    allIssues = allIssues.stream()
                            .filter(i -> includeIssueIds.contains(i.getId()))
                            .toList();
                }
            }

            // 权限过滤：私有任务仅项目成员可见
            List<Issue> filteredIssues = new ArrayList<>();
            for (Issue issue : allIssues) {
                // 检查私有任务权限
                if (Boolean.TRUE.equals(issue.getIsPrivate()) && !isAdmin) {
                    // 检查用户是否是项目成员
                    LambdaQueryWrapper<Member> memberQuery = new LambdaQueryWrapper<>();
                    memberQuery.eq(Member::getUserId, currentUserId)
                            .eq(Member::getProjectId, issue.getProjectId());
                    Member member = memberMapper.selectOne(memberQuery);
                    if (member == null) {
                        // 不是项目成员，跳过私有任务
                        continue;
                    }
                }
                filteredIssues.add(issue);
            }

            // 构建树形结构
            List<IssueTreeNodeResponseDTO> tree = buildIssueTree(filteredIssues, rootId);

            MDC.put("count", String.valueOf(tree.size()));
            log.info("任务树查询成功，项目ID: {}, 根任务ID: {}, 共查询到 {} 个根节点", projectId, rootId, tree.size());

            return tree;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("任务树查询失败，项目ID: {}, 根任务ID: {}", projectId, rootId, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "任务树查询失败");
        } finally {
            MDC.clear();
        }
    }

    /**
     * 递归收集所有子孙任务ID
     *
     * @param allIssues 所有任务列表
     * @param parentId  父任务ID
     * @param result    结果集合
     */
    private void collectDescendantIssueIds(List<Issue> allIssues, Long parentId, Set<Long> result) {
        for (Issue issue : allIssues) {
            if (parentId.equals(issue.getParentId())) {
                result.add(issue.getId());
                collectDescendantIssueIds(allIssues, issue.getId(), result);
            }
        }
    }

    /**
     * 构建任务树形结构
     *
     * @param allIssues 所有任务列表
     * @param rootId    根任务ID（如果为null，返回所有顶级任务）
     * @return 任务树列表
     */
    private List<IssueTreeNodeResponseDTO> buildIssueTree(List<Issue> allIssues, Long rootId) {
        // 将任务列表转换为 Map，便于查找
        Map<Long, IssueTreeNodeResponseDTO> nodeMap = new HashMap<>();
        for (Issue issue : allIssues) {
            IssueTreeNodeResponseDTO node = toIssueTreeNodeResponseDTO(issue);
            node.setChildren(new ArrayList<>());
            nodeMap.put(issue.getId(), node);
        }

        // 构建树形结构
        List<IssueTreeNodeResponseDTO> roots = new ArrayList<>();
        for (Issue issue : allIssues) {
            IssueTreeNodeResponseDTO node = nodeMap.get(issue.getId());
            Long parentId = issue.getParentId();

            if (parentId == null) {
                // 顶级任务
                if (rootId == null || issue.getId().equals(rootId)) {
                    roots.add(node);
                }
            } else {
                // 有父任务的任务
                IssueTreeNodeResponseDTO parentNode = nodeMap.get(parentId);
                if (parentNode != null) {
                    // 父节点在列表中，添加到父节点的子节点列表
                    parentNode.getChildren().add(node);
                } else {
                    // 父节点不在列表中（可能是权限过滤导致的），作为根节点处理
                    if (rootId == null) {
                        roots.add(node);
                    }
                }
            }
        }

        // 如果指定了根任务ID，返回根任务节点（包含子树）
        if (rootId != null) {
            IssueTreeNodeResponseDTO rootNode = nodeMap.get(rootId);
            if (rootNode != null) {
                return List.of(rootNode);
            }
            // 如果根任务不在列表中，返回空列表
            return List.of();
        }

        return roots;
    }

    /**
     * 将 Issue 实体转换为 IssueTreeNodeResponseDTO
     *
     * @param issue 任务实体
     * @return 响应 DTO
     */
    private IssueTreeNodeResponseDTO toIssueTreeNodeResponseDTO(Issue issue) {
        IssueTreeNodeResponseDTO dto = new IssueTreeNodeResponseDTO();
        dto.setId(issue.getId());
        dto.setTrackerId(issue.getTrackerId());
        dto.setProjectId(issue.getProjectId());
        dto.setSubject(issue.getSubject());
        dto.setStatusId(issue.getStatusId());
        dto.setAssignedToId(issue.getAssignedToId());
        dto.setAuthorId(issue.getAuthorId());
        dto.setParentId(issue.getParentId());
        dto.setRootId(issue.getRootId());
        dto.setCreatedOn(issue.getCreatedOn());
        dto.setUpdatedOn(issue.getUpdatedOn());
        dto.setDueDate(issue.getDueDate());
        dto.setDoneRatio(issue.getDoneRatio());
        dto.setIsPrivate(issue.getIsPrivate());

        // 填充关联信息
        fillTreeNodeRelatedInfo(dto, issue);

        return dto;
    }

    /**
     * 填充树节点关联信息（项目名称、跟踪器名称、状态名称等）
     */
    private void fillTreeNodeRelatedInfo(IssueTreeNodeResponseDTO dto, Issue issue) {
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
    }

    /**
     * 获取任务统计信息
     *
     * @param projectId 项目ID（可选，如果不指定，统计所有项目的任务）
     * @return 任务统计信息
     */
    public IssueStatisticsResponseDTO getIssueStatistics(Long projectId) {
        MDC.put("operation", "get_issue_statistics");
        if (projectId != null) {
            MDC.put("projectId", String.valueOf(projectId));
        }

        try {
            log.debug("开始查询任务统计信息，项目ID: {}", projectId);

            // 获取当前用户信息
            User currentUser = securityUtils.getCurrentUser();
            Long currentUserId = currentUser.getId();
            boolean isAdmin = Boolean.TRUE.equals(currentUser.getAdmin());

            // 如果指定了项目ID，验证项目是否存在
            Project project = null;
            if (projectId != null) {
                project = projectMapper.selectById(projectId);
                if (project == null) {
                    log.warn("项目不存在，项目ID: {}", projectId);
                    throw new BusinessException(ResultCode.PROJECT_NOT_FOUND);
                }

                // 验证用户是否有权限访问项目
                if (!isAdmin) {
                    if (!projectPermissionService.hasPermission(currentUserId, projectId, "view_issues")) {
                        log.warn("用户无权限查看项目任务，项目ID: {}, 用户ID: {}", projectId, currentUserId);
                        throw new BusinessException(ResultCode.FORBIDDEN, "无权限查看项目任务，需要 view_issues 权限");
                    }
                }
            }

            // 构建查询条件
            LambdaQueryWrapper<Issue> queryWrapper = new LambdaQueryWrapper<>();

            // 如果指定了项目ID，过滤项目
            if (projectId != null) {
                queryWrapper.eq(Issue::getProjectId, projectId);
            } else {
                // 如果没有指定项目ID，需要根据权限过滤项目
                if (!isAdmin) {
                    // 获取当前用户是成员的项目ID集合
                    LambdaQueryWrapper<Member> memberQuery = new LambdaQueryWrapper<>();
                    memberQuery.eq(Member::getUserId, currentUserId);
                    List<Member> members = memberMapper.selectList(memberQuery);
                    Set<Long> memberProjectIds = members.stream()
                            .map(Member::getProjectId)
                            .collect(Collectors.toSet());

                    // 获取公开项目ID集合
                    LambdaQueryWrapper<Project> projectQuery = new LambdaQueryWrapper<>();
                    projectQuery.eq(Project::getIsPublic, true);
                    List<Project> publicProjects = projectMapper.selectList(projectQuery);
                    Set<Long> publicProjectIds = publicProjects.stream()
                            .map(Project::getId)
                            .collect(Collectors.toSet());

                    // 合并项目ID集合
                    Set<Long> accessibleProjectIds = new java.util.HashSet<>();
                    accessibleProjectIds.addAll(memberProjectIds);
                    accessibleProjectIds.addAll(publicProjectIds);

                    if (accessibleProjectIds.isEmpty()) {
                        // 用户没有任何可访问的项目，返回空统计
                        log.info("用户无任何可访问的项目，用户ID: {}", currentUserId);
                        IssueStatisticsResponseDTO emptyStatistics = new IssueStatisticsResponseDTO();
                        emptyStatistics.setTotalCount(0);
                        emptyStatistics.setInProgressCount(0);
                        emptyStatistics.setCompletedCount(0);
                        emptyStatistics.setCompletionRate(0.0);
                        emptyStatistics.setStatusStatistics(List.of());
                        emptyStatistics.setTrackerStatistics(List.of());
                        emptyStatistics.setPriorityStatistics(List.of());
                        emptyStatistics.setAssigneeStatistics(List.of());
                        emptyStatistics.setAuthorStatistics(List.of());
                        return emptyStatistics;
                    }

                    queryWrapper.in(Issue::getProjectId, accessibleProjectIds);
                }
            }

            // 查询所有符合条件的任务
            List<Issue> allIssues = issueMapper.selectList(queryWrapper);

            // 权限过滤：私有任务仅项目成员可见
            List<Issue> filteredIssues = new ArrayList<>();
            for (Issue issue : allIssues) {
                // 检查私有任务权限
                if (Boolean.TRUE.equals(issue.getIsPrivate()) && !isAdmin) {
                    // 检查用户是否是项目成员
                    LambdaQueryWrapper<Member> memberQuery = new LambdaQueryWrapper<>();
                    memberQuery.eq(Member::getUserId, currentUserId)
                            .eq(Member::getProjectId, issue.getProjectId());
                    Member member = memberMapper.selectOne(memberQuery);
                    if (member == null) {
                        // 不是项目成员，跳过私有任务
                        continue;
                    }
                }
                filteredIssues.add(issue);
            }

            // 构建统计信息
            IssueStatisticsResponseDTO statistics = new IssueStatisticsResponseDTO();
            if (projectId != null && project != null) {
                statistics.setProjectId(projectId);
                statistics.setProjectName(project.getName());
            }

            // 基本统计
            int totalCount = filteredIssues.size();
            statistics.setTotalCount(totalCount);

            // 获取所有状态信息（用于判断是否已关闭）
            List<IssueStatus> allStatuses = issueStatusMapper.selectList(null);
            Map<Integer, IssueStatus> statusMap = allStatuses.stream()
                    .collect(Collectors.toMap(IssueStatus::getId, s -> s));

            // 统计进行中和已完成的任务
            int inProgressCount = 0;
            int completedCount = 0;
            for (Issue issue : filteredIssues) {
                IssueStatus status = statusMap.get(issue.getStatusId());
                if (status != null && Boolean.TRUE.equals(status.getIsClosed())) {
                    completedCount++;
                } else {
                    inProgressCount++;
                }
            }
            statistics.setInProgressCount(inProgressCount);
            statistics.setCompletedCount(completedCount);

            // 计算完成率
            double completionRate = totalCount > 0 ? (completedCount * 100.0 / totalCount) : 0.0;
            statistics.setCompletionRate(Math.round(completionRate * 100.0) / 100.0); // 保留两位小数

            // 按状态统计
            Map<Integer, Integer> statusCountMap = new HashMap<>();
            for (Issue issue : filteredIssues) {
                statusCountMap.put(issue.getStatusId(),
                        statusCountMap.getOrDefault(issue.getStatusId(), 0) + 1);
            }
            List<IssueStatisticsResponseDTO.StatusStatistics> statusStatistics = new ArrayList<>();
            for (Map.Entry<Integer, Integer> entry : statusCountMap.entrySet()) {
                IssueStatus status = statusMap.get(entry.getKey());
                if (status != null) {
                    IssueStatisticsResponseDTO.StatusStatistics stat = new IssueStatisticsResponseDTO.StatusStatistics();
                    stat.setStatusId(status.getId());
                    stat.setStatusName(status.getName());
                    stat.setCount(entry.getValue());
                    stat.setIsClosed(status.getIsClosed());
                    statusStatistics.add(stat);
                }
            }
            // 按状态ID排序
            statusStatistics.sort((a, b) -> Integer.compare(a.getStatusId(), b.getStatusId()));
            statistics.setStatusStatistics(statusStatistics);

            // 按跟踪器统计
            Map<Integer, Integer> trackerCountMap = new HashMap<>();
            for (Issue issue : filteredIssues) {
                trackerCountMap.put(issue.getTrackerId(),
                        trackerCountMap.getOrDefault(issue.getTrackerId(), 0) + 1);
            }
            List<IssueStatisticsResponseDTO.TrackerStatistics> trackerStatistics = new ArrayList<>();
            for (Map.Entry<Integer, Integer> entry : trackerCountMap.entrySet()) {
                Tracker tracker = trackerMapper.selectById(entry.getKey().longValue());
                if (tracker != null) {
                    IssueStatisticsResponseDTO.TrackerStatistics stat = new IssueStatisticsResponseDTO.TrackerStatistics();
                    stat.setTrackerId(tracker.getId().intValue());
                    stat.setTrackerName(tracker.getName());
                    stat.setCount(entry.getValue());
                    trackerStatistics.add(stat);
                }
            }
            // 按跟踪器ID排序
            trackerStatistics.sort((a, b) -> Integer.compare(a.getTrackerId(), b.getTrackerId()));
            statistics.setTrackerStatistics(trackerStatistics);

            // 按优先级统计
            Map<Integer, Integer> priorityCountMap = new HashMap<>();
            for (Issue issue : filteredIssues) {
                priorityCountMap.put(issue.getPriorityId(),
                        priorityCountMap.getOrDefault(issue.getPriorityId(), 0) + 1);
            }
            List<IssueStatisticsResponseDTO.PriorityStatistics> priorityStatistics = new ArrayList<>();
            for (Map.Entry<Integer, Integer> entry : priorityCountMap.entrySet()) {
                IssueStatisticsResponseDTO.PriorityStatistics stat = new IssueStatisticsResponseDTO.PriorityStatistics();
                stat.setPriorityId(entry.getKey());
                // 填充优先级名称
                String priorityName = getPriorityName(entry.getKey());
                stat.setPriorityName(priorityName != null ? priorityName : "优先级 " + entry.getKey());
                stat.setCount(entry.getValue());
                priorityStatistics.add(stat);
            }
            // 按优先级ID排序
            priorityStatistics.sort((a, b) -> Integer.compare(a.getPriorityId(), b.getPriorityId()));
            statistics.setPriorityStatistics(priorityStatistics);

            // 按指派人统计
            Map<Long, Integer> assigneeCountMap = new HashMap<>();
            for (Issue issue : filteredIssues) {
                if (issue.getAssignedToId() != null) {
                    assigneeCountMap.put(issue.getAssignedToId(),
                            assigneeCountMap.getOrDefault(issue.getAssignedToId(), 0) + 1);
                }
            }
            List<IssueStatisticsResponseDTO.AssigneeStatistics> assigneeStatistics = new ArrayList<>();
            for (Map.Entry<Long, Integer> entry : assigneeCountMap.entrySet()) {
                User user = userMapper.selectById(entry.getKey());
                if (user != null) {
                    IssueStatisticsResponseDTO.AssigneeStatistics stat = new IssueStatisticsResponseDTO.AssigneeStatistics();
                    stat.setUserId(user.getId());
                    stat.setUserName(user.getLogin());
                    stat.setCount(entry.getValue());
                    assigneeStatistics.add(stat);
                }
            }
            // 按任务数量降序排序
            assigneeStatistics.sort((a, b) -> Integer.compare(b.getCount(), a.getCount()));
            statistics.setAssigneeStatistics(assigneeStatistics);

            // 按创建者统计
            Map<Long, Integer> authorCountMap = new HashMap<>();
            for (Issue issue : filteredIssues) {
                authorCountMap.put(issue.getAuthorId(),
                        authorCountMap.getOrDefault(issue.getAuthorId(), 0) + 1);
            }
            List<IssueStatisticsResponseDTO.AuthorStatistics> authorStatistics = new ArrayList<>();
            for (Map.Entry<Long, Integer> entry : authorCountMap.entrySet()) {
                User user = userMapper.selectById(entry.getKey());
                if (user != null) {
                    IssueStatisticsResponseDTO.AuthorStatistics stat = new IssueStatisticsResponseDTO.AuthorStatistics();
                    stat.setUserId(user.getId());
                    stat.setUserName(user.getLogin());
                    stat.setCount(entry.getValue());
                    authorStatistics.add(stat);
                }
            }
            // 按任务数量降序排序
            authorStatistics.sort((a, b) -> Integer.compare(b.getCount(), a.getCount()));
            statistics.setAuthorStatistics(authorStatistics);

            log.info("任务统计信息查询成功，项目ID: {}, 任务总数: {}", projectId, totalCount);
            return statistics;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("任务统计信息查询失败，项目ID: {}", projectId, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "任务统计信息查询失败");
        } finally {
            MDC.clear();
        }
    }

    /**
     * 获取任务可用的状态转换
     *
     * @param issueId 任务ID
     * @return 可用的状态转换
     */
    public WorkflowTransitionResponseDTO getIssueAvailableTransitions(Long issueId) {
        MDC.put("operation", "get_issue_available_transitions");
        MDC.put("issueId", String.valueOf(issueId));

        try {
            log.debug("开始查询任务可用的状态转换，任务ID: {}", issueId);

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

            // 获取用户在项目中的角色ID列表
            List<Integer> userRoleIds = getUserProjectRoleIds(currentUserId, issue.getProjectId());

            // 获取可用的状态转换（基于工作流规则）
            WorkflowTransitionResponseDTO availableTransitions = workflowService.getAvailableTransitions(
                    issue.getTrackerId(),
                    issue.getStatusId(),
                    userRoleIds.isEmpty() ? null : userRoleIds);

            // 根据任务的指派人、创建者等信息，过滤当前用户无法执行的转换
            List<AvailableTransitionDTO> filteredTransitions = new ArrayList<>();
            for (AvailableTransitionDTO transition : availableTransitions.getAvailableTransitions()) {
                // 检查指派人限制
                if (Boolean.TRUE.equals(transition.getAssignee())) {
                    // 如果转换需要指派人权限，但当前用户不是指派人，则跳过
                    if (issue.getAssignedToId() == null || !issue.getAssignedToId().equals(currentUserId)) {
                        log.debug("跳过需要指派人权限的转换，任务ID: {}, 状态ID: {}, 指派人ID: {}, 当前用户ID: {}",
                                issueId, transition.getStatusId(), issue.getAssignedToId(), currentUserId);
                        continue;
                    }
                }

                // 检查创建者限制
                if (Boolean.TRUE.equals(transition.getAuthor())) {
                    // 如果转换需要创建者权限，但当前用户不是创建者，则跳过
                    if (issue.getAuthorId() == null || !issue.getAuthorId().equals(currentUserId)) {
                        log.debug("跳过需要创建者权限的转换，任务ID: {}, 状态ID: {}, 创建者ID: {}, 当前用户ID: {}",
                                issueId, transition.getStatusId(), issue.getAuthorId(), currentUserId);
                        continue;
                    }
                }

                // 如果通过了所有限制检查，添加到可用转换列表
                filteredTransitions.add(transition);
            }

            // 构建响应（使用过滤后的转换列表）
            WorkflowTransitionResponseDTO response = WorkflowTransitionResponseDTO.builder()
                    .currentStatusId(availableTransitions.getCurrentStatusId())
                    .currentStatusName(availableTransitions.getCurrentStatusName())
                    .availableTransitions(filteredTransitions)
                    .build();

            log.info("查询任务可用的状态转换成功，任务ID: {}, 可用转换数量: {}", issueId, filteredTransitions.size());
            return response;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("查询任务可用的状态转换失败，任务ID: {}", issueId, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "查询任务可用的状态转换失败");
        } finally {
            MDC.clear();
        }
    }

    /**
     * 复制任务
     *
     * @param sourceIssueId 源任务ID
     * @param requestDTO    复制请求
     * @return 新任务详情
     */
    @Transactional(rollbackFor = Exception.class)
    public IssueDetailResponseDTO copyIssue(Long sourceIssueId, IssueCopyRequestDTO requestDTO) {
        MDC.put("operation", "copy_issue");
        MDC.put("sourceIssueId", String.valueOf(sourceIssueId));

        try {
            log.info("开始复制任务，源任务ID: {}", sourceIssueId);

            // 查询源任务是否存在
            Issue sourceIssue = issueMapper.selectById(sourceIssueId);
            if (sourceIssue == null) {
                log.warn("源任务不存在，源任务ID: {}", sourceIssueId);
                throw new BusinessException(ResultCode.SYSTEM_ERROR, "源任务不存在");
            }

            // 获取当前用户信息
            User currentUser = securityUtils.getCurrentUser();
            Long currentUserId = currentUser.getId();
            boolean isAdmin = Boolean.TRUE.equals(currentUser.getAdmin());

            // 确定目标项目ID：如果为 null 或 0，则使用源任务的项目ID
            Long targetProjectId = (requestDTO.getTargetProjectId() != null && requestDTO.getTargetProjectId() != 0)
                    ? requestDTO.getTargetProjectId()
                    : sourceIssue.getProjectId();

            // 验证源任务权限：需要 view_issues 权限或系统管理员
            if (!isAdmin) {
                if (!projectPermissionService.hasPermission(currentUserId, sourceIssue.getProjectId(), "view_issues")) {
                    log.warn("用户无权限查看源任务，源任务ID: {}, 项目ID: {}, 用户ID: {}",
                            sourceIssueId, sourceIssue.getProjectId(), currentUserId);
                    throw new BusinessException(ResultCode.FORBIDDEN, "无权限查看源任务，需要 view_issues 权限");
                }
            }

            // 验证目标项目权限：需要 add_issues 权限或系统管理员
            if (!isAdmin) {
                if (!projectPermissionService.hasPermission(currentUserId, targetProjectId, "add_issues")) {
                    log.warn("用户无权限在目标项目创建任务，目标项目ID: {}, 用户ID: {}", targetProjectId, currentUserId);
                    throw new BusinessException(ResultCode.FORBIDDEN, "无权限在目标项目创建任务，需要 add_issues 权限");
                }
            }

            // 验证目标项目是否存在
            Project targetProject = projectMapper.selectById(targetProjectId);
            if (targetProject == null) {
                log.warn("目标项目不存在，项目ID: {}", targetProjectId);
                throw new BusinessException(ResultCode.PROJECT_NOT_FOUND);
            }

            // 确定新任务标题
            String newSubject = requestDTO.getSubject();
            if (newSubject == null || newSubject.trim().isEmpty()) {
                newSubject = sourceIssue.getSubject() + " (副本)";
            }

            // 创建新任务实体（复制源任务的基本字段）
            Issue newIssue = new Issue();
            newIssue.setTrackerId(sourceIssue.getTrackerId());
            newIssue.setProjectId(targetProjectId);
            newIssue.setSubject(newSubject);
            newIssue.setDescription(sourceIssue.getDescription());

            // 状态：使用跟踪器的默认状态（新任务应该从初始状态开始）
            Tracker tracker = trackerMapper.selectById(sourceIssue.getTrackerId());
            Integer statusId = null;
            if (tracker != null && tracker.getDefaultStatusId() != null) {
                statusId = tracker.getDefaultStatusId().intValue();
            } else {
                // 获取第一个可用状态
                List<IssueStatus> statuses = issueStatusMapper.selectList(
                        new LambdaQueryWrapper<IssueStatus>()
                                .orderByAsc(IssueStatus::getPosition)
                                .orderByAsc(IssueStatus::getId)
                                .last("LIMIT 1"));
                if (!statuses.isEmpty()) {
                    statusId = statuses.get(0).getId();
                }
            }
            if (statusId == null) {
                log.error("系统中没有可用的任务状态");
                throw new BusinessException(ResultCode.SYSTEM_ERROR, "系统中没有可用的任务状态");
            }
            newIssue.setStatusId(statusId);

            newIssue.setPriorityId(sourceIssue.getPriorityId());

            // 指派人：如果源任务有指派人，复制；否则设置为当前用户
            if (sourceIssue.getAssignedToId() != null) {
                // 验证指派人是否存在
                User assignedUser = userMapper.selectById(sourceIssue.getAssignedToId());
                if (assignedUser != null) {
                    newIssue.setAssignedToId(sourceIssue.getAssignedToId());
                } else {
                    newIssue.setAssignedToId(currentUserId);
                }
            } else {
                newIssue.setAssignedToId(currentUserId);
            }

            // 分类：如果跨项目复制，需要验证分类是否属于目标项目
            if (sourceIssue.getCategoryId() != null && !targetProjectId.equals(sourceIssue.getProjectId())) {
                // 跨项目复制，不复制分类（分类是项目级别的）
                newIssue.setCategoryId(null);
            } else {
                newIssue.setCategoryId(sourceIssue.getCategoryId());
            }

            // 版本：如果跨项目复制，不复制版本（版本是项目级别的）
            if (sourceIssue.getFixedVersionId() != null && !targetProjectId.equals(sourceIssue.getProjectId())) {
                newIssue.setFixedVersionId(null);
            } else {
                newIssue.setFixedVersionId(sourceIssue.getFixedVersionId());
            }

            newIssue.setStartDate(sourceIssue.getStartDate());
            newIssue.setDueDate(sourceIssue.getDueDate());
            newIssue.setEstimatedHours(sourceIssue.getEstimatedHours());
            // 完成度重置为 0（新任务）
            newIssue.setDoneRatio(0);
            // 父任务：不复制（新任务没有父任务）
            newIssue.setParentId(null);
            newIssue.setIsPrivate(sourceIssue.getIsPrivate() != null ? sourceIssue.getIsPrivate() : false);

            // 设置创建者和时间
            newIssue.setAuthorId(currentUserId);
            newIssue.setLockVersion(0);
            LocalDateTime now = LocalDateTime.now();
            newIssue.setCreatedOn(now);
            newIssue.setUpdatedOn(now);
            // 关闭时间重置为 null（新任务）
            newIssue.setClosedOn(null);

            // TODO: 处理树形结构（parent_id, root_id, lft, rgt）
            // 暂时设置为 null，后续实现树形结构逻辑

            // 保存新任务
            int insertResult = issueMapper.insert(newIssue);
            if (insertResult <= 0) {
                log.error("任务复制失败，插入数据库失败");
                throw new BusinessException(ResultCode.SYSTEM_ERROR, "任务复制失败");
            }

            Long newIssueId = newIssue.getId();
            log.info("任务复制成功，源任务ID: {}, 新任务ID: {}", sourceIssueId, newIssueId);

            // 复制子任务（如果启用）
            if (Boolean.TRUE.equals(requestDTO.getCopyChildren())) {
                LambdaQueryWrapper<Issue> childrenQuery = new LambdaQueryWrapper<>();
                childrenQuery.eq(Issue::getParentId, sourceIssueId);
                List<Issue> children = issueMapper.selectList(childrenQuery);
                for (Issue child : children) {
                    // 递归复制子任务
                    IssueCopyRequestDTO childCopyRequest = new IssueCopyRequestDTO();
                    childCopyRequest.setTargetProjectId(targetProjectId);
                    childCopyRequest.setCopyChildren(true); // 递归复制子任务的子任务
                    childCopyRequest.setCopyRelations(requestDTO.getCopyRelations());
                    childCopyRequest.setCopyWatchers(requestDTO.getCopyWatchers());
                    childCopyRequest.setCopyJournals(requestDTO.getCopyJournals());
                    IssueDetailResponseDTO copiedChild = copyIssue(child.getId(), childCopyRequest);
                    // 设置新任务的父任务ID
                    Issue copiedChildIssue = issueMapper.selectById(copiedChild.getId());
                    if (copiedChildIssue != null) {
                        copiedChildIssue.setParentId(newIssueId);
                        issueMapper.updateById(copiedChildIssue);
                    }
                }
                log.info("子任务复制成功，源任务ID: {}, 子任务数量: {}", sourceIssueId, children.size());
            }

            // 复制关联关系（如果启用）
            if (Boolean.TRUE.equals(requestDTO.getCopyRelations())) {
                LambdaQueryWrapper<IssueRelation> relationQuery = new LambdaQueryWrapper<>();
                relationQuery.and(wrapper -> wrapper
                        .eq(IssueRelation::getIssueFromId, sourceIssueId.intValue())
                        .or()
                        .eq(IssueRelation::getIssueToId, sourceIssueId.intValue()));
                List<IssueRelation> relations = issueRelationMapper.selectList(relationQuery);
                for (IssueRelation relation : relations) {
                    IssueRelation newRelation = new IssueRelation();
                    // 确定关联的源任务和目标任务
                    if (relation.getIssueFromId().equals(sourceIssueId.intValue())) {
                        // 源任务是关联的源，目标任务保持不变（如果目标任务存在）
                        newRelation.setIssueFromId(newIssueId.intValue());
                        newRelation.setIssueToId(relation.getIssueToId());
                    } else {
                        // 源任务是关联的目标，源任务保持不变（如果源任务存在）
                        newRelation.setIssueFromId(relation.getIssueFromId());
                        newRelation.setIssueToId(newIssueId.intValue());
                    }
                    newRelation.setRelationType(relation.getRelationType());
                    newRelation.setDelay(relation.getDelay());
                    issueRelationMapper.insert(newRelation);
                }
                log.info("任务关联复制成功，源任务ID: {}, 关联数量: {}", sourceIssueId, relations.size());
            }

            // 复制关注者（如果启用）
            if (Boolean.TRUE.equals(requestDTO.getCopyWatchers())) {
                LambdaQueryWrapper<Watcher> watcherQuery = new LambdaQueryWrapper<>();
                watcherQuery.eq(Watcher::getWatchableType, "Issue")
                        .eq(Watcher::getWatchableId, sourceIssueId.intValue());
                List<Watcher> watchers = watcherMapper.selectList(watcherQuery);
                for (Watcher watcher : watchers) {
                    // 检查是否已存在（避免重复）
                    LambdaQueryWrapper<Watcher> checkQuery = new LambdaQueryWrapper<>();
                    checkQuery.eq(Watcher::getWatchableType, "Issue")
                            .eq(Watcher::getWatchableId, newIssueId.intValue())
                            .eq(Watcher::getUserId, watcher.getUserId());
                    Watcher existing = watcherMapper.selectOne(checkQuery);
                    if (existing == null) {
                        Watcher newWatcher = new Watcher();
                        newWatcher.setWatchableType("Issue");
                        newWatcher.setWatchableId(newIssueId.intValue());
                        newWatcher.setUserId(watcher.getUserId());
                        watcherMapper.insert(newWatcher);
                    }
                }
                log.info("任务关注者复制成功，源任务ID: {}, 关注者数量: {}", sourceIssueId, watchers.size());
            }

            // 复制评论/活动日志（如果启用）
            if (Boolean.TRUE.equals(requestDTO.getCopyJournals())) {
                LambdaQueryWrapper<Journal> journalQuery = new LambdaQueryWrapper<>();
                journalQuery.eq(Journal::getJournalizedId, sourceIssueId.intValue())
                        .eq(Journal::getJournalizedType, "Issue");
                List<Journal> journals = journalMapper.selectList(journalQuery);
                for (Journal journal : journals) {
                    Journal newJournal = new Journal();
                    newJournal.setJournalizedId(newIssueId.intValue());
                    newJournal.setJournalizedType("Issue");
                    newJournal.setUserId(journal.getUserId());
                    newJournal.setNotes(journal.getNotes());
                    newJournal.setPrivateNotes(journal.getPrivateNotes());
                    newJournal.setCreatedOn(LocalDateTime.now());
                    newJournal.setUpdatedOn(LocalDateTime.now());
                    journalMapper.insert(newJournal);
                }
                log.info("任务评论复制成功，源任务ID: {}, 评论数量: {}", sourceIssueId, journals.size());
            }

            // 查询新创建的任务（包含关联信息）
            return getIssueDetailById(newIssueId);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("任务复制失败，源任务ID: {}", sourceIssueId, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "任务复制失败");
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

            // 保存旧任务对象（用于记录变更历史）
            Issue oldIssue = new Issue();
            copyIssueProperties(issue, oldIssue);

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

            // 验证字段规则（必填、只读等）
            validateFieldRules(issue, issue.getTrackerId(), issue.getStatusId(), newStatusId, userRoleIds);

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

            // 记录状态变更历史到 journals 表
            // 包括备注信息（requestDTO.getNotes()）
            recordIssueChanges(oldIssue, issue, requestDTO.getNotes());

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
     * 验证字段规则（必填、只读等）
     *
     * @param issue        任务对象
     * @param trackerId    跟踪器ID
     * @param oldStatusId  旧状态ID
     * @param newStatusId  新状态ID
     * @param userRoleIds  用户角色ID列表
     */
    private void validateFieldRules(Issue issue, Integer trackerId, Integer oldStatusId, 
                                     Integer newStatusId, List<Integer> userRoleIds) {
        try {
            // 查询该状态转换对应的字段规则（type='field'）
            LambdaQueryWrapper<Workflow> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Workflow::getType, WorkflowType.FIELD.getCode())
                    .eq(Workflow::getNewStatusId, newStatusId);

            // 跟踪器条件：所有跟踪器（trackerId = 0）或指定跟踪器
            queryWrapper.and(wrapper -> wrapper
                    .eq(Workflow::getTrackerId, 0)  // 所有跟踪器
                    .or()
                    .eq(Workflow::getTrackerId, trackerId)  // 指定跟踪器
            );

            // 旧状态条件：所有状态（oldStatusId = 0）或当前状态
            queryWrapper.and(wrapper -> wrapper
                    .eq(Workflow::getOldStatusId, 0)  // 所有状态
                    .or()
                    .eq(Workflow::getOldStatusId, oldStatusId)  // 当前状态
            );

            // 角色条件：如果 roleIds 为空，只查询所有角色（role_id = 0）
            // 如果 roleIds 不为空，查询所有角色或用户角色
            if (userRoleIds == null || userRoleIds.isEmpty()) {
                queryWrapper.eq(Workflow::getRoleId, 0);  // 只查询所有角色
            } else {
                queryWrapper.and(wrapper -> wrapper
                        .eq(Workflow::getRoleId, 0)  // 所有角色
                        .or()
                        .in(Workflow::getRoleId, userRoleIds)  // 用户角色
                );
            }

            List<Workflow> fieldRules = workflowMapper.selectList(queryWrapper);

            if (fieldRules.isEmpty()) {
                log.debug("未找到字段规则，跳过验证，任务ID: {}, 跟踪器ID: {}, 旧状态ID: {}, 新状态ID: {}",
                        issue.getId(), trackerId, oldStatusId, newStatusId);
                return;
            }

            // 按字段名和规则类型分组
            Map<String, List<Workflow>> fieldRulesMap = new HashMap<>();
            for (Workflow rule : fieldRules) {
                if (rule.getFieldName() != null && rule.getRule() != null) {
                    fieldRulesMap.computeIfAbsent(rule.getFieldName(), k -> new ArrayList<>()).add(rule);
                }
            }

            // 验证必填字段
            for (Map.Entry<String, List<Workflow>> entry : fieldRulesMap.entrySet()) {
                String fieldName = entry.getKey();
                List<Workflow> rules = entry.getValue();

                // 检查是否有必填规则
                boolean isRequired = rules.stream()
                        .anyMatch(rule -> WorkflowRule.REQUIRED.getCode().equals(rule.getRule()));

                if (isRequired) {
                    // 验证字段是否有值
                    if (!isFieldHasValue(issue, fieldName)) {
                        String fieldDisplayName = getFieldDisplayName(fieldName);
                        log.warn("字段规则验证失败：字段 {} 为必填，但未填写，任务ID: {}", fieldName, issue.getId());
                        throw new BusinessException(ResultCode.PARAM_INVALID,
                                String.format("状态转换失败：字段 \"%s\" 为必填项，请先填写该字段", fieldDisplayName));
                    }
                }
            }

            log.debug("字段规则验证通过，任务ID: {}, 跟踪器ID: {}, 旧状态ID: {}, 新状态ID: {}, 规则数量: {}",
                    issue.getId(), trackerId, oldStatusId, newStatusId, fieldRules.size());

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("字段规则验证失败，任务ID: {}", issue.getId(), e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "字段规则验证失败");
        }
    }

    /**
     * 检查字段是否有值
     *
     * @param issue     任务对象
     * @param fieldName 字段名（数据库字段名，如 assigned_to, priority 等）
     * @return 如果字段有值返回 true，否则返回 false
     */
    private boolean isFieldHasValue(Issue issue, String fieldName) {
        if (fieldName == null || fieldName.trim().isEmpty()) {
            return false;
        }

        switch (fieldName) {
            case "assigned_to":
                return issue.getAssignedToId() != null && issue.getAssignedToId() != 0;
            case "priority":
                return issue.getPriorityId() != null && issue.getPriorityId() != 0;
            case "description":
                return issue.getDescription() != null && !issue.getDescription().trim().isEmpty();
            case "due_date":
                return issue.getDueDate() != null;
            case "category_id":
                return issue.getCategoryId() != null && issue.getCategoryId() != 0;
            case "fixed_version_id":
                return issue.getFixedVersionId() != null && issue.getFixedVersionId() != 0;
            case "start_date":
                return issue.getStartDate() != null;
            case "estimated_hours":
                return issue.getEstimatedHours() != null && issue.getEstimatedHours() > 0;
            default:
                log.warn("未知的字段名，无法验证：{}", fieldName);
                return true;  // 未知字段默认认为有值，避免误报
        }
    }

    /**
     * 获取字段的显示名称
     *
     * @param fieldName 字段名（数据库字段名）
     * @return 字段的显示名称
     */
    private String getFieldDisplayName(String fieldName) {
        if (fieldName == null || fieldName.trim().isEmpty()) {
            return fieldName;
        }

        switch (fieldName) {
            case "assigned_to":
                return "指派人";
            case "priority":
                return "优先级";
            case "description":
                return "描述";
            case "due_date":
                return "截止日期";
            case "category_id":
                return "分类";
            case "fixed_version_id":
                return "修复版本";
            case "start_date":
                return "开始日期";
            case "estimated_hours":
                return "预估工时";
            default:
                return fieldName;
        }
    }

    /**
     * 验证优先级是否存在且有效
     *
     * @param priorityId 优先级ID
     * @throws BusinessException 如果优先级不存在或无效
     */
    private void validatePriority(Integer priorityId) {
        if (priorityId == null || priorityId == 0) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "优先级ID不能为空");
        }

        try {
            // 查询优先级枚举（type='IssuePriority'）
            LambdaQueryWrapper<Enumeration> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Enumeration::getId, priorityId)
                    .eq(Enumeration::getType, "IssuePriority")
                    .eq(Enumeration::getActive, true);
            Enumeration enumeration = enumerationMapper.selectOne(queryWrapper);

            if (enumeration == null) {
                log.warn("优先级不存在或已禁用，优先级ID: {}", priorityId);
                throw new BusinessException(ResultCode.PARAM_INVALID, "优先级不存在或已禁用，优先级ID: " + priorityId);
            }

            log.debug("优先级验证通过，优先级ID: {}, 优先级名称: {}", priorityId, enumeration.getName());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("验证优先级失败，优先级ID: {}", priorityId, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "验证优先级失败");
        }
    }

    /**
     * 获取优先级名称
     *
     * @param priorityId 优先级ID
     * @return 优先级名称，如果不存在则返回null
     */
    private String getPriorityName(Integer priorityId) {
        if (priorityId == null || priorityId == 0) {
            return null;
        }

        try {
            // 查询优先级枚举（type='IssuePriority'）
            LambdaQueryWrapper<Enumeration> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Enumeration::getId, priorityId)
                    .eq(Enumeration::getType, "IssuePriority")
                    .eq(Enumeration::getActive, true);
            Enumeration enumeration = enumerationMapper.selectOne(queryWrapper);

            if (enumeration != null && enumeration.getName() != null) {
                return enumeration.getName();
            }

            log.debug("未找到优先级，优先级ID: {}", priorityId);
            return null;
        } catch (Exception e) {
            log.warn("查询优先级名称失败，优先级ID: {}", priorityId, e);
            return null;
        }
    }

    /**
     * 验证版本是否存在且属于指定项目
     *
     * @param versionId 版本ID
     * @param projectId 项目ID
     * @throws BusinessException 如果版本不存在或不属于该项目
     */
    private void validateVersion(Long versionId, Long projectId) {
        if (versionId == null || versionId == 0) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "版本ID不能为空");
        }

        if (projectId == null) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "项目ID不能为空");
        }

        try {
            // 查询版本
            Version version = versionMapper.selectById(versionId.intValue());
            if (version == null) {
                log.warn("版本不存在，版本ID: {}", versionId);
                throw new BusinessException(ResultCode.PARAM_INVALID, "版本不存在，版本ID: " + versionId);
            }

            // 验证版本是否属于该项目
            if (version.getProjectId() == null || !version.getProjectId().equals(projectId.intValue())) {
                log.warn("版本不属于该项目，版本ID: {}, 版本所属项目ID: {}, 任务项目ID: {}",
                        versionId, version.getProjectId(), projectId);
                throw new BusinessException(ResultCode.PARAM_INVALID,
                        "版本不属于该项目，版本ID: " + versionId + "，项目ID: " + projectId);
            }

            log.debug("版本验证通过，版本ID: {}, 版本名称: {}, 项目ID: {}",
                    versionId, version.getName(), projectId);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("验证版本失败，版本ID: {}, 项目ID: {}", versionId, projectId, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "验证版本失败");
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

            // 保存旧任务对象（用于记录变更历史）
            Issue oldIssue = new Issue();
            copyIssueProperties(issue, oldIssue);

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

            // 记录变更历史到 journals 表
            recordIssueChanges(oldIssue, issue, null);

            // 发送通知给新指派人（如果指派人发生变化且新指派人存在）
            if (assignedToId != null && assignedToId != 0 && !assignedToId.equals(oldAssignedToId)) {
                try {
                    // 获取新指派人信息
                    User assignedUser = userMapper.selectById(assignedToId);
                    if (assignedUser != null) {
                        // 获取新指派人的邮箱地址
                        LambdaQueryWrapper<EmailAddress> emailQuery = new LambdaQueryWrapper<>();
                        emailQuery.eq(EmailAddress::getUserId, assignedToId)
                                .eq(EmailAddress::getIsDefault, true)
                                .eq(EmailAddress::getNotify, true);
                        EmailAddress emailAddress = emailAddressMapper.selectOne(emailQuery);

                        if (emailAddress != null && emailAddress.getAddress() != null
                                && !emailAddress.getAddress().trim().isEmpty()) {
                            // 获取项目信息
                            Project project = projectMapper.selectById(issue.getProjectId());
                            String projectName = project != null ? project.getName() : "未知项目";

                            // 获取分配人姓名
                            String assignerName = currentUser.getFirstname() + " " + currentUser.getLastname();
                            assignerName = assignerName.trim();
                            if (assignerName.isEmpty()) {
                                assignerName = currentUser.getLogin();
                            }

                            // 获取指派人姓名
                            String assigneeName = assignedUser.getFirstname() + " " + assignedUser.getLastname();
                            assigneeName = assigneeName.trim();
                            if (assigneeName.isEmpty()) {
                                assigneeName = assignedUser.getLogin();
                            }

                            // 发送邮件通知
                            emailService.sendIssueAssignmentEmail(
                                    emailAddress.getAddress(),
                                    assigneeName,
                                    id,
                                    issue.getSubject(),
                                    projectName,
                                    assignerName);
                            log.info("任务分配通知邮件已发送，任务ID: {}, 指派人ID: {}, 邮箱: {}",
                                    id, assignedToId, emailAddress.getAddress());
                        } else {
                            log.debug("新指派人未配置邮箱或已关闭通知，跳过邮件发送，任务ID: {}, 指派人ID: {}",
                                    id, assignedToId);
                        }
                    }
                } catch (Exception e) {
                    // 邮件发送失败不应该影响任务分配流程，只记录错误日志
                    log.error("发送任务分配通知邮件失败，任务ID: {}, 指派人ID: {}", id, assignedToId, e);
                }
            }

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
     * 获取任务活动日志列表
     *
     * @param issueId    任务ID
     * @param requestDTO 查询请求参数
     * @return 分页响应
     */
    public PageResponse<IssueJournalResponseDTO> listIssueJournals(Long issueId,
            IssueJournalListRequestDTO requestDTO) {
        MDC.put("operation", "list_issue_journals");
        MDC.put("issueId", String.valueOf(issueId));

        try {
            // 分页参数已通过注解验证，null 值使用默认值
            Integer current = requestDTO.getCurrent() != null ? requestDTO.getCurrent() : 1;
            Integer size = requestDTO.getSize() != null ? requestDTO.getSize() : 10;

            log.debug("开始查询任务活动日志列表，任务ID: {}, 页码: {}, 每页数量: {}", issueId, current, size);

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

            // 检查用户是否是项目成员（用于过滤私有备注）
            boolean isProjectMember = isAdmin;
            if (!isProjectMember) {
                LambdaQueryWrapper<Member> memberQuery = new LambdaQueryWrapper<>();
                memberQuery.eq(Member::getUserId, currentUserId)
                        .eq(Member::getProjectId, issue.getProjectId());
                Member member = memberMapper.selectOne(memberQuery);
                isProjectMember = (member != null);
            }

            // 创建分页对象
            Page<Journal> page = new Page<>(current, size);

            // 构建查询条件
            LambdaQueryWrapper<Journal> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Journal::getJournalizedId, issueId.intValue())
                    .eq(Journal::getJournalizedType, "Issue");

            // 过滤私有备注：如果不是项目成员，只显示公开评论
            if (!isProjectMember) {
                queryWrapper.and(wrapper -> wrapper
                        .eq(Journal::getPrivateNotes, false)
                        .or()
                        .isNull(Journal::getPrivateNotes));
            }

            // 按创建时间倒序排序（最新的在前）
            queryWrapper.orderByDesc(Journal::getCreatedOn)
                    .orderByDesc(Journal::getId);

            // 执行分页查询
            Page<Journal> result = journalMapper.selectPage(page, queryWrapper);

            MDC.put("total", String.valueOf(result.getTotal()));
            log.info("任务活动日志列表查询成功，任务ID: {}, 共查询到 {} 条记录", issueId, result.getTotal());

            // 转换为响应 DTO
            List<IssueJournalResponseDTO> dtoList = result.getRecords().stream()
                    .map(journal -> toIssueJournalResponseDTO(journal, issueId))
                    .toList();

            return PageResponse.of(
                    dtoList,
                    (int) result.getTotal(),
                    (int) result.getCurrent(),
                    (int) result.getSize());

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("任务活动日志列表查询失败，任务ID: {}", issueId, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "任务活动日志列表查询失败");
        } finally {
            MDC.clear();
        }
    }

    /**
     * 将 Journal 实体转换为 IssueJournalResponseDTO
     *
     * @param journal 活动日志实体
     * @param issueId 任务ID
     * @return 响应 DTO
     */
    private IssueJournalResponseDTO toIssueJournalResponseDTO(Journal journal, Long issueId) {
        IssueJournalResponseDTO dto = new IssueJournalResponseDTO();
        dto.setId(journal.getId());
        dto.setIssueId(issueId);
        dto.setNotes(journal.getNotes());
        dto.setCreatedOn(journal.getCreatedOn());
        dto.setUpdatedOn(journal.getUpdatedOn());
        dto.setPrivateNotes(journal.getPrivateNotes());

        // 填充作者信息
        if (journal.getUserId() != null) {
            dto.setUserId(journal.getUserId().longValue());
            User author = userMapper.selectById(journal.getUserId().longValue());
            if (author != null) {
                dto.setUserName(author.getLogin());
            }
        }

        // 填充更新者信息
        if (journal.getUpdatedById() != null) {
            dto.setUpdatedById(journal.getUpdatedById().longValue());
            User updatedBy = userMapper.selectById(journal.getUpdatedById().longValue());
            if (updatedBy != null) {
                dto.setUpdatedByName(updatedBy.getLogin());
            }
        }

        return dto;
    }

    /**
     * 获取任务分类列表
     *
     * @param projectId  项目ID
     * @param requestDTO 查询请求参数
     * @return 分页响应
     */
    public PageResponse<IssueCategoryResponseDTO> listIssueCategories(Long projectId,
            IssueCategoryListRequestDTO requestDTO) {
        MDC.put("operation", "list_issue_categories");
        MDC.put("projectId", String.valueOf(projectId));

        try {
            // 分页参数已通过注解验证，null 值使用默认值
            Integer current = requestDTO.getCurrent() != null ? requestDTO.getCurrent() : 1;
            Integer size = requestDTO.getSize() != null ? requestDTO.getSize() : 10;

            log.debug("开始查询任务分类列表，项目ID: {}, 页码: {}, 每页数量: {}", projectId, current, size);

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

            // 权限验证：需要 view_issues 权限或系统管理员
            if (!isAdmin) {
                if (!projectPermissionService.hasPermission(currentUserId, projectId, "view_issues")) {
                    log.warn("用户无权限查看任务分类，项目ID: {}, 用户ID: {}", projectId, currentUserId);
                    throw new BusinessException(ResultCode.FORBIDDEN, "无权限查看任务分类，需要 view_issues 权限");
                }
            }

            // 创建分页对象
            Page<IssueCategory> page = new Page<>(current, size);

            // 构建查询条件
            LambdaQueryWrapper<IssueCategory> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(IssueCategory::getProjectId, projectId.intValue());

            // 名称模糊查询（如果提供）
            if (requestDTO.getName() != null && !requestDTO.getName().trim().isEmpty()) {
                queryWrapper.like(IssueCategory::getName, requestDTO.getName().trim());
            }

            // 按 ID 排序（ID 是主键，有索引，查询更快）
            queryWrapper.orderByAsc(IssueCategory::getId);

            // 执行分页查询
            Page<IssueCategory> result = issueCategoryMapper.selectPage(page, queryWrapper);

            MDC.put("total", String.valueOf(result.getTotal()));
            log.info("任务分类列表查询成功，项目ID: {}, 共查询到 {} 条记录", projectId, result.getTotal());

            // 转换为响应 DTO
            List<IssueCategoryResponseDTO> dtoList = result.getRecords().stream()
                    .map(category -> toIssueCategoryResponseDTO(category, projectId))
                    .toList();

            return PageResponse.of(
                    dtoList,
                    (int) result.getTotal(),
                    (int) result.getCurrent(),
                    (int) result.getSize());

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("任务分类列表查询失败，项目ID: {}", projectId, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "任务分类列表查询失败");
        } finally {
            MDC.clear();
        }
    }

    /**
     * 将 IssueCategory 实体转换为 IssueCategoryResponseDTO
     *
     * @param category  任务分类实体
     * @param projectId 项目ID
     * @return 响应 DTO
     */
    private IssueCategoryResponseDTO toIssueCategoryResponseDTO(IssueCategory category, Long projectId) {
        IssueCategoryResponseDTO dto = new IssueCategoryResponseDTO();
        dto.setId(category.getId());
        dto.setProjectId(projectId);
        dto.setName(category.getName());
        dto.setAssignedToId(
                (category.getAssignedToId() != null) ? category.getAssignedToId().longValue() : null);

        // 填充默认指派人名称
        if (category.getAssignedToId() != null) {
            User assignedUser = userMapper.selectById(category.getAssignedToId().longValue());
            if (assignedUser != null) {
                dto.setAssignedToName(assignedUser.getLogin());
            }
        }

        return dto;
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

    /**
     * 复制 Issue 对象属性
     */
    private void copyIssueProperties(Issue source, Issue target) {
        target.setId(source.getId());
        target.setTrackerId(source.getTrackerId());
        target.setProjectId(source.getProjectId());
        target.setSubject(source.getSubject());
        target.setDescription(source.getDescription());
        target.setDueDate(source.getDueDate());
        target.setCategoryId(source.getCategoryId());
        target.setStatusId(source.getStatusId());
        target.setAssignedToId(source.getAssignedToId());
        target.setPriorityId(source.getPriorityId());
        target.setFixedVersionId(source.getFixedVersionId());
        target.setAuthorId(source.getAuthorId());
        target.setLockVersion(source.getLockVersion());
        target.setCreatedOn(source.getCreatedOn());
        target.setUpdatedOn(source.getUpdatedOn());
        target.setStartDate(source.getStartDate());
        target.setDoneRatio(source.getDoneRatio());
        target.setEstimatedHours(source.getEstimatedHours());
        target.setParentId(source.getParentId());
        target.setRootId(source.getRootId());
        target.setLft(source.getLft());
        target.setRgt(source.getRgt());
        target.setIsPrivate(source.getIsPrivate());
        target.setClosedOn(source.getClosedOn());
    }

    /**
     * 记录任务变更历史
     * 比较旧任务和新任务的字段，记录所有变更到 journals 和 journal_details 表
     *
     * @param oldIssue 旧任务对象
     * @param newIssue 新任务对象
     * @param notes    备注信息（可选）
     */
    private void recordIssueChanges(Issue oldIssue, Issue newIssue, String notes) {
        try {
            // 获取当前用户
            User currentUser = securityUtils.getCurrentUser();
            Long currentUserId = currentUser.getId();

            // 创建 Journal 记录
            Journal journal = new Journal();
            journal.setJournalizedId(newIssue.getId().intValue());
            journal.setJournalizedType("Issue");
            journal.setUserId(currentUserId.intValue());
            journal.setNotes(notes);
            journal.setPrivateNotes(false);
            journal.setCreatedOn(LocalDateTime.now());
            journal.setUpdatedOn(LocalDateTime.now());

            // 保存 Journal 记录
            int journalInsertResult = journalMapper.insert(journal);
            if (journalInsertResult <= 0) {
                log.warn("创建活动日志失败，任务ID: {}", newIssue.getId());
                return;
            }

            // 比较字段变更并记录
            List<JournalDetail> details = new ArrayList<>();

            // 比较状态ID
            if (!Objects.equals(oldIssue.getStatusId(), newIssue.getStatusId())) {
                String oldValue = oldIssue.getStatusId() != null ? getStatusName(oldIssue.getStatusId()) : "";
                String newValue = newIssue.getStatusId() != null ? getStatusName(newIssue.getStatusId()) : "";
                details.add(createJournalDetail(journal.getId(), "attr", "status_id", oldValue, newValue));
            }

            // 比较指派人ID
            if (!Objects.equals(oldIssue.getAssignedToId(), newIssue.getAssignedToId())) {
                String oldValue = oldIssue.getAssignedToId() != null ? getUserName(oldIssue.getAssignedToId()) : "";
                String newValue = newIssue.getAssignedToId() != null ? getUserName(newIssue.getAssignedToId()) : "";
                details.add(createJournalDetail(journal.getId(), "attr", "assigned_to_id", oldValue, newValue));
            }

            // 比较优先级ID
            if (!Objects.equals(oldIssue.getPriorityId(), newIssue.getPriorityId())) {
                String oldValue = oldIssue.getPriorityId() != null ? String.valueOf(oldIssue.getPriorityId()) : "";
                String newValue = newIssue.getPriorityId() != null ? String.valueOf(newIssue.getPriorityId()) : "";
                details.add(createJournalDetail(journal.getId(), "attr", "priority_id", oldValue, newValue));
            }

            // 比较分类ID
            if (!Objects.equals(oldIssue.getCategoryId(), newIssue.getCategoryId())) {
                String oldValue = oldIssue.getCategoryId() != null ? getCategoryName(oldIssue.getCategoryId()) : "";
                String newValue = newIssue.getCategoryId() != null ? getCategoryName(newIssue.getCategoryId()) : "";
                details.add(createJournalDetail(journal.getId(), "attr", "category_id", oldValue, newValue));
            }

            // 比较修复版本ID
            if (!Objects.equals(oldIssue.getFixedVersionId(), newIssue.getFixedVersionId())) {
                String oldValue = oldIssue.getFixedVersionId() != null ? String.valueOf(oldIssue.getFixedVersionId())
                        : "";
                String newValue = newIssue.getFixedVersionId() != null ? String.valueOf(newIssue.getFixedVersionId())
                        : "";
                details.add(createJournalDetail(journal.getId(), "attr", "fixed_version_id", oldValue, newValue));
            }

            // 比较标题
            if (!Objects.equals(oldIssue.getSubject(), newIssue.getSubject())) {
                String oldValue = oldIssue.getSubject() != null ? oldIssue.getSubject() : "";
                String newValue = newIssue.getSubject() != null ? newIssue.getSubject() : "";
                details.add(createJournalDetail(journal.getId(), "attr", "subject", oldValue, newValue));
            }

            // 比较描述
            if (!Objects.equals(oldIssue.getDescription(), newIssue.getDescription())) {
                String oldValue = oldIssue.getDescription() != null ? oldIssue.getDescription() : "";
                String newValue = newIssue.getDescription() != null ? newIssue.getDescription() : "";
                details.add(createJournalDetail(journal.getId(), "attr", "description", oldValue, newValue));
            }

            // 比较开始日期
            if (!Objects.equals(oldIssue.getStartDate(), newIssue.getStartDate())) {
                String oldValue = oldIssue.getStartDate() != null ? oldIssue.getStartDate().toString() : "";
                String newValue = newIssue.getStartDate() != null ? newIssue.getStartDate().toString() : "";
                details.add(createJournalDetail(journal.getId(), "attr", "start_date", oldValue, newValue));
            }

            // 比较截止日期
            if (!Objects.equals(oldIssue.getDueDate(), newIssue.getDueDate())) {
                String oldValue = oldIssue.getDueDate() != null ? oldIssue.getDueDate().toString() : "";
                String newValue = newIssue.getDueDate() != null ? newIssue.getDueDate().toString() : "";
                details.add(createJournalDetail(journal.getId(), "attr", "due_date", oldValue, newValue));
            }

            // 比较预估工时
            if (!Objects.equals(oldIssue.getEstimatedHours(), newIssue.getEstimatedHours())) {
                String oldValue = oldIssue.getEstimatedHours() != null ? String.valueOf(oldIssue.getEstimatedHours())
                        : "";
                String newValue = newIssue.getEstimatedHours() != null ? String.valueOf(newIssue.getEstimatedHours())
                        : "";
                details.add(createJournalDetail(journal.getId(), "attr", "estimated_hours", oldValue, newValue));
            }

            // 比较完成度
            if (!Objects.equals(oldIssue.getDoneRatio(), newIssue.getDoneRatio())) {
                String oldValue = oldIssue.getDoneRatio() != null ? String.valueOf(oldIssue.getDoneRatio()) : "";
                String newValue = newIssue.getDoneRatio() != null ? String.valueOf(newIssue.getDoneRatio()) : "";
                details.add(createJournalDetail(journal.getId(), "attr", "done_ratio", oldValue, newValue));
            }

            // 比较父任务ID
            if (!Objects.equals(oldIssue.getParentId(), newIssue.getParentId())) {
                String oldValue = oldIssue.getParentId() != null ? String.valueOf(oldIssue.getParentId()) : "";
                String newValue = newIssue.getParentId() != null ? String.valueOf(newIssue.getParentId()) : "";
                details.add(createJournalDetail(journal.getId(), "attr", "parent_id", oldValue, newValue));
            }

            // 比较是否私有
            if (!Objects.equals(oldIssue.getIsPrivate(), newIssue.getIsPrivate())) {
                String oldValue = oldIssue.getIsPrivate() != null ? String.valueOf(oldIssue.getIsPrivate()) : "";
                String newValue = newIssue.getIsPrivate() != null ? String.valueOf(newIssue.getIsPrivate()) : "";
                details.add(createJournalDetail(journal.getId(), "attr", "is_private", oldValue, newValue));
            }

            // 比较关闭时间
            if (!Objects.equals(oldIssue.getClosedOn(), newIssue.getClosedOn())) {
                String oldValue = oldIssue.getClosedOn() != null ? oldIssue.getClosedOn().toString() : "";
                String newValue = newIssue.getClosedOn() != null ? newIssue.getClosedOn().toString() : "";
                details.add(createJournalDetail(journal.getId(), "attr", "closed_on", oldValue, newValue));
            }

            // 批量保存变更详情
            if (!details.isEmpty()) {
                for (JournalDetail detail : details) {
                    journalDetailMapper.insert(detail);
                }
                log.debug("记录任务变更历史成功，任务ID: {}, 变更字段数: {}", newIssue.getId(), details.size());
            } else {
                // 如果没有字段变更，但有备注，仍然保留 Journal 记录
                log.debug("记录任务备注，任务ID: {}, 无字段变更", newIssue.getId());
            }

        } catch (Exception e) {
            log.error("记录任务变更历史失败，任务ID: {}", newIssue.getId(), e);
            // 不抛出异常，避免影响主流程
        }
    }

    /**
     * 创建 JournalDetail 对象
     */
    private JournalDetail createJournalDetail(Integer journalId, String property, String propKey, String oldValue,
            String value) {
        JournalDetail detail = new JournalDetail();
        detail.setJournalId(journalId);
        detail.setProperty(property);
        detail.setPropKey(propKey);
        detail.setOldValue(oldValue);
        detail.setValue(value);
        return detail;
    }

    /**
     * 获取状态名称
     */
    private String getStatusName(Integer statusId) {
        if (statusId == null) {
            return "";
        }
        try {
            IssueStatus status = issueStatusMapper.selectById(statusId);
            return status != null ? status.getName() : String.valueOf(statusId);
        } catch (Exception e) {
            log.warn("获取状态名称失败，状态ID: {}", statusId, e);
            return String.valueOf(statusId);
        }
    }

    /**
     * 获取用户名称
     */
    private String getUserName(Long userId) {
        if (userId == null) {
            return "";
        }
        try {
            User user = userMapper.selectById(userId);
            return user != null ? user.getLogin() : String.valueOf(userId);
        } catch (Exception e) {
            log.warn("获取用户名称失败，用户ID: {}", userId, e);
            return String.valueOf(userId);
        }
    }

    /**
     * 获取分类名称
     */
    private String getCategoryName(Integer categoryId) {
        if (categoryId == null) {
            return "";
        }
        try {
            IssueCategory category = issueCategoryMapper.selectById(categoryId);
            return category != null ? category.getName() : String.valueOf(categoryId);
        } catch (Exception e) {
            log.warn("获取分类名称失败，分类ID: {}", categoryId, e);
            return String.valueOf(categoryId);
        }
    }
}
