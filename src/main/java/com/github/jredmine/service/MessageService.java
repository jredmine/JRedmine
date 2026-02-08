package com.github.jredmine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.jredmine.dto.request.board.TopicCreateRequestDTO;
import com.github.jredmine.dto.response.board.MessageDetailResponseDTO;
import com.github.jredmine.entity.Board;
import com.github.jredmine.entity.EnabledModule;
import com.github.jredmine.entity.Message;
import com.github.jredmine.entity.Project;
import com.github.jredmine.entity.User;
import com.github.jredmine.enums.ProjectModule;
import com.github.jredmine.enums.ResultCode;
import com.github.jredmine.exception.BusinessException;
import com.github.jredmine.mapper.BoardMapper;
import com.github.jredmine.mapper.MessageMapper;
import com.github.jredmine.mapper.project.EnabledModuleMapper;
import com.github.jredmine.mapper.project.ProjectMapper;
import com.github.jredmine.mapper.user.UserMapper;
import com.github.jredmine.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * 论坛消息/主题服务：发主题、回复、更新、删除等（本期实现发主题）
 *
 * @author panfeng
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageMapper messageMapper;
    private final BoardMapper boardMapper;
    private final EnabledModuleMapper enabledModuleMapper;
    private final ProjectMapper projectMapper;
    private final UserMapper userMapper;
    private final SecurityUtils securityUtils;

    /**
     * 发主题：在板块下新增一条 parent_id 为空的 message，并更新板块的 topics_count、messages_count、last_message_id。
     */
    @Transactional(rollbackFor = Exception.class)
    public MessageDetailResponseDTO createTopic(Long projectId, Integer boardId, TopicCreateRequestDTO dto) {
        if (dto == null || dto.getSubject() == null || dto.getSubject().isBlank()) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "主题标题不能为空");
        }
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException(ResultCode.PROJECT_NOT_FOUND);
        }
        if (!isBoardsEnabledForProject(projectId)) {
            throw new BusinessException(ResultCode.BOARDS_NOT_ENABLED);
        }
        Board board = getBoardByProjectAndId(projectId, boardId);
        Long currentUserId = securityUtils.getCurrentUserId();
        String subject = dto.getSubject().trim();
        String content = dto.getContent() != null ? dto.getContent() : "";
        Date now = new Date();
        Message message = new Message();
        message.setBoardId(boardId);
        message.setParentId(null);
        message.setSubject(subject);
        message.setContent(content);
        message.setAuthorId(currentUserId.intValue());
        message.setRepliesCount(0);
        message.setCreatedOn(now);
        message.setUpdatedOn(now);
        message.setLocked(false);
        message.setSticky(0);
        messageMapper.insert(message);
        board.setTopicsCount((board.getTopicsCount() != null ? board.getTopicsCount() : 0) + 1);
        board.setMessagesCount((board.getMessagesCount() != null ? board.getMessagesCount() : 0) + 1);
        board.setLastMessageId(message.getId());
        boardMapper.updateById(board);
        log.info("论坛主题已创建: projectId={}, boardId={}, messageId={}, subject={}", projectId, boardId, message.getId(), subject);
        return toMessageDetailResponse(message);
    }

    private boolean isBoardsEnabledForProject(Long projectId) {
        LambdaQueryWrapper<EnabledModule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EnabledModule::getProjectId, projectId)
                .eq(EnabledModule::getName, ProjectModule.BOARDS.getCode());
        return enabledModuleMapper.selectCount(wrapper) > 0;
    }

    private Board getBoardByProjectAndId(Long projectId, Integer boardId) {
        Board board = boardMapper.selectById(boardId);
        if (board == null || !board.getProjectId().equals(projectId.intValue())) {
            throw new BusinessException(ResultCode.BOARD_NOT_FOUND);
        }
        return board;
    }

    private String getAuthorDisplayName(Integer authorId) {
        if (authorId == null) return null;
        User user = userMapper.selectById(authorId.longValue());
        if (user == null) return null;
        String name = ((user.getFirstname() != null ? user.getFirstname() : "") + " "
                + (user.getLastname() != null ? user.getLastname() : "")).trim();
        return name.isEmpty() ? user.getLogin() : name;
    }

    private MessageDetailResponseDTO toMessageDetailResponse(Message message) {
        return MessageDetailResponseDTO.builder()
                .id(message.getId())
                .boardId(message.getBoardId())
                .parentId(message.getParentId())
                .subject(message.getSubject())
                .content(message.getContent())
                .authorId(message.getAuthorId())
                .authorName(getAuthorDisplayName(message.getAuthorId()))
                .repliesCount(message.getRepliesCount() != null ? message.getRepliesCount() : 0)
                .createdOn(message.getCreatedOn())
                .updatedOn(message.getUpdatedOn())
                .locked(message.getLocked())
                .sticky(message.getSticky())
                .build();
    }
}
