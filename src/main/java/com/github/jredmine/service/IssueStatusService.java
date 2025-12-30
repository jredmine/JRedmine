package com.github.jredmine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.jredmine.dto.response.workflow.IssueStatusResponseDTO;
import com.github.jredmine.entity.IssueStatus;
import com.github.jredmine.enums.ResultCode;
import com.github.jredmine.exception.BusinessException;
import com.github.jredmine.mapper.workflow.IssueStatusMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 任务状态服务
 *
 * @author panfeng
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IssueStatusService {

    private final IssueStatusMapper issueStatusMapper;

    /**
     * 获取所有任务状态列表
     *
     * @return 任务状态列表
     */
    public List<IssueStatusResponseDTO> listAllStatuses() {
        MDC.put("operation", "list_all_statuses");

        try {
            log.debug("开始查询所有任务状态列表");

            List<IssueStatus> statuses = issueStatusMapper.selectList(
                    new LambdaQueryWrapper<IssueStatus>()
                            .orderByAsc(IssueStatus::getPosition)
                            .orderByAsc(IssueStatus::getId)
            );

            log.info("任务状态列表查询成功，共 {} 条记录", statuses.size());

            return statuses.stream()
                    .map(this::toResponseDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("任务状态列表查询失败", e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "任务状态列表查询失败");
        } finally {
            MDC.clear();
        }
    }

    /**
     * 根据ID查询任务状态
     *
     * @param id 状态ID
     * @return 任务状态详情
     */
    public IssueStatusResponseDTO getStatusById(Integer id) {
        MDC.put("operation", "get_status_by_id");
        MDC.put("statusId", String.valueOf(id));

        try {
            log.debug("开始查询任务状态详情，状态ID: {}", id);

            IssueStatus status = issueStatusMapper.selectById(id);

            if (status == null) {
                log.warn("任务状态不存在，状态ID: {}", id);
                throw new BusinessException(ResultCode.SYSTEM_ERROR, "任务状态不存在");
            }

            log.info("任务状态详情查询成功，状态ID: {}", id);

            return toResponseDTO(status);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("任务状态详情查询失败，状态ID: {}", id, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "任务状态详情查询失败");
        } finally {
            MDC.clear();
        }
    }

    /**
     * 转换为响应 DTO
     */
    private IssueStatusResponseDTO toResponseDTO(IssueStatus status) {
        return IssueStatusResponseDTO.builder()
                .id(status.getId())
                .name(status.getName())
                .description(status.getDescription())
                .isClosed(status.getIsClosed())
                .position(status.getPosition())
                .defaultDoneRatio(status.getDefaultDoneRatio())
                .build();
    }
}

