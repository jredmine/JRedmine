package com.github.jredmine.controller;

import com.github.jredmine.dto.request.board.BoardCreateRequestDTO;
import com.github.jredmine.dto.request.board.BoardUpdateRequestDTO;
import com.github.jredmine.dto.request.board.TopicCreateRequestDTO;
import com.github.jredmine.dto.response.ApiResponse;
import com.github.jredmine.dto.response.board.BoardDetailResponseDTO;
import com.github.jredmine.dto.response.board.BoardListItemResponseDTO;
import com.github.jredmine.dto.response.board.MessageDetailResponseDTO;
import com.github.jredmine.service.BoardService;
import com.github.jredmine.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 论坛板块控制器
 *
 * @author panfeng
 */
@Tag(name = "论坛板块", description = "项目论坛板块的创建、列表、详情、更新、删除")
@RestController
@RequestMapping("/api/projects/{projectId}/boards")
public class BoardController {

    private final BoardService boardService;
    private final MessageService messageService;

    public BoardController(BoardService boardService, MessageService messageService) {
        this.boardService = boardService;
        this.messageService = messageService;
    }

    @Operation(summary = "板块列表", description = "按项目查询所有板块（含主题数、消息总数、最后消息 ID），按 position、name 排序。需项目已启用论坛模块。需要 view_messages 权限或系统管理员。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN') or hasPermission(#projectId, 'Project', 'view_messages')")
    @GetMapping
    public ApiResponse<List<BoardListItemResponseDTO>> list(@PathVariable Long projectId) {
        List<BoardListItemResponseDTO> list = boardService.listBoards(projectId);
        return ApiResponse.success(list);
    }

    @Operation(summary = "板块详情", description = "根据板块 ID 获取板块详情（含主题数、消息总数、最后消息 ID 等统计）。需项目已启用论坛模块。需要 view_messages 权限或系统管理员。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN') or hasPermission(#projectId, 'Project', 'view_messages')")
    @GetMapping("/{boardId}")
    public ApiResponse<BoardDetailResponseDTO> getDetail(
            @PathVariable Long projectId,
            @PathVariable Integer boardId) {
        BoardDetailResponseDTO result = boardService.getDetail(projectId, boardId);
        return ApiResponse.success(result);
    }

    @Operation(summary = "发主题", description = "在板块下发布新主题（subject 必填，content 可选）。需项目已启用论坛模块。需要 add_messages 权限或系统管理员。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN') or hasPermission(#projectId, 'Project', 'add_messages')")
    @PostMapping("/{boardId}/topics")
    public ApiResponse<MessageDetailResponseDTO> createTopic(
            @PathVariable Long projectId,
            @PathVariable Integer boardId,
            @Valid @RequestBody TopicCreateRequestDTO request) {
        MessageDetailResponseDTO result = messageService.createTopic(projectId, boardId, request);
        return ApiResponse.success("主题发布成功", result);
    }

    @Operation(summary = "创建板块", description = "在项目下新增一个论坛板块（name 必填，description、position、parentId 可选）。需项目已启用论坛模块；同项目下板块名称不可重复。需要 manage_boards 权限或系统管理员。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN') or hasPermission(#projectId, 'Project', 'manage_boards')")
    @PostMapping
    public ApiResponse<BoardDetailResponseDTO> create(
            @PathVariable Long projectId,
            @Valid @RequestBody BoardCreateRequestDTO request) {
        BoardDetailResponseDTO result = boardService.create(projectId, request);
        return ApiResponse.success("板块创建成功", result);
    }

    @Operation(summary = "更新板块", description = "更新板块的 name、description、position；仅更新请求体中提供的非空字段。同项目下板块名称不可重复。需要 manage_boards 权限或系统管理员。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN') or hasPermission(#projectId, 'Project', 'manage_boards')")
    @PutMapping("/{boardId}")
    public ApiResponse<BoardDetailResponseDTO> update(
            @PathVariable Long projectId,
            @PathVariable Integer boardId,
            @RequestBody(required = false) BoardUpdateRequestDTO request) {
        BoardDetailResponseDTO result = boardService.update(projectId, boardId, request);
        return ApiResponse.success("板块更新成功", result);
    }

    @Operation(summary = "删除板块", description = "仅允许删除空板块（板块下无消息时可删除）；若板块下仍有消息则不允许删除。需要 manage_boards 权限或系统管理员。", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN') or hasPermission(#projectId, 'Project', 'manage_boards')")
    @DeleteMapping("/{boardId}")
    public ApiResponse<Void> delete(
            @PathVariable Long projectId,
            @PathVariable Integer boardId) {
        boardService.delete(projectId, boardId);
        return ApiResponse.success();
    }
}
