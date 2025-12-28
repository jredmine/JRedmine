package com.github.jredmine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.jredmine.dto.response.PageResponse;
import com.github.jredmine.dto.response.project.ProjectDetailResponseDTO;
import com.github.jredmine.dto.response.project.ProjectListItemResponseDTO;
import com.github.jredmine.entity.Member;
import com.github.jredmine.entity.Project;
import com.github.jredmine.entity.User;
import com.github.jredmine.enums.ProjectStatus;
import com.github.jredmine.enums.ResultCode;
import com.github.jredmine.exception.BusinessException;
import com.github.jredmine.mapper.project.MemberMapper;
import com.github.jredmine.mapper.project.ProjectMapper;
import com.github.jredmine.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

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

