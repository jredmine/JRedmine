package com.github.jredmine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import com.github.yulichang.toolkit.JoinWrappers;
import com.github.jredmine.dto.request.project.MemberRoleAssignRequestDTO;
import com.github.jredmine.dto.request.project.ProjectCreateRequestDTO;
import com.github.jredmine.dto.request.project.ProjectMemberCreateRequestDTO;
import com.github.jredmine.dto.request.project.ProjectMemberUpdateRequestDTO;
import com.github.jredmine.dto.request.project.ProjectUpdateRequestDTO;
import com.github.jredmine.dto.response.PageResponse;
import com.github.jredmine.dto.response.project.ProjectDetailResponseDTO;
import com.github.jredmine.dto.response.project.ProjectListItemResponseDTO;
import com.github.jredmine.dto.response.project.ProjectMemberJoinDTO;
import com.github.jredmine.dto.response.project.ProjectMemberResponseDTO;
import com.github.jredmine.entity.EnabledModule;
import com.github.jredmine.entity.EmailAddress;
import com.github.jredmine.entity.Member;
import com.github.jredmine.entity.MemberRole;
import com.github.jredmine.entity.Project;
import com.github.jredmine.entity.ProjectTracker;
import com.github.jredmine.entity.Role;
import com.github.jredmine.entity.Tracker;
import com.github.jredmine.entity.User;
import com.github.jredmine.enums.ProjectModule;
import com.github.jredmine.enums.ProjectStatus;
import com.github.jredmine.enums.ResultCode;
import com.github.jredmine.exception.BusinessException;
import com.github.jredmine.mapper.TrackerMapper;
import com.github.jredmine.mapper.project.EnabledModuleMapper;
import com.github.jredmine.mapper.project.MemberMapper;
import com.github.jredmine.mapper.project.ProjectMapper;
import com.github.jredmine.mapper.project.ProjectTrackerMapper;
import com.github.jredmine.mapper.user.EmailAddressMapper;
import com.github.jredmine.mapper.user.MemberRoleMapper;
import com.github.jredmine.mapper.user.RoleMapper;
import com.github.jredmine.mapper.user.UserMapper;
import com.github.jredmine.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
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
    private final TrackerMapper trackerMapper;
    private final UserMapper userMapper;
    private final EmailAddressMapper emailAddressMapper;
    private final RoleMapper roleMapper;
    private final MemberRoleMapper memberRoleMapper;
    private final SecurityUtils securityUtils;

    /**
     * 分页查询项目列表
     *
     * @param current  当前页码
     * @param size     每页数量
     * @param name     项目名称（模糊查询）
     * @param status   项目状态（1=活跃，5=关闭，9=归档）
     * @param isPublic 是否公开
     * @param parentId 父项目ID
     * @return 分页响应
     */
    public PageResponse<ProjectListItemResponseDTO> listProjects(
            Integer current, Integer size, String name, Integer status, Boolean isPublic, Long parentId) {
        MDC.put("operation", "list_projects");

        try {
            log.debug("开始查询项目列表，页码: {}, 每页数量: {}", current, size);

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
            Page<Project> page = new Page<>(current, size);

            // 构建查询条件
            LambdaQueryWrapper<Project> queryWrapper = new LambdaQueryWrapper<>();

            // 名称模糊查询
            if (name != null && !name.trim().isEmpty()) {
                queryWrapper.like(Project::getName, name);
            }

            // 状态筛选
            if (status != null && ProjectStatus.isValidCode(status)) {
                queryWrapper.eq(Project::getStatus, status);
            } else {
                // 默认不显示归档项目
                queryWrapper.ne(Project::getStatus, ProjectStatus.ARCHIVED.getCode());
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

            // 权限验证：如果不是管理员，需要检查是否有权限查看
            if (!isAdmin) {
                // 公开项目所有用户可见
                if (Boolean.TRUE.equals(project.getIsPublic())) {
                    log.debug("项目是公开项目，允许访问，项目ID: {}", id);
                } else {
                    // 私有项目需要检查用户是否是项目成员
                    Long currentUserId = currentUser.getId();
                    LambdaQueryWrapper<Member> memberQuery = new LambdaQueryWrapper<>();
                    memberQuery.eq(Member::getProjectId, id)
                            .eq(Member::getUserId, currentUserId);
                    Member member = memberMapper.selectOne(memberQuery);

                    if (member == null) {
                        log.warn("用户无权限访问私有项目，项目ID: {}, 用户ID: {}", id, currentUserId);
                        throw new BusinessException(ResultCode.PROJECT_ACCESS_DENIED);
                    }
                    log.debug("用户是项目成员，允许访问，项目ID: {}, 用户ID: {}", id, currentUserId);
                }
            }

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

            // 权限验证：需要管理员权限（后续可以扩展为检查 create_projects 权限）
            securityUtils.requireAdmin();

            // 获取当前用户
            User currentUser = securityUtils.getCurrentUser();
            Long currentUserId = currentUser.getId();

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

            // 权限验证：需要管理员权限（后续可以扩展为检查 edit_projects 权限）
            securityUtils.requireAdmin();

            // 查询项目是否存在
            Project project = projectMapper.selectById(id);
            if (project == null) {
                log.warn("项目不存在，项目ID: {}", id);
                throw new BusinessException(ResultCode.PROJECT_NOT_FOUND);
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

            // 权限验证：需要管理员权限（后续可以扩展为检查 delete_projects 权限）
            securityUtils.requireAdmin();

            // 查询项目是否存在
            Project project = projectMapper.selectById(id);
            if (project == null) {
                log.warn("项目不存在，项目ID: {}", id);
                throw new BusinessException(ResultCode.PROJECT_NOT_FOUND);
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
            log.debug("开始查询项目成员列表，项目ID: {}, 页码: {}, 每页数量: {}", projectId, current, size);

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
                    return PageResponse.of(List.of(), 0, current, size);
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
            int start = (current - 1) * size;
            if (start >= totalCount) {
                return PageResponse.of(
                        List.of(),
                        totalCount,
                        current,
                        size);
            }

            // 获取当前页的成员ID列表
            List<Long> memberIds = allMemberIds.stream()
                    .skip(start)
                    .limit(size)
                    .collect(Collectors.toList());

            if (memberIds.isEmpty()) {
                return PageResponse.of(
                        List.of(),
                        totalCount,
                        current,
                        size);
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
                    current,
                    size);
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
                // TODO: 检查用户是否有 manage_projects 权限
                // 这里暂时只允许管理员操作，后续可以添加权限检查
                log.warn("用户无权限添加项目成员，项目ID: {}, 用户ID: {}", projectId, currentUser.getId());
                throw new BusinessException(ResultCode.FORBIDDEN, "无权限添加项目成员");
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
                // TODO: 检查用户是否有 manage_projects 权限
                // 这里暂时只允许管理员操作，后续可以添加权限检查
                log.warn("用户无权限更新项目成员，项目ID: {}, 用户ID: {}", projectId, currentUser.getId());
                throw new BusinessException(ResultCode.FORBIDDEN, "无权限更新项目成员");
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
                // TODO: 检查用户是否有 manage_projects 权限
                // 这里暂时只允许管理员操作，后续可以添加权限检查
                log.warn("用户无权限移除项目成员，项目ID: {}, 用户ID: {}", projectId, currentUser.getId());
                throw new BusinessException(ResultCode.FORBIDDEN, "无权限移除项目成员");
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
                // TODO: 检查用户是否有 manage_projects 权限
                // 这里暂时只允许管理员操作，后续可以添加权限检查
                log.warn("用户无权限分配角色给项目成员，项目ID: {}, 用户ID: {}", projectId, currentUser.getId());
                throw new BusinessException(ResultCode.FORBIDDEN, "无权限分配角色给项目成员");
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
}
