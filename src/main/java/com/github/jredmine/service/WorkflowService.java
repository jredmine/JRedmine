package com.github.jredmine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.jredmine.dto.request.workflow.WorkflowCreateRequestDTO;
import com.github.jredmine.dto.request.workflow.WorkflowUpdateRequestDTO;
import com.github.jredmine.dto.response.PageResponse;
import com.github.jredmine.dto.response.workflow.AvailableTransitionDTO;
import com.github.jredmine.dto.response.workflow.WorkflowResponseDTO;
import com.github.jredmine.dto.response.workflow.WorkflowTransitionResponseDTO;
import com.github.jredmine.entity.IssueStatus;
import com.github.jredmine.entity.Role;
import com.github.jredmine.entity.Tracker;
import com.github.jredmine.entity.Workflow;
import com.github.jredmine.enums.ResultCode;
import com.github.jredmine.enums.WorkflowRule;
import com.github.jredmine.enums.WorkflowType;
import com.github.jredmine.exception.BusinessException;
import com.github.jredmine.mapper.TrackerMapper;
import com.github.jredmine.mapper.user.RoleMapper;
import com.github.jredmine.mapper.workflow.IssueStatusMapper;
import com.github.jredmine.mapper.workflow.WorkflowMapper;
import com.github.jredmine.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 工作流服务
 *
 * @author panfeng
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowService {

    private final WorkflowMapper workflowMapper;
    private final IssueStatusMapper issueStatusMapper;
    private final TrackerMapper trackerMapper;
    private final RoleMapper roleMapper;
    private final SecurityUtils securityUtils;

    /**
     * 分页查询工作流列表
     *
     * @param current      当前页码
     * @param size         每页数量
     * @param trackerId    跟踪器ID（可选）
     * @param oldStatusId  旧状态ID（可选）
     * @param newStatusId  新状态ID（可选）
     * @param roleId       角色ID（可选）
     * @param type         类型（可选：'transition' 或 'field'）
     * @return 分页响应
     */
    public PageResponse<WorkflowResponseDTO> listWorkflows(
            Integer current, Integer size,
            Integer trackerId, Integer oldStatusId, Integer newStatusId,
            Integer roleId, String type) {
        MDC.put("operation", "list_workflows");

        try {
            log.debug("开始查询工作流列表，页码: {}, 每页数量: {}", current, size);

            // 创建分页对象
            Page<Workflow> page = new Page<>(current, size);

            // 构建查询条件
            LambdaQueryWrapper<Workflow> queryWrapper = new LambdaQueryWrapper<>();
            if (trackerId != null) {
                queryWrapper.eq(Workflow::getTrackerId, trackerId);
            }
            if (oldStatusId != null) {
                queryWrapper.eq(Workflow::getOldStatusId, oldStatusId);
            }
            if (newStatusId != null) {
                queryWrapper.eq(Workflow::getNewStatusId, newStatusId);
            }
            if (roleId != null) {
                queryWrapper.eq(Workflow::getRoleId, roleId);
            }
            if (type != null && !type.trim().isEmpty()) {
                queryWrapper.eq(Workflow::getType, type);
            }
            queryWrapper.orderByDesc(Workflow::getId);

            // 执行分页查询
            Page<Workflow> result = workflowMapper.selectPage(page, queryWrapper);

            MDC.put("total", String.valueOf(result.getTotal()));
            log.info("工作流列表查询成功，共查询到 {} 条记录", result.getTotal());

            // 转换为响应 DTO
            List<WorkflowResponseDTO> dtos = result.getRecords().stream()
                    .map(this::toResponseDTO)
                    .collect(Collectors.toList());

            return PageResponse.of(
                    dtos,
                    (int) result.getTotal(),
                    (int) result.getCurrent(),
                    (int) result.getSize());
        } catch (Exception e) {
            log.error("工作流列表查询失败", e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "工作流列表查询失败");
        } finally {
            MDC.clear();
        }
    }

    /**
     * 根据ID查询工作流详情
     *
     * @param id 工作流ID
     * @return 工作流详情
     */
    public WorkflowResponseDTO getWorkflowById(Integer id) {
        MDC.put("operation", "get_workflow_by_id");
        MDC.put("workflowId", String.valueOf(id));

        try {
            log.debug("开始查询工作流详情，工作流ID: {}", id);

            Workflow workflow = workflowMapper.selectById(id);

            if (workflow == null) {
                log.warn("工作流不存在，工作流ID: {}", id);
                throw new BusinessException(ResultCode.SYSTEM_ERROR, "工作流不存在");
            }

            log.info("工作流详情查询成功，工作流ID: {}", id);

            return toResponseDTO(workflow);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("工作流详情查询失败，工作流ID: {}", id, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "工作流详情查询失败");
        } finally {
            MDC.clear();
        }
    }

    /**
     * 创建工作流规则
     *
     * @param requestDTO 工作流创建请求
     * @return 创建的工作流详情
     */
    @Transactional(rollbackFor = Exception.class)
    public WorkflowResponseDTO createWorkflow(WorkflowCreateRequestDTO requestDTO) {
        MDC.put("operation", "create_workflow");

        try {
            log.debug("开始创建工作流规则");

            // 权限验证：需要管理员权限
            securityUtils.requireAdmin();

            // 验证类型
            WorkflowType workflowType = WorkflowType.fromCode(requestDTO.getType());
            if (workflowType == null) {
                throw new BusinessException(ResultCode.PARAM_INVALID, "无效的工作流类型: " + requestDTO.getType());
            }

            // 验证字段规则
            if (WorkflowType.FIELD.equals(workflowType)) {
                if (requestDTO.getFieldName() == null || requestDTO.getFieldName().trim().isEmpty()) {
                    throw new BusinessException(ResultCode.PARAM_INVALID, "字段规则必须指定字段名");
                }
                if (requestDTO.getRule() == null || requestDTO.getRule().trim().isEmpty()) {
                    throw new BusinessException(ResultCode.PARAM_INVALID, "字段规则必须指定规则");
                }
                WorkflowRule rule = WorkflowRule.fromCode(requestDTO.getRule());
                if (rule == null) {
                    throw new BusinessException(ResultCode.PARAM_INVALID, "无效的字段规则: " + requestDTO.getRule());
                }
            }

            // 验证跟踪器（如果指定）
            if (requestDTO.getTrackerId() != null && requestDTO.getTrackerId() > 0) {
                Tracker tracker = trackerMapper.selectById(requestDTO.getTrackerId());
                if (tracker == null) {
                    throw new BusinessException(ResultCode.TRACKER_NOT_FOUND);
                }
            }

            // 验证状态（如果指定）
            if (requestDTO.getOldStatusId() != null && requestDTO.getOldStatusId() > 0) {
                IssueStatus oldStatus = issueStatusMapper.selectById(requestDTO.getOldStatusId());
                if (oldStatus == null) {
                    throw new BusinessException(ResultCode.SYSTEM_ERROR, "旧状态不存在");
                }
            }

            if (requestDTO.getNewStatusId() != null && requestDTO.getNewStatusId() > 0) {
                IssueStatus newStatus = issueStatusMapper.selectById(requestDTO.getNewStatusId());
                if (newStatus == null) {
                    throw new BusinessException(ResultCode.SYSTEM_ERROR, "新状态不存在");
                }
            }

            // 验证角色（如果指定）
            if (requestDTO.getRoleId() != null && requestDTO.getRoleId() > 0) {
                Role role = roleMapper.selectById(requestDTO.getRoleId());
                if (role == null) {
                    throw new BusinessException(ResultCode.ROLE_NOT_FOUND);
                }
            }

            // 创建实体
            Workflow workflow = new Workflow();
            workflow.setTrackerId(requestDTO.getTrackerId());
            workflow.setOldStatusId(requestDTO.getOldStatusId());
            workflow.setNewStatusId(requestDTO.getNewStatusId());
            workflow.setRoleId(requestDTO.getRoleId());
            workflow.setAssignee(requestDTO.getAssignee() != null ? requestDTO.getAssignee() : false);
            workflow.setAuthor(requestDTO.getAuthor() != null ? requestDTO.getAuthor() : false);
            workflow.setType(requestDTO.getType());
            workflow.setFieldName(requestDTO.getFieldName());
            workflow.setRule(requestDTO.getRule());

            // 保存到数据库
            int result = workflowMapper.insert(workflow);

            if (result <= 0) {
                log.error("工作流创建失败");
                throw new BusinessException(ResultCode.SYSTEM_ERROR, "工作流创建失败");
            }

            log.info("工作流创建成功，工作流ID: {}", workflow.getId());

            return toResponseDTO(workflow);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("工作流创建失败", e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "工作流创建失败");
        } finally {
            MDC.clear();
        }
    }

    /**
     * 更新工作流规则
     *
     * @param id          工作流ID
     * @param requestDTO  工作流更新请求
     * @return 更新后的工作流详情
     */
    @Transactional(rollbackFor = Exception.class)
    public WorkflowResponseDTO updateWorkflow(Integer id, WorkflowUpdateRequestDTO requestDTO) {
        MDC.put("operation", "update_workflow");
        MDC.put("workflowId", String.valueOf(id));

        try {
            log.debug("开始更新工作流规则，工作流ID: {}", id);

            // 权限验证：需要管理员权限
            securityUtils.requireAdmin();

            // 查询工作流
            Workflow workflow = workflowMapper.selectById(id);
            if (workflow == null) {
                log.warn("工作流不存在，工作流ID: {}", id);
                throw new BusinessException(ResultCode.SYSTEM_ERROR, "工作流不存在");
            }

            // 更新字段
            if (requestDTO.getTrackerId() != null) {
                workflow.setTrackerId(requestDTO.getTrackerId());
            }
            if (requestDTO.getOldStatusId() != null) {
                workflow.setOldStatusId(requestDTO.getOldStatusId());
            }
            if (requestDTO.getNewStatusId() != null) {
                workflow.setNewStatusId(requestDTO.getNewStatusId());
            }
            if (requestDTO.getRoleId() != null) {
                workflow.setRoleId(requestDTO.getRoleId());
            }
            if (requestDTO.getAssignee() != null) {
                workflow.setAssignee(requestDTO.getAssignee());
            }
            if (requestDTO.getAuthor() != null) {
                workflow.setAuthor(requestDTO.getAuthor());
            }
            if (requestDTO.getType() != null) {
                workflow.setType(requestDTO.getType());
            }
            if (requestDTO.getFieldName() != null) {
                workflow.setFieldName(requestDTO.getFieldName());
            }
            if (requestDTO.getRule() != null) {
                workflow.setRule(requestDTO.getRule());
            }

            // 保存到数据库
            int result = workflowMapper.updateById(workflow);

            if (result <= 0) {
                log.error("工作流更新失败，工作流ID: {}", id);
                throw new BusinessException(ResultCode.SYSTEM_ERROR, "工作流更新失败");
            }

            log.info("工作流更新成功，工作流ID: {}", id);

            return toResponseDTO(workflow);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("工作流更新失败，工作流ID: {}", id, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "工作流更新失败");
        } finally {
            MDC.clear();
        }
    }

    /**
     * 删除工作流规则
     *
     * @param id 工作流ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteWorkflow(Integer id) {
        MDC.put("operation", "delete_workflow");
        MDC.put("workflowId", String.valueOf(id));

        try {
            log.debug("开始删除工作流规则，工作流ID: {}", id);

            // 权限验证：需要管理员权限
            securityUtils.requireAdmin();

            // 查询工作流
            Workflow workflow = workflowMapper.selectById(id);
            if (workflow == null) {
                log.warn("工作流不存在，工作流ID: {}", id);
                throw new BusinessException(ResultCode.SYSTEM_ERROR, "工作流不存在");
            }

            // 删除
            int result = workflowMapper.deleteById(id);

            if (result <= 0) {
                log.error("工作流删除失败，工作流ID: {}", id);
                throw new BusinessException(ResultCode.SYSTEM_ERROR, "工作流删除失败");
            }

            log.info("工作流删除成功，工作流ID: {}", id);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("工作流删除失败，工作流ID: {}", id, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "工作流删除失败");
        } finally {
            MDC.clear();
        }
    }

    /**
     * 获取可用的状态转换
     * 根据跟踪器、当前状态和用户角色，返回可以转换到的目标状态列表
     *
     * @param trackerId      跟踪器ID
     * @param currentStatusId 当前状态ID
     * @param roleIds         用户角色ID列表
     * @return 可用的状态转换
     */
    public WorkflowTransitionResponseDTO getAvailableTransitions(
            Integer trackerId, Integer currentStatusId, List<Integer> roleIds) {
        MDC.put("operation", "get_available_transitions");
        MDC.put("trackerId", String.valueOf(trackerId));
        MDC.put("currentStatusId", String.valueOf(currentStatusId));

        try {
            log.debug("开始查询可用的状态转换，跟踪器ID: {}, 当前状态ID: {}, 角色IDs: {}",
                    trackerId, currentStatusId, roleIds);

            // 查询当前状态
            IssueStatus currentStatus = issueStatusMapper.selectById(currentStatusId);
            if (currentStatus == null) {
                throw new BusinessException(ResultCode.SYSTEM_ERROR, "当前状态不存在");
            }

            // 查询工作流规则
            LambdaQueryWrapper<Workflow> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Workflow::getType, WorkflowType.TRANSITION.getCode());
            queryWrapper.and(wrapper -> wrapper
                    .eq(Workflow::getTrackerId, 0)  // 所有跟踪器
                    .or()
                    .eq(Workflow::getTrackerId, trackerId)  // 指定跟踪器
            );
            queryWrapper.and(wrapper -> wrapper
                    .eq(Workflow::getOldStatusId, 0)  // 所有状态
                    .or()
                    .eq(Workflow::getOldStatusId, currentStatusId)  // 当前状态
            );
            // 角色条件：如果 roleIds 为空，只查询所有角色（role_id = 0）
            // 如果 roleIds 不为空，查询所有角色或用户角色
            if (roleIds == null || roleIds.isEmpty()) {
                queryWrapper.eq(Workflow::getRoleId, 0);  // 只查询所有角色
            } else {
                queryWrapper.and(wrapper -> wrapper
                        .eq(Workflow::getRoleId, 0)  // 所有角色
                        .or()
                        .in(Workflow::getRoleId, roleIds)  // 用户角色
                );
            }

            List<Workflow> workflows = workflowMapper.selectList(queryWrapper);

            // 提取可用的目标状态
            Set<Integer> availableStatusIds = new HashSet<>();
            Map<Integer, AvailableTransitionDTO> transitionMap = new HashMap<>();

            for (Workflow workflow : workflows) {
                Integer newStatusId = workflow.getNewStatusId();
                if (newStatusId != null && newStatusId > 0) {
                    availableStatusIds.add(newStatusId);

                    // 如果已存在，合并限制条件（取更严格的限制）
                    AvailableTransitionDTO transition = transitionMap.get(newStatusId);
                    if (transition == null) {
                        transition = AvailableTransitionDTO.builder()
                                .statusId(newStatusId)
                                .assignee(workflow.getAssignee())
                                .author(workflow.getAuthor())
                                .build();
                        transitionMap.put(newStatusId, transition);
                    } else {
                        // 合并限制：如果任一规则要求 assignee 或 author，则设置为 true
                        if (workflow.getAssignee() != null && workflow.getAssignee()) {
                            transition.setAssignee(true);
                        }
                        if (workflow.getAuthor() != null && workflow.getAuthor()) {
                            transition.setAuthor(true);
                        }
                    }
                }
            }

            // 查询状态详情并构建响应
            List<AvailableTransitionDTO> transitions = new ArrayList<>();
            for (Integer statusId : availableStatusIds) {
                IssueStatus status = issueStatusMapper.selectById(statusId);
                if (status != null) {
                    AvailableTransitionDTO transition = transitionMap.get(statusId);
                    transition.setStatusName(status.getName());
                    transitions.add(transition);
                }
            }

            // 按状态ID排序
            transitions.sort(Comparator.comparing(AvailableTransitionDTO::getStatusId));

            log.info("查询可用的状态转换成功，跟踪器ID: {}, 当前状态ID: {}, 可用转换数量: {}",
                    trackerId, currentStatusId, transitions.size());

            return WorkflowTransitionResponseDTO.builder()
                    .currentStatusId(currentStatusId)
                    .currentStatusName(currentStatus.getName())
                    .availableTransitions(transitions)
                    .build();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("查询可用的状态转换失败，跟踪器ID: {}, 当前状态ID: {}", trackerId, currentStatusId, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "查询可用的状态转换失败");
        } finally {
            MDC.clear();
        }
    }

    /**
     * 转换为响应 DTO
     */
    private WorkflowResponseDTO toResponseDTO(Workflow workflow) {
        WorkflowResponseDTO.WorkflowResponseDTOBuilder builder = WorkflowResponseDTO.builder()
                .id(workflow.getId())
                .trackerId(workflow.getTrackerId())
                .oldStatusId(workflow.getOldStatusId())
                .newStatusId(workflow.getNewStatusId())
                .roleId(workflow.getRoleId())
                .assignee(workflow.getAssignee())
                .author(workflow.getAuthor())
                .type(workflow.getType())
                .fieldName(workflow.getFieldName())
                .rule(workflow.getRule());

        // 查询并设置跟踪器名称
        if (workflow.getTrackerId() != null && workflow.getTrackerId() > 0) {
            Tracker tracker = trackerMapper.selectById(workflow.getTrackerId());
            if (tracker != null) {
                builder.trackerName(tracker.getName());
            }
        } else {
            builder.trackerName("所有跟踪器");
        }

        // 查询并设置旧状态名称
        if (workflow.getOldStatusId() != null && workflow.getOldStatusId() > 0) {
            IssueStatus oldStatus = issueStatusMapper.selectById(workflow.getOldStatusId());
            if (oldStatus != null) {
                builder.oldStatusName(oldStatus.getName());
            }
        } else {
            builder.oldStatusName("所有状态");
        }

        // 查询并设置新状态名称
        if (workflow.getNewStatusId() != null && workflow.getNewStatusId() > 0) {
            IssueStatus newStatus = issueStatusMapper.selectById(workflow.getNewStatusId());
            if (newStatus != null) {
                builder.newStatusName(newStatus.getName());
            }
        }

        // 查询并设置角色名称
        if (workflow.getRoleId() != null && workflow.getRoleId() > 0) {
            Role role = roleMapper.selectById(workflow.getRoleId());
            if (role != null) {
                builder.roleName(role.getName());
            }
        } else {
            builder.roleName("所有角色");
        }

        return builder.build();
    }
}

