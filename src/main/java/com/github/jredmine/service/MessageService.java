package com.github.jredmine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.jredmine.dto.request.activity.CommentUpdateRequestDTO;
import com.github.jredmine.dto.request.board.MessageCommentCreateRequestDTO;
import com.github.jredmine.dto.request.board.MessageUpdateRequestDTO;
import com.github.jredmine.dto.request.board.ReplyCreateRequestDTO;
import com.github.jredmine.dto.request.board.TopicCreateRequestDTO;
import com.github.jredmine.dto.response.PageResponse;
import com.github.jredmine.dto.response.activity.CommentResponseDTO;
import com.github.jredmine.dto.response.board.MessageDetailResponseDTO;
import com.github.jredmine.dto.response.board.MessageReplyListItemResponseDTO;
import com.github.jredmine.dto.response.board.MessageTopicDetailResponseDTO;
import com.github.jredmine.dto.response.board.MessageTopicListItemResponseDTO;
import com.github.jredmine.entity.Board;
import com.github.jredmine.entity.Comment;
import com.github.jredmine.entity.EnabledModule;
import com.github.jredmine.entity.Message;
import com.github.jredmine.entity.Project;
import com.github.jredmine.entity.User;
import com.github.jredmine.enums.ProjectModule;
import com.github.jredmine.enums.ResultCode;
import com.github.jredmine.exception.BusinessException;
import com.github.jredmine.mapper.BoardMapper;
import com.github.jredmine.mapper.CommentMapper;
import com.github.jredmine.mapper.MessageMapper;
import com.github.jredmine.security.ProjectPermissionService;
import com.github.jredmine.mapper.project.EnabledModuleMapper;
import com.github.jredmine.mapper.project.ProjectMapper;
import com.github.jredmine.mapper.user.UserMapper;
import com.github.jredmine.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 论坛消息/主题服务：发主题、回复、更新、删除等
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
    private final ProjectPermissionService projectPermissionService;
    private final CommentMapper commentMapper;

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

    /**
     * 回复主题：在主题下新增一条 parent_id=主题id 的 message，并更新主题的 replies_count、last_reply_id 及板块的 messages_count、last_message_id。
     * 若主题已锁定则禁止回复。
     */
    @Transactional(rollbackFor = Exception.class)
    public MessageDetailResponseDTO createReply(Long projectId, Integer boardId, Integer topicMessageId, ReplyCreateRequestDTO dto) {
        if (dto == null || dto.getContent() == null || dto.getContent().isBlank()) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "回复内容不能为空");
        }
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException(ResultCode.PROJECT_NOT_FOUND);
        }
        if (!isBoardsEnabledForProject(projectId)) {
            throw new BusinessException(ResultCode.BOARDS_NOT_ENABLED);
        }
        Board board = getBoardByProjectAndId(projectId, boardId);
        Message topic = getTopicMessageOrThrow(boardId, topicMessageId);
        if (Boolean.TRUE.equals(topic.getLocked())) {
            throw new BusinessException(ResultCode.MESSAGE_TOPIC_LOCKED);
        }
        Long currentUserId = securityUtils.getCurrentUserId();
        String content = dto.getContent().trim();
        Date now = new Date();
        Message reply = new Message();
        reply.setBoardId(boardId);
        reply.setParentId(topicMessageId);
        reply.setSubject(topic.getSubject() != null ? topic.getSubject() : "");
        reply.setContent(content);
        reply.setAuthorId(currentUserId.intValue());
        reply.setRepliesCount(0);
        reply.setCreatedOn(now);
        reply.setUpdatedOn(now);
        reply.setLocked(false);
        reply.setSticky(0);
        messageMapper.insert(reply);
        topic.setRepliesCount((topic.getRepliesCount() != null ? topic.getRepliesCount() : 0) + 1);
        topic.setLastReplyId(reply.getId());
        topic.setUpdatedOn(now);
        messageMapper.updateById(topic);
        board.setMessagesCount((board.getMessagesCount() != null ? board.getMessagesCount() : 0) + 1);
        board.setLastMessageId(reply.getId());
        boardMapper.updateById(board);
        log.info("论坛回复已创建: projectId={}, boardId={}, topicId={}, replyId={}", projectId, boardId, topicMessageId, reply.getId());
        return toMessageDetailResponse(reply);
    }

    /**
     * 主题分页列表：sticky 优先、updated_on 倒序；keyword 可选（模糊匹配 subject、content）。
     * 要求项目存在、已启用论坛模块、板块存在且属于项目。
     */
    public PageResponse<MessageTopicListItemResponseDTO> listTopics(Long projectId, Integer boardId,
            Integer current, Integer size, String keyword) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException(ResultCode.PROJECT_NOT_FOUND);
        }
        if (!isBoardsEnabledForProject(projectId)) {
            throw new BusinessException(ResultCode.BOARDS_NOT_ENABLED);
        }
        getBoardByProjectAndId(projectId, boardId);
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getBoardId, boardId)
                .isNull(Message::getParentId);
        if (keyword != null && !keyword.isBlank()) {
            String k = keyword.trim();
            wrapper.and(w -> w.like(Message::getSubject, k).or().like(Message::getContent, k));
        }
        wrapper.orderByDesc(Message::getSticky)
                .orderByDesc(Message::getUpdatedOn);
        int pageNum = (current != null && current > 0) ? current : 1;
        int pageSize = (size != null && size > 0) ? size : 20;
        Page<Message> page = new Page<>(pageNum, pageSize);
        IPage<Message> result = messageMapper.selectPage(page, wrapper);
        List<MessageTopicListItemResponseDTO> list = new ArrayList<>();
        for (Message msg : result.getRecords()) {
            list.add(toTopicListItemResponse(msg));
        }
        return PageResponse.of(list, (int) result.getTotal(), (int) result.getCurrent(), (int) result.getSize());
    }

    /**
     * 主题详情：返回主题信息及该主题下的回复分页列表（回复按创建时间正序）。
     * 要求项目存在、已启用论坛模块、板块存在且属于项目、消息为当前板块下的主题。
     */
    public MessageTopicDetailResponseDTO getTopicDetail(Long projectId, Integer boardId, Integer messageId,
            Integer current, Integer size) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException(ResultCode.PROJECT_NOT_FOUND);
        }
        if (!isBoardsEnabledForProject(projectId)) {
            throw new BusinessException(ResultCode.BOARDS_NOT_ENABLED);
        }
        getBoardByProjectAndId(projectId, boardId);
        Message topic = getTopicMessageOrThrow(boardId, messageId);
        MessageDetailResponseDTO topicDto = toMessageDetailResponse(topic);
        int pageNum = (current != null && current > 0) ? current : 1;
        int pageSize = (size != null && size > 0) ? size : 20;
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getBoardId, boardId)
                .eq(Message::getParentId, messageId)
                .orderByAsc(Message::getId);
        Page<Message> page = new Page<>(pageNum, pageSize);
        IPage<Message> replyPage = messageMapper.selectPage(page, wrapper);
        List<MessageReplyListItemResponseDTO> replyList = new ArrayList<>();
        for (Message msg : replyPage.getRecords()) {
            replyList.add(toReplyListItemResponse(msg));
        }
        PageResponse<MessageReplyListItemResponseDTO> replies = PageResponse.of(
                replyList,
                (int) replyPage.getTotal(),
                (int) replyPage.getCurrent(),
                (int) replyPage.getSize());
        return MessageTopicDetailResponseDTO.builder()
                .topic(topicDto)
                .replies(replies)
                .build();
    }

    /**
     * 更新消息：仅更新请求体中提供的非空字段。
     * 权限：当前用户为消息作者或拥有 manage_boards；否则 403。
     * subject、locked、sticky 仅对主题帖（parent_id 为空）有效，回复只更新 content。
     */
    @Transactional(rollbackFor = Exception.class)
    public MessageDetailResponseDTO updateMessage(Long projectId, Integer boardId, Integer messageId, MessageUpdateRequestDTO dto) {
        if (dto == null) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "请求体不能为空");
        }
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException(ResultCode.PROJECT_NOT_FOUND);
        }
        if (!isBoardsEnabledForProject(projectId)) {
            throw new BusinessException(ResultCode.BOARDS_NOT_ENABLED);
        }
        getBoardByProjectAndId(projectId, boardId);
        Message message = getMessageByBoardAndIdOrThrow(boardId, messageId);
        Long currentUserId = securityUtils.getCurrentUserId();
        boolean isAuthor = message.getAuthorId() != null && message.getAuthorId().equals(currentUserId.intValue());
        boolean hasManageBoards = projectPermissionService.hasPermission(currentUserId, projectId, "manage_boards");
        if (!isAuthor && !hasManageBoards) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权限编辑该消息");
        }
        boolean isTopic = message.getParentId() == null;
        boolean subjectChanged = false;
        String newSubject = null;
        boolean changed = false;
        if (dto.getSubject() != null && isTopic) {
            newSubject = dto.getSubject().trim();
            message.setSubject(newSubject);
            subjectChanged = true;
            changed = true;
        }
        if (dto.getContent() != null) {
            message.setContent(dto.getContent());
            changed = true;
        }
        if (dto.getLocked() != null && isTopic) {
            message.setLocked(dto.getLocked());
            changed = true;
        }
        if (dto.getSticky() != null && isTopic) {
            message.setSticky(dto.getSticky());
            changed = true;
        }
        if (changed) {
            Date now = new Date();
            message.setUpdatedOn(now);
            messageMapper.updateById(message);
            // 主题标题变更时，级联更新该主题下所有回复的 subject，保持与主题一致
            if (subjectChanged && newSubject != null) {
                Message replyUpdate = new Message();
                replyUpdate.setSubject(newSubject);
                replyUpdate.setUpdatedOn(now);
                LambdaUpdateWrapper<Message> wrapper = new LambdaUpdateWrapper<>();
                wrapper.eq(Message::getParentId, messageId);
                messageMapper.update(replyUpdate, wrapper);
            }
        }
        Message updated = messageMapper.selectById(messageId);
        log.info("论坛消息已更新: projectId={}, boardId={}, messageId={}, subjectCascaded={}", projectId, boardId, messageId, subjectChanged);
        return toMessageDetailResponse(updated);
    }

    /**
     * 删除消息：主题会级联删除其下所有回复；回复仅删本条。
     * 权限：当前用户为消息作者或拥有 manage_boards；否则 403。
     * 同时维护板块的 topics_count、messages_count、last_message_id 及主题的 replies_count、last_reply_id。
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteMessage(Long projectId, Integer boardId, Integer messageId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException(ResultCode.PROJECT_NOT_FOUND);
        }
        if (!isBoardsEnabledForProject(projectId)) {
            throw new BusinessException(ResultCode.BOARDS_NOT_ENABLED);
        }
        Board board = getBoardByProjectAndId(projectId, boardId);
        Message message = getMessageByBoardAndIdOrThrow(boardId, messageId);
        Long currentUserId = securityUtils.getCurrentUserId();
        boolean isAuthor = message.getAuthorId() != null && message.getAuthorId().equals(currentUserId.intValue());
        boolean hasManageBoards = projectPermissionService.hasPermission(currentUserId, projectId, "manage_boards");
        if (!isAuthor && !hasManageBoards) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权限删除该消息");
        }
        boolean isTopic = message.getParentId() == null;
        if (isTopic) {
            // 主题：先删该主题下所有回复及其评论，再删主题及其评论
            LambdaQueryWrapper<Message> replyWrapper = new LambdaQueryWrapper<>();
            replyWrapper.eq(Message::getBoardId, boardId).eq(Message::getParentId, messageId);
            List<Message> replies = messageMapper.selectList(replyWrapper);
            int replyCount = replies.size();
            for (Message reply : replies) {
                deleteCommentsForMessage(reply.getId());
            }
            messageMapper.delete(replyWrapper);
            deleteCommentsForMessage(messageId);
            messageMapper.deleteById(messageId);
            board.setTopicsCount(Math.max(0, (board.getTopicsCount() != null ? board.getTopicsCount() : 0) - 1));
            board.setMessagesCount(Math.max(0, (board.getMessagesCount() != null ? board.getMessagesCount() : 0) - 1 - replyCount));
        } else {
            // 回复：删本条及其评论，并更新主题的 replies_count、last_reply_id
            deleteCommentsForMessage(messageId);
            Integer topicId = message.getParentId();
            Message topic = messageMapper.selectById(topicId);
            messageMapper.deleteById(messageId);
            if (topic != null) {
                int newRepliesCount = Math.max(0, (topic.getRepliesCount() != null ? topic.getRepliesCount() : 0) - 1);
                topic.setRepliesCount(newRepliesCount);
                if (messageId.equals(topic.getLastReplyId())) {
                    LambdaQueryWrapper<Message> prevReply = new LambdaQueryWrapper<>();
                    prevReply.eq(Message::getParentId, topicId).lt(Message::getId, messageId).orderByDesc(Message::getId).last("LIMIT 1");
                    Message prev = messageMapper.selectOne(prevReply);
                    topic.setLastReplyId(prev != null ? prev.getId() : null);
                }
                topic.setUpdatedOn(new Date());
                messageMapper.updateById(topic);
            }
            board.setMessagesCount(Math.max(0, (board.getMessagesCount() != null ? board.getMessagesCount() : 0) - 1));
        }
        // 若被删的是板块的最后一条消息，则重算 last_message_id
        if (messageId.equals(board.getLastMessageId())) {
            LambdaQueryWrapper<Message> lastWrapper = new LambdaQueryWrapper<>();
            lastWrapper.eq(Message::getBoardId, boardId).orderByDesc(Message::getId).last("LIMIT 1");
            Message last = messageMapper.selectOne(lastWrapper);
            board.setLastMessageId(last != null ? last.getId() : null);
        }
        boardMapper.updateById(board);
        log.info("论坛消息已删除: projectId={}, boardId={}, messageId={}, isTopic={}", projectId, boardId, messageId, isTopic);
    }

    /**
     * 消息评论分页列表（comments 表）。要求项目存在、已启用论坛、板块与消息有效。
     */
    public PageResponse<CommentResponseDTO> listMessageComments(Long projectId, Integer boardId, Integer messageId,
            Integer current, Integer size) {
        validateProjectBoardMessage(projectId, boardId, messageId);
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getCommentedType, "Message")
                .eq(Comment::getCommentedId, messageId)
                .orderByDesc(Comment::getId);
        int pageNum = (current != null && current > 0) ? current : 1;
        int pageSize = (size != null && size > 0) ? size : 20;
        Page<Comment> page = new Page<>(pageNum, pageSize);
        IPage<Comment> result = commentMapper.selectPage(page, wrapper);
        List<CommentResponseDTO> list = new ArrayList<>();
        for (Comment c : result.getRecords()) {
            list.add(toCommentResponseDTO(c));
        }
        return PageResponse.of(list, (int) result.getTotal(), (int) result.getCurrent(), (int) result.getSize());
    }

    /**
     * 为消息添加评论（写入 comments 表）。要求项目存在、已启用论坛、板块与消息有效。
     */
    public CommentResponseDTO addMessageComment(Long projectId, Integer boardId, Integer messageId,
            MessageCommentCreateRequestDTO dto) {
        if (dto == null || dto.getNotes() == null || dto.getNotes().isBlank()) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "评论内容不能为空");
        }
        validateProjectBoardMessage(projectId, boardId, messageId);
        Long currentUserId = securityUtils.getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();
        Comment comment = new Comment();
        comment.setCommentedType("Message");
        comment.setCommentedId(messageId);
        comment.setAuthorId(currentUserId.intValue());
        comment.setContent(dto.getNotes().trim());
        comment.setCreatedOn(now);
        comment.setUpdatedOn(now);
        commentMapper.insert(comment);
        log.info("消息评论已添加: projectId={}, boardId={}, messageId={}, commentId={}", projectId, boardId, messageId, comment.getId());
        return toCommentResponseDTO(comment);
    }

    /**
     * 更新消息下的某条评论。仅评论作者或拥有 manage_boards 可更新。
     */
    public CommentResponseDTO updateMessageComment(Long projectId, Integer boardId, Integer messageId, Long commentId,
            CommentUpdateRequestDTO dto) {
        if (dto == null || dto.getNotes() == null || dto.getNotes().isBlank()) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "评论内容不能为空");
        }
        validateProjectBoardMessage(projectId, boardId, messageId);
        Comment comment = ensureCommentBelongsToMessage(commentId, messageId);
        Long currentUserId = securityUtils.getCurrentUserId();
        boolean isAuthor = comment.getAuthorId() != null && comment.getAuthorId().equals(currentUserId.intValue());
        boolean hasManage = projectPermissionService.hasPermission(currentUserId, projectId, "manage_boards");
        if (!isAuthor && !hasManage) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权限编辑该评论");
        }
        comment.setContent(dto.getNotes().trim());
        comment.setUpdatedOn(LocalDateTime.now());
        commentMapper.updateById(comment);
        return toCommentResponseDTO(commentMapper.selectById(comment.getId()));
    }

    /**
     * 删除消息下的某条评论。仅评论作者或拥有 manage_boards 可删除。
     */
    public void deleteMessageComment(Long projectId, Integer boardId, Integer messageId, Long commentId) {
        validateProjectBoardMessage(projectId, boardId, messageId);
        Comment comment = ensureCommentBelongsToMessage(commentId, messageId);
        Long currentUserId = securityUtils.getCurrentUserId();
        boolean isAuthor = comment.getAuthorId() != null && comment.getAuthorId().equals(currentUserId.intValue());
        boolean hasManage = projectPermissionService.hasPermission(currentUserId, projectId, "manage_boards");
        if (!isAuthor && !hasManage) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权限删除该评论");
        }
        commentMapper.deleteById(comment.getId());
        log.info("消息评论已删除: projectId={}, messageId={}, commentId={}", projectId, messageId, commentId);
    }

    private void validateProjectBoardMessage(Long projectId, Integer boardId, Integer messageId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException(ResultCode.PROJECT_NOT_FOUND);
        }
        if (!isBoardsEnabledForProject(projectId)) {
            throw new BusinessException(ResultCode.BOARDS_NOT_ENABLED);
        }
        getBoardByProjectAndId(projectId, boardId);
        getMessageByBoardAndIdOrThrow(boardId, messageId);
    }

    /** 校验评论属于该消息，否则抛 COMMENT_NOT_FOUND；通过时返回 Comment。 */
    private Comment ensureCommentBelongsToMessage(Long commentId, Integer messageId) {
        Comment comment = (commentId != null) ? commentMapper.selectById(commentId.intValue()) : null;
        if (comment == null || !"Message".equals(comment.getCommentedType())
                || comment.getCommentedId() == null || !comment.getCommentedId().equals(messageId)) {
            throw new BusinessException(ResultCode.COMMENT_NOT_FOUND);
        }
        return comment;
    }

    /** 删除某条消息下的所有评论（comments 表） */
    private void deleteCommentsForMessage(Integer messageId) {
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getCommentedType, "Message").eq(Comment::getCommentedId, messageId);
        commentMapper.delete(wrapper);
    }

    private CommentResponseDTO toCommentResponseDTO(Comment comment) {
        if (comment == null) return null;
        User author = comment.getAuthorId() != null ? userMapper.selectById(comment.getAuthorId().longValue()) : null;
        return CommentResponseDTO.builder()
                .id(comment.getId().longValue())
                .objectType(comment.getCommentedType())
                .objectId(comment.getCommentedId() != null ? comment.getCommentedId().longValue() : null)
                .notes(comment.getContent())
                .isPrivate(false)
                .userId(comment.getAuthorId() != null ? comment.getAuthorId().longValue() : null)
                .userName(getAuthorDisplayName(comment.getAuthorId()))
                .userLogin(author != null ? author.getLogin() : null)
                .createdOn(comment.getCreatedOn())
                .updatedOn(comment.getUpdatedOn())
                .updatedById(null)
                .updatedByName(null)
                .build();
    }

    /**
     * 根据板块 ID 和消息 ID 获取消息，不属于该板块则抛 MESSAGE_NOT_FOUND。
     */
    private Message getMessageByBoardAndIdOrThrow(Integer boardId, Integer messageId) {
        Message message = messageMapper.selectById(messageId);
        if (message == null || !message.getBoardId().equals(boardId)) {
            throw new BusinessException(ResultCode.MESSAGE_NOT_FOUND);
        }
        return message;
    }

    /**
     * 获取主题消息（parent_id 为空且属于该板块），否则抛 MESSAGE_NOT_FOUND。
     */
    private Message getTopicMessageOrThrow(Integer boardId, Integer messageId) {
        Message message = messageMapper.selectById(messageId);
        if (message == null || !message.getBoardId().equals(boardId) || message.getParentId() != null) {
            throw new BusinessException(ResultCode.MESSAGE_NOT_FOUND);
        }
        return message;
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

    private MessageTopicListItemResponseDTO toTopicListItemResponse(Message message) {
        return MessageTopicListItemResponseDTO.builder()
                .id(message.getId())
                .boardId(message.getBoardId())
                .subject(message.getSubject())
                .authorId(message.getAuthorId())
                .authorName(getAuthorDisplayName(message.getAuthorId()))
                .repliesCount(message.getRepliesCount() != null ? message.getRepliesCount() : 0)
                .createdOn(message.getCreatedOn())
                .updatedOn(message.getUpdatedOn())
                .locked(message.getLocked())
                .sticky(message.getSticky())
                .build();
    }

    private MessageReplyListItemResponseDTO toReplyListItemResponse(Message message) {
        return MessageReplyListItemResponseDTO.builder()
                .id(message.getId())
                .parentId(message.getParentId())
                .content(message.getContent())
                .authorId(message.getAuthorId())
                .authorName(getAuthorDisplayName(message.getAuthorId()))
                .createdOn(message.getCreatedOn())
                .updatedOn(message.getUpdatedOn())
                .build();
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
