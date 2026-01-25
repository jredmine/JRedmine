package com.github.jredmine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import com.github.yulichang.toolkit.JoinWrappers;
import com.github.jredmine.dto.request.project.MemberRoleAssignRequestDTO;
import com.github.jredmine.dto.request.project.ProjectArchiveRequestDTO;
import com.github.jredmine.dto.request.project.ProjectCopyRequestDTO;
import com.github.jredmine.dto.request.project.ProjectCreateRequestDTO;
import com.github.jredmine.dto.request.project.ProjectFromTemplateRequestDTO;
import com.github.jredmine.dto.request.project.ProjectTemplateCreateRequestDTO;
import com.github.jredmine.dto.request.project.ProjectTemplateUpdateRequestDTO;
import com.github.jredmine.dto.request.project.ProjectMemberCreateRequestDTO;
import com.github.jredmine.dto.request.project.ProjectMemberUpdateRequestDTO;
import com.github.jredmine.dto.request.project.ProjectUpdateRequestDTO;
import com.github.jredmine.dto.request.project.VersionCreateRequestDTO;
import com.github.jredmine.dto.request.project.VersionListRequestDTO;
import com.github.jredmine.dto.request.project.VersionUpdateRequestDTO;
import com.github.jredmine.dto.request.project.VersionIssuesRequestDTO;
import com.github.jredmine.dto.request.project.VersionIssuesBatchAssignRequestDTO;
import com.github.jredmine.dto.request.project.VersionIssuesBatchUnassignRequestDTO;
import com.github.jredmine.dto.response.project.VersionIssuesBatchAssignResponseDTO;
import com.github.jredmine.dto.response.project.VersionIssuesBatchUnassignResponseDTO;
import com.github.jredmine.dto.response.PageResponse;
import com.github.jredmine.dto.response.project.ProjectDetailResponseDTO;
import com.github.jredmine.dto.response.project.ProjectListItemResponseDTO;
import com.github.jredmine.dto.response.project.ProjectMemberJoinDTO;
import com.github.jredmine.dto.response.project.ProjectMemberResponseDTO;
import com.github.jredmine.dto.response.issue.IssueStatisticsResponseDTO;
import com.github.jredmine.dto.response.project.ProjectStatisticsResponseDTO;
import com.github.jredmine.dto.response.project.ProjectTemplateResponseDTO;
import com.github.jredmine.dto.response.project.ProjectTreeNodeResponseDTO;
import com.github.jredmine.dto.response.project.VersionResponseDTO;
import com.github.jredmine.dto.response.project.VersionStatisticsResponseDTO;
import com.github.jredmine.dto.response.issue.IssueListItemResponseDTO;
import com.github.jredmine.entity.EnabledModule;
import com.github.jredmine.entity.EmailAddress;
import com.github.jredmine.entity.Member;
import com.github.jredmine.entity.MemberRole;
import com.github.jredmine.entity.Project;
import com.github.jredmine.entity.ProjectTemplateRole;
import com.github.jredmine.entity.ProjectTracker;
import com.github.jredmine.entity.Role;
import com.github.jredmine.entity.Tracker;
import com.github.jredmine.entity.User;
import com.github.jredmine.entity.Version;
import com.github.jredmine.entity.Issue;
import com.github.jredmine.enums.ProjectModule;
import com.github.jredmine.enums.ProjectStatus;
import com.github.jredmine.enums.ResultCode;
import com.github.jredmine.enums.VersionSharing;
import com.github.jredmine.enums.VersionStatus;
import com.github.jredmine.exception.BusinessException;
import com.github.jredmine.mapper.TrackerMapper;
import com.github.jredmine.mapper.issue.IssueMapper;
import com.github.jredmine.mapper.TimeEntryMapper;
import com.github.jredmine.mapper.workflow.IssueStatusMapper;
import com.github.jredmine.mapper.workflow.EnumerationMapper;
import com.github.jredmine.entity.IssueStatus;
import com.github.jredmine.entity.TimeEntry;
import com.github.jredmine.entity.Enumeration;
import com.github.jredmine.mapper.project.EnabledModuleMapper;
import com.github.jredmine.mapper.project.MemberMapper;
import com.github.jredmine.mapper.project.ProjectMapper;
import com.github.jredmine.mapper.project.ProjectTemplateRoleMapper;
import com.github.jredmine.mapper.project.ProjectTrackerMapper;
import com.github.jredmine.mapper.project.VersionMapper;
import com.github.jredmine.mapper.user.EmailAddressMapper;
import com.github.jredmine.mapper.user.MemberRoleMapper;
import com.github.jredmine.mapper.user.RoleMapper;
import com.github.jredmine.mapper.user.UserMapper;
import com.github.jredmine.security.ProjectPermissionService;
import com.github.jredmine.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 项目服务
 *
 * @author panfeng
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectMapper projectMapper;
    private final MemberMapper memberMapper;
    private final EnabledModuleMapper enabledModuleMapper;
    private final ProjectTrackerMapper projectTrackerMapper;
    private final ProjectTemplateRoleMapper projectTemplateRoleMapper;
    private final TrackerMapper trackerMapper;
    private final UserMapper userMapper;
    private final EmailAddressMapper emailAddressMapper;
    private final RoleMapper roleMapper;
    private final MemberRoleMapper memberRoleMapper;
    private final VersionMapper versionMapper;
    private final IssueMapper issueMapper;
    private final TimeEntryMapper timeEntryMapper;
    private final IssueStatusMapper issueStatusMapper;
    private final EnumerationMapper enumerationMapper;
    private final SecurityUtils securityUtils;
    private final ProjectPermissionService projectPermissionService;
    private final IssueService issueService;

    /**
     * 分页查询项目列表
     *
     * @param current  当前页码
     * @param size     每页数量
     * @param name     项目名称（模糊查询，只在名称中搜索）
     * @param keyword  搜索关键词（在名称和描述中搜索，优先级高于 name）
     * @param status   项目状态（1=活跃，5=关闭，9=归档）
     * @param isPublic 是否公开
     * @param parentId 父项目ID
     * @return 分页响应
     */
    public PageResponse<ProjectListItemResponseDTO> listProjects(
            Integer current, Integer size, String name, String keyword, Integer status, Boolean isPublic,
            Long parentId) {
        MDC.put("operation", "list_projects");

        try {
            // 设置默认值并验证：current 至少为 1，size 至少为 10
            Integer validCurrent = (current != null && current > 0) ? current : 1;
            Integer validSize = (size != null && size > 0) ? size : 10;

            log.debug("开始查询项目列表，页码: {}, 每页数量: {}, 关键词: {}", validCurrent, validSize, keyword);

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
            Page<Project> page = new Page<>(validCurrent, validSize);

            // 构建查询条件
            LambdaQueryWrapper<Project> queryWrapper = new LambdaQueryWrapper<>();

            // 搜索条件：如果提供了 keyword，则在名称和描述中搜索；否则如果提供了 name，则只在名称中搜索
            if (keyword != null && !keyword.trim().isEmpty()) {
                // 在名称和描述中搜索关键词
                queryWrapper.and(wrapper -> {
                    wrapper.like(Project::getName, keyword.trim())
                            .or()
                            .like(Project::getDescription, keyword.trim());
                });
            } else if (name != null && !name.trim().isEmpty()) {
                // 只在名称中模糊查询（保持向后兼容）
                queryWrapper.like(Project::getName, name.trim());
            }

            // 状态筛选
            if (status != null && ProjectStatus.isValidCode(status)) {
                queryWrapper.eq(Project::getStatus, status);
            } else {
                // 默认不显示归档项目和模板
                queryWrapper.ne(Project::getStatus, ProjectStatus.ARCHIVED.getCode())
                        .ne(Project::getStatus, ProjectStatus.TEMPLATE.getCode());
            }

            // 是否公开筛选
            if (isPublic != null) {
                queryWrapper.eq(Project::getIsPublic, isPublic);
            }

            // 父项目ID筛选
            if (parentId != null) {
                queryWrapper.eq(Project::getParentId, parentId);
            }

            // 权限过滤：如果不是管理员，只显示公开项目或用户是成员的项目
            if (!isAdmin) {
                final Set<Long> finalMemberProjectIds = memberProjectIds;
                queryWrapper.and(wrapper -> {
                    wrapper.eq(Project::getIsPublic, true)
                            .or(finalMemberProjectIds != null && !finalMemberProjectIds.isEmpty(),
                                    w -> w.in(Project::getId, finalMemberProjectIds));
                });
            }

            // 按 ID 倒序排序（ID 是主键，有索引，查询更快）
            queryWrapper.orderByDesc(Project::getId);

            // 执行分页查询
            Page<Project> result = projectMapper.selectPage(page, queryWrapper);

            MDC.put("total", String.valueOf(result.getTotal()));
            log.info("项目列表查询成功，共查询到 {} 条记录", result.getTotal());

            // 转换为响应 DTO
            List<ProjectListItemResponseDTO> dtoList = result.getRecords().stream()
                    .map(this::toProjectListItemResponseDTO)
                    .toList();

            return PageResponse.of(
                    dtoList,
                    (int) result.getTotal(),
                    (int) result.getCurrent(),
                    (int) result.getSize());
        } finally {
            MDC.clear();
        }
    }

    /**
     * 根据ID获取项目详情
     *
     * @param id 项目ID
     * @return 项目详情
     */
    public ProjectDetailResponseDTO getProjectById(Long id) {
        MDC.put("operation", "get_project_by_id");
        MDC.put("projectId", String.valueOf(id));

        try {
            log.debug("开始查询项目详情，项目ID: {}", id);

            // 查询项目
            Project project = projectMapper.selectById(id);
            if (project == null) {
                log.warn("项目不存在，项目ID: {}", id);
                throw new BusinessException(ResultCode.PROJECT_NOT_FOUND);
            }

            // 获取当前用户信息
            User currentUser = securityUtils.getCurrentUser();
            boolean isAdmin = Boolean.TRUE.equals(currentUser.getAdmin());

            // 权限验证
            validateProjectAccess(project, currentUser, isAdmin);

            log.info("项目详情查询成功，项目ID: {}", id);

            // 转换为响应 DTO
            return toProjectDetailResponseDTO(project);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("项目详情查询失败，项目ID: {}", id, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "项目详情查询失败");
        } finally {
            MDC.clear();
        }
    }

    /**
     * 将 Project 实体转换为 ProjectListItemResponseDTO
     *
     * @param project 项目实体
     * @return 响应 DTO
     */
    private ProjectListItemResponseDTO toProjectListItemResponseDTO(Project project) {
        ProjectListItemResponseDTO dto = new ProjectListItemResponseDTO();
        dto.setId(project.getId());
        dto.setName(project.getName());
        dto.setDescription(project.getDescription());
        dto.setIdentifier(project.getIdentifier());
        dto.setIsPublic(project.getIsPublic());
        dto.setStatus(project.getStatus());
        dto.setParentId(project.getParentId());
        dto.setCreatedOn(project.getCreatedOn());
        dto.setUpdatedOn(project.getUpdatedOn());
        return dto;
    }

    /**
     * 创建项目
     *
     * @param requestDTO 创建项目请求
     * @return 项目详情
     */
    @Transactional(rollbackFor = Exception.class)
    public ProjectDetailResponseDTO createProject(ProjectCreateRequestDTO requestDTO) {
        MDC.put("operation", "create_project");

        try {
            log.info("开始创建项目，项目名称: {}", requestDTO.getName());

            // 权限验证：需要 create_projects 权限或系统管理员
            User currentUser = securityUtils.getCurrentUser();
            Long currentUserId = currentUser.getId();
            boolean isAdmin = Boolean.TRUE.equals(currentUser.getAdmin());
            if (!isAdmin) {
                // 检查用户是否在任何项目中拥有 create_projects 权限
                Set<String> allPermissions = projectPermissionService.getUserAllPermissions(currentUserId);
                if (!allPermissions.contains("create_projects")) {
                    log.warn("用户无权限创建项目，用户ID: {}", currentUserId);
                    throw new BusinessException(ResultCode.FORBIDDEN, "无权限创建项目，需要 create_projects 权限");
                }
            }

            // 验证项目名称唯一性
            LambdaQueryWrapper<Project> nameQuery = new LambdaQueryWrapper<>();
            nameQuery.eq(Project::getName, requestDTO.getName());
            Project existingProjectByName = projectMapper.selectOne(nameQuery);
            if (existingProjectByName != null) {
                log.warn("项目名称已存在: {}", requestDTO.getName());
                throw new BusinessException(ResultCode.PROJECT_NAME_EXISTS);
            }

            // 验证项目标识符唯一性（如果提供了标识符）
            if (requestDTO.getIdentifier() != null && !requestDTO.getIdentifier().trim().isEmpty()) {
                LambdaQueryWrapper<Project> identifierQuery = new LambdaQueryWrapper<>();
                identifierQuery.eq(Project::getIdentifier, requestDTO.getIdentifier());
                Project existingProjectByIdentifier = projectMapper.selectOne(identifierQuery);
                if (existingProjectByIdentifier != null) {
                    log.warn("项目标识符已存在: {}", requestDTO.getIdentifier());
                    throw new BusinessException(ResultCode.PROJECT_IDENTIFIER_EXISTS);
                }
            }

            // 验证父项目是否存在（如果指定了父项目，且 parentId > 0）
            if (requestDTO.getParentId() != null && requestDTO.getParentId() > 0) {
                Project parentProject = projectMapper.selectById(requestDTO.getParentId());
                if (parentProject == null) {
                    log.warn("父项目不存在，父项目ID: {}", requestDTO.getParentId());
                    throw new BusinessException(ResultCode.PROJECT_PARENT_NOT_FOUND);
                }
            }

            // 验证模块名称有效性
            if (requestDTO.getEnabledModules() != null && !requestDTO.getEnabledModules().isEmpty()) {
                for (String moduleName : requestDTO.getEnabledModules()) {
                    if (!ProjectModule.isValidCode(moduleName)) {
                        log.warn("无效的项目模块: {}", moduleName);
                        throw new BusinessException(ResultCode.PROJECT_MODULE_INVALID, "无效的项目模块: " + moduleName);
                    }
                }
            }

            // 验证跟踪器是否存在
            if (requestDTO.getTrackerIds() != null && !requestDTO.getTrackerIds().isEmpty()) {
                for (Long trackerId : requestDTO.getTrackerIds()) {
                    Tracker tracker = trackerMapper.selectById(trackerId);
                    if (tracker == null) {
                        log.warn("跟踪器不存在，跟踪器ID: {}", trackerId);
                        throw new BusinessException(ResultCode.TRACKER_NOT_FOUND, "跟踪器不存在，ID: " + trackerId);
                    }
                }
            }

            // 创建项目实体
            Project project = new Project();
            project.setName(requestDTO.getName());
            project.setDescription(requestDTO.getDescription());
            project.setHomepage(requestDTO.getHomepage());
            project.setIsPublic(requestDTO.getIsPublic() != null ? requestDTO.getIsPublic() : true);
            // 如果 parentId 为 0 或 null，设置为 null（表示没有父项目）
            project.setParentId(requestDTO.getParentId() != null && requestDTO.getParentId() > 0
                    ? requestDTO.getParentId()
                    : null);
            project.setIdentifier(requestDTO.getIdentifier());
            project.setStatus(ProjectStatus.ACTIVE.getCode());
            project.setInheritMembers(requestDTO.getInheritMembers() != null ? requestDTO.getInheritMembers() : false);
            Date now = new Date();
            project.setCreatedOn(now);
            project.setUpdatedOn(now);

            // 保存项目
            projectMapper.insert(project);
            Long projectId = project.getId();
            log.debug("项目创建成功，项目ID: {}", projectId);

            // 创建项目成员（创建者自动成为成员）
            Member member = new Member();
            member.setProjectId(projectId);
            member.setUserId(currentUserId);
            member.setCreatedOn(now);
            member.setMailNotification(false);
            memberMapper.insert(member);
            log.debug("项目成员创建成功，项目ID: {}, 用户ID: {}", projectId, currentUserId);

            // 创建启用的模块记录
            if (requestDTO.getEnabledModules() != null && !requestDTO.getEnabledModules().isEmpty()) {
                for (String moduleName : requestDTO.getEnabledModules()) {
                    EnabledModule enabledModule = new EnabledModule();
                    enabledModule.setProjectId(projectId);
                    enabledModule.setName(moduleName);
                    enabledModuleMapper.insert(enabledModule);
                }
                log.debug("项目模块创建成功，项目ID: {}, 模块数量: {}", projectId, requestDTO.getEnabledModules().size());
            }

            // 创建项目跟踪器关联
            if (requestDTO.getTrackerIds() != null && !requestDTO.getTrackerIds().isEmpty()) {
                for (Long trackerId : requestDTO.getTrackerIds()) {
                    ProjectTracker projectTracker = new ProjectTracker();
                    projectTracker.setProjectId(projectId);
                    projectTracker.setTrackerId(trackerId);
                    projectTrackerMapper.insert(projectTracker);
                }
                log.debug("项目跟踪器关联创建成功，项目ID: {}, 跟踪器数量: {}", projectId, requestDTO.getTrackerIds().size());
            }

            log.info("项目创建成功，项目ID: {}, 项目名称: {}", projectId, requestDTO.getName());

            // 返回项目详情
            return toProjectDetailResponseDTO(project);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("项目创建失败，项目名称: {}", requestDTO.getName(), e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "项目创建失败");
        } finally {
            MDC.clear();
        }
    }

    /**
     * 更新项目
     *
     * @param id         项目ID
     * @param requestDTO 更新项目请求
     * @return 项目详情
     */
    @Transactional(rollbackFor = Exception.class)
    public ProjectDetailResponseDTO updateProject(Long id, ProjectUpdateRequestDTO requestDTO) {
        MDC.put("operation", "update_project");
        MDC.put("projectId", String.valueOf(id));

        try {
            log.info("开始更新项目，项目ID: {}", id);

            // 查询项目是否存在
            Project project = projectMapper.selectById(id);
            if (project == null) {
                log.warn("项目不存在，项目ID: {}", id);
                throw new BusinessException(ResultCode.PROJECT_NOT_FOUND);
            }

            // 权限验证：需要 edit_projects 权限或系统管理员
            User currentUser = securityUtils.getCurrentUser();
            boolean isAdmin = Boolean.TRUE.equals(currentUser.getAdmin());
            if (!isAdmin) {
                if (!projectPermissionService.hasPermission(currentUser.getId(), id, "edit_projects")) {
                    log.warn("用户无权限更新项目，项目ID: {}, 用户ID: {}", id, currentUser.getId());
                    throw new BusinessException(ResultCode.FORBIDDEN, "无权限更新项目，需要 edit_projects 权限");
                }
            }

            // 验证项目名称唯一性（排除当前项目）
            if (requestDTO.getName() != null && !requestDTO.getName().equals(project.getName())) {
                LambdaQueryWrapper<Project> nameQuery = new LambdaQueryWrapper<>();
                nameQuery.eq(Project::getName, requestDTO.getName())
                        .ne(Project::getId, id);
                Project existingProjectByName = projectMapper.selectOne(nameQuery);
                if (existingProjectByName != null) {
                    log.warn("项目名称已存在: {}", requestDTO.getName());
                    throw new BusinessException(ResultCode.PROJECT_NAME_EXISTS);
                }
                project.setName(requestDTO.getName());
            }

            // 验证项目标识符唯一性（排除当前项目）
            if (requestDTO.getIdentifier() != null && !requestDTO.getIdentifier().trim().isEmpty()
                    && !requestDTO.getIdentifier().equals(project.getIdentifier())) {
                LambdaQueryWrapper<Project> identifierQuery = new LambdaQueryWrapper<>();
                identifierQuery.eq(Project::getIdentifier, requestDTO.getIdentifier())
                        .ne(Project::getId, id);
                Project existingProjectByIdentifier = projectMapper.selectOne(identifierQuery);
                if (existingProjectByIdentifier != null) {
                    log.warn("项目标识符已存在: {}", requestDTO.getIdentifier());
                    throw new BusinessException(ResultCode.PROJECT_IDENTIFIER_EXISTS);
                }
                project.setIdentifier(requestDTO.getIdentifier());
            }

            // 验证父项目是否存在（如果指定了父项目，且 parentId > 0）
            if (requestDTO.getParentId() != null) {
                if (requestDTO.getParentId() > 0) {
                    // 不能将项目设置为自己的父项目
                    if (requestDTO.getParentId().equals(id)) {
                        log.warn("不能将项目设置为自己的父项目，项目ID: {}", id);
                        throw new BusinessException(ResultCode.PARAM_INVALID, "不能将项目设置为自己的父项目");
                    }
                    Project parentProject = projectMapper.selectById(requestDTO.getParentId());
                    if (parentProject == null) {
                        log.warn("父项目不存在，父项目ID: {}", requestDTO.getParentId());
                        throw new BusinessException(ResultCode.PROJECT_PARENT_NOT_FOUND);
                    }
                    project.setParentId(requestDTO.getParentId());
                } else {
                    // parentId 为 0，表示没有父项目
                    project.setParentId(null);
                }
            }

            // 更新其他字段
            if (requestDTO.getDescription() != null) {
                project.setDescription(requestDTO.getDescription());
            }
            if (requestDTO.getHomepage() != null) {
                project.setHomepage(requestDTO.getHomepage());
            }
            if (requestDTO.getIsPublic() != null) {
                project.setIsPublic(requestDTO.getIsPublic());
            }
            if (requestDTO.getStatus() != null && ProjectStatus.isValidCode(requestDTO.getStatus())) {
                project.setStatus(requestDTO.getStatus());
            }
            if (requestDTO.getInheritMembers() != null) {
                project.setInheritMembers(requestDTO.getInheritMembers());
            }

            // 更新更新时间
            project.setUpdatedOn(new Date());

            // 保存项目
            projectMapper.updateById(project);
            log.debug("项目信息更新成功，项目ID: {}", id);

            // 更新启用的模块（如果提供了模块列表）
            if (requestDTO.getEnabledModules() != null) {
                // 验证模块名称有效性
                for (String moduleName : requestDTO.getEnabledModules()) {
                    if (!ProjectModule.isValidCode(moduleName)) {
                        log.warn("无效的项目模块: {}", moduleName);
                        throw new BusinessException(ResultCode.PROJECT_MODULE_INVALID, "无效的项目模块: " + moduleName);
                    }
                }

                // 删除旧的模块记录
                LambdaQueryWrapper<EnabledModule> moduleDeleteQuery = new LambdaQueryWrapper<>();
                moduleDeleteQuery.eq(EnabledModule::getProjectId, id);
                enabledModuleMapper.delete(moduleDeleteQuery);

                // 创建新的模块记录
                if (!requestDTO.getEnabledModules().isEmpty()) {
                    for (String moduleName : requestDTO.getEnabledModules()) {
                        EnabledModule enabledModule = new EnabledModule();
                        enabledModule.setProjectId(id);
                        enabledModule.setName(moduleName);
                        enabledModuleMapper.insert(enabledModule);
                    }
                    log.debug("项目模块更新成功，项目ID: {}, 模块数量: {}", id, requestDTO.getEnabledModules().size());
                } else {
                    log.debug("项目模块已清空，项目ID: {}", id);
                }
            }

            // 更新项目跟踪器关联（如果提供了跟踪器列表）
            if (requestDTO.getTrackerIds() != null) {
                // 验证跟踪器是否存在
                for (Long trackerId : requestDTO.getTrackerIds()) {
                    Tracker tracker = trackerMapper.selectById(trackerId);
                    if (tracker == null) {
                        log.warn("跟踪器不存在，跟踪器ID: {}", trackerId);
                        throw new BusinessException(ResultCode.TRACKER_NOT_FOUND, "跟踪器不存在，ID: " + trackerId);
                    }
                }

                // 删除旧的跟踪器关联
                LambdaQueryWrapper<ProjectTracker> trackerDeleteQuery = new LambdaQueryWrapper<>();
                trackerDeleteQuery.eq(ProjectTracker::getProjectId, id);
                projectTrackerMapper.delete(trackerDeleteQuery);

                // 创建新的跟踪器关联
                if (!requestDTO.getTrackerIds().isEmpty()) {
                    for (Long trackerId : requestDTO.getTrackerIds()) {
                        ProjectTracker projectTracker = new ProjectTracker();
                        projectTracker.setProjectId(id);
                        projectTracker.setTrackerId(trackerId);
                        projectTrackerMapper.insert(projectTracker);
                    }
                    log.debug("项目跟踪器关联更新成功，项目ID: {}, 跟踪器数量: {}", id, requestDTO.getTrackerIds().size());
                } else {
                    log.debug("项目跟踪器关联已清空，项目ID: {}", id);
                }
            }

            log.info("项目更新成功，项目ID: {}", id);

            // 重新查询项目（获取最新数据）
            project = projectMapper.selectById(id);
            return toProjectDetailResponseDTO(project);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("项目更新失败，项目ID: {}", id, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "项目更新失败");
        } finally {
            MDC.clear();
        }
    }

    /**
     * 删除项目（软删除，更新状态为归档）
     *
     * @param id 项目ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteProject(Long id) {
        MDC.put("operation", "delete_project");
        MDC.put("projectId", String.valueOf(id));

        try {
            log.info("开始删除项目，项目ID: {}", id);

            // 查询项目是否存在
            Project project = projectMapper.selectById(id);
            if (project == null) {
                log.warn("项目不存在，项目ID: {}", id);
                throw new BusinessException(ResultCode.PROJECT_NOT_FOUND);
            }

            // 权限验证：需要 delete_projects 权限或系统管理员
            User currentUser = securityUtils.getCurrentUser();
            boolean isAdmin = Boolean.TRUE.equals(currentUser.getAdmin());
            if (!isAdmin) {
                if (!projectPermissionService.hasPermission(currentUser.getId(), id, "delete_projects")) {
                    log.warn("用户无权限删除项目，项目ID: {}, 用户ID: {}", id, currentUser.getId());
                    throw new BusinessException(ResultCode.FORBIDDEN, "无权限删除项目，需要 delete_projects 权限");
                }
            }

            // 检查项目是否已经是归档状态
            if (ProjectStatus.ARCHIVED.getCode().equals(project.getStatus())) {
                log.warn("项目已经是归档状态，项目ID: {}", id);
                throw new BusinessException(ResultCode.PARAM_INVALID, "项目已经是归档状态");
            }

            // 检查是否有子项目
            LambdaQueryWrapper<Project> childrenQuery = new LambdaQueryWrapper<>();
            childrenQuery.eq(Project::getParentId, id)
                    .ne(Project::getStatus, ProjectStatus.ARCHIVED.getCode()); // 排除已归档的子项目
            Long childrenCount = projectMapper.selectCount(childrenQuery);
            if (childrenCount > 0) {
                log.warn("项目存在子项目，不能删除，项目ID: {}, 子项目数量: {}", id, childrenCount);
                throw new BusinessException(ResultCode.PROJECT_HAS_CHILDREN,
                        "项目存在 " + childrenCount + " 个子项目，请先删除或归档子项目");
            }

            // 更新项目状态为归档（软删除）
            project.setStatus(ProjectStatus.ARCHIVED.getCode());
            project.setUpdatedOn(new Date());
            projectMapper.updateById(project);

            log.info("项目删除成功（已归档），项目ID: {}, 项目名称: {}", id, project.getName());

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("项目删除失败，项目ID: {}", id, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "项目删除失败");
        } finally {
            MDC.clear();
        }
    }

    /**
     * 复制项目
     *
     * @param sourceProjectId 源项目ID
     * @param requestDTO      请求DTO
     * @return 新项目详情
     */
    @Transactional(rollbackFor = Exception.class)
    public ProjectDetailResponseDTO copyProject(Long sourceProjectId, ProjectCopyRequestDTO requestDTO) {
        MDC.put("operation", "copy_project");
        MDC.put("sourceProjectId", String.valueOf(sourceProjectId));

        try {
            log.debug("开始复制项目，源项目ID: {}, 新项目名称: {}", sourceProjectId, requestDTO.getName());

            // 权限验证：需要 create_projects 权限或系统管理员
            User currentUser = securityUtils.getCurrentUser();
            boolean isAdmin = Boolean.TRUE.equals(currentUser.getAdmin());
            if (!isAdmin) {
                // 检查用户是否在任何项目中拥有 create_projects 权限
                Set<String> allPermissions = projectPermissionService.getUserAllPermissions(currentUser.getId());
                if (!allPermissions.contains("create_projects")) {
                    log.warn("用户无权限复制项目，源项目ID: {}, 用户ID: {}", sourceProjectId, currentUser.getId());
                    throw new BusinessException(ResultCode.FORBIDDEN, "无权限复制项目，需要 create_projects 权限");
                }
            }

            // 验证源项目是否存在
            Project sourceProject = projectMapper.selectById(sourceProjectId);
            if (sourceProject == null) {
                log.warn("源项目不存在，源项目ID: {}", sourceProjectId);
                throw new BusinessException(ResultCode.PROJECT_NOT_FOUND);
            }

            // 验证新项目名称（必填）
            if (requestDTO.getName() == null || requestDTO.getName().trim().isEmpty()) {
                log.warn("新项目名称不能为空");
                throw new BusinessException(ResultCode.PARAM_ERROR, "新项目名称不能为空");
            }

            // 验证项目名称唯一性
            LambdaQueryWrapper<Project> nameQuery = new LambdaQueryWrapper<>();
            nameQuery.eq(Project::getName, requestDTO.getName());
            Project existingProjectByName = projectMapper.selectOne(nameQuery);
            if (existingProjectByName != null) {
                log.warn("项目名称已存在: {}", requestDTO.getName());
                throw new BusinessException(ResultCode.PROJECT_NAME_EXISTS);
            }

            // 验证项目标识符唯一性（如果提供了标识符）
            if (requestDTO.getIdentifier() != null && !requestDTO.getIdentifier().trim().isEmpty()) {
                LambdaQueryWrapper<Project> identifierQuery = new LambdaQueryWrapper<>();
                identifierQuery.eq(Project::getIdentifier, requestDTO.getIdentifier());
                Project existingProjectByIdentifier = projectMapper.selectOne(identifierQuery);
                if (existingProjectByIdentifier != null) {
                    log.warn("项目标识符已存在: {}", requestDTO.getIdentifier());
                    throw new BusinessException(ResultCode.PROJECT_IDENTIFIER_EXISTS);
                }
            }

            // 创建新项目（复制基本信息）
            Date now = new Date();
            Project newProject = new Project();
            newProject.setName(requestDTO.getName());
            newProject.setDescription(sourceProject.getDescription());
            newProject.setHomepage(sourceProject.getHomepage());
            newProject.setIsPublic(sourceProject.getIsPublic());
            newProject.setParentId(null); // 复制的项目默认没有父项目
            newProject.setIdentifier(requestDTO.getIdentifier());
            newProject.setStatus(ProjectStatus.ACTIVE.getCode());
            newProject.setInheritMembers(sourceProject.getInheritMembers());
            newProject.setCreatedOn(now);
            newProject.setUpdatedOn(now);

            // 保存新项目
            projectMapper.insert(newProject);
            Long newProjectId = newProject.getId();
            log.debug("新项目创建成功，项目ID: {}, 项目名称: {}", newProjectId, requestDTO.getName());

            // 创建项目成员（创建者自动成为成员）
            Long currentUserId = currentUser.getId();
            Member member = new Member();
            member.setProjectId(newProjectId);
            member.setUserId(currentUserId);
            member.setCreatedOn(now);
            member.setMailNotification(false);
            memberMapper.insert(member);
            log.debug("项目成员创建成功，项目ID: {}, 用户ID: {}", newProjectId, currentUserId);

            // 复制模块（如果启用）
            if (Boolean.TRUE.equals(requestDTO.getCopyModules())) {
                LambdaQueryWrapper<EnabledModule> moduleQuery = new LambdaQueryWrapper<>();
                moduleQuery.eq(EnabledModule::getProjectId, sourceProjectId);
                List<EnabledModule> sourceModules = enabledModuleMapper.selectList(moduleQuery);
                for (EnabledModule sourceModule : sourceModules) {
                    EnabledModule newModule = new EnabledModule();
                    newModule.setProjectId(newProjectId);
                    newModule.setName(sourceModule.getName());
                    enabledModuleMapper.insert(newModule);
                }
                log.debug("项目模块复制成功，项目ID: {}, 模块数量: {}", newProjectId, sourceModules.size());
            }

            // 复制跟踪器（如果启用）
            if (Boolean.TRUE.equals(requestDTO.getCopyTrackers())) {
                LambdaQueryWrapper<ProjectTracker> trackerQuery = new LambdaQueryWrapper<>();
                trackerQuery.eq(ProjectTracker::getProjectId, sourceProjectId);
                List<ProjectTracker> sourceTrackers = projectTrackerMapper.selectList(trackerQuery);
                for (ProjectTracker sourceTracker : sourceTrackers) {
                    ProjectTracker newTracker = new ProjectTracker();
                    newTracker.setProjectId(newProjectId);
                    newTracker.setTrackerId(sourceTracker.getTrackerId());
                    projectTrackerMapper.insert(newTracker);
                }
                log.debug("项目跟踪器复制成功，项目ID: {}, 跟踪器数量: {}", newProjectId, sourceTrackers.size());
            }

            // 复制成员（如果启用）
            if (Boolean.TRUE.equals(requestDTO.getCopyMembers())) {
                LambdaQueryWrapper<Member> memberQuery = new LambdaQueryWrapper<>();
                memberQuery.eq(Member::getProjectId, sourceProjectId);
                List<Member> sourceMembers = memberMapper.selectList(memberQuery);
                for (Member sourceMember : sourceMembers) {
                    // 跳过创建者（已经添加过了）
                    if (sourceMember.getUserId().equals(currentUserId)) {
                        continue;
                    }

                    Member newMember = new Member();
                    newMember.setProjectId(newProjectId);
                    newMember.setUserId(sourceMember.getUserId());
                    newMember.setCreatedOn(now);
                    newMember.setMailNotification(sourceMember.getMailNotification());
                    memberMapper.insert(newMember);
                    Long newMemberId = newMember.getId();

                    // 复制成员角色
                    LambdaQueryWrapper<MemberRole> roleQuery = new LambdaQueryWrapper<>();
                    roleQuery.eq(MemberRole::getMemberId, sourceMember.getId().intValue());
                    List<MemberRole> sourceMemberRoles = memberRoleMapper.selectList(roleQuery);
                    for (MemberRole sourceMemberRole : sourceMemberRoles) {
                        MemberRole newMemberRole = new MemberRole();
                        newMemberRole.setMemberId(newMemberId.intValue());
                        newMemberRole.setRoleId(sourceMemberRole.getRoleId());
                        newMemberRole.setInheritedFrom(null); // 复制的角色不是继承的
                        memberRoleMapper.insert(newMemberRole);
                    }
                }
                log.debug("项目成员复制成功，项目ID: {}, 成员数量: {}", newProjectId, sourceMembers.size());
            }

            // 复制版本（暂不支持，记录日志）
            if (Boolean.TRUE.equals(requestDTO.getCopyVersions())) {
                log.warn("复制版本功能暂未实现，项目ID: {}", newProjectId);
            }

            // 复制任务（暂不支持，记录日志）
            if (Boolean.TRUE.equals(requestDTO.getCopyIssues())) {
                log.warn("复制任务功能暂未实现，项目ID: {}", newProjectId);
            }

            log.info("项目复制成功，源项目ID: {}, 新项目ID: {}, 新项目名称: {}",
                    sourceProjectId, newProjectId, requestDTO.getName());

            // 返回新项目详情
            return toProjectDetailResponseDTO(newProject);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("项目复制失败，源项目ID: {}", sourceProjectId, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "项目复制失败");
        } finally {
            MDC.clear();
        }
    }

    /**
     * 归档或取消归档项目
     *
     * @param id         项目ID
     * @param requestDTO 请求DTO
     * @return 项目详情
     */
    @Transactional(rollbackFor = Exception.class)
    public ProjectDetailResponseDTO archiveProject(Long id, ProjectArchiveRequestDTO requestDTO) {
        MDC.put("operation", "archive_project");
        MDC.put("projectId", String.valueOf(id));
        MDC.put("archived", String.valueOf(requestDTO.getArchived()));

        try {
            log.debug("开始{}项目，项目ID: {}", requestDTO.getArchived() ? "归档" : "取消归档", id);

            // 查询项目是否存在
            Project project = projectMapper.selectById(id);
            if (project == null) {
                log.warn("项目不存在，项目ID: {}", id);
                throw new BusinessException(ResultCode.PROJECT_NOT_FOUND);
            }

            // 权限验证：需要 delete_projects 权限或系统管理员
            User currentUser = securityUtils.getCurrentUser();
            boolean isAdmin = Boolean.TRUE.equals(currentUser.getAdmin());
            if (!isAdmin) {
                if (!projectPermissionService.hasPermission(currentUser.getId(), id, "delete_projects")) {
                    log.warn("用户无权限归档项目，项目ID: {}, 用户ID: {}", id, currentUser.getId());
                    throw new BusinessException(ResultCode.FORBIDDEN, "无权限归档项目，需要 delete_projects 权限");
                }
            }

            // 如果是要归档项目
            if (Boolean.TRUE.equals(requestDTO.getArchived())) {
                // 检查项目是否已经是归档状态
                if (ProjectStatus.ARCHIVED.getCode().equals(project.getStatus())) {
                    log.warn("项目已经是归档状态，项目ID: {}", id);
                    throw new BusinessException(ResultCode.PARAM_INVALID, "项目已经是归档状态");
                }

                // 检查是否有未归档的子项目
                LambdaQueryWrapper<Project> childrenQuery = new LambdaQueryWrapper<>();
                childrenQuery.eq(Project::getParentId, id)
                        .ne(Project::getStatus, ProjectStatus.ARCHIVED.getCode()); // 排除已归档的子项目
                Long childrenCount = projectMapper.selectCount(childrenQuery);
                if (childrenCount > 0) {
                    log.warn("项目存在未归档的子项目，不能归档，项目ID: {}, 子项目数量: {}", id, childrenCount);
                    throw new BusinessException(ResultCode.PROJECT_HAS_CHILDREN,
                            "项目存在 " + childrenCount + " 个未归档的子项目，请先归档子项目");
                }

                // 更新项目状态为归档
                project.setStatus(ProjectStatus.ARCHIVED.getCode());
                log.info("项目归档成功，项目ID: {}, 项目名称: {}", id, project.getName());
            } else {
                // 取消归档：更新项目状态为活跃
                if (ProjectStatus.ARCHIVED.getCode().equals(project.getStatus())) {
                    project.setStatus(ProjectStatus.ACTIVE.getCode());
                    log.info("项目取消归档成功，项目ID: {}, 项目名称: {}", id, project.getName());
                } else {
                    log.warn("项目不是归档状态，无法取消归档，项目ID: {}, 当前状态: {}", id, project.getStatus());
                    throw new BusinessException(ResultCode.PARAM_INVALID, "项目不是归档状态，无法取消归档");
                }
            }

            // 更新项目
            project.setUpdatedOn(new Date());
            projectMapper.updateById(project);

            // 重新查询项目（获取最新数据）
            project = projectMapper.selectById(id);
            return toProjectDetailResponseDTO(project);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("项目{}失败，项目ID: {}", requestDTO.getArchived() ? "归档" : "取消归档", id, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR,
                    "项目" + (requestDTO.getArchived() ? "归档" : "取消归档") + "失败");
        } finally {
            MDC.clear();
        }
    }

    /**
     * 获取项目成员列表
     *
     * @param projectId 项目ID
     * @param current   当前页码
     * @param size      每页数量
     * @param name      用户名称（模糊查询）
     * @return 分页响应
     */
    public PageResponse<ProjectMemberResponseDTO> listProjectMembers(
            Long projectId, Integer current, Integer size, String name) {
        MDC.put("operation", "list_project_members");
        MDC.put("projectId", String.valueOf(projectId));

        try {
            // 设置默认值并验证：current 至少为 1，size 至少为 10
            Integer validCurrent = (current != null && current > 0) ? current : 1;
            Integer validSize = (size != null && size > 0) ? size : 10;

            log.debug("开始查询项目成员列表，项目ID: {}, 页码: {}, 每页数量: {}", projectId, validCurrent, validSize);

            // 验证项目是否存在
            Project project = projectMapper.selectById(projectId);
            if (project == null) {
                log.warn("项目不存在，项目ID: {}", projectId);
                throw new BusinessException(ResultCode.PROJECT_NOT_FOUND);
            }

            // 获取当前用户信息
            User currentUser = securityUtils.getCurrentUser();
            boolean isAdmin = Boolean.TRUE.equals(currentUser.getAdmin());

            // 权限验证：如果不是管理员，需要检查是否有权限查看成员
            if (!isAdmin) {
                // 公开项目所有用户可见，私有项目需要是成员
                if (Boolean.FALSE.equals(project.getIsPublic())) {
                    Long currentUserId = currentUser.getId();
                    LambdaQueryWrapper<Member> memberQuery = new LambdaQueryWrapper<>();
                    memberQuery.eq(Member::getProjectId, projectId)
                            .eq(Member::getUserId, currentUserId);
                    Member member = memberMapper.selectOne(memberQuery);

                    if (member == null) {
                        log.warn("用户无权限查看私有项目成员，项目ID: {}, 用户ID: {}", projectId, currentUserId);
                        throw new BusinessException(ResultCode.PROJECT_ACCESS_DENIED);
                    }
                }
            }

            // 先查询去重后的成员ID列表（用于分页）
            // 使用普通查询获取成员列表，然后根据用户名称过滤
            LambdaQueryWrapper<Member> memberQuery = new LambdaQueryWrapper<>();
            memberQuery.eq(Member::getProjectId, projectId);
            List<Member> allMembers = memberMapper.selectList(memberQuery);

            // 如果提供了名称，需要关联用户表进行过滤
            if (name != null && !name.trim().isEmpty()) {
                // 获取所有成员的用户ID
                List<Long> userIds = allMembers.stream()
                        .map(Member::getUserId)
                        .distinct()
                        .collect(Collectors.toList());

                if (userIds.isEmpty()) {
                    return PageResponse.of(List.of(), 0, validCurrent, validSize);
                }

                // 查询用户信息并过滤
                LambdaQueryWrapper<User> userQuery = new LambdaQueryWrapper<>();
                userQuery.in(User::getId, userIds)
                        .and(w -> w
                                .like(User::getFirstname, name)
                                .or()
                                .like(User::getLastname, name)
                                .or()
                                .like(User::getLogin, name));
                List<User> filteredUsers = userMapper.selectList(userQuery);
                Set<Long> filteredUserIds = filteredUsers.stream()
                        .map(User::getId)
                        .collect(Collectors.toSet());

                // 过滤成员列表
                allMembers = allMembers.stream()
                        .filter(m -> filteredUserIds.contains(m.getUserId()))
                        .toList();
            }

            // 去重并获取成员ID列表
            List<Long> allMemberIds = allMembers.stream()
                    .map(Member::getId)
                    .distinct()
                    .toList();
            int totalCount = allMemberIds.size();

            // 计算分页范围
            int start = (validCurrent - 1) * validSize;
            if (start >= totalCount) {
                return PageResponse.of(
                        List.of(),
                        totalCount,
                        validCurrent,
                        validSize);
            }

            // 获取当前页的成员ID列表
            List<Long> memberIds = allMemberIds.stream()
                    .skip(start)
                    .limit(validSize)
                    .collect(Collectors.toList());

            if (memberIds.isEmpty()) {
                return PageResponse.of(
                        List.of(),
                        totalCount,
                        validCurrent,
                        validSize);
            }

            // 使用 mybatis-plus-join 进行连表查询（只查询当前页的成员）
            MPJLambdaWrapper<Member> wrapper = JoinWrappers.lambda(Member.class)
                    // 查询成员表字段
                    .select(Member::getId, Member::getUserId, Member::getCreatedOn, Member::getMailNotification)
                    // 查询用户表字段
                    .select(User::getLogin, User::getFirstname, User::getLastname)
                    // 查询邮箱表字段（默认邮箱）
                    .selectAs(EmailAddress::getAddress, ProjectMemberJoinDTO::getEmail)
                    // 查询角色表字段
                    .selectAs(Role::getId, ProjectMemberJoinDTO::getRoleId)
                    .selectAs(Role::getName, ProjectMemberJoinDTO::getRoleName)
                    // 查询成员角色关联表字段（是否继承）
                    .select(MemberRole::getInheritedFrom)
                    // LEFT JOIN 用户表
                    .leftJoin(User.class, User::getId, Member::getUserId)
                    // LEFT JOIN 邮箱表（默认邮箱）
                    // 注意：mybatis-plus-join 的 JOIN 条件写法
                    // 使用子查询方式获取默认邮箱，或者先 JOIN 所有邮箱再过滤
                    .leftJoin(EmailAddress.class, EmailAddress::getUserId, User::getId)
                    // LEFT JOIN 成员角色关联表
                    .leftJoin(MemberRole.class, MemberRole::getMemberId, Member::getId)
                    // LEFT JOIN 角色表
                    .leftJoin(Role.class, Role::getId, MemberRole::getRoleId)
                    // 查询条件：项目ID 和 成员ID列表
                    .eq(Member::getProjectId, projectId)
                    .in(Member::getId, memberIds)
                    // 按成员ID排序
                    .orderByAsc(Member::getId);

            // 执行连表查询（不分页，因为已经手动分页了）
            List<ProjectMemberJoinDTO> joinResults = memberMapper.selectJoinList(
                    ProjectMemberJoinDTO.class, wrapper);

            // 由于一个成员可能有多个角色，连表查询会返回多条记录
            // 需要按成员ID分组，组装成最终结果
            java.util.Map<Long, ProjectMemberResponseDTO> memberMap = new java.util.LinkedHashMap<>();

            // 先查询所有用户的默认邮箱，用于后续过滤
            Set<Long> userIds = joinResults.stream()
                    .map(ProjectMemberJoinDTO::getUserId)
                    .filter(java.util.Objects::nonNull)
                    .collect(java.util.stream.Collectors.toSet());

            Map<Long, String> userEmailMap = new java.util.HashMap<>();
            if (!userIds.isEmpty()) {
                LambdaQueryWrapper<EmailAddress> emailQuery = new LambdaQueryWrapper<>();
                emailQuery.in(EmailAddress::getUserId, userIds)
                        .eq(EmailAddress::getIsDefault, true);
                List<EmailAddress> defaultEmails = emailAddressMapper.selectList(emailQuery);
                userEmailMap = defaultEmails.stream()
                        .collect(java.util.stream.Collectors.toMap(
                                EmailAddress::getUserId,
                                EmailAddress::getAddress,
                                (v1, v2) -> v1)); // 如果有多个默认邮箱，取第一个
            }

            for (ProjectMemberJoinDTO joinDTO : joinResults) {
                Long memberId = joinDTO.getId();
                ProjectMemberResponseDTO dto = memberMap.get(memberId);

                if (dto == null) {
                    // 创建新的成员DTO
                    dto = new ProjectMemberResponseDTO();
                    dto.setId(joinDTO.getId());
                    dto.setUserId(joinDTO.getUserId());
                    dto.setLogin(joinDTO.getLogin());
                    dto.setFirstname(joinDTO.getFirstname());
                    dto.setLastname(joinDTO.getLastname());
                    // 使用查询到的默认邮箱，而不是 JOIN 结果中的邮箱（因为可能 JOIN 到多个邮箱）
                    dto.setEmail(userEmailMap.get(joinDTO.getUserId()));
                    dto.setCreatedOn(joinDTO.getCreatedOn());
                    dto.setMailNotification(joinDTO.getMailNotification());
                    dto.setRoles(new java.util.ArrayList<>());
                    memberMap.put(memberId, dto);
                }

                // 添加角色信息（如果存在且未重复）
                if (joinDTO.getRoleId() != null) {
                    // 检查是否已经添加过该角色（避免重复）
                    boolean roleExists = dto.getRoles().stream()
                            .anyMatch(r -> r.getRoleId().equals(joinDTO.getRoleId()));
                    if (!roleExists) {
                        ProjectMemberResponseDTO.MemberRoleInfo roleInfo = new ProjectMemberResponseDTO.MemberRoleInfo();
                        roleInfo.setRoleId(joinDTO.getRoleId());
                        roleInfo.setRoleName(joinDTO.getRoleName());
                        roleInfo.setInherited(joinDTO.getInheritedFrom() != null);
                        dto.getRoles().add(roleInfo);
                    }
                }
            }

            // 转换为列表（按成员ID顺序）
            List<ProjectMemberResponseDTO> dtoList = new java.util.ArrayList<>(memberMap.values());

            MDC.put("total", String.valueOf(totalCount));
            log.info("项目成员列表查询成功，项目ID: {}, 共查询到 {} 条记录", projectId, totalCount);

            return PageResponse.of(
                    dtoList,
                    totalCount,
                    validCurrent,
                    validSize);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("项目成员列表查询失败，项目ID: {}", projectId, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "项目成员列表查询失败");
        } finally {
            MDC.clear();
        }
    }

    /**
     * 新增项目成员
     *
     * @param projectId  项目ID
     * @param requestDTO 请求DTO
     * @return 项目成员响应DTO
     */
    @Transactional(rollbackFor = Exception.class)
    public ProjectMemberResponseDTO createProjectMember(Long projectId, ProjectMemberCreateRequestDTO requestDTO) {
        MDC.put("operation", "create_project_member");
        MDC.put("projectId", String.valueOf(projectId));
        MDC.put("userId", String.valueOf(requestDTO.getUserId()));

        try {
            log.debug("开始新增项目成员，项目ID: {}, 用户ID: {}", projectId, requestDTO.getUserId());

            // 验证项目是否存在
            Project project = projectMapper.selectById(projectId);
            if (project == null) {
                log.warn("项目不存在，项目ID: {}", projectId);
                throw new BusinessException(ResultCode.PROJECT_NOT_FOUND);
            }

            // 权限验证：需要 manage_projects 权限或系统管理员
            User currentUser = securityUtils.getCurrentUser();
            boolean isAdmin = Boolean.TRUE.equals(currentUser.getAdmin());
            if (!isAdmin) {
                if (!projectPermissionService.hasPermission(currentUser.getId(), projectId, "manage_projects")) {
                    log.warn("用户无权限添加项目成员，项目ID: {}, 用户ID: {}", projectId, currentUser.getId());
                    throw new BusinessException(ResultCode.FORBIDDEN, "无权限添加项目成员，需要 manage_projects 权限");
                }
            }

            // 验证用户是否存在
            User user = userMapper.selectById(requestDTO.getUserId());
            if (user == null) {
                log.warn("用户不存在，用户ID: {}", requestDTO.getUserId());
                throw new BusinessException(ResultCode.USER_NOT_FOUND);
            }

            // 验证用户是否已经是项目成员
            LambdaQueryWrapper<Member> memberQuery = new LambdaQueryWrapper<>();
            memberQuery.eq(Member::getProjectId, projectId)
                    .eq(Member::getUserId, requestDTO.getUserId());
            Member existingMember = memberMapper.selectOne(memberQuery);
            if (existingMember != null) {
                log.warn("用户已经是项目成员，项目ID: {}, 用户ID: {}", projectId, requestDTO.getUserId());
                throw new BusinessException(ResultCode.PARAM_ERROR, "用户已经是项目成员");
            }

            // 验证角色是否存在且可分配
            if (requestDTO.getRoleIds() != null && !requestDTO.getRoleIds().isEmpty()) {
                for (Integer roleId : requestDTO.getRoleIds()) {
                    Role role = roleMapper.selectById(roleId);
                    if (role == null) {
                        log.warn("角色不存在，角色ID: {}", roleId);
                        throw new BusinessException(ResultCode.ROLE_NOT_FOUND);
                    }
                    // 检查角色是否可分配
                    if (Boolean.FALSE.equals(role.getAssignable())) {
                        log.warn("角色不可分配，角色ID: {}", roleId);
                        throw new BusinessException(ResultCode.PARAM_ERROR, "角色不可分配");
                    }
                }
            }

            // 创建成员记录
            Date now = new Date();
            Member member = new Member();
            member.setProjectId(projectId);
            member.setUserId(requestDTO.getUserId());
            member.setCreatedOn(now);
            member.setMailNotification(requestDTO.getMailNotification() != null
                    ? requestDTO.getMailNotification()
                    : false);
            memberMapper.insert(member);
            Long memberId = member.getId();
            log.debug("项目成员创建成功，项目ID: {}, 用户ID: {}, 成员ID: {}", projectId, requestDTO.getUserId(), memberId);

            // 创建成员角色关联
            if (requestDTO.getRoleIds() != null && !requestDTO.getRoleIds().isEmpty()) {
                for (Integer roleId : requestDTO.getRoleIds()) {
                    MemberRole memberRole = new MemberRole();
                    memberRole.setMemberId(memberId.intValue());
                    memberRole.setRoleId(roleId);
                    memberRole.setInheritedFrom(null); // 直接分配的角色，不是继承的
                    memberRoleMapper.insert(memberRole);
                }
                log.debug("项目成员角色关联创建成功，项目ID: {}, 成员ID: {}, 角色数量: {}",
                        projectId, memberId, requestDTO.getRoleIds().size());
            }

            // 查询并返回成员信息（使用 listProjectMembers 的逻辑，但只查询当前成员）
            // 简化处理：直接构造响应DTO
            ProjectMemberResponseDTO responseDTO = new ProjectMemberResponseDTO();
            responseDTO.setId(memberId);
            responseDTO.setUserId(user.getId());
            responseDTO.setLogin(user.getLogin());
            responseDTO.setFirstname(user.getFirstname());
            responseDTO.setLastname(user.getLastname());
            responseDTO.setCreatedOn(member.getCreatedOn());
            responseDTO.setMailNotification(member.getMailNotification());

            // 查询用户默认邮箱
            LambdaQueryWrapper<EmailAddress> emailQuery = new LambdaQueryWrapper<>();
            emailQuery.eq(EmailAddress::getUserId, user.getId())
                    .eq(EmailAddress::getIsDefault, true);
            EmailAddress defaultEmail = emailAddressMapper.selectOne(emailQuery);
            responseDTO.setEmail(defaultEmail != null ? defaultEmail.getAddress() : null);

            // 查询角色信息
            List<ProjectMemberResponseDTO.MemberRoleInfo> roles = new java.util.ArrayList<>();
            if (requestDTO.getRoleIds() != null && !requestDTO.getRoleIds().isEmpty()) {
                for (Integer roleId : requestDTO.getRoleIds()) {
                    Role role = roleMapper.selectById(roleId);
                    if (role != null) {
                        ProjectMemberResponseDTO.MemberRoleInfo roleInfo = new ProjectMemberResponseDTO.MemberRoleInfo();
                        roleInfo.setRoleId(role.getId());
                        roleInfo.setRoleName(role.getName());
                        roleInfo.setInherited(false);
                        roles.add(roleInfo);
                    }
                }
            }
            responseDTO.setRoles(roles);

            MDC.put("memberId", String.valueOf(memberId));
            log.info("项目成员新增成功，项目ID: {}, 用户ID: {}, 成员ID: {}",
                    projectId, requestDTO.getUserId(), memberId);

            return responseDTO;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("项目成员新增失败，项目ID: {}, 用户ID: {}", projectId, requestDTO.getUserId(), e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "项目成员新增失败");
        } finally {
            MDC.clear();
        }
    }

    /**
     * 更新项目成员
     *
     * @param projectId  项目ID
     * @param memberId   成员ID
     * @param requestDTO 请求DTO
     * @return 项目成员响应DTO
     */
    @Transactional(rollbackFor = Exception.class)
    public ProjectMemberResponseDTO updateProjectMember(Long projectId, Long memberId,
            ProjectMemberUpdateRequestDTO requestDTO) {
        MDC.put("operation", "update_project_member");
        MDC.put("projectId", String.valueOf(projectId));
        MDC.put("memberId", String.valueOf(memberId));

        try {
            log.debug("开始更新项目成员，项目ID: {}, 成员ID: {}", projectId, memberId);

            // 验证项目是否存在
            Project project = projectMapper.selectById(projectId);
            if (project == null) {
                log.warn("项目不存在，项目ID: {}", projectId);
                throw new BusinessException(ResultCode.PROJECT_NOT_FOUND);
            }

            // 权限验证：需要 manage_projects 权限或系统管理员
            User currentUser = securityUtils.getCurrentUser();
            boolean isAdmin = Boolean.TRUE.equals(currentUser.getAdmin());
            if (!isAdmin) {
                if (!projectPermissionService.hasPermission(currentUser.getId(), projectId, "manage_projects")) {
                    log.warn("用户无权限更新项目成员，项目ID: {}, 用户ID: {}", projectId, currentUser.getId());
                    throw new BusinessException(ResultCode.FORBIDDEN, "无权限更新项目成员，需要 manage_projects 权限");
                }
            }

            // 验证成员是否存在且属于该项目
            Member member = memberMapper.selectById(memberId);
            if (member == null) {
                log.warn("成员不存在，成员ID: {}", memberId);
                throw new BusinessException(ResultCode.PARAM_ERROR, "成员不存在");
            }
            if (!member.getProjectId().equals(projectId)) {
                log.warn("成员不属于该项目，项目ID: {}, 成员ID: {}", projectId, memberId);
                throw new BusinessException(ResultCode.PARAM_ERROR, "成员不属于该项目");
            }

            // 更新成员信息（邮件通知设置）
            if (requestDTO.getMailNotification() != null) {
                member.setMailNotification(requestDTO.getMailNotification());
                memberMapper.updateById(member);
                log.debug("项目成员信息更新成功，成员ID: {}, 邮件通知: {}", memberId, requestDTO.getMailNotification());
            }

            // 更新成员角色（删除旧的，添加新的）
            if (requestDTO.getRoleIds() != null) {
                // 验证角色是否存在且可分配
                for (Integer roleId : requestDTO.getRoleIds()) {
                    Role role = roleMapper.selectById(roleId);
                    if (role == null) {
                        log.warn("角色不存在，角色ID: {}", roleId);
                        throw new BusinessException(ResultCode.ROLE_NOT_FOUND);
                    }
                    // 检查角色是否可分配
                    if (Boolean.FALSE.equals(role.getAssignable())) {
                        log.warn("角色不可分配，角色ID: {}", roleId);
                        throw new BusinessException(ResultCode.PARAM_ERROR, "角色不可分配");
                    }
                }

                // 删除旧的成员角色关联（只删除非继承的角色）
                LambdaQueryWrapper<MemberRole> deleteQuery = new LambdaQueryWrapper<>();
                deleteQuery.eq(MemberRole::getMemberId, memberId.intValue())
                        .isNull(MemberRole::getInheritedFrom); // 只删除直接分配的角色，保留继承的角色
                memberRoleMapper.delete(deleteQuery);
                log.debug("删除旧的成员角色关联，成员ID: {}", memberId);

                // 添加新的成员角色关联
                if (!requestDTO.getRoleIds().isEmpty()) {
                    for (Integer roleId : requestDTO.getRoleIds()) {
                        MemberRole memberRole = new MemberRole();
                        memberRole.setMemberId(memberId.intValue());
                        memberRole.setRoleId(roleId);
                        memberRole.setInheritedFrom(null); // 直接分配的角色，不是继承的
                        memberRoleMapper.insert(memberRole);
                    }
                    log.debug("项目成员角色关联更新成功，成员ID: {}, 角色数量: {}",
                            memberId, requestDTO.getRoleIds().size());
                }
            }

            // 查询并返回更新后的成员信息
            // 查询用户信息
            User user = userMapper.selectById(member.getUserId());
            if (user == null) {
                log.warn("用户不存在，用户ID: {}", member.getUserId());
                throw new BusinessException(ResultCode.USER_NOT_FOUND);
            }

            // 构造响应DTO
            ProjectMemberResponseDTO responseDTO = new ProjectMemberResponseDTO();
            responseDTO.setId(memberId);
            responseDTO.setUserId(user.getId());
            responseDTO.setLogin(user.getLogin());
            responseDTO.setFirstname(user.getFirstname());
            responseDTO.setLastname(user.getLastname());
            responseDTO.setCreatedOn(member.getCreatedOn());
            responseDTO.setMailNotification(member.getMailNotification());

            // 查询用户默认邮箱
            LambdaQueryWrapper<EmailAddress> emailQuery = new LambdaQueryWrapper<>();
            emailQuery.eq(EmailAddress::getUserId, user.getId())
                    .eq(EmailAddress::getIsDefault, true);
            EmailAddress defaultEmail = emailAddressMapper.selectOne(emailQuery);
            responseDTO.setEmail(defaultEmail != null ? defaultEmail.getAddress() : null);

            // 查询角色信息（包括继承的角色）
            List<ProjectMemberResponseDTO.MemberRoleInfo> roles = new java.util.ArrayList<>();
            LambdaQueryWrapper<MemberRole> roleQuery = new LambdaQueryWrapper<>();
            roleQuery.eq(MemberRole::getMemberId, memberId.intValue());
            List<MemberRole> memberRoles = memberRoleMapper.selectList(roleQuery);

            for (MemberRole memberRole : memberRoles) {
                Role role = roleMapper.selectById(memberRole.getRoleId());
                if (role != null) {
                    ProjectMemberResponseDTO.MemberRoleInfo roleInfo = new ProjectMemberResponseDTO.MemberRoleInfo();
                    roleInfo.setRoleId(role.getId());
                    roleInfo.setRoleName(role.getName());
                    roleInfo.setInherited(memberRole.getInheritedFrom() != null);
                    roles.add(roleInfo);
                }
            }
            responseDTO.setRoles(roles);

            log.info("项目成员更新成功，项目ID: {}, 成员ID: {}", projectId, memberId);

            return responseDTO;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("项目成员更新失败，项目ID: {}, 成员ID: {}", projectId, memberId, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "项目成员更新失败");
        } finally {
            MDC.clear();
        }
    }

    /**
     * 移除项目成员
     *
     * @param projectId 项目ID
     * @param memberId  成员ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void removeProjectMember(Long projectId, Long memberId) {
        MDC.put("operation", "remove_project_member");
        MDC.put("projectId", String.valueOf(projectId));
        MDC.put("memberId", String.valueOf(memberId));

        try {
            log.debug("开始移除项目成员，项目ID: {}, 成员ID: {}", projectId, memberId);

            // 验证项目是否存在
            Project project = projectMapper.selectById(projectId);
            if (project == null) {
                log.warn("项目不存在，项目ID: {}", projectId);
                throw new BusinessException(ResultCode.PROJECT_NOT_FOUND);
            }

            // 权限验证：需要 manage_projects 权限或系统管理员
            User currentUser = securityUtils.getCurrentUser();
            boolean isAdmin = Boolean.TRUE.equals(currentUser.getAdmin());
            if (!isAdmin) {
                if (!projectPermissionService.hasPermission(currentUser.getId(), projectId, "manage_projects")) {
                    log.warn("用户无权限移除项目成员，项目ID: {}, 用户ID: {}", projectId, currentUser.getId());
                    throw new BusinessException(ResultCode.FORBIDDEN, "无权限移除项目成员，需要 manage_projects 权限");
                }
            }

            // 验证成员是否存在且属于该项目
            Member member = memberMapper.selectById(memberId);
            if (member == null) {
                log.warn("成员不存在，成员ID: {}", memberId);
                throw new BusinessException(ResultCode.PARAM_ERROR, "成员不存在");
            }
            if (!member.getProjectId().equals(projectId)) {
                log.warn("成员不属于该项目，项目ID: {}, 成员ID: {}", projectId, memberId);
                throw new BusinessException(ResultCode.PARAM_ERROR, "成员不属于该项目");
            }

            // 删除成员角色关联（member_roles 表）
            LambdaQueryWrapper<MemberRole> roleQuery = new LambdaQueryWrapper<>();
            roleQuery.eq(MemberRole::getMemberId, memberId.intValue());
            int deletedRoles = memberRoleMapper.delete(roleQuery);
            log.debug("删除成员角色关联，成员ID: {}, 删除数量: {}", memberId, deletedRoles);

            // 删除成员记录（members 表）
            memberMapper.deleteById(memberId);
            log.debug("删除成员记录，成员ID: {}", memberId);

            log.info("项目成员移除成功，项目ID: {}, 成员ID: {}, 用户ID: {}",
                    projectId, memberId, member.getUserId());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("项目成员移除失败，项目ID: {}, 成员ID: {}", projectId, memberId, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "项目成员移除失败");
        } finally {
            MDC.clear();
        }
    }

    /**
     * 分配角色给项目成员
     *
     * @param projectId  项目ID
     * @param memberId   成员ID
     * @param requestDTO 请求DTO
     */
    @Transactional(rollbackFor = Exception.class)
    public void assignRolesToMember(Long projectId, Long memberId, MemberRoleAssignRequestDTO requestDTO) {
        MDC.put("operation", "assign_roles_to_member");
        MDC.put("projectId", String.valueOf(projectId));
        MDC.put("memberId", String.valueOf(memberId));

        try {
            log.debug("开始分配角色给项目成员，项目ID: {}, 成员ID: {}, 角色IDs: {}",
                    projectId, memberId, requestDTO.getRoleIds());

            // 验证项目是否存在
            Project project = projectMapper.selectById(projectId);
            if (project == null) {
                log.warn("项目不存在，项目ID: {}", projectId);
                throw new BusinessException(ResultCode.PROJECT_NOT_FOUND);
            }

            // 权限验证：需要 manage_projects 权限或系统管理员
            User currentUser = securityUtils.getCurrentUser();
            boolean isAdmin = Boolean.TRUE.equals(currentUser.getAdmin());
            if (!isAdmin) {
                if (!projectPermissionService.hasPermission(currentUser.getId(), projectId, "manage_projects")) {
                    log.warn("用户无权限分配角色给项目成员，项目ID: {}, 用户ID: {}", projectId, currentUser.getId());
                    throw new BusinessException(ResultCode.FORBIDDEN, "无权限分配角色给项目成员，需要 manage_projects 权限");
                }
            }

            // 验证成员是否存在且属于该项目
            Member member = memberMapper.selectById(memberId);
            if (member == null) {
                log.warn("成员不存在，成员ID: {}", memberId);
                throw new BusinessException(ResultCode.PARAM_ERROR, "成员不存在");
            }
            if (!member.getProjectId().equals(projectId)) {
                log.warn("成员不属于该项目，项目ID: {}, 成员ID: {}", projectId, memberId);
                throw new BusinessException(ResultCode.PARAM_ERROR, "成员不属于该项目");
            }

            // 验证角色是否存在且可分配（分配角色接口要求必须有角色）
            if (requestDTO.getRoleIds() == null || requestDTO.getRoleIds().isEmpty()) {
                log.warn("角色ID列表不能为空");
                throw new BusinessException(ResultCode.PARAM_ERROR, "角色ID列表不能为空");
            }

            for (Integer roleId : requestDTO.getRoleIds()) {
                Role role = roleMapper.selectById(roleId);
                if (role == null) {
                    log.warn("角色不存在，角色ID: {}", roleId);
                    throw new BusinessException(ResultCode.ROLE_NOT_FOUND);
                }
                // 检查角色是否可分配
                if (Boolean.FALSE.equals(role.getAssignable())) {
                    log.warn("角色不可分配，角色ID: {}", roleId);
                    throw new BusinessException(ResultCode.PARAM_ERROR, "角色不可分配");
                }
            }

            // 查询成员已有的角色（只查询直接分配的角色，不包括继承的角色）
            LambdaQueryWrapper<MemberRole> existingRoleQuery = new LambdaQueryWrapper<>();
            existingRoleQuery.eq(MemberRole::getMemberId, memberId.intValue())
                    .isNull(MemberRole::getInheritedFrom);
            List<MemberRole> existingMemberRoles = memberRoleMapper.selectList(existingRoleQuery);
            Set<Integer> existingRoleIds = existingMemberRoles.stream()
                    .map(MemberRole::getRoleId)
                    .collect(java.util.stream.Collectors.toSet());

            // 添加新的角色关联（跳过已存在的角色）
            int addedCount = 0;
            for (Integer roleId : requestDTO.getRoleIds()) {
                // 如果角色已存在，跳过
                if (existingRoleIds.contains(roleId)) {
                    log.debug("成员已有该角色，跳过，成员ID: {}, 角色ID: {}", memberId, roleId);
                    continue;
                }

                // 创建新的成员角色关联
                MemberRole memberRole = new MemberRole();
                memberRole.setMemberId(memberId.intValue());
                memberRole.setRoleId(roleId);
                memberRole.setInheritedFrom(null); // 直接分配的角色，不是继承的
                memberRoleMapper.insert(memberRole);
                addedCount++;
                log.debug("角色分配成功，成员ID: {}, 角色ID: {}", memberId, roleId);
            }

            log.info("项目成员角色分配成功，项目ID: {}, 成员ID: {}, 新增角色数量: {}",
                    projectId, memberId, addedCount);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("项目成员角色分配失败，项目ID: {}, 成员ID: {}", projectId, memberId, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "项目成员角色分配失败");
        } finally {
            MDC.clear();
        }
    }

    /**
     * 更新项目成员角色（替换现有角色）
     *
     * @param projectId  项目ID
     * @param memberId   成员ID
     * @param requestDTO 请求DTO
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateMemberRoles(Long projectId, Long memberId, MemberRoleAssignRequestDTO requestDTO) {
        MDC.put("operation", "update_member_roles");
        MDC.put("projectId", String.valueOf(projectId));
        MDC.put("memberId", String.valueOf(memberId));

        try {
            log.debug("开始更新项目成员角色，项目ID: {}, 成员ID: {}, 角色IDs: {}",
                    projectId, memberId, requestDTO.getRoleIds());

            // 验证项目是否存在
            Project project = projectMapper.selectById(projectId);
            if (project == null) {
                log.warn("项目不存在，项目ID: {}", projectId);
                throw new BusinessException(ResultCode.PROJECT_NOT_FOUND);
            }

            // 权限验证：需要 manage_projects 权限或系统管理员
            User currentUser = securityUtils.getCurrentUser();
            boolean isAdmin = Boolean.TRUE.equals(currentUser.getAdmin());
            if (!isAdmin) {
                if (!projectPermissionService.hasPermission(currentUser.getId(), projectId, "manage_projects")) {
                    log.warn("用户无权限更新项目成员角色，项目ID: {}, 用户ID: {}", projectId, currentUser.getId());
                    throw new BusinessException(ResultCode.FORBIDDEN, "无权限更新项目成员角色，需要 manage_projects 权限");
                }
            }

            // 验证成员是否存在且属于该项目
            Member member = memberMapper.selectById(memberId);
            if (member == null) {
                log.warn("成员不存在，成员ID: {}", memberId);
                throw new BusinessException(ResultCode.PARAM_ERROR, "成员不存在");
            }
            if (!member.getProjectId().equals(projectId)) {
                log.warn("成员不属于该项目，项目ID: {}, 成员ID: {}", projectId, memberId);
                throw new BusinessException(ResultCode.PARAM_ERROR, "成员不属于该项目");
            }

            // 验证角色是否存在且可分配（如果提供了角色列表）
            if (requestDTO.getRoleIds() != null && !requestDTO.getRoleIds().isEmpty()) {
                for (Integer roleId : requestDTO.getRoleIds()) {
                    Role role = roleMapper.selectById(roleId);
                    if (role == null) {
                        log.warn("角色不存在，角色ID: {}", roleId);
                        throw new BusinessException(ResultCode.ROLE_NOT_FOUND);
                    }
                    // 检查角色是否可分配
                    if (Boolean.FALSE.equals(role.getAssignable())) {
                        log.warn("角色不可分配，角色ID: {}", roleId);
                        throw new BusinessException(ResultCode.PARAM_ERROR, "角色不可分配");
                    }
                }
            }

            // 删除旧的直接分配的角色（保留继承的角色）
            LambdaQueryWrapper<MemberRole> deleteQuery = new LambdaQueryWrapper<>();
            deleteQuery.eq(MemberRole::getMemberId, memberId.intValue())
                    .isNull(MemberRole::getInheritedFrom); // 只删除直接分配的角色，保留继承的角色
            int deletedCount = memberRoleMapper.delete(deleteQuery);
            log.debug("删除旧的成员角色关联，成员ID: {}, 删除数量: {}", memberId, deletedCount);

            // 添加新的角色关联
            if (requestDTO.getRoleIds() != null && !requestDTO.getRoleIds().isEmpty()) {
                for (Integer roleId : requestDTO.getRoleIds()) {
                    MemberRole memberRole = new MemberRole();
                    memberRole.setMemberId(memberId.intValue());
                    memberRole.setRoleId(roleId);
                    memberRole.setInheritedFrom(null); // 直接分配的角色，不是继承的
                    memberRoleMapper.insert(memberRole);
                }
                log.debug("项目成员角色更新成功，成员ID: {}, 角色数量: {}",
                        memberId, requestDTO.getRoleIds().size());
            } else {
                log.debug("项目成员角色已清空，成员ID: {}", memberId);
            }

            int roleCount = requestDTO.getRoleIds() != null ? requestDTO.getRoleIds().size() : 0;
            log.info("项目成员角色更新成功，项目ID: {}, 成员ID: {}, 角色数量: {}",
                    projectId, memberId, roleCount);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("项目成员角色更新失败，项目ID: {}, 成员ID: {}", projectId, memberId, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "项目成员角色更新失败");
        } finally {
            MDC.clear();
        }
    }

    /**
     * 获取项目子项目列表
     *
     * @param projectId 项目ID
     * @return 子项目列表
     */
    public List<ProjectListItemResponseDTO> getProjectChildren(Long projectId) {
        MDC.put("operation", "get_project_children");
        MDC.put("projectId", String.valueOf(projectId));

        try {
            log.debug("开始查询项目子项目列表，项目ID: {}", projectId);

            // 验证项目是否存在
            Project project = projectMapper.selectById(projectId);
            if (project == null) {
                log.warn("项目不存在，项目ID: {}", projectId);
                throw new BusinessException(ResultCode.PROJECT_NOT_FOUND);
            }

            // 获取当前用户信息
            User currentUser = securityUtils.getCurrentUser();
            boolean isAdmin = Boolean.TRUE.equals(currentUser.getAdmin());

            // 权限验证
            validateProjectAccess(project, currentUser, isAdmin);

            // 查询子项目（使用 parent_id 查询）
            LambdaQueryWrapper<Project> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Project::getParentId, projectId)
                    // 默认不显示归档项目
                    .ne(Project::getStatus, ProjectStatus.ARCHIVED.getCode())
                    // 按 ID 倒序排序
                    .orderByDesc(Project::getId);

            // 权限过滤：如果不是管理员，只显示公开项目或用户是成员的项目
            if (!isAdmin) {
                Long currentUserId = currentUser.getId();
                // 获取当前用户是成员的项目ID集合
                LambdaQueryWrapper<Member> memberQuery = new LambdaQueryWrapper<>();
                memberQuery.eq(Member::getUserId, currentUserId);
                List<Member> members = memberMapper.selectList(memberQuery);
                Set<Long> memberProjectIds = members.stream()
                        .map(Member::getProjectId)
                        .collect(Collectors.toSet());

                queryWrapper.and(wrapper -> {
                    wrapper.eq(Project::getIsPublic, true)
                            .or(!memberProjectIds.isEmpty(),
                                    w -> w.in(Project::getId, memberProjectIds));
                });
            }

            List<Project> children = projectMapper.selectList(queryWrapper);

            // 转换为响应 DTO
            List<ProjectListItemResponseDTO> dtoList = children.stream()
                    .map(this::toProjectListItemResponseDTO)
                    .toList();

            MDC.put("count", String.valueOf(dtoList.size()));
            log.info("项目子项目列表查询成功，项目ID: {}, 共查询到 {} 条记录", projectId, dtoList.size());

            return dtoList;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("项目子项目列表查询失败，项目ID: {}", projectId, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "项目子项目列表查询失败");
        } finally {
            MDC.clear();
        }
    }

    /**
     * 验证用户是否有权限访问项目
     *
     * @param project     项目实体
     * @param currentUser 当前用户
     * @param isAdmin     是否是管理员
     */
    private void validateProjectAccess(Project project, User currentUser, boolean isAdmin) {
        // 管理员可以访问所有项目
        if (isAdmin) {
            return;
        }

        // 公开项目所有用户可见
        if (Boolean.TRUE.equals(project.getIsPublic())) {
            log.debug("项目是公开项目，允许访问，项目ID: {}", project.getId());
            return;
        }

        // 私有项目需要检查用户是否是项目成员
        Long currentUserId = currentUser.getId();
        LambdaQueryWrapper<Member> memberQuery = new LambdaQueryWrapper<>();
        memberQuery.eq(Member::getProjectId, project.getId())
                .eq(Member::getUserId, currentUserId);
        Member member = memberMapper.selectOne(memberQuery);

        if (member == null) {
            log.warn("用户无权限访问私有项目，项目ID: {}, 用户ID: {}", project.getId(), currentUserId);
            throw new BusinessException(ResultCode.PROJECT_ACCESS_DENIED);
        }

        log.debug("用户是项目成员，允许访问，项目ID: {}, 用户ID: {}", project.getId(), currentUserId);
    }

    /**
     * 获取项目树
     *
     * @param rootId 根项目ID（可选，如果不指定，返回所有顶级项目）
     * @return 项目树列表
     */
    public List<ProjectTreeNodeResponseDTO> getProjectTree(Long rootId) {
        MDC.put("operation", "get_project_tree");
        if (rootId != null) {
            MDC.put("rootId", String.valueOf(rootId));
        }

        try {
            log.debug("开始查询项目树，根项目ID: {}", rootId);

            // 获取当前用户信息
            User currentUser = securityUtils.getCurrentUser();
            boolean isAdmin = Boolean.TRUE.equals(currentUser.getAdmin());

            // 构建查询条件
            LambdaQueryWrapper<Project> queryWrapper = new LambdaQueryWrapper<>();
            // 默认不显示归档项目
            queryWrapper.ne(Project::getStatus, ProjectStatus.ARCHIVED.getCode());

            // 权限过滤：如果不是管理员，只显示公开项目或用户是成员的项目
            if (!isAdmin) {
                Long currentUserId = currentUser.getId();
                // 获取当前用户是成员的项目ID集合
                LambdaQueryWrapper<Member> memberQuery = new LambdaQueryWrapper<>();
                memberQuery.eq(Member::getUserId, currentUserId);
                List<Member> members = memberMapper.selectList(memberQuery);
                Set<Long> memberProjectIds = members.stream()
                        .map(Member::getProjectId)
                        .collect(Collectors.toSet());

                queryWrapper.and(wrapper -> {
                    wrapper.eq(Project::getIsPublic, true)
                            .or(!memberProjectIds.isEmpty(),
                                    w -> w.in(Project::getId, memberProjectIds));
                });
            }

            // 如果指定了根项目ID，验证根项目是否存在
            if (rootId != null) {
                Project rootProject = projectMapper.selectById(rootId);
                if (rootProject == null) {
                    log.warn("根项目不存在，根项目ID: {}", rootId);
                    throw new BusinessException(ResultCode.PROJECT_NOT_FOUND);
                }

                // 验证用户是否有权限访问根项目
                validateProjectAccess(rootProject, currentUser, isAdmin);

                // 查询以根项目为根的子树（使用 lft 和 rgt，如果可用）
                // 如果 lft 和 rgt 为空，则使用 parent_id 递归查询
                if (rootProject.getLft() != null && rootProject.getRgt() != null) {
                    // 使用嵌套集合模型查询
                    queryWrapper.ge(Project::getLft, rootProject.getLft())
                            .le(Project::getRgt, rootProject.getRgt());
                } else {
                    // 使用 parent_id 递归查询（需要查询所有项目，然后在内存中构建树）
                    // 这里先查询所有项目，然后过滤
                }
            }

            // 按 ID 排序
            queryWrapper.orderByAsc(Project::getId);

            // 执行查询
            List<Project> allProjects = projectMapper.selectList(queryWrapper);

            // 如果指定了根项目ID且使用 parent_id，需要过滤出子树
            if (rootId != null) {
                // 收集所有需要包含的项目ID（包括根项目及其所有子孙项目）
                Set<Long> includeProjectIds = new java.util.HashSet<>();
                includeProjectIds.add(rootId);
                collectDescendantIds(allProjects, rootId, includeProjectIds);

                // 过滤出子树项目
                allProjects = allProjects.stream()
                        .filter(p -> includeProjectIds.contains(p.getId()))
                        .toList();
            }

            // 构建树形结构
            List<ProjectTreeNodeResponseDTO> tree = buildProjectTree(allProjects, rootId);

            MDC.put("count", String.valueOf(tree.size()));
            log.info("项目树查询成功，根项目ID: {}, 共查询到 {} 个根节点", rootId, tree.size());

            return tree;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("项目树查询失败，根项目ID: {}", rootId, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "项目树查询失败");
        } finally {
            MDC.clear();
        }
    }

    /**
     * 递归收集所有子孙项目ID
     *
     * @param allProjects 所有项目列表
     * @param parentId    父项目ID
     * @param result      结果集合
     */
    private void collectDescendantIds(List<Project> allProjects, Long parentId, Set<Long> result) {
        for (Project project : allProjects) {
            if (parentId.equals(project.getParentId())) {
                result.add(project.getId());
                collectDescendantIds(allProjects, project.getId(), result);
            }
        }
    }

    /**
     * 构建项目树形结构
     *
     * @param allProjects 所有项目列表
     * @param rootId      根项目ID（如果为null，返回所有顶级项目）
     * @return 项目树列表
     */
    private List<ProjectTreeNodeResponseDTO> buildProjectTree(List<Project> allProjects, Long rootId) {
        // 将项目列表转换为 Map，便于查找
        Map<Long, ProjectTreeNodeResponseDTO> nodeMap = new java.util.HashMap<>();
        for (Project project : allProjects) {
            ProjectTreeNodeResponseDTO node = toProjectTreeNodeResponseDTO(project);
            node.setChildren(new java.util.ArrayList<>());
            nodeMap.put(project.getId(), node);
        }

        // 构建树形结构
        List<ProjectTreeNodeResponseDTO> roots = new java.util.ArrayList<>();
        for (Project project : allProjects) {
            ProjectTreeNodeResponseDTO node = nodeMap.get(project.getId());
            Long parentId = project.getParentId();

            if (parentId == null) {
                // 顶级项目
                if (rootId == null || project.getId().equals(rootId)) {
                    roots.add(node);
                }
            } else {
                // 有父项目的项目
                ProjectTreeNodeResponseDTO parentNode = nodeMap.get(parentId);
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

        // 如果指定了根项目ID，返回根项目节点（包含子树）
        if (rootId != null) {
            ProjectTreeNodeResponseDTO rootNode = nodeMap.get(rootId);
            if (rootNode != null) {
                return List.of(rootNode);
            }
            // 如果根项目不在列表中，返回空列表
            return List.of();
        }

        return roots;
    }

    /**
     * 将 Project 实体转换为 ProjectTreeNodeResponseDTO
     *
     * @param project 项目实体
     * @return 响应 DTO
     */
    private ProjectTreeNodeResponseDTO toProjectTreeNodeResponseDTO(Project project) {
        ProjectTreeNodeResponseDTO dto = new ProjectTreeNodeResponseDTO();
        dto.setId(project.getId());
        dto.setName(project.getName());
        dto.setDescription(project.getDescription());
        dto.setIdentifier(project.getIdentifier());
        dto.setIsPublic(project.getIsPublic());
        dto.setStatus(project.getStatus());
        dto.setParentId(project.getParentId());
        dto.setCreatedOn(project.getCreatedOn());
        dto.setUpdatedOn(project.getUpdatedOn());
        return dto;
    }

    /**
     * 将 Project 实体转换为 ProjectDetailResponseDTO
     *
     * @param project 项目实体
     * @return 响应 DTO
     */
    private ProjectDetailResponseDTO toProjectDetailResponseDTO(Project project) {
        ProjectDetailResponseDTO dto = new ProjectDetailResponseDTO();
        dto.setId(project.getId());
        dto.setName(project.getName());
        dto.setDescription(project.getDescription());
        dto.setHomepage(project.getHomepage());
        dto.setIsPublic(project.getIsPublic());
        dto.setParentId(project.getParentId());
        dto.setCreatedOn(project.getCreatedOn());
        dto.setUpdatedOn(project.getUpdatedOn());
        dto.setIdentifier(project.getIdentifier());
        dto.setStatus(project.getStatus());
        dto.setInheritMembers(project.getInheritMembers());
        dto.setDefaultVersionId(project.getDefaultVersionId());
        dto.setDefaultAssignedToId(project.getDefaultAssignedToId());
        dto.setDefaultIssueQueryId(project.getDefaultIssueQueryId());
        return dto;
    }

    /**
     * 获取项目统计信息
     *
     * @param projectId 项目ID
     * @return 项目统计信息
     */
    public ProjectStatisticsResponseDTO getProjectStatistics(Long projectId) {
        MDC.put("operation", "get_project_statistics");
        MDC.put("projectId", String.valueOf(projectId));

        try {
            log.debug("开始查询项目统计信息，项目ID: {}", projectId);

            // 验证项目是否存在
            Project project = projectMapper.selectById(projectId);
            if (project == null) {
                log.warn("项目不存在，项目ID: {}", projectId);
                throw new BusinessException(ResultCode.PROJECT_NOT_FOUND);
            }

            // 获取当前用户信息
            User currentUser = securityUtils.getCurrentUser();
            boolean isAdmin = Boolean.TRUE.equals(currentUser.getAdmin());

            // 权限验证
            validateProjectAccess(project, currentUser, isAdmin);

            // 构建统计信息
            ProjectStatisticsResponseDTO statistics = new ProjectStatisticsResponseDTO();
            statistics.setProjectId(projectId);
            statistics.setProjectName(project.getName());
            statistics.setLastUpdatedOn(project.getUpdatedOn());

            // 统计成员数量
            LambdaQueryWrapper<Member> memberQuery = new LambdaQueryWrapper<>();
            memberQuery.eq(Member::getProjectId, projectId);
            Long memberCount = memberMapper.selectCount(memberQuery);
            statistics.setMemberCount(memberCount.intValue());
            log.debug("项目成员数量: {}", memberCount);

            // 统计子项目数量（不包括归档的子项目）
            LambdaQueryWrapper<Project> childrenQuery = new LambdaQueryWrapper<>();
            childrenQuery.eq(Project::getParentId, projectId)
                    .ne(Project::getStatus, ProjectStatus.ARCHIVED.getCode());
            Long childrenCount = projectMapper.selectCount(childrenQuery);
            statistics.setChildrenCount(childrenCount.intValue());
            log.debug("项目子项目数量: {}", childrenCount);

            // 统计启用的模块数量
            LambdaQueryWrapper<EnabledModule> moduleQuery = new LambdaQueryWrapper<>();
            moduleQuery.eq(EnabledModule::getProjectId, projectId);
            Long moduleCount = enabledModuleMapper.selectCount(moduleQuery);
            statistics.setEnabledModuleCount(moduleCount.intValue());
            log.debug("项目启用模块数量: {}", moduleCount);

            // 统计跟踪器数量
            LambdaQueryWrapper<ProjectTracker> trackerQuery = new LambdaQueryWrapper<>();
            trackerQuery.eq(ProjectTracker::getProjectId, projectId);
            Long trackerCount = projectTrackerMapper.selectCount(trackerQuery);
            statistics.setTrackerCount(trackerCount.intValue());
            log.debug("项目跟踪器数量: {}", trackerCount);

            // 任务统计
            try {
                IssueStatisticsResponseDTO issueStatistics = issueService.getIssueStatistics(projectId);
                if (issueStatistics != null) {
                    ProjectStatisticsResponseDTO.IssueStatistics projectIssueStatistics = 
                            new ProjectStatisticsResponseDTO.IssueStatistics();
                    projectIssueStatistics.setTotalCount(issueStatistics.getTotalCount());
                    projectIssueStatistics.setInProgressCount(issueStatistics.getInProgressCount());
                    projectIssueStatistics.setCompletedCount(issueStatistics.getCompletedCount());
                    projectIssueStatistics.setCompletionRate(issueStatistics.getCompletionRate());
                    statistics.setIssueStatistics(projectIssueStatistics);
                    log.debug("项目任务统计信息已填充，项目ID: {}, 任务总数: {}", 
                            projectId, issueStatistics.getTotalCount());
                } else {
                    statistics.setIssueStatistics(null);
                }
            } catch (Exception e) {
                // 任务统计获取失败不应该影响项目统计，只记录警告日志
                log.warn("获取项目任务统计失败，项目ID: {}", projectId, e);
                statistics.setIssueStatistics(null);
            }

            // 工时统计（暂不支持，返回null）
            // TODO: 等工时管理模块实现后，添加工时统计逻辑
            statistics.setTimeEntryStatistics(null);

            log.info("项目统计信息查询成功，项目ID: {}", projectId);

            return statistics;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("项目统计信息查询失败，项目ID: {}", projectId, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "项目统计信息查询失败");
        } finally {
            MDC.clear();
        }
    }

    /**
     * 创建项目模板
     *
     * @param requestDTO 创建模板请求
     * @return 模板详情
     */
    @Transactional(rollbackFor = Exception.class)
    public ProjectTemplateResponseDTO createTemplate(ProjectTemplateCreateRequestDTO requestDTO) {
        MDC.put("operation", "create_template");

        try {
            log.info("开始创建项目模板，模板名称: {}", requestDTO.getName());

            // 权限验证：需要管理员权限
            securityUtils.requireAdmin();

            // 验证模板名称
            if (requestDTO.getName() == null || requestDTO.getName().trim().isEmpty()) {
                log.warn("模板名称不能为空");
                throw new BusinessException(ResultCode.PARAM_INVALID, "模板名称不能为空");
            }

            // 验证模块有效性
            if (requestDTO.getEnabledModules() != null && !requestDTO.getEnabledModules().isEmpty()) {
                for (String moduleName : requestDTO.getEnabledModules()) {
                    if (!ProjectModule.isValidCode(moduleName)) {
                        log.warn("无效的模块名称: {}", moduleName);
                        throw new BusinessException(ResultCode.PARAM_INVALID, "无效的模块名称: " + moduleName);
                    }
                }
            }

            // 验证跟踪器是否存在
            if (requestDTO.getTrackerIds() != null && !requestDTO.getTrackerIds().isEmpty()) {
                for (Long trackerId : requestDTO.getTrackerIds()) {
                    Tracker tracker = trackerMapper.selectById(trackerId);
                    if (tracker == null) {
                        log.warn("跟踪器不存在，跟踪器ID: {}", trackerId);
                        throw new BusinessException(ResultCode.TRACKER_NOT_FOUND, "跟踪器不存在，ID: " + trackerId);
                    }
                }
            }

            // 验证角色是否存在
            if (requestDTO.getDefaultRoles() != null && !requestDTO.getDefaultRoles().isEmpty()) {
                for (Integer roleId : requestDTO.getDefaultRoles()) {
                    Role role = roleMapper.selectById(roleId);
                    if (role == null) {
                        log.warn("角色不存在，角色ID: {}", roleId);
                        throw new BusinessException(ResultCode.ROLE_NOT_FOUND, "角色不存在，ID: " + roleId);
                    }
                }
            }

            // 创建模板项目实体（使用 TEMPLATE 状态）
            Project template = new Project();
            template.setName(requestDTO.getName());
            template.setDescription(requestDTO.getDescription());
            template.setIsPublic(false); // 模板默认私有
            template.setStatus(ProjectStatus.TEMPLATE.getCode());
            Date now = new Date();
            template.setCreatedOn(now);
            template.setUpdatedOn(now);

            // 保存模板
            projectMapper.insert(template);
            Long templateId = template.getId();
            log.debug("模板创建成功，模板ID: {}", templateId);

            // 创建启用的模块记录
            if (requestDTO.getEnabledModules() != null && !requestDTO.getEnabledModules().isEmpty()) {
                for (String moduleName : requestDTO.getEnabledModules()) {
                    EnabledModule enabledModule = new EnabledModule();
                    enabledModule.setProjectId(templateId);
                    enabledModule.setName(moduleName);
                    enabledModuleMapper.insert(enabledModule);
                }
                log.debug("模板模块创建成功，模板ID: {}, 模块数量: {}", templateId, requestDTO.getEnabledModules().size());
            }

            // 创建模板跟踪器关联
            if (requestDTO.getTrackerIds() != null && !requestDTO.getTrackerIds().isEmpty()) {
                for (Long trackerId : requestDTO.getTrackerIds()) {
                    ProjectTracker projectTracker = new ProjectTracker();
                    projectTracker.setProjectId(templateId);
                    projectTracker.setTrackerId(trackerId);
                    projectTrackerMapper.insert(projectTracker);
                }
                log.debug("模板跟踪器关联创建成功，模板ID: {}, 跟踪器数量: {}", templateId, requestDTO.getTrackerIds().size());
            }

            // 创建模板默认角色关联
            if (requestDTO.getDefaultRoles() != null && !requestDTO.getDefaultRoles().isEmpty()) {
                for (Integer roleId : requestDTO.getDefaultRoles()) {
                    ProjectTemplateRole templateRole = new ProjectTemplateRole();
                    templateRole.setProjectId(templateId);
                    templateRole.setRoleId(roleId);
                    projectTemplateRoleMapper.insert(templateRole);
                }
                log.debug("模板默认角色关联创建成功，模板ID: {}, 角色数量: {}", templateId, requestDTO.getDefaultRoles().size());
            }

            log.info("项目模板创建成功，模板ID: {}, 模板名称: {}", templateId, requestDTO.getName());

            // 返回模板详情
            return toProjectTemplateResponseDTO(template);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("项目模板创建失败，模板名称: {}", requestDTO.getName(), e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "项目模板创建失败");
        } finally {
            MDC.clear();
        }
    }

    /**
     * 获取项目模板列表
     *
     * @param current 当前页码
     * @param size    每页数量
     * @param name    模板名称（模糊查询）
     * @return 分页响应
     */
    public PageResponse<ProjectTemplateResponseDTO> listTemplates(Integer current, Integer size, String name) {
        MDC.put("operation", "list_templates");

        try {
            log.debug("开始查询项目模板列表，页码: {}, 每页数量: {}", current, size);

            // 权限验证：需要管理员权限
            securityUtils.requireAdmin();

            // 构建查询条件
            LambdaQueryWrapper<Project> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Project::getStatus, ProjectStatus.TEMPLATE.getCode());

            // 名称模糊查询
            if (name != null && !name.trim().isEmpty()) {
                queryWrapper.like(Project::getName, name.trim());
            }

            // 按ID倒序排序
            queryWrapper.orderByDesc(Project::getId);

            // 设置默认值并验证：current 至少为 1，size 至少为 10
            Integer validCurrent = (current != null && current > 0) ? current : 1;
            Integer validSize = (size != null && size > 0) ? size : 10;

            // 分页查询
            Page<Project> page = new Page<>(validCurrent, validSize);
            Page<Project> resultPage = projectMapper.selectPage(page, queryWrapper);

            // 转换为响应DTO
            List<ProjectTemplateResponseDTO> templateList = resultPage.getRecords().stream()
                    .map(this::toProjectTemplateResponseDTO)
                    .collect(Collectors.toList());

            log.info("项目模板列表查询成功，总数: {}, 当前页: {}", resultPage.getTotal(), current);

            return PageResponse.of(templateList, (int) resultPage.getTotal(), (int) resultPage.getCurrent(),
                    (int) resultPage.getSize());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("项目模板列表查询失败", e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "项目模板列表查询失败");
        } finally {
            MDC.clear();
        }
    }

    /**
     * 获取项目模板详情
     *
     * @param templateId 模板ID
     * @return 模板详情
     */
    public ProjectTemplateResponseDTO getTemplateById(Long templateId) {
        MDC.put("operation", "get_template");
        MDC.put("templateId", String.valueOf(templateId));

        try {
            log.info("开始查询项目模板详情，模板ID: {}", templateId);

            // 权限验证：需要管理员权限
            securityUtils.requireAdmin();

            // 查询模板
            Project template = projectMapper.selectById(templateId);
            if (template == null) {
                log.warn("项目模板不存在，模板ID: {}", templateId);
                throw new BusinessException(ResultCode.PROJECT_NOT_FOUND, "项目模板不存在");
            }

            // 验证是否为模板
            if (!ProjectStatus.TEMPLATE.getCode().equals(template.getStatus())) {
                log.warn("项目不是模板，项目ID: {}, 状态: {}", templateId, template.getStatus());
                throw new BusinessException(ResultCode.PARAM_INVALID, "项目不是模板");
            }

            log.info("项目模板详情查询成功，模板ID: {}", templateId);

            return toProjectTemplateResponseDTO(template);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("项目模板详情查询失败，模板ID: {}", templateId, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "项目模板详情查询失败");
        } finally {
            MDC.clear();
        }
    }

    /**
     * 更新项目模板
     *
     * @param templateId 模板ID
     * @param requestDTO 更新模板请求
     * @return 模板详情
     */
    @Transactional(rollbackFor = Exception.class)
    public ProjectTemplateResponseDTO updateTemplate(Long templateId, ProjectTemplateUpdateRequestDTO requestDTO) {
        MDC.put("operation", "update_template");
        MDC.put("templateId", String.valueOf(templateId));

        try {
            log.info("开始更新项目模板，模板ID: {}", templateId);

            // 权限验证：需要管理员权限
            securityUtils.requireAdmin();

            // 查询模板
            Project template = projectMapper.selectById(templateId);
            if (template == null) {
                log.warn("项目模板不存在，模板ID: {}", templateId);
                throw new BusinessException(ResultCode.PROJECT_NOT_FOUND, "项目模板不存在");
            }

            // 验证是否为模板
            if (!ProjectStatus.TEMPLATE.getCode().equals(template.getStatus())) {
                log.warn("项目不是模板，项目ID: {}, 状态: {}", templateId, template.getStatus());
                throw new BusinessException(ResultCode.PARAM_INVALID, "项目不是模板");
            }

            // 验证模板名称唯一性（排除当前模板）
            if (requestDTO.getName() != null && !requestDTO.getName().trim().isEmpty()
                    && !requestDTO.getName().equals(template.getName())) {
                LambdaQueryWrapper<Project> nameQuery = new LambdaQueryWrapper<>();
                nameQuery.eq(Project::getName, requestDTO.getName().trim())
                        .ne(Project::getId, templateId);
                Project existingTemplate = projectMapper.selectOne(nameQuery);
                if (existingTemplate != null) {
                    log.warn("模板名称已存在: {}", requestDTO.getName());
                    throw new BusinessException(ResultCode.PROJECT_NAME_EXISTS, "模板名称已存在");
                }
                template.setName(requestDTO.getName().trim());
            }

            // 更新描述
            if (requestDTO.getDescription() != null) {
                template.setDescription(requestDTO.getDescription());
            }

            // 更新更新时间
            template.setUpdatedOn(new Date());

            // 保存模板
            projectMapper.updateById(template);
            log.debug("模板信息更新成功，模板ID: {}", templateId);

            // 更新启用的模块（如果提供了模块列表）
            if (requestDTO.getEnabledModules() != null) {
                // 验证模块名称有效性
                for (String moduleName : requestDTO.getEnabledModules()) {
                    if (!ProjectModule.isValidCode(moduleName)) {
                        log.warn("无效的模块名称: {}", moduleName);
                        throw new BusinessException(ResultCode.PROJECT_MODULE_INVALID, "无效的模块名称: " + moduleName);
                    }
                }

                // 删除旧的模块
                LambdaQueryWrapper<EnabledModule> moduleQuery = new LambdaQueryWrapper<>();
                moduleQuery.eq(EnabledModule::getProjectId, templateId);
                enabledModuleMapper.delete(moduleQuery);
                log.debug("模板旧模块删除成功，模板ID: {}", templateId);

                // 添加新的模块
                if (!requestDTO.getEnabledModules().isEmpty()) {
                    for (String moduleName : requestDTO.getEnabledModules()) {
                        EnabledModule enabledModule = new EnabledModule();
                        enabledModule.setProjectId(templateId);
                        enabledModule.setName(moduleName);
                        enabledModuleMapper.insert(enabledModule);
                    }
                    log.debug("模板模块更新成功，模板ID: {}, 模块数量: {}", templateId, requestDTO.getEnabledModules().size());
                }
            }

            // 更新跟踪器关联（如果提供了跟踪器列表）
            if (requestDTO.getTrackerIds() != null) {
                // 验证跟踪器是否存在
                if (!requestDTO.getTrackerIds().isEmpty()) {
                    for (Long trackerId : requestDTO.getTrackerIds()) {
                        Tracker tracker = trackerMapper.selectById(trackerId);
                        if (tracker == null) {
                            log.warn("跟踪器不存在，跟踪器ID: {}", trackerId);
                            throw new BusinessException(ResultCode.TRACKER_NOT_FOUND, "跟踪器不存在，ID: " + trackerId);
                        }
                    }
                }

                // 删除旧的跟踪器关联
                LambdaQueryWrapper<ProjectTracker> trackerQuery = new LambdaQueryWrapper<>();
                trackerQuery.eq(ProjectTracker::getProjectId, templateId);
                projectTrackerMapper.delete(trackerQuery);
                log.debug("模板旧跟踪器关联删除成功，模板ID: {}", templateId);

                // 添加新的跟踪器关联
                if (!requestDTO.getTrackerIds().isEmpty()) {
                    for (Long trackerId : requestDTO.getTrackerIds()) {
                        ProjectTracker projectTracker = new ProjectTracker();
                        projectTracker.setProjectId(templateId);
                        projectTracker.setTrackerId(trackerId);
                        projectTrackerMapper.insert(projectTracker);
                    }
                    log.debug("模板跟踪器关联更新成功，模板ID: {}, 跟踪器数量: {}", templateId, requestDTO.getTrackerIds().size());
                }
            }

            // 更新默认角色关联（如果提供了角色列表）
            if (requestDTO.getDefaultRoles() != null) {
                // 验证角色是否存在
                if (!requestDTO.getDefaultRoles().isEmpty()) {
                    for (Integer roleId : requestDTO.getDefaultRoles()) {
                        Role role = roleMapper.selectById(roleId);
                        if (role == null) {
                            log.warn("角色不存在，角色ID: {}", roleId);
                            throw new BusinessException(ResultCode.ROLE_NOT_FOUND, "角色不存在，ID: " + roleId);
                        }
                    }
                }

                // 删除旧的默认角色关联
                LambdaQueryWrapper<ProjectTemplateRole> templateRoleQuery = new LambdaQueryWrapper<>();
                templateRoleQuery.eq(ProjectTemplateRole::getProjectId, templateId);
                projectTemplateRoleMapper.delete(templateRoleQuery);
                log.debug("模板旧默认角色关联删除成功，模板ID: {}", templateId);

                // 添加新的默认角色关联
                if (!requestDTO.getDefaultRoles().isEmpty()) {
                    for (Integer roleId : requestDTO.getDefaultRoles()) {
                        ProjectTemplateRole templateRole = new ProjectTemplateRole();
                        templateRole.setProjectId(templateId);
                        templateRole.setRoleId(roleId);
                        projectTemplateRoleMapper.insert(templateRole);
                    }
                    log.debug("模板默认角色关联更新成功，模板ID: {}, 角色数量: {}", templateId, requestDTO.getDefaultRoles().size());
                }
            }

            log.info("项目模板更新成功，模板ID: {}", templateId);

            // 返回模板详情
            return toProjectTemplateResponseDTO(template);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("项目模板更新失败，模板ID: {}", templateId, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "项目模板更新失败");
        } finally {
            MDC.clear();
        }
    }

    /**
     * 从模板创建项目
     *
     * @param templateId 模板ID
     * @param requestDTO 创建项目请求
     * @return 项目详情
     */
    @Transactional(rollbackFor = Exception.class)
    public ProjectDetailResponseDTO createProjectFromTemplate(Long templateId,
            ProjectFromTemplateRequestDTO requestDTO) {
        MDC.put("operation", "create_project_from_template");
        MDC.put("templateId", String.valueOf(templateId));

        try {
            log.info("开始从模板创建项目，模板ID: {}, 项目名称: {}", templateId, requestDTO.getName());

            // 权限验证：需要 create_projects 权限或系统管理员
            User currentUser = securityUtils.getCurrentUser();
            boolean isAdmin = Boolean.TRUE.equals(currentUser.getAdmin());
            if (!isAdmin) {
                // 检查用户是否在任何项目中拥有 create_projects 权限
                Set<String> allPermissions = projectPermissionService.getUserAllPermissions(currentUser.getId());
                if (!allPermissions.contains("create_projects")) {
                    log.warn("用户无权限从模板创建项目，模板ID: {}, 用户ID: {}", templateId, currentUser.getId());
                    throw new BusinessException(ResultCode.FORBIDDEN, "无权限从模板创建项目，需要 create_projects 权限");
                }
                log.warn("用户无权限创建项目，用户ID: {}", currentUser.getId());
                throw new BusinessException(ResultCode.FORBIDDEN, "无权限创建项目");
            }

            // 查询模板
            Project template = projectMapper.selectById(templateId);
            if (template == null) {
                log.warn("项目模板不存在，模板ID: {}", templateId);
                throw new BusinessException(ResultCode.PROJECT_NOT_FOUND, "项目模板不存在");
            }

            // 验证是否为模板
            if (!ProjectStatus.TEMPLATE.getCode().equals(template.getStatus())) {
                log.warn("项目不是模板，项目ID: {}, 状态: {}", templateId, template.getStatus());
                throw new BusinessException(ResultCode.PARAM_INVALID, "项目不是模板");
            }

            // 验证项目名称
            if (requestDTO.getName() == null || requestDTO.getName().trim().isEmpty()) {
                log.warn("项目名称不能为空");
                throw new BusinessException(ResultCode.PARAM_INVALID, "项目名称不能为空");
            }

            // 验证项目标识符唯一性
            if (requestDTO.getIdentifier() != null && !requestDTO.getIdentifier().trim().isEmpty()) {
                LambdaQueryWrapper<Project> identifierQuery = new LambdaQueryWrapper<>();
                identifierQuery.eq(Project::getIdentifier, requestDTO.getIdentifier().trim())
                        .ne(Project::getStatus, ProjectStatus.TEMPLATE.getCode()); // 排除模板
                Project existingProject = projectMapper.selectOne(identifierQuery);
                if (existingProject != null) {
                    log.warn("项目标识符已存在: {}", requestDTO.getIdentifier());
                    throw new BusinessException(ResultCode.PROJECT_IDENTIFIER_EXISTS, "项目标识符已存在");
                }
            }

            // 使用 copyProject 方法从模板创建项目
            ProjectCopyRequestDTO copyRequest = new ProjectCopyRequestDTO();
            copyRequest.setName(requestDTO.getName());
            copyRequest.setIdentifier(requestDTO.getIdentifier());
            copyRequest.setCopyMembers(requestDTO.getCopyMembers() != null ? requestDTO.getCopyMembers() : false);
            copyRequest.setCopyModules(true);
            copyRequest.setCopyTrackers(true);

            ProjectDetailResponseDTO newProject = copyProject(templateId, copyRequest);

            // 如果模板有默认角色，自动分配给项目创建者
            if (newProject != null) {
                LambdaQueryWrapper<ProjectTemplateRole> templateRoleQuery = new LambdaQueryWrapper<>();
                templateRoleQuery.eq(ProjectTemplateRole::getProjectId, templateId);
                List<ProjectTemplateRole> templateRoles = projectTemplateRoleMapper.selectList(templateRoleQuery);

                if (!templateRoles.isEmpty()) {
                    Long newProjectId = newProject.getId();
                    Long currentUserId = currentUser.getId();

                    // 查询新项目的成员（创建者）
                    LambdaQueryWrapper<Member> memberQuery = new LambdaQueryWrapper<>();
                    memberQuery.eq(Member::getProjectId, newProjectId)
                            .eq(Member::getUserId, currentUserId);
                    Member member = memberMapper.selectOne(memberQuery);

                    if (member != null) {
                        // 分配默认角色给创建者
                        for (ProjectTemplateRole templateRole : templateRoles) {
                            MemberRole memberRole = new MemberRole();
                            memberRole.setMemberId(member.getId().intValue());
                            memberRole.setRoleId(templateRole.getRoleId());
                            memberRoleMapper.insert(memberRole);
                        }
                        log.debug("默认角色分配成功，项目ID: {}, 用户ID: {}, 角色数量: {}",
                                newProjectId, currentUserId, templateRoles.size());
                    }
                }
            }

            log.info("从模板创建项目成功，模板ID: {}, 新项目ID: {}", templateId, newProject != null ? newProject.getId() : null);

            return newProject;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("从模板创建项目失败，模板ID: {}", templateId, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "从模板创建项目失败");
        } finally {
            MDC.clear();
        }
    }

    /**
     * 删除项目模板
     *
     * @param templateId 模板ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteTemplate(Long templateId) {
        MDC.put("operation", "delete_template");
        MDC.put("templateId", String.valueOf(templateId));

        try {
            log.info("开始删除项目模板，模板ID: {}", templateId);

            // 权限验证：需要管理员权限
            securityUtils.requireAdmin();

            // 查询模板
            Project template = projectMapper.selectById(templateId);
            if (template == null) {
                log.warn("项目模板不存在，模板ID: {}", templateId);
                throw new BusinessException(ResultCode.PROJECT_NOT_FOUND, "项目模板不存在");
            }

            // 验证是否为模板
            if (!ProjectStatus.TEMPLATE.getCode().equals(template.getStatus())) {
                log.warn("项目不是模板，项目ID: {}, 状态: {}", templateId, template.getStatus());
                throw new BusinessException(ResultCode.PARAM_INVALID, "项目不是模板");
            }

            // 删除模板的默认角色关联
            LambdaQueryWrapper<ProjectTemplateRole> templateRoleQuery = new LambdaQueryWrapper<>();
            templateRoleQuery.eq(ProjectTemplateRole::getProjectId, templateId);
            projectTemplateRoleMapper.delete(templateRoleQuery);
            log.debug("模板默认角色关联删除成功，模板ID: {}", templateId);

            // 删除模板的跟踪器关联
            LambdaQueryWrapper<ProjectTracker> trackerQuery = new LambdaQueryWrapper<>();
            trackerQuery.eq(ProjectTracker::getProjectId, templateId);
            projectTrackerMapper.delete(trackerQuery);
            log.debug("模板跟踪器关联删除成功，模板ID: {}", templateId);

            // 删除模板的模块
            LambdaQueryWrapper<EnabledModule> moduleQuery = new LambdaQueryWrapper<>();
            moduleQuery.eq(EnabledModule::getProjectId, templateId);
            enabledModuleMapper.delete(moduleQuery);
            log.debug("模板模块删除成功，模板ID: {}", templateId);

            // 删除模板的成员（如果有）
            LambdaQueryWrapper<Member> memberQuery = new LambdaQueryWrapper<>();
            memberQuery.eq(Member::getProjectId, templateId);
            List<Member> members = memberMapper.selectList(memberQuery);
            if (!members.isEmpty()) {
                for (Member member : members) {
                    // 删除成员的角色
                    LambdaQueryWrapper<MemberRole> memberRoleQuery = new LambdaQueryWrapper<>();
                    memberRoleQuery.eq(MemberRole::getMemberId, member.getId().intValue());
                    memberRoleMapper.delete(memberRoleQuery);
                }
                memberMapper.delete(memberQuery);
                log.debug("模板成员删除成功，模板ID: {}, 成员数量: {}", templateId, members.size());
            }

            // 删除模板
            projectMapper.deleteById(templateId);
            log.info("项目模板删除成功，模板ID: {}", templateId);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("项目模板删除失败，模板ID: {}", templateId, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "项目模板删除失败");
        } finally {
            MDC.clear();
        }
    }

    /**
     * 将 Project 实体转换为 ProjectTemplateResponseDTO
     *
     * @param template 模板项目实体
     * @return 模板响应DTO
     */
    private ProjectTemplateResponseDTO toProjectTemplateResponseDTO(Project template) {
        ProjectTemplateResponseDTO dto = new ProjectTemplateResponseDTO();
        dto.setId(template.getId());
        dto.setName(template.getName());
        dto.setDescription(template.getDescription());
        dto.setCreatedOn(template.getCreatedOn());
        dto.setUpdatedOn(template.getUpdatedOn());

        // 查询启用的模块
        LambdaQueryWrapper<EnabledModule> moduleQuery = new LambdaQueryWrapper<>();
        moduleQuery.eq(EnabledModule::getProjectId, template.getId());
        List<EnabledModule> enabledModules = enabledModuleMapper.selectList(moduleQuery);
        dto.setEnabledModules(enabledModules.stream()
                .map(EnabledModule::getName)
                .collect(Collectors.toList()));

        // 查询跟踪器ID列表
        LambdaQueryWrapper<ProjectTracker> trackerQuery = new LambdaQueryWrapper<>();
        trackerQuery.eq(ProjectTracker::getProjectId, template.getId());
        List<ProjectTracker> projectTrackers = projectTrackerMapper.selectList(trackerQuery);
        dto.setTrackerIds(projectTrackers.stream()
                .map(ProjectTracker::getTrackerId)
                .collect(Collectors.toList()));

        // 查询默认角色ID列表
        LambdaQueryWrapper<ProjectTemplateRole> templateRoleQuery = new LambdaQueryWrapper<>();
        templateRoleQuery.eq(ProjectTemplateRole::getProjectId, template.getId());
        List<ProjectTemplateRole> templateRoles = projectTemplateRoleMapper.selectList(templateRoleQuery);
        dto.setDefaultRoles(templateRoles.stream()
                .map(ProjectTemplateRole::getRoleId)
                .collect(Collectors.toList()));

        return dto;
    }

    // ==================== 版本管理 ====================

    /**
     * 分页查询版本列表
     *
     * @param projectId  项目ID
     * @param requestDTO 查询条件
     * @return 版本列表
     */
    public PageResponse<VersionResponseDTO> listVersions(Long projectId, VersionListRequestDTO requestDTO) {
        MDC.put("operation", "list_versions");
        MDC.put("projectId", String.valueOf(projectId));

        try {
            log.info("开始查询版本列表，项目ID: {}", projectId);

            // 验证项目是否存在
            Project project = projectMapper.selectById(projectId);
            if (project == null) {
                log.warn("项目不存在，项目ID: {}", projectId);
                throw new BusinessException(ResultCode.PROJECT_NOT_FOUND);
            }

            // 构建查询条件
            LambdaQueryWrapper<Version> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Version::getProjectId, projectId.intValue());

            // 按名称模糊查询
            if (requestDTO.getName() != null && !requestDTO.getName().trim().isEmpty()) {
                queryWrapper.like(Version::getName, requestDTO.getName().trim());
            }

            // 按状态筛选
            if (requestDTO.getStatus() != null && !requestDTO.getStatus().trim().isEmpty()) {
                queryWrapper.eq(Version::getStatus, requestDTO.getStatus());
            }

            // 按生效日期倒序排序（最新的在前）
            queryWrapper.orderByDesc(Version::getEffectiveDate);
            queryWrapper.orderByDesc(Version::getId);

            // 分页查询
            Page<Version> page = new Page<>(requestDTO.getCurrent(), requestDTO.getSize());
            Page<Version> resultPage = versionMapper.selectPage(page, queryWrapper);

            // 转换为 DTO
            List<VersionResponseDTO> dtoList = resultPage.getRecords().stream()
                    .map(this::convertToVersionResponseDTO)
                    .collect(Collectors.toList());

            log.info("版本列表查询成功，项目ID: {}, 总数: {}", projectId, resultPage.getTotal());

            return PageResponse.of(dtoList, resultPage.getTotal(), resultPage.getCurrent(), resultPage.getSize());
        } finally {
            MDC.remove("operation");
            MDC.remove("projectId");
        }
    }

    /**
     * 根据ID获取版本详情
     *
     * @param projectId 项目ID
     * @param id        版本ID
     * @return 版本详情
     */
    public VersionResponseDTO getVersionById(Long projectId, Integer id) {
        MDC.put("operation", "get_version");
        MDC.put("projectId", String.valueOf(projectId));
        MDC.put("versionId", String.valueOf(id));

        try {
            log.info("开始查询版本详情，项目ID: {}, 版本ID: {}", projectId, id);

            // 验证项目是否存在
            Project project = projectMapper.selectById(projectId);
            if (project == null) {
                log.warn("项目不存在，项目ID: {}", projectId);
                throw new BusinessException(ResultCode.PROJECT_NOT_FOUND);
            }

            // 查询版本
            Version version = versionMapper.selectById(id);
            if (version == null) {
                log.warn("版本不存在，版本ID: {}", id);
                throw new BusinessException(ResultCode.PARAM_INVALID, "版本不存在");
            }

            // 验证版本是否属于该项目
            if (!version.getProjectId().equals(projectId.intValue())) {
                log.warn("版本不属于该项目，版本ID: {}, 版本项目ID: {}, 请求项目ID: {}",
                        id, version.getProjectId(), projectId);
                throw new BusinessException(ResultCode.PARAM_INVALID, "版本不属于该项目");
            }

            log.info("版本详情查询成功，版本ID: {}, 版本名称: {}", id, version.getName());

            return convertToVersionResponseDTO(version);
        } finally {
            MDC.remove("operation");
            MDC.remove("projectId");
            MDC.remove("versionId");
        }
    }

    /**
     * 创建版本
     *
     * @param projectId  项目ID
     * @param requestDTO 创建版本请求
     * @return 版本详情
     */
    @Transactional(rollbackFor = Exception.class)
    public VersionResponseDTO createVersion(Long projectId, VersionCreateRequestDTO requestDTO) {
        MDC.put("operation", "create_version");
        MDC.put("projectId", String.valueOf(projectId));
        MDC.put("versionName", requestDTO.getName());

        try {
            log.info("开始创建版本，项目ID: {}, 版本名称: {}", projectId, requestDTO.getName());

            // 验证项目是否存在
            Project project = projectMapper.selectById(projectId);
            if (project == null) {
                log.warn("项目不存在，项目ID: {}", projectId);
                throw new BusinessException(ResultCode.PROJECT_NOT_FOUND);
            }

            // 获取当前用户信息
            User currentUser = securityUtils.getCurrentUser();
            Long currentUserId = currentUser.getId();
            boolean isAdmin = Boolean.TRUE.equals(currentUser.getAdmin());

            // 权限验证：需要 manage_versions 权限或系统管理员
            if (!isAdmin) {
                if (!projectPermissionService.hasPermission(currentUserId, projectId, "manage_versions")) {
                    log.warn("用户无权限创建版本，项目ID: {}, 用户ID: {}", projectId, currentUserId);
                    throw new BusinessException(ResultCode.FORBIDDEN, "无权限创建版本，需要 manage_versions 权限");
                }
            }

            // 验证版本名称在项目中是否已存在
            LambdaQueryWrapper<Version> checkQuery = new LambdaQueryWrapper<>();
            checkQuery.eq(Version::getProjectId, projectId.intValue())
                    .eq(Version::getName, requestDTO.getName().trim());
            Version existingVersion = versionMapper.selectOne(checkQuery);
            if (existingVersion != null) {
                log.warn("版本名称已存在，项目ID: {}, 版本名称: {}", projectId, requestDTO.getName());
                throw new BusinessException(ResultCode.PARAM_INVALID, "该版本名称已存在");
            }

            // 创建版本
            Version version = new Version();
            version.setProjectId(projectId.intValue());
            version.setName(requestDTO.getName().trim());
            version.setDescription(requestDTO.getDescription());
            version.setEffectiveDate(requestDTO.getEffectiveDate());
            version.setWikiPageTitle(requestDTO.getWikiPageTitle());
            version.setStatus(requestDTO.getStatus() != null ? requestDTO.getStatus() : VersionStatus.OPEN.getCode());
            version.setSharing(requestDTO.getSharing() != null ? requestDTO.getSharing() : VersionSharing.NONE.getCode());
            
            // 设置创建时间和更新时间
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            version.setCreatedOn(now);
            version.setUpdatedOn(now);

            int insertResult = versionMapper.insert(version);
            if (insertResult <= 0) {
                log.error("版本创建失败，插入数据库失败");
                throw new BusinessException(ResultCode.SYSTEM_ERROR, "版本创建失败");
            }

            log.info("版本创建成功，版本ID: {}, 版本名称: {}", version.getId(), version.getName());

            return convertToVersionResponseDTO(version);
        } finally {
            MDC.remove("operation");
            MDC.remove("projectId");
            MDC.remove("versionName");
        }
    }

    /**
     * 更新版本
     *
     * @param projectId  项目ID
     * @param id         版本ID
     * @param requestDTO 更新版本请求
     * @return 版本详情
     */
    @Transactional(rollbackFor = Exception.class)
    public VersionResponseDTO updateVersion(Long projectId, Integer id, VersionUpdateRequestDTO requestDTO) {
        MDC.put("operation", "update_version");
        MDC.put("projectId", String.valueOf(projectId));
        MDC.put("versionId", String.valueOf(id));

        try {
            log.info("开始更新版本，项目ID: {}, 版本ID: {}", projectId, id);

            // 验证项目是否存在
            Project project = projectMapper.selectById(projectId);
            if (project == null) {
                log.warn("项目不存在，项目ID: {}", projectId);
                throw new BusinessException(ResultCode.PROJECT_NOT_FOUND);
            }

            // 查询版本
            Version version = versionMapper.selectById(id);
            if (version == null) {
                log.warn("版本不存在，版本ID: {}", id);
                throw new BusinessException(ResultCode.PARAM_INVALID, "版本不存在");
            }

            // 验证版本是否属于该项目
            if (!version.getProjectId().equals(projectId.intValue())) {
                log.warn("版本不属于该项目，版本ID: {}, 版本项目ID: {}, 请求项目ID: {}",
                        id, version.getProjectId(), projectId);
                throw new BusinessException(ResultCode.PARAM_INVALID, "版本不属于该项目");
            }

            // 获取当前用户信息
            User currentUser = securityUtils.getCurrentUser();
            Long currentUserId = currentUser.getId();
            boolean isAdmin = Boolean.TRUE.equals(currentUser.getAdmin());

            // 权限验证：需要 manage_versions 权限或系统管理员
            if (!isAdmin) {
                if (!projectPermissionService.hasPermission(currentUserId, projectId, "manage_versions")) {
                    log.warn("用户无权限更新版本，项目ID: {}, 用户ID: {}", projectId, currentUserId);
                    throw new BusinessException(ResultCode.FORBIDDEN, "无权限更新版本，需要 manage_versions 权限");
                }
            }

            // 如果更新名称，验证是否与其他版本重名
            if (requestDTO.getName() != null && !requestDTO.getName().trim().isEmpty()) {
                if (!requestDTO.getName().trim().equals(version.getName())) {
                    LambdaQueryWrapper<Version> checkQuery = new LambdaQueryWrapper<>();
                    checkQuery.eq(Version::getProjectId, projectId.intValue())
                            .eq(Version::getName, requestDTO.getName().trim())
                            .ne(Version::getId, id);
                    Version existingVersion = versionMapper.selectOne(checkQuery);
                    if (existingVersion != null) {
                        log.warn("版本名称已存在，项目ID: {}, 版本名称: {}", projectId, requestDTO.getName());
                        throw new BusinessException(ResultCode.PARAM_INVALID, "该版本名称已存在");
                    }
                }
                version.setName(requestDTO.getName().trim());
            }

            // 更新其他字段
            if (requestDTO.getDescription() != null) {
                version.setDescription(requestDTO.getDescription());
            }
            if (requestDTO.getEffectiveDate() != null) {
                version.setEffectiveDate(requestDTO.getEffectiveDate());
            }
            if (requestDTO.getWikiPageTitle() != null) {
                version.setWikiPageTitle(requestDTO.getWikiPageTitle());
            }
            if (requestDTO.getStatus() != null) {
                version.setStatus(requestDTO.getStatus());
            }
            if (requestDTO.getSharing() != null) {
                version.setSharing(requestDTO.getSharing());
            }

            // 更新更新时间
            version.setUpdatedOn(java.time.LocalDateTime.now());

            int updateResult = versionMapper.updateById(version);
            if (updateResult <= 0) {
                log.error("版本更新失败，更新数据库失败");
                throw new BusinessException(ResultCode.SYSTEM_ERROR, "版本更新失败");
            }

            log.info("版本更新成功，版本ID: {}, 版本名称: {}", id, version.getName());

            return convertToVersionResponseDTO(version);
        } finally {
            MDC.remove("operation");
            MDC.remove("projectId");
            MDC.remove("versionId");
        }
    }

    /**
     * 删除版本
     *
     * @param projectId 项目ID
     * @param id        版本ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteVersion(Long projectId, Integer id) {
        MDC.put("operation", "delete_version");
        MDC.put("projectId", String.valueOf(projectId));
        MDC.put("versionId", String.valueOf(id));

        try {
            log.info("开始删除版本，项目ID: {}, 版本ID: {}", projectId, id);

            // 验证项目是否存在
            Project project = projectMapper.selectById(projectId);
            if (project == null) {
                log.warn("项目不存在，项目ID: {}", projectId);
                throw new BusinessException(ResultCode.PROJECT_NOT_FOUND);
            }

            // 查询版本
            Version version = versionMapper.selectById(id);
            if (version == null) {
                log.warn("版本不存在，版本ID: {}", id);
                throw new BusinessException(ResultCode.PARAM_INVALID, "版本不存在");
            }

            // 验证版本是否属于该项目
            if (!version.getProjectId().equals(projectId.intValue())) {
                log.warn("版本不属于该项目，版本ID: {}, 版本项目ID: {}, 请求项目ID: {}",
                        id, version.getProjectId(), projectId);
                throw new BusinessException(ResultCode.PARAM_INVALID, "版本不属于该项目");
            }

            // 获取当前用户信息
            User currentUser = securityUtils.getCurrentUser();
            Long currentUserId = currentUser.getId();
            boolean isAdmin = Boolean.TRUE.equals(currentUser.getAdmin());

            // 权限验证：需要 manage_versions 权限或系统管理员
            if (!isAdmin) {
                if (!projectPermissionService.hasPermission(currentUserId, projectId, "manage_versions")) {
                    log.warn("用户无权限删除版本，项目ID: {}, 用户ID: {}", projectId, currentUserId);
                    throw new BusinessException(ResultCode.FORBIDDEN, "无权限删除版本，需要 manage_versions 权限");
                }
            }

            // 检查是否有任务使用该版本
            LambdaQueryWrapper<Issue> issueQuery = new LambdaQueryWrapper<>();
            issueQuery.eq(Issue::getFixedVersionId, id.longValue());
            Long issueCount = issueMapper.selectCount(issueQuery);
            if (issueCount > 0) {
                log.warn("版本正在被任务使用，无法删除，版本ID: {}, 关联任务数: {}", id, issueCount);
                throw new BusinessException(ResultCode.PARAM_INVALID,
                        "该版本正在被 " + issueCount + " 个任务使用，无法删除");
            }

            // 删除版本
            int deleteResult = versionMapper.deleteById(id);
            if (deleteResult <= 0) {
                log.error("版本删除失败，删除数据库失败");
                throw new BusinessException(ResultCode.SYSTEM_ERROR, "版本删除失败");
            }

            log.info("版本删除成功，版本ID: {}, 版本名称: {}", id, version.getName());
        } finally {
            MDC.remove("operation");
            MDC.remove("projectId");
            MDC.remove("versionId");
        }
    }

    /**
     * 将 Version 实体转换为 VersionResponseDTO
     *
     * @param version 版本实体
     * @return 版本响应DTO
     */
    private VersionResponseDTO convertToVersionResponseDTO(Version version) {
        VersionResponseDTO dto = new VersionResponseDTO();
        dto.setId(version.getId());
        dto.setProjectId(version.getProjectId());
        dto.setName(version.getName());
        dto.setDescription(version.getDescription());
        dto.setEffectiveDate(version.getEffectiveDate());
        dto.setCreatedOn(version.getCreatedOn());
        dto.setUpdatedOn(version.getUpdatedOn());
        dto.setWikiPageTitle(version.getWikiPageTitle());
        dto.setStatus(version.getStatus());
        dto.setSharing(version.getSharing());

        // 填充项目名称
        if (version.getProjectId() != null) {
            Project project = projectMapper.selectById(version.getProjectId());
            if (project != null) {
                dto.setProjectName(project.getName());
            }
        }

        // 统计关联的任务数量
        LambdaQueryWrapper<Issue> issueQuery = new LambdaQueryWrapper<>();
        issueQuery.eq(Issue::getFixedVersionId, version.getId().longValue());
        Long issueCount = issueMapper.selectCount(issueQuery);
        dto.setIssueCount(issueCount.intValue());

        return dto;
    }

    /**
     * 获取版本统计信息
     *
     * @param projectId 项目ID
     * @param versionId 版本ID
     * @return 版本统计信息
     */
    public VersionStatisticsResponseDTO getVersionStatistics(Long projectId, Integer versionId) {
        MDC.put("operation", "get_version_statistics");
        MDC.put("projectId", String.valueOf(projectId));
        MDC.put("versionId", String.valueOf(versionId));

        try {
            log.info("开始查询版本统计，项目ID: {}, 版本ID: {}", projectId, versionId);

            // 验证项目是否存在
            Project project = projectMapper.selectById(projectId);
            if (project == null) {
                log.warn("项目不存在，项目ID: {}", projectId);
                throw new BusinessException(ResultCode.PROJECT_NOT_FOUND);
            }

            // 查询版本
            Version version = versionMapper.selectById(versionId);
            if (version == null) {
                log.warn("版本不存在，版本ID: {}", versionId);
                throw new BusinessException(ResultCode.PARAM_INVALID, "版本不存在");
            }

            // 验证版本是否属于该项目
            if (!version.getProjectId().equals(projectId.intValue())) {
                log.warn("版本不属于该项目，版本ID: {}, 版本项目ID: {}, 请求项目ID: {}",
                        versionId, version.getProjectId(), projectId);
                throw new BusinessException(ResultCode.PARAM_INVALID, "版本不属于该项目");
            }

            // 查询版本关联的所有任务
            LambdaQueryWrapper<Issue> issueQuery = new LambdaQueryWrapper<>();
            issueQuery.eq(Issue::getFixedVersionId, versionId.longValue())
                     .eq(Issue::getProjectId, projectId);
            List<Issue> issues = issueMapper.selectList(issueQuery);

            // 统计任务数量
            long totalIssues = issues.size();
            
            // 获取所有状态信息（用于状态名称映射）
            List<IssueStatus> allStatuses = issueStatusMapper.selectList(null);
            Map<Integer, String> statusMap = allStatuses.stream()
                    .collect(Collectors.toMap(IssueStatus::getId, IssueStatus::getName));

            // 按状态统计任务
            Map<String, Long> issuesByStatus = new HashMap<>();
            long completedIssues = 0;
            long inProgressIssues = 0;
            long pendingIssues = 0;
            long closedIssues = 0;
            
            for (Issue issue : issues) {
                String statusName = statusMap.getOrDefault(issue.getStatusId(), "未知");
                issuesByStatus.merge(statusName, 1L, Long::sum);
                
                // 判断任务状态分类
                if (issue.getDoneRatio() != null && issue.getDoneRatio() >= 100) {
                    completedIssues++;
                } else if (issue.getDoneRatio() != null && issue.getDoneRatio() > 0) {
                    inProgressIssues++;
                } else {
                    pendingIssues++;
                }
                
                // 检查是否为关闭状态（通常状态ID对应关闭状态）
                IssueStatus status = allStatuses.stream()
                        .filter(s -> s.getId().equals(issue.getStatusId()))
                        .findFirst()
                        .orElse(null);
                if (status != null && status.getIsClosed() != null && status.getIsClosed()) {
                    closedIssues++;
                }
            }

            // 计算版本完成度（基于任务完成度）
            double totalDoneRatio = issues.stream()
                    .filter(i -> i.getDoneRatio() != null)
                    .mapToInt(Issue::getDoneRatio)
                    .sum();
            double completionPercentage = totalIssues > 0 ? totalDoneRatio / totalIssues : 0.0;

            // 工时统计
            double estimatedHours = issues.stream()
                    .filter(i -> i.getEstimatedHours() != null)
                    .mapToDouble(Issue::getEstimatedHours)
                    .sum();

            // 查询已消耗工时（通过time_entries表）
            double spentHours = 0.0;
            if (!issues.isEmpty()) {
                List<Long> issueIds = issues.stream().map(Issue::getId).collect(Collectors.toList());
                LambdaQueryWrapper<TimeEntry> timeEntryQuery = new LambdaQueryWrapper<>();
                timeEntryQuery.eq(TimeEntry::getProjectId, projectId)
                             .in(TimeEntry::getIssueId, issueIds)
                             .isNotNull(TimeEntry::getHours);
                List<TimeEntry> timeEntries = timeEntryMapper.selectList(timeEntryQuery);
                
                spentHours = timeEntries.stream()
                        .filter(te -> te.getHours() != null)
                        .mapToDouble(TimeEntry::getHours)
                        .sum();
            }
            
            double remainingHours = Math.max(0, estimatedHours - spentHours);
            double hoursCompletionPercentage = estimatedHours > 0 
                    ? (spentHours / estimatedHours) * 100 
                    : 0.0;

            // 时间统计
            LocalDate earliestStartDate = issues.stream()
                    .filter(i -> i.getStartDate() != null)
                    .map(Issue::getStartDate)
                    .min(LocalDate::compareTo)
                    .orElse(null);

            LocalDate latestDueDate = issues.stream()
                    .filter(i -> i.getDueDate() != null)
                    .map(Issue::getDueDate)
                    .max(LocalDate::compareTo)
                    .orElse(null);

            // 预计完成时间（使用版本生效日期或最晚截止日期）
            LocalDate estimatedCompletionDate = version.getEffectiveDate() != null 
                    ? version.getEffectiveDate() 
                    : latestDueDate;

            // 按跟踪器统计
            Map<String, Long> issuesByTracker = issues.stream()
                    .collect(Collectors.groupingBy(
                        issue -> {
                            Tracker tracker = trackerMapper.selectById(issue.getTrackerId());
                            return tracker != null ? tracker.getName() : "未知";
                        },
                        Collectors.counting()
                    ));

            // 按优先级统计（需要获取优先级名称）
            Map<String, Long> issuesByPriority = issues.stream()
                    .collect(Collectors.groupingBy(
                        issue -> "优先级" + issue.getPriorityId(),
                        Collectors.counting()
                    ));

            log.info("版本统计查询成功，版本ID: {}, 任务总数: {}", versionId, totalIssues);

            return VersionStatisticsResponseDTO.builder()
                    .versionId(versionId)
                    .versionName(version.getName())
                    .versionStatus(version.getStatus())
                    .effectiveDate(version.getEffectiveDate())
                    .totalIssues(totalIssues)
                    .completedIssues(completedIssues)
                    .inProgressIssues(inProgressIssues)
                    .pendingIssues(pendingIssues)
                    .closedIssues(closedIssues)
                    .completionPercentage(Math.round(completionPercentage * 100.0) / 100.0)
                    .estimatedHours(Math.round(estimatedHours * 100.0) / 100.0)
                    .spentHours(Math.round(spentHours * 100.0) / 100.0)
                    .remainingHours(Math.round(remainingHours * 100.0) / 100.0)
                    .hoursCompletionPercentage(Math.round(hoursCompletionPercentage * 100.0) / 100.0)
                    .earliestStartDate(earliestStartDate)
                    .latestDueDate(latestDueDate)
                    .estimatedCompletionDate(estimatedCompletionDate)
                    .issuesByStatus(issuesByStatus)
                    .issuesByTracker(issuesByTracker)
                    .issuesByPriority(issuesByPriority)
                    .build();
        } finally {
            MDC.remove("operation");
            MDC.remove("projectId");
            MDC.remove("versionId");
        }
    }

    /**
     * 获取版本关联的任务列表
     *
     * @param projectId 项目ID
     * @param versionId 版本ID
     * @param requestDTO 查询请求
     * @return 任务列表
     */
    public PageResponse<IssueListItemResponseDTO> getVersionIssues(Long projectId, Integer versionId, VersionIssuesRequestDTO requestDTO) {
        MDC.put("operation", "get_version_issues");
        MDC.put("projectId", String.valueOf(projectId));
        MDC.put("versionId", String.valueOf(versionId));

        try {
            log.info("开始查询版本关联任务，项目ID: {}, 版本ID: {}", projectId, versionId);

            // 验证项目是否存在
            Project project = projectMapper.selectById(projectId);
            if (project == null) {
                log.warn("项目不存在，项目ID: {}", projectId);
                throw new BusinessException(ResultCode.PROJECT_NOT_FOUND);
            }

            // 查询版本
            Version version = versionMapper.selectById(versionId);
            if (version == null) {
                log.warn("版本不存在，版本ID: {}", versionId);
                throw new BusinessException(ResultCode.PARAM_INVALID, "版本不存在");
            }

            // 验证版本是否属于该项目
            if (!version.getProjectId().equals(projectId.intValue())) {
                log.warn("版本不属于该项目，版本ID: {}, 版本项目ID: {}, 请求项目ID: {}",
                        versionId, version.getProjectId(), projectId);
                throw new BusinessException(ResultCode.PARAM_INVALID, "版本不属于该项目");
            }

            // 获取当前用户信息
            User currentUser = securityUtils.getCurrentUser();
            Long currentUserId = currentUser.getId();
            boolean isAdmin = Boolean.TRUE.equals(currentUser.getAdmin());

            // 构建查询条件
            LambdaQueryWrapper<Issue> queryWrapper = new LambdaQueryWrapper<>();
            
            // 固定版本ID和项目ID
            queryWrapper.eq(Issue::getFixedVersionId, versionId.longValue())
                       .eq(Issue::getProjectId, projectId);

            // 状态筛选
            if (requestDTO.getStatusId() != null && requestDTO.getStatusId() > 0) {
                queryWrapper.eq(Issue::getStatusId, requestDTO.getStatusId());
            }

            // 跟踪器筛选
            if (requestDTO.getTrackerId() != null && requestDTO.getTrackerId() > 0) {
                queryWrapper.eq(Issue::getTrackerId, requestDTO.getTrackerId());
            }

            // 优先级筛选
            if (requestDTO.getPriorityId() != null && requestDTO.getPriorityId() > 0) {
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
            if (requestDTO.getAuthorId() != null && requestDTO.getAuthorId() > 0) {
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
                // 检查用户是否有项目权限
                if (!projectPermissionService.hasPermission(currentUserId, projectId, "view_issues")) {
                    log.warn("用户无权限查看任务，项目ID: {}, 用户ID: {}", projectId, currentUserId);
                    throw new BusinessException(ResultCode.FORBIDDEN, "无权限查看任务");
                }
            }

            // 排序处理
            String sortOrder = requestDTO.getSortOrder() != null ? requestDTO.getSortOrder() : "desc";
            if (requestDTO.getSortBy() != null && !requestDTO.getSortBy().trim().isEmpty()) {
                String sortField = requestDTO.getSortBy().trim().toLowerCase();
                boolean ascending = "asc".equalsIgnoreCase(sortOrder);
                switch (sortField) {
                    case "created_on":
                        if (ascending) {
                            queryWrapper.orderByAsc(Issue::getCreatedOn);
                        } else {
                            queryWrapper.orderByDesc(Issue::getCreatedOn);
                        }
                        break;
                    case "updated_on":
                        if (ascending) {
                            queryWrapper.orderByAsc(Issue::getUpdatedOn);
                        } else {
                            queryWrapper.orderByDesc(Issue::getUpdatedOn);
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

            // 分页查询
            Page<Issue> page = new Page<>(requestDTO.getCurrent(), requestDTO.getSize());
            Page<Issue> result = issueMapper.selectPage(page, queryWrapper);

            // 转换为响应DTO
            List<IssueListItemResponseDTO> dtoList = result.getRecords().stream()
                    .map(this::convertToIssueListItemResponseDTO)
                    .collect(Collectors.toList());

            log.info("版本关联任务查询成功，版本ID: {}, 任务总数: {}", versionId, result.getTotal());

            return PageResponse.of(
                    dtoList,
                    result.getTotal(),
                    result.getCurrent(),
                    result.getSize());
        } finally {
            MDC.remove("operation");
            MDC.remove("projectId");
            MDC.remove("versionId");
        }
    }

    /**
     * 转换为任务列表项响应DTO
     */
    private IssueListItemResponseDTO convertToIssueListItemResponseDTO(Issue issue) {
        IssueListItemResponseDTO dto = new IssueListItemResponseDTO();
        dto.setId(issue.getId());
        dto.setTrackerId(issue.getTrackerId());
        dto.setProjectId(issue.getProjectId());
        dto.setSubject(issue.getSubject());
        dto.setStatusId(issue.getStatusId());
        dto.setPriorityId(issue.getPriorityId());
        dto.setAssignedToId(issue.getAssignedToId());
        dto.setAuthorId(issue.getAuthorId());
        dto.setDoneRatio(issue.getDoneRatio());
        dto.setDueDate(issue.getDueDate());
        dto.setIsPrivate(issue.getIsPrivate());
        dto.setCreatedOn(issue.getCreatedOn());
        dto.setUpdatedOn(issue.getUpdatedOn());

        // 获取跟踪器名称
        if (issue.getTrackerId() != null) {
            Tracker tracker = trackerMapper.selectById(issue.getTrackerId());
            dto.setTrackerName(tracker != null ? tracker.getName() : null);
        }

        // 获取项目名称
        if (issue.getProjectId() != null) {
            Project project = projectMapper.selectById(issue.getProjectId());
            dto.setProjectName(project != null ? project.getName() : null);
        }

        // 获取状态名称
        if (issue.getStatusId() != null) {
            IssueStatus status = issueStatusMapper.selectById(issue.getStatusId());
            dto.setStatusName(status != null ? status.getName() : null);
        }

        // 获取指派人信息
        if (issue.getAssignedToId() != null) {
            User assignedUser = userMapper.selectById(issue.getAssignedToId());
            if (assignedUser != null) {
                dto.setAssignedToName(getUserDisplayName(assignedUser));
            }
        }

        // 获取创建者信息
        if (issue.getAuthorId() != null) {
            User author = userMapper.selectById(issue.getAuthorId());
            if (author != null) {
                dto.setAuthorName(getUserDisplayName(author));
            }
        }

        // 获取优先级名称
        if (issue.getPriorityId() != null) {
            String priorityName = getPriorityName(issue.getPriorityId());
            dto.setPriorityName(priorityName);
        }

        return dto;
    }

    /**
     * 获取优先级名称
     */
    private String getPriorityName(Integer priorityId) {
        if (priorityId == null || priorityId == 0) {
            return null;
        }

        try {
            LambdaQueryWrapper<Enumeration> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Enumeration::getId, priorityId)
                       .eq(Enumeration::getType, "IssuePriority")
                       .eq(Enumeration::getActive, true);
            Enumeration enumeration = enumerationMapper.selectOne(queryWrapper);

            return enumeration != null ? enumeration.getName() : null;
        } catch (Exception e) {
            log.warn("查询优先级名称失败，优先级ID: {}", priorityId, e);
            return null;
        }
    }

    /**
     * 获取用户显示名称
     */
    private String getUserDisplayName(User user) {
        if (user == null) {
            return null;
        }
        if ((user.getFirstname() != null && !user.getFirstname().isEmpty()) ||
            (user.getLastname() != null && !user.getLastname().isEmpty())) {
            return (user.getFirstname() != null ? user.getFirstname() : "") +
                   " " +
                   (user.getLastname() != null ? user.getLastname() : "");
        }
        return user.getLogin();
    }

    /**
     * 批量关联任务到版本
     *
     * @param projectId 项目ID
     * @param versionId 版本ID
     * @param requestDTO 批量关联请求
     * @return 批量关联结果
     */
    @Transactional(rollbackFor = Exception.class)
    public VersionIssuesBatchAssignResponseDTO batchAssignIssuesToVersion(
            Long projectId, Integer versionId, VersionIssuesBatchAssignRequestDTO requestDTO) {
        MDC.put("operation", "batch_assign_issues_to_version");
        MDC.put("projectId", String.valueOf(projectId));
        MDC.put("versionId", String.valueOf(versionId));

        try {
            log.info("开始批量关联任务到版本，项目ID: {}, 版本ID: {}, 任务数量: {}", 
                    projectId, versionId, requestDTO.getIssueIds().size());

            // 验证项目是否存在
            Project project = projectMapper.selectById(projectId);
            if (project == null) {
                log.warn("项目不存在，项目ID: {}", projectId);
                throw new BusinessException(ResultCode.PROJECT_NOT_FOUND);
            }

            // 查询版本
            Version version = versionMapper.selectById(versionId);
            if (version == null) {
                log.warn("版本不存在，版本ID: {}", versionId);
                throw new BusinessException(ResultCode.PARAM_INVALID, "版本不存在");
            }

            // 验证版本是否属于该项目
            if (!version.getProjectId().equals(projectId.intValue())) {
                log.warn("版本不属于该项目，版本ID: {}, 版本项目ID: {}, 请求项目ID: {}",
                        versionId, version.getProjectId(), projectId);
                throw new BusinessException(ResultCode.PARAM_INVALID, "版本不属于该项目");
            }

            // 获取当前用户信息
            User currentUser = securityUtils.getCurrentUser();
            Long currentUserId = currentUser.getId();
            boolean isAdmin = Boolean.TRUE.equals(currentUser.getAdmin());

            // 权限验证：需要 manage_versions 权限或系统管理员
            if (!isAdmin) {
                if (!projectPermissionService.hasPermission(currentUserId, projectId, "manage_versions")) {
                    log.warn("用户无权限管理版本，项目ID: {}, 用户ID: {}", projectId, currentUserId);
                    throw new BusinessException(ResultCode.FORBIDDEN, "无权限管理版本，需要 manage_versions 权限");
                }
            }

            // 查询所有任务
            List<Long> issueIds = requestDTO.getIssueIds();
            List<Issue> issues = issueMapper.selectBatchIds(issueIds);

            if (issues.isEmpty()) {
                log.warn("未找到任何任务，任务ID列表: {}", issueIds);
                throw new BusinessException(ResultCode.PARAM_INVALID, "未找到任何任务");
            }

            // 验证任务是否属于该项目，并检查编辑权限
            List<Long> successIssueIds = new ArrayList<>();
            List<Long> failIssueIds = new ArrayList<>();
            Map<Long, String> errors = new HashMap<>();
            int successCount = 0;

            for (Issue issue : issues) {
                Long issueId = issue.getId();
                
                try {
                    // 验证任务是否属于该项目
                    if (!issue.getProjectId().equals(projectId)) {
                        String errorMsg = "任务不属于该项目";
                        failIssueIds.add(issueId);
                        errors.put(issueId, errorMsg);
                        log.warn("任务不属于该项目，任务ID: {}, 任务项目ID: {}, 请求项目ID: {}", 
                                issueId, issue.getProjectId(), projectId);
                        continue;
                    }

                    // 权限验证：需要 edit_issues 权限或系统管理员
                    if (!isAdmin) {
                        if (!projectPermissionService.hasPermission(currentUserId, projectId, "edit_issues")) {
                            String errorMsg = "无权限编辑任务";
                            failIssueIds.add(issueId);
                            errors.put(issueId, errorMsg);
                            log.warn("用户无权限编辑任务，任务ID: {}, 项目ID: {}, 用户ID: {}", 
                                    issueId, projectId, currentUserId);
                            continue;
                        }
                    }

                    // 更新任务的fixed_version_id
                    issue.setFixedVersionId(versionId.longValue());
                    issue.setUpdatedOn(java.time.LocalDateTime.now());
                    
                    int updateResult = issueMapper.updateById(issue);
                    if (updateResult > 0) {
                        successIssueIds.add(issueId);
                        successCount++;
                        log.debug("任务关联版本成功，任务ID: {}, 版本ID: {}", issueId, versionId);
                    } else {
                        String errorMsg = "更新任务失败";
                        failIssueIds.add(issueId);
                        errors.put(issueId, errorMsg);
                        log.warn("更新任务失败，任务ID: {}", issueId);
                    }
                } catch (Exception e) {
                    String errorMsg = "处理任务失败: " + e.getMessage();
                    failIssueIds.add(issueId);
                    errors.put(issueId, errorMsg);
                    log.warn("处理任务失败，任务ID: {}, 错误: {}", issueId, e.getMessage());
                }
            }

            // 检查是否有未找到的任务ID
            Set<Long> foundIssueIds = issues.stream().map(Issue::getId).collect(Collectors.toSet());
            for (Long issueId : issueIds) {
                if (!foundIssueIds.contains(issueId)) {
                    failIssueIds.add(issueId);
                    errors.put(issueId, "任务不存在");
                }
            }

            log.info("批量关联任务到版本完成，项目ID: {}, 版本ID: {}, 成功: {}, 失败: {}", 
                    projectId, versionId, successCount, failIssueIds.size());

            return VersionIssuesBatchAssignResponseDTO.builder()
                    .successCount(successCount)
                    .failCount(failIssueIds.size())
                    .successIssueIds(successIssueIds)
                    .failIssueIds(failIssueIds)
                    .errors(errors)
                    .build();
        } finally {
            MDC.remove("operation");
            MDC.remove("projectId");
            MDC.remove("versionId");
        }
    }

    /**
     * 批量取消任务与版本的关联
     *
     * @param projectId 项目ID
     * @param versionId 版本ID
     * @param requestDTO 批量取消关联请求
     * @return 批量取消关联结果
     */
    @Transactional(rollbackFor = Exception.class)
    public VersionIssuesBatchUnassignResponseDTO batchUnassignIssuesFromVersion(
            Long projectId, Integer versionId, VersionIssuesBatchUnassignRequestDTO requestDTO) {
        MDC.put("operation", "batch_unassign_issues_from_version");
        MDC.put("projectId", String.valueOf(projectId));
        MDC.put("versionId", String.valueOf(versionId));

        try {
            log.info("开始批量取消任务与版本关联，项目ID: {}, 版本ID: {}, 任务数量: {}", 
                    projectId, versionId, requestDTO.getIssueIds().size());

            // 验证项目是否存在
            Project project = projectMapper.selectById(projectId);
            if (project == null) {
                log.warn("项目不存在，项目ID: {}", projectId);
                throw new BusinessException(ResultCode.PROJECT_NOT_FOUND);
            }

            // 查询版本
            Version version = versionMapper.selectById(versionId);
            if (version == null) {
                log.warn("版本不存在，版本ID: {}", versionId);
                throw new BusinessException(ResultCode.PARAM_INVALID, "版本不存在");
            }

            // 验证版本是否属于该项目
            if (!version.getProjectId().equals(projectId.intValue())) {
                log.warn("版本不属于该项目，版本ID: {}, 版本项目ID: {}, 请求项目ID: {}",
                        versionId, version.getProjectId(), projectId);
                throw new BusinessException(ResultCode.PARAM_INVALID, "版本不属于该项目");
            }

            // 获取当前用户信息
            User currentUser = securityUtils.getCurrentUser();
            Long currentUserId = currentUser.getId();
            boolean isAdmin = Boolean.TRUE.equals(currentUser.getAdmin());

            // 权限验证：需要 manage_versions 权限或系统管理员
            if (!isAdmin) {
                if (!projectPermissionService.hasPermission(currentUserId, projectId, "manage_versions")) {
                    log.warn("用户无权限管理版本，项目ID: {}, 用户ID: {}", projectId, currentUserId);
                    throw new BusinessException(ResultCode.FORBIDDEN, "无权限管理版本，需要 manage_versions 权限");
                }
            }

            // 查询所有任务
            List<Long> issueIds = requestDTO.getIssueIds();
            List<Issue> issues = issueMapper.selectBatchIds(issueIds);

            if (issues.isEmpty()) {
                log.warn("未找到任何任务，任务ID列表: {}", issueIds);
                throw new BusinessException(ResultCode.PARAM_INVALID, "未找到任何任务");
            }

            // 验证任务并取消关联
            List<Long> successIssueIds = new ArrayList<>();
            List<Long> failIssueIds = new ArrayList<>();
            Map<Long, String> errors = new HashMap<>();
            int successCount = 0;

            for (Issue issue : issues) {
                Long issueId = issue.getId();
                
                try {
                    // 验证任务是否属于该项目
                    if (!issue.getProjectId().equals(projectId)) {
                        String errorMsg = "任务不属于该项目";
                        failIssueIds.add(issueId);
                        errors.put(issueId, errorMsg);
                        log.warn("任务不属于该项目，任务ID: {}, 任务项目ID: {}, 请求项目ID: {}", 
                                issueId, issue.getProjectId(), projectId);
                        continue;
                    }

                    // 验证任务是否关联到该版本
                    if (issue.getFixedVersionId() == null || !issue.getFixedVersionId().equals(versionId.longValue())) {
                        String errorMsg = "任务未关联到该版本";
                        failIssueIds.add(issueId);
                        errors.put(issueId, errorMsg);
                        log.warn("任务未关联到该版本，任务ID: {}, 任务版本ID: {}, 请求版本ID: {}", 
                                issueId, issue.getFixedVersionId(), versionId);
                        continue;
                    }

                    // 权限验证：需要 edit_issues 权限或系统管理员
                    if (!isAdmin) {
                        if (!projectPermissionService.hasPermission(currentUserId, projectId, "edit_issues")) {
                            String errorMsg = "无权限编辑任务";
                            failIssueIds.add(issueId);
                            errors.put(issueId, errorMsg);
                            log.warn("用户无权限编辑任务，任务ID: {}, 项目ID: {}, 用户ID: {}", 
                                    issueId, projectId, currentUserId);
                            continue;
                        }
                    }

                    // 取消关联：将fixed_version_id设置为null
                    // 使用LambdaUpdateWrapper显式设置null值，因为MyBatis-Plus的updateById默认不会更新null值
                    LambdaUpdateWrapper<Issue> updateWrapper = new LambdaUpdateWrapper<>();
                    updateWrapper.eq(Issue::getId, issueId)
                                .set(Issue::getFixedVersionId, null)
                                .set(Issue::getUpdatedOn, java.time.LocalDateTime.now());
                    
                    int updateResult = issueMapper.update(null, updateWrapper);
                    if (updateResult > 0) {
                        successIssueIds.add(issueId);
                        successCount++;
                        log.debug("任务取消版本关联成功，任务ID: {}, 版本ID: {}", issueId, versionId);
                    } else {
                        String errorMsg = "更新任务失败";
                        failIssueIds.add(issueId);
                        errors.put(issueId, errorMsg);
                        log.warn("更新任务失败，任务ID: {}", issueId);
                    }
                } catch (Exception e) {
                    String errorMsg = "处理任务失败: " + e.getMessage();
                    failIssueIds.add(issueId);
                    errors.put(issueId, errorMsg);
                    log.warn("处理任务失败，任务ID: {}, 错误: {}", issueId, e.getMessage());
                }
            }

            // 检查是否有未找到的任务ID
            Set<Long> foundIssueIds = issues.stream().map(Issue::getId).collect(Collectors.toSet());
            for (Long issueId : issueIds) {
                if (!foundIssueIds.contains(issueId)) {
                    failIssueIds.add(issueId);
                    errors.put(issueId, "任务不存在");
                }
            }

            log.info("批量取消任务与版本关联完成，项目ID: {}, 版本ID: {}, 成功: {}, 失败: {}", 
                    projectId, versionId, successCount, failIssueIds.size());

            return VersionIssuesBatchUnassignResponseDTO.builder()
                    .successCount(successCount)
                    .failCount(failIssueIds.size())
                    .successIssueIds(successIssueIds)
                    .failIssueIds(failIssueIds)
                    .errors(errors)
                    .build();
        } finally {
            MDC.remove("operation");
            MDC.remove("projectId");
            MDC.remove("versionId");
        }
    }
}
