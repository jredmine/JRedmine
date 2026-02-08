package com.github.jredmine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.jredmine.dto.request.board.BoardCreateRequestDTO;
import com.github.jredmine.dto.request.board.BoardUpdateRequestDTO;
import com.github.jredmine.dto.response.board.BoardDetailResponseDTO;
import com.github.jredmine.dto.response.board.BoardListItemResponseDTO;
import com.github.jredmine.entity.Board;
import com.github.jredmine.entity.EnabledModule;
import com.github.jredmine.entity.Project;
import com.github.jredmine.enums.ProjectModule;
import com.github.jredmine.enums.ResultCode;
import com.github.jredmine.exception.BusinessException;
import com.github.jredmine.mapper.BoardMapper;
import com.github.jredmine.mapper.project.EnabledModuleMapper;
import com.github.jredmine.mapper.project.ProjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 论坛板块服务：创建、列表、详情、更新、删除
 *
 * @author panfeng
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BoardService {

    private final BoardMapper boardMapper;
    private final EnabledModuleMapper enabledModuleMapper;
    private final ProjectMapper projectMapper;

    /**
     * 板块列表（含统计）：按项目查询所有板块，按 position、name 排序。
     * 要求项目存在且已启用论坛模块。lastMessageSubject、lastMessageUpdatedOn 待消息模块实现后可填充。
     */
    public List<BoardListItemResponseDTO> listBoards(Long projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException(ResultCode.PROJECT_NOT_FOUND);
        }
        if (!isBoardsEnabledForProject(projectId)) {
            throw new BusinessException(ResultCode.BOARDS_NOT_ENABLED);
        }
        LambdaQueryWrapper<Board> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Board::getProjectId, projectId.intValue())
                .orderByAsc(Board::getPosition)
                .orderByAsc(Board::getName);
        List<Board> list = boardMapper.selectList(wrapper);
        return list.stream()
                .map(this::toListItemResponse)
                .collect(Collectors.toList());
    }

    /**
     * 板块详情（含统计）：按 boardId 返回板块信息及 topicsCount、messagesCount、lastMessageId。
     * 要求板块属于当前项目且项目已启用论坛模块。
     */
    public BoardDetailResponseDTO getDetail(Long projectId, Integer boardId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException(ResultCode.PROJECT_NOT_FOUND);
        }
        if (!isBoardsEnabledForProject(projectId)) {
            throw new BusinessException(ResultCode.BOARDS_NOT_ENABLED);
        }
        Board board = getBoardByProjectAndId(projectId, boardId);
        return toDetailResponse(board);
    }

    /**
     * 检查项目是否启用了论坛模块
     */
    public boolean isBoardsEnabledForProject(Long projectId) {
        LambdaQueryWrapper<EnabledModule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EnabledModule::getProjectId, projectId)
                .eq(EnabledModule::getName, ProjectModule.BOARDS.getCode());
        return enabledModuleMapper.selectCount(wrapper) > 0;
    }

    /**
     * 创建板块：项目下新增一条 boards 记录。
     * 要求项目已启用 boards 模块；name 必填，description、position、parentId 可选；同项目下板块名称不可重复。
     */
    @Transactional(rollbackFor = Exception.class)
    public BoardDetailResponseDTO create(Long projectId, BoardCreateRequestDTO dto) {
        if (dto == null || dto.getName() == null || dto.getName().isBlank()) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "板块名称不能为空");
        }
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException(ResultCode.PROJECT_NOT_FOUND);
        }
        if (!isBoardsEnabledForProject(projectId)) {
            throw new BusinessException(ResultCode.BOARDS_NOT_ENABLED);
        }
        String name = dto.getName().trim();
        LambdaQueryWrapper<Board> existWrapper = new LambdaQueryWrapper<>();
        existWrapper.eq(Board::getProjectId, projectId.intValue()).eq(Board::getName, name);
        if (boardMapper.selectCount(existWrapper) > 0) {
            throw new BusinessException(ResultCode.BOARD_NAME_EXISTS);
        }
        if (dto.getParentId() != null && dto.getParentId() > 0) {
            Board parent = boardMapper.selectById(dto.getParentId());
            if (parent == null || !parent.getProjectId().equals(projectId.intValue())) {
                throw new BusinessException(ResultCode.BOARD_NOT_FOUND, "父板块不存在或不属于当前项目");
            }
        }
        Integer position = dto.getPosition() != null ? dto.getPosition() : 0;
        Board board = new Board();
        board.setProjectId(projectId.intValue());
        board.setName(name);
        board.setDescription(dto.getDescription());
        board.setPosition(position);
        board.setTopicsCount(0);
        board.setMessagesCount(0);
        board.setParentId(dto.getParentId());
        boardMapper.insert(board);
        log.info("论坛板块创建成功: projectId={}, boardId={}, name={}", projectId, board.getId(), name);
        return toDetailResponse(board);
    }

    /**
     * 更新板块：仅更新请求中非空字段（name、description、position）。
     * 若更新 name，同项目下不可与其他板块重名。
     */
    @Transactional(rollbackFor = Exception.class)
    public BoardDetailResponseDTO update(Long projectId, Integer boardId, BoardUpdateRequestDTO dto) {
        if (dto == null) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "更新内容不能为空");
        }
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException(ResultCode.PROJECT_NOT_FOUND);
        }
        if (!isBoardsEnabledForProject(projectId)) {
            throw new BusinessException(ResultCode.BOARDS_NOT_ENABLED);
        }
        Board board = getBoardByProjectAndId(projectId, boardId);
        if (dto.getName() != null) {
            String name = dto.getName().trim();
            if (name.isEmpty()) {
                throw new BusinessException(ResultCode.PARAM_INVALID, "板块名称不能为空");
            }
            LambdaQueryWrapper<Board> sameName = new LambdaQueryWrapper<>();
            sameName.eq(Board::getProjectId, projectId.intValue())
                    .eq(Board::getName, name)
                    .ne(Board::getId, boardId);
            if (boardMapper.selectCount(sameName) > 0) {
                throw new BusinessException(ResultCode.BOARD_NAME_EXISTS);
            }
            board.setName(name);
        }
        if (dto.getDescription() != null) {
            board.setDescription(dto.getDescription());
        }
        if (dto.getPosition() != null) {
            board.setPosition(dto.getPosition());
        }
        boardMapper.updateById(board);
        log.info("论坛板块已更新: projectId={}, boardId={}", projectId, boardId);
        return toDetailResponse(board);
    }

    /**
     * 按项目与板块 ID 获取板块，不存在或不属于该项目则抛 BOARD_NOT_FOUND。
     */
    private Board getBoardByProjectAndId(Long projectId, Integer boardId) {
        Board board = boardMapper.selectById(boardId);
        if (board == null || !board.getProjectId().equals(projectId.intValue())) {
            throw new BusinessException(ResultCode.BOARD_NOT_FOUND);
        }
        return board;
    }

    private BoardListItemResponseDTO toListItemResponse(Board board) {
        return BoardListItemResponseDTO.builder()
                .id(board.getId())
                .projectId(board.getProjectId())
                .name(board.getName())
                .description(board.getDescription())
                .position(board.getPosition())
                .topicsCount(board.getTopicsCount() != null ? board.getTopicsCount() : 0)
                .messagesCount(board.getMessagesCount() != null ? board.getMessagesCount() : 0)
                .lastMessageId(board.getLastMessageId())
                .lastMessageSubject(null)
                .lastMessageUpdatedOn(null)
                .parentId(board.getParentId())
                .build();
    }

    private BoardDetailResponseDTO toDetailResponse(Board board) {
        return BoardDetailResponseDTO.builder()
                .id(board.getId())
                .projectId(board.getProjectId())
                .name(board.getName())
                .description(board.getDescription())
                .position(board.getPosition())
                .topicsCount(board.getTopicsCount() != null ? board.getTopicsCount() : 0)
                .messagesCount(board.getMessagesCount() != null ? board.getMessagesCount() : 0)
                .lastMessageId(board.getLastMessageId())
                .parentId(board.getParentId())
                .build();
    }
}
