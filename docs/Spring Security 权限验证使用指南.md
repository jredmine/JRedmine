# Spring Security 权限验证使用指南

## 概述

本项目已集成 Spring Security 权限验证系统，支持基于角色的访问控制（RBAC）和项目级别的权限管理。

## 权限体系

### 1. 用户角色

- **系统管理员** (`admin = true`): 拥有所有权限，不受项目限制
- **普通用户**: 通过项目成员身份获得权限

### 2. 项目级权限

权限是项目级别的，通过以下关系链获得：
```
用户 (users) 
  → 项目成员 (members) 
    → 成员角色 (member_roles) 
      → 角色 (roles) 
        → 权限列表 (permissions)
```

### 3. 权限枚举

所有可用权限定义在 `Permission` 枚举中，包括：
- 项目管理权限：`view_projects`, `create_projects`, `edit_projects`, `delete_projects`, `manage_projects`
- 任务管理权限：`view_issues`, `add_issues`, `edit_issues`, `delete_issues`, `manage_issues`
- 其他模块权限...

## 使用方法

### 1. 方法级权限验证（推荐）

使用 `@PreAuthorize` 注解在 Controller 方法上添加权限检查：

```java
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    /**
     * 创建项目 - 需要 create_projects 权限
     */
    @PreAuthorize("hasPermission(#projectId, 'Project', 'create_projects')")
    @PostMapping
    public ApiResponse<ProjectResponseDTO> createProject(@RequestBody ProjectCreateRequestDTO request) {
        // ...
    }

    /**
     * 更新项目 - 需要 edit_projects 权限
     */
    @PreAuthorize("hasPermission(#id, 'Project', 'edit_projects')")
    @PutMapping("/{id}")
    public ApiResponse<ProjectResponseDTO> updateProject(
            @PathVariable Long id, 
            @RequestBody ProjectUpdateRequestDTO request) {
        // ...
    }

    /**
     * 删除项目 - 需要 delete_projects 权限
     */
    @PreAuthorize("hasPermission(#id, 'Project', 'delete_projects')")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteProject(@PathVariable Long id) {
        // ...
    }

    /**
     * 管理项目成员 - 需要 manage_projects 权限
     */
    @PreAuthorize("hasPermission(#projectId, 'Project', 'manage_projects')")
    @PostMapping("/{projectId}/members")
    public ApiResponse<ProjectMemberResponseDTO> addMember(
            @PathVariable Long projectId,
            @RequestBody ProjectMemberCreateRequestDTO request) {
        // ...
    }
}
```

### 2. 管理员权限检查

```java
/**
 * 仅管理员可访问
 */
@PreAuthorize("hasRole('ADMIN')")
@PostMapping("/admin-only")
public ApiResponse<Void> adminOnlyOperation() {
    // ...
}
```

### 3. 在 Service 层进行权限检查

如果需要在 Service 层进行权限检查，可以使用 `SecurityUtils`：

```java
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final SecurityUtils securityUtils;
    private final PermissionService permissionService;

    public void updateProject(Long projectId, ProjectUpdateRequestDTO request) {
        // 检查用户是否是管理员
        if (securityUtils.isAdmin()) {
            // 管理员可以更新任何项目
        } else {
            // 检查用户是否有 edit_projects 权限
            Long userId = securityUtils.getCurrentUserId();
            if (!permissionService.hasPermission(userId, projectId, "edit_projects")) {
                throw new BusinessException(ResultCode.FORBIDDEN, "无权限编辑项目");
            }
        }
        // ...
    }
}
```

### 4. 获取当前用户信息

```java
@Service
@RequiredArgsConstructor
public class SomeService {

    private final SecurityUtils securityUtils;

    public void someMethod() {
        // 获取当前用户ID
        Long userId = securityUtils.getCurrentUserId();
        
        // 获取当前用户信息
        User user = securityUtils.getCurrentUser();
        
        // 获取当前用户主体（包含权限信息）
        UserPrincipal userPrincipal = securityUtils.getCurrentUserPrincipal();
        
        // 检查是否是管理员
        boolean isAdmin = securityUtils.isAdmin();
        
        // 检查用户是否拥有权限（项目级别）
        if (userPrincipal.hasPermission("view_issues")) {
            // 注意：这个方法检查的是全局权限（所有项目的权限并集）
            // 项目级别的权限检查应该使用 PermissionService.hasPermission()
        }
    }
}
```

## 权限表达式说明

### hasPermission 表达式

```java
@PreAuthorize("hasPermission(#projectId, 'Project', 'edit_projects')")
```

- `#projectId`: 方法参数中的项目ID
- `'Project'`: 目标类型（固定为 'Project'）
- `'edit_projects'`: 权限键

### hasRole 表达式

```java
@PreAuthorize("hasRole('ADMIN')")
```

- `'ADMIN'`: 角色名称（系统管理员角色）

### 组合表达式

```java
@PreAuthorize("hasRole('ADMIN') or hasPermission(#projectId, 'Project', 'manage_projects')")
```

## 权限验证流程

1. **JWT 认证**: `JwtAuthenticationFilter` 从 JWT Token 中提取用户信息
2. **加载权限**: `CustomUserDetailsService` 从数据库加载用户权限
3. **设置认证**: 将 `UserPrincipal` 设置到 `SecurityContext`
4. **权限检查**: `@PreAuthorize` 注解触发 `ProjectPermissionEvaluator` 进行权限评估
5. **返回结果**: 有权限则继续执行，无权限则返回 403 Forbidden

## 注意事项

1. **项目级权限**: 权限是项目级别的，同一个用户在不同项目中可能有不同的权限
2. **管理员权限**: 系统管理员 (`admin = true`) 拥有所有权限，不受项目限制
3. **权限缓存**: 当前实现每次请求都会从数据库加载权限，如需优化可考虑添加缓存
4. **权限字符串格式**: 角色表中的 `permissions` 字段支持 JSON 数组格式和序列化字符串格式

## 示例：完整的 Controller 方法

```java
@RestController
@RequestMapping("/api/projects/{projectId}/issues")
@Tag(name = "任务管理", description = "项目任务管理接口")
public class IssueController {

    private final IssueService issueService;

    /**
     * 创建任务 - 需要 add_issues 权限
     */
    @Operation(summary = "创建任务", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasPermission(#projectId, 'Project', 'add_issues')")
    @PostMapping
    public ApiResponse<IssueResponseDTO> createIssue(
            @PathVariable Long projectId,
            @Valid @RequestBody IssueCreateRequestDTO request) {
        IssueResponseDTO response = issueService.createIssue(projectId, request);
        return ApiResponse.success("任务创建成功", response);
    }

    /**
     * 更新任务 - 需要 edit_issues 权限
     */
    @Operation(summary = "更新任务", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasPermission(#projectId, 'Project', 'edit_issues')")
    @PutMapping("/{issueId}")
    public ApiResponse<IssueResponseDTO> updateIssue(
            @PathVariable Long projectId,
            @PathVariable Long issueId,
            @Valid @RequestBody IssueUpdateRequestDTO request) {
        IssueResponseDTO response = issueService.updateIssue(projectId, issueId, request);
        return ApiResponse.success("任务更新成功", response);
    }

    /**
     * 删除任务 - 需要 delete_issues 权限
     */
    @Operation(summary = "删除任务", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasPermission(#projectId, 'Project', 'delete_issues')")
    @DeleteMapping("/{issueId}")
    public ApiResponse<Void> deleteIssue(
            @PathVariable Long projectId,
            @PathVariable Long issueId) {
        issueService.deleteIssue(projectId, issueId);
        return ApiResponse.success("任务删除成功");
    }
}
```

## 错误响应

### 401 Unauthorized
当用户未认证时返回：
```json
{
  "code": 401,
  "message": "未认证，请先登录",
  "data": null
}
```

### 403 Forbidden
当用户已认证但无权限时返回：
```json
{
  "code": 403,
  "message": "无权限，访问被拒绝",
  "data": null
}
```

