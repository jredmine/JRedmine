package com.github.jredmine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.jredmine.dto.request.project.ProjectCreateRequestDTO;
import com.github.jredmine.dto.response.PageResponse;
import com.github.jredmine.dto.response.project.ProjectDetailResponseDTO;
import com.github.jredmine.dto.response.project.ProjectListItemResponseDTO;
import com.github.jredmine.entity.EnabledModule;
import com.github.jredmine.entity.Member;
import com.github.jredmine.entity.Project;
import com.github.jredmine.entity.ProjectTracker;
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
import com.github.jredmine.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
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
                    ? requestDTO.getParentId() : null);
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

