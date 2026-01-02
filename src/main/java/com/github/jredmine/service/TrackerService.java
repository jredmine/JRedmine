package com.github.jredmine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.jredmine.dto.request.tracker.TrackerCreateRequestDTO;
import com.github.jredmine.dto.request.tracker.TrackerUpdateRequestDTO;
import com.github.jredmine.dto.response.PageResponse;
import com.github.jredmine.dto.response.tracker.TrackerDetailResponseDTO;
import com.github.jredmine.dto.response.tracker.TrackerListItemResponseDTO;
import com.github.jredmine.entity.ProjectTracker;
import com.github.jredmine.entity.Tracker;
import com.github.jredmine.enums.ResultCode;
import com.github.jredmine.exception.BusinessException;
import com.github.jredmine.mapper.TrackerMapper;
import com.github.jredmine.mapper.project.ProjectTrackerMapper;
import com.github.jredmine.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 跟踪器服务
 *
 * @author panfeng
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrackerService {

    private final TrackerMapper trackerMapper;
    private final ProjectTrackerMapper projectTrackerMapper;
    private final SecurityUtils securityUtils;

    /**
     * 分页查询跟踪器列表
     *
     * @param current 当前页码
     * @param size    每页数量
     * @param name    跟踪器名称（模糊查询）
     * @return 分页响应
     */
    public PageResponse<TrackerListItemResponseDTO> listTrackers(
            Integer current, Integer size, String name) {
        MDC.put("operation", "list_trackers");

        try {
            // 设置默认值并验证：current 至少为 1，size 至少为 10
            Integer validCurrent = (current != null && current > 0) ? current : 1;
            Integer validSize = (size != null && size > 0) ? size : 10;

            log.debug("开始查询跟踪器列表，页码: {}, 每页数量: {}", validCurrent, validSize);

            // 创建分页对象
            Page<Tracker> page = new Page<>(validCurrent, validSize);

            // 构建查询条件
            LambdaQueryWrapper<Tracker> queryWrapper = new LambdaQueryWrapper<>();
            if (name != null && !name.trim().isEmpty()) {
                queryWrapper.like(Tracker::getName, name);
            }
            // 按 position 排序，如果 position 相同则按 id 排序
            queryWrapper.orderByAsc(Tracker::getPosition).orderByAsc(Tracker::getId);

            // 执行分页查询
            Page<Tracker> result = trackerMapper.selectPage(page, queryWrapper);

            MDC.put("total", String.valueOf(result.getTotal()));
            log.info("跟踪器列表查询成功，共查询到 {} 条记录", result.getTotal());

            // 转换为响应 DTO
            List<TrackerListItemResponseDTO> dtoList = result.getRecords().stream()
                    .map(this::toTrackerListItemResponseDTO)
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
     * 根据ID获取跟踪器详情
     *
     * @param id 跟踪器ID
     * @return 跟踪器详情
     */
    public TrackerDetailResponseDTO getTrackerById(Long id) {
        MDC.put("operation", "get_tracker_by_id");
        MDC.put("trackerId", String.valueOf(id));

        try {
            log.debug("开始查询跟踪器详情，跟踪器ID: {}", id);

            // 查询跟踪器
            Tracker tracker = trackerMapper.selectById(id);
            if (tracker == null) {
                log.warn("跟踪器不存在，跟踪器ID: {}", id);
                throw new BusinessException(ResultCode.TRACKER_NOT_FOUND);
            }

            log.info("跟踪器详情查询成功，跟踪器ID: {}", id);

            // 转换为响应 DTO
            return toTrackerDetailResponseDTO(tracker);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("跟踪器详情查询失败，跟踪器ID: {}", id, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "跟踪器详情查询失败");
        } finally {
            MDC.clear();
        }
    }

    /**
     * 创建跟踪器
     *
     * @param requestDTO 创建跟踪器请求
     * @return 跟踪器详情
     */
    @Transactional(rollbackFor = Exception.class)
    public TrackerDetailResponseDTO createTracker(TrackerCreateRequestDTO requestDTO) {
        MDC.put("operation", "create_tracker");

        try {
            log.info("开始创建跟踪器，跟踪器名称: {}", requestDTO.getName());

            // 权限验证：需要管理员权限
            securityUtils.requireAdmin();

            // 验证跟踪器名称唯一性
            LambdaQueryWrapper<Tracker> nameQuery = new LambdaQueryWrapper<>();
            nameQuery.eq(Tracker::getName, requestDTO.getName());
            Tracker existingTracker = trackerMapper.selectOne(nameQuery);
            if (existingTracker != null) {
                log.warn("跟踪器名称已存在: {}", requestDTO.getName());
                throw new BusinessException(ResultCode.TRACKER_NAME_EXISTS);
            }

            // 创建跟踪器实体
            Tracker tracker = new Tracker();
            tracker.setName(requestDTO.getName());
            tracker.setDescription(requestDTO.getDescription());
            tracker.setPosition(requestDTO.getPosition());
            tracker.setIsInRoadmap(requestDTO.getIsInRoadmap() != null ? requestDTO.getIsInRoadmap() : true);
            tracker.setFieldsBits(requestDTO.getFieldsBits() != null ? requestDTO.getFieldsBits() : 0);
            tracker.setDefaultStatusId(requestDTO.getDefaultStatusId());

            // 保存跟踪器
            trackerMapper.insert(tracker);
            Long trackerId = tracker.getId();
            log.debug("跟踪器创建成功，跟踪器ID: {}", trackerId);

            log.info("跟踪器创建成功，跟踪器ID: {}, 跟踪器名称: {}", trackerId, requestDTO.getName());

            // 返回跟踪器详情
            return toTrackerDetailResponseDTO(tracker);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("跟踪器创建失败，跟踪器名称: {}", requestDTO.getName(), e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "跟踪器创建失败");
        } finally {
            MDC.clear();
        }
    }

    /**
     * 更新跟踪器
     *
     * @param id         跟踪器ID
     * @param requestDTO 更新跟踪器请求
     * @return 跟踪器详情
     */
    @Transactional(rollbackFor = Exception.class)
    public TrackerDetailResponseDTO updateTracker(Long id, TrackerUpdateRequestDTO requestDTO) {
        MDC.put("operation", "update_tracker");
        MDC.put("trackerId", String.valueOf(id));

        try {
            log.info("开始更新跟踪器，跟踪器ID: {}", id);

            // 权限验证：需要管理员权限
            securityUtils.requireAdmin();

            // 查询跟踪器是否存在
            Tracker tracker = trackerMapper.selectById(id);
            if (tracker == null) {
                log.warn("跟踪器不存在，跟踪器ID: {}", id);
                throw new BusinessException(ResultCode.TRACKER_NOT_FOUND);
            }

            // 验证跟踪器名称唯一性（排除当前跟踪器）
            if (requestDTO.getName() != null && !requestDTO.getName().equals(tracker.getName())) {
                LambdaQueryWrapper<Tracker> nameQuery = new LambdaQueryWrapper<>();
                nameQuery.eq(Tracker::getName, requestDTO.getName())
                        .ne(Tracker::getId, id);
                Tracker existingTracker = trackerMapper.selectOne(nameQuery);
                if (existingTracker != null) {
                    log.warn("跟踪器名称已存在: {}", requestDTO.getName());
                    throw new BusinessException(ResultCode.TRACKER_NAME_EXISTS);
                }
                tracker.setName(requestDTO.getName());
            }

            // 更新其他字段
            if (requestDTO.getDescription() != null) {
                tracker.setDescription(requestDTO.getDescription());
            }
            if (requestDTO.getPosition() != null) {
                tracker.setPosition(requestDTO.getPosition());
            }
            if (requestDTO.getIsInRoadmap() != null) {
                tracker.setIsInRoadmap(requestDTO.getIsInRoadmap());
            }
            if (requestDTO.getFieldsBits() != null) {
                tracker.setFieldsBits(requestDTO.getFieldsBits());
            }
            if (requestDTO.getDefaultStatusId() != null) {
                tracker.setDefaultStatusId(requestDTO.getDefaultStatusId());
            }

            // 保存跟踪器
            trackerMapper.updateById(tracker);
            log.debug("跟踪器信息更新成功，跟踪器ID: {}", id);

            log.info("跟踪器更新成功，跟踪器ID: {}", id);

            // 重新查询跟踪器（获取最新数据）
            tracker = trackerMapper.selectById(id);
            return toTrackerDetailResponseDTO(tracker);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("跟踪器更新失败，跟踪器ID: {}", id, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "跟踪器更新失败");
        } finally {
            MDC.clear();
        }
    }

    /**
     * 删除跟踪器
     *
     * @param id 跟踪器ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteTracker(Long id) {
        MDC.put("operation", "delete_tracker");
        MDC.put("trackerId", String.valueOf(id));

        try {
            log.info("开始删除跟踪器，跟踪器ID: {}", id);

            // 权限验证：需要管理员权限
            securityUtils.requireAdmin();

            // 查询跟踪器是否存在
            Tracker tracker = trackerMapper.selectById(id);
            if (tracker == null) {
                log.warn("跟踪器不存在，跟踪器ID: {}", id);
                throw new BusinessException(ResultCode.TRACKER_NOT_FOUND);
            }

            // 检查是否有项目使用此跟踪器
            LambdaQueryWrapper<ProjectTracker> projectTrackerQuery = new LambdaQueryWrapper<>();
            projectTrackerQuery.eq(ProjectTracker::getTrackerId, id);
            Long projectTrackerCount = projectTrackerMapper.selectCount(projectTrackerQuery);
            if (projectTrackerCount > 0) {
                log.warn("跟踪器正在被项目使用，不能删除，跟踪器ID: {}, 使用数量: {}", id, projectTrackerCount);
                throw new BusinessException(ResultCode.TRACKER_IN_USE,
                        "跟踪器正在被 " + projectTrackerCount + " 个项目使用，请先移除关联后再删除");
            }

            // 删除跟踪器
            trackerMapper.deleteById(id);

            log.info("跟踪器删除成功，跟踪器ID: {}, 跟踪器名称: {}", id, tracker.getName());

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("跟踪器删除失败，跟踪器ID: {}", id, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "跟踪器删除失败");
        } finally {
            MDC.clear();
        }
    }

    /**
     * 将 Tracker 实体转换为 TrackerListItemResponseDTO
     *
     * @param tracker 跟踪器实体
     * @return 响应 DTO
     */
    private TrackerListItemResponseDTO toTrackerListItemResponseDTO(Tracker tracker) {
        TrackerListItemResponseDTO dto = new TrackerListItemResponseDTO();
        dto.setId(tracker.getId());
        dto.setName(tracker.getName());
        dto.setDescription(tracker.getDescription());
        dto.setPosition(tracker.getPosition());
        dto.setIsInRoadmap(tracker.getIsInRoadmap());
        return dto;
    }

    /**
     * 将 Tracker 实体转换为 TrackerDetailResponseDTO
     *
     * @param tracker 跟踪器实体
     * @return 响应 DTO
     */
    private TrackerDetailResponseDTO toTrackerDetailResponseDTO(Tracker tracker) {
        TrackerDetailResponseDTO dto = new TrackerDetailResponseDTO();
        dto.setId(tracker.getId());
        dto.setName(tracker.getName());
        dto.setDescription(tracker.getDescription());
        dto.setPosition(tracker.getPosition());
        dto.setIsInRoadmap(tracker.getIsInRoadmap());
        dto.setFieldsBits(tracker.getFieldsBits());
        dto.setDefaultStatusId(tracker.getDefaultStatusId());
        return dto;
    }
}
