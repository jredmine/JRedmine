# Spring Security 权限验证系统详解

## 📋 目录

1. [系统架构概述](#系统架构概述)
2. [权限验证流程](#权限验证流程)
3. [核心组件详解](#核心组件详解)
4. [文件结构说明](#文件结构说明)
5. [权限验证表达式](#权限验证表达式)
6. [使用示例](#使用示例)
7. [常见问题](#常见问题)

---

## 系统架构概述

### 权限体系设计

本项目采用**基于角色的访问控制（RBAC）**和**项目级权限管理**相结合的方式：

```
用户 (users)
  ↓
  ├─ 系统管理员 (admin = true) → 拥有所有权限
  └─ 普通用户
      ↓
      项目成员 (members: user_id + project_id)
        ↓
        成员角色 (member_roles: member_id + role_id)
          ↓
          角色 (roles: 包含权限列表)
            ↓
            权限 (permissions: 预定义枚举值)
```

### 权限类型

1. **系统级权限**：管理员权限（`ROLE_ADMIN`）
2. **全局权限**：如 `create_projects`（用户在任何项目中拥有即可）
3. **项目级权限**：如 `edit_projects`、`manage_projects`（必须在特定项目中拥有）

---

## 权限验证流程

### 完整请求流程

```
1. HTTP 请求到达
   ↓
2. JwtAuthenticationFilter (JWT 认证过滤器)
   ├─ 提取 JWT Token
   ├─ 验证 Token 有效性
   ├─ 调用 CustomUserDetailsService 加载用户信息
   ├─ 调用 PermissionService 加载用户权限
   ├─ 创建 UserPrincipal 对象
   └─ 设置到 SecurityContext
   ↓
3. SecurityFilterChain (安全过滤器链)
   ├─ 检查是否需要认证
   ├─ 检查是否需要权限
   └─ 路由到对应的 Controller
   ↓
4. @PreAuthorize 注解拦截 (方法级权限验证)
   ├─ 解析权限表达式
   ├─ 调用 ProjectPermissionEvaluator
   ├─ 检查用户权限
   └─ 允许/拒绝访问
   ↓
5. Controller 方法执行
   ↓
6. 返回响应
```

### 详细流程图

```
┌─────────────────────────────────────────────────────────────┐
│                    HTTP 请求到达                              │
│              Authorization: Bearer <JWT_TOKEN>                │
└───────────────────────────┬───────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│          JwtAuthenticationFilter.doFilterInternal()          │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ 1. 提取 Authorization Header                          │  │
│  │ 2. 验证 JWT Token 有效性                              │  │
│  │ 3. 提取 username 和 userId                           │  │
│  └──────────────────────────────────────────────────────┘  │
└───────────────────────────┬───────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│        CustomUserDetailsService.loadUserByUsername()         │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ 1. 从数据库查询用户信息 (User)                        │  │
│  │ 2. 判断是否是管理员                                    │  │
│  │ 3. 调用 PermissionService.getUserAllPermissions()     │  │
│  │ 4. 创建 UserPrincipal 对象                            │  │
│  └──────────────────────────────────────────────────────┘  │
└───────────────────────────┬───────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│         PermissionService.getUserAllPermissions()            │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ 1. 查询用户的所有项目成员关系 (members)               │  │
│  │ 2. 查询所有成员角色 (member_roles)                    │  │
│  │ 3. 查询所有角色信息 (roles)                           │  │
│  │ 4. 解析角色权限列表 (JSON/序列化字符串)                │  │
│  │ 5. 返回所有权限的并集                                  │  │
│  └──────────────────────────────────────────────────────┘  │
└───────────────────────────┬───────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│         创建 UsernamePasswordAuthenticationToken             │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ principal: UserPrincipal                              │  │
│  │ authorities: [ROLE_ADMIN] 或 [ROLE_USER]              │  │
│  └──────────────────────────────────────────────────────┘  │
└───────────────────────────┬───────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│      SecurityContextHolder.getContext()                      │
│              .setAuthentication(authToken)                    │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ 将认证信息存储到 SecurityContext (ThreadLocal)        │  │
│  └──────────────────────────────────────────────────────┘  │
└───────────────────────────┬───────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│              SecurityFilterChain 检查                        │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ 1. 检查请求路径是否需要认证                            │  │
│  │ 2. 检查是否有认证信息                                  │  │
│  │ 3. 路由到对应的 Controller                            │  │
│  └──────────────────────────────────────────────────────┘  │
└───────────────────────────┬───────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│          @PreAuthorize 注解拦截                              │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ 表达式: hasPermission(#id, 'Project', 'edit_projects')│ │
│  └──────────────────────────────────────────────────────┘  │
└───────────────────────────┬───────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│    ProjectPermissionEvaluator.hasPermission()               │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ 1. 获取 UserPrincipal                                 │  │
│  │ 2. 检查是否是管理员 (直接返回 true)                   │  │
│  │ 3. 调用 PermissionService.hasPermission()             │  │
│  │ 4. 检查用户在指定项目中是否有权限                      │  │
│  └──────────────────────────────────────────────────────┘  │
└───────────────────────────┬───────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│    PermissionService.hasPermission(userId, projectId, perm) │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ 1. 查询用户是否是项目成员                             │  │
│  │ 2. 查询成员的所有角色                                 │  │
│  │ 3. 查询角色的权限列表                                 │  │
│  │ 4. 检查是否包含指定权限                               │  │
│  └──────────────────────────────────────────────────────┘  │
└───────────────────────────┬───────────────────────────────────┘
                            │
                            ▼
                   允许/拒绝访问
```

---

## 核心组件详解

### 1. SecurityConfig - 安全配置类

**文件位置**: `src/main/java/com/github/jredmine/config/SecurityConfig.java`

**功能**:
- 配置 Spring Security 的安全过滤器链
- 定义哪些路径需要认证，哪些可以公开访问
- 配置 JWT 认证过滤器
- 启用方法级权限验证
- 配置自定义的权限评估器

**关键配置**:

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)  // 启用方法级权限验证
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http
            .csrf(AbstractHttpConfigurer::disable)  // 禁用 CSRF（使用 JWT）
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()  // 公开访问
                .anyRequest().authenticated()  // 其他需要认证
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)  // 无状态
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
    }
    
    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler() {
        DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
        handler.setPermissionEvaluator(permissionEvaluator);  // 设置自定义权限评估器
        return handler;
    }
}
```

**作用**:
- 定义安全策略（哪些接口需要认证）
- 配置异常处理（401/403 响应）
- 集成 JWT 认证过滤器
- 启用 `@PreAuthorize` 注解支持

---

### 2. JwtAuthenticationFilter - JWT 认证过滤器

**文件位置**: `src/main/java/com/github/jredmine/config/JwtAuthenticationFilter.java`

**功能**:
- 在每个 HTTP 请求中拦截并验证 JWT Token
- 从 Token 中提取用户信息
- 加载用户权限并设置到 SecurityContext

**执行流程**:

```java
@Override
protected void doFilterInternal(HttpServletRequest request, 
                                HttpServletResponse response, 
                                FilterChain filterChain) {
    // 1. 提取 Authorization Header
    String authHeader = request.getHeader("Authorization");
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        filterChain.doFilter(request, response);
        return;
    }
    
    // 2. 提取并验证 JWT Token
    String jwt = authHeader.substring(7);
    if (jwtUtils.validateToken(jwt)) {
        String username = jwtUtils.extractUsername(jwt);
        Long userId = jwtUtils.extractUserId(jwt);
        
        // 3. 加载用户信息和权限
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        
        // 4. 创建认证对象
        UsernamePasswordAuthenticationToken authToken = 
            new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
            );
        
        // 5. 设置到 SecurityContext
        SecurityContextHolder.getContext().setAuthentication(authToken);
    }
    
    filterChain.doFilter(request, response);
}
```

**关键点**:
- 在 `UsernamePasswordAuthenticationFilter` 之前执行
- 使用 `SecurityContextHolder` 存储认证信息（ThreadLocal）
- 每个请求都会执行，确保权限信息是最新的

---

### 3. CustomUserDetailsService - 用户详情服务

**文件位置**: `src/main/java/com/github/jredmine/security/CustomUserDetailsService.java`

**功能**:
- 实现 Spring Security 的 `UserDetailsService` 接口
- 从数据库加载用户信息
- 加载用户权限并创建 `UserPrincipal` 对象

**实现逻辑**:

```java
@Override
public UserDetails loadUserByUsername(String username) {
    // 1. 查询用户
    User user = userMapper.selectOne(queryWrapper);
    if (user == null) {
        throw new UsernameNotFoundException("用户不存在");
    }
    
    // 2. 判断是否是管理员
    if (Boolean.TRUE.equals(user.getAdmin())) {
        return UserPrincipal.createAdmin(user);
    }
    
    // 3. 加载用户的所有权限（所有项目的权限并集）
    Set<String> permissions = permissionService.getUserAllPermissions(user.getId());
    
    // 4. 创建 UserPrincipal
    return UserPrincipal.create(user, permissions);
}
```

**调用时机**:
- JWT 认证过滤器验证 Token 后
- 需要加载用户信息时

---

### 4. UserPrincipal - 用户主体对象

**文件位置**: `src/main/java/com/github/jredmine/security/UserPrincipal.java`

**功能**:
- 实现 Spring Security 的 `UserDetails` 接口
- 封装用户信息和权限信息
- 提供权限检查方法

**数据结构**:

```java
public class UserPrincipal implements UserDetails {
    private final Long id;                    // 用户ID
    private final String username;            // 用户名
    private final String password;            // 密码（已加密）
    private final boolean admin;              // 是否是管理员
    private final Collection<GrantedAuthority> authorities;  // 角色（ROLE_ADMIN/ROLE_USER）
    private final Set<String> permissions;    // 权限集合（所有项目的权限并集）
    
    // 检查用户是否拥有指定权限
    public boolean hasPermission(String permission) {
        if (admin) {
            return true;  // 管理员拥有所有权限
        }
        return permissions.contains(permission);
    }
}
```

**使用场景**:
- 存储在 `SecurityContext` 中
- 在权限表达式中使用：`authentication.principal.hasPermission('create_projects')`
- 在代码中获取：`((UserPrincipal) authentication.getPrincipal()).getId()`

---

### 5. PermissionService - 权限服务

**文件位置**: `src/main/java/com/github/jredmine/security/PermissionService.java`

**功能**:
- 查询用户的项目权限
- 检查用户是否在指定项目中拥有指定权限
- 解析角色权限列表（支持 JSON 和序列化字符串格式）

**核心方法**:

#### 5.1 获取用户所有权限

```java
public Set<String> getUserAllPermissions(Long userId) {
    // 1. 查询用户的所有项目成员关系
    List<Member> members = memberMapper.selectList(...);
    
    // 2. 查询所有成员角色
    List<MemberRole> memberRoles = memberRoleMapper.selectList(...);
    
    // 3. 查询所有角色信息
    List<Role> roles = roleMapper.selectBatchIds(roleIds);
    
    // 4. 解析并合并所有权限
    Set<String> permissions = new HashSet<>();
    for (Role role : roles) {
        List<String> rolePermissions = parsePermissions(role.getPermissions());
        permissions.addAll(rolePermissions);
    }
    
    return permissions;
}
```

**用途**: 在 JWT 认证时加载用户的所有权限（用于全局权限检查）

#### 5.2 获取用户项目权限

```java
public Set<String> getUserProjectPermissions(Long userId, Long projectId) {
    // 1. 查询用户是否是项目成员
    Member member = memberMapper.selectOne(...);
    if (member == null) {
        return Collections.emptySet();
    }
    
    // 2. 查询成员的所有角色
    List<MemberRole> memberRoles = memberRoleMapper.selectList(...);
    
    // 3. 查询角色信息并解析权限
    // ... 同上
    
    return permissions;
}
```

**用途**: 检查用户在特定项目中的权限

#### 5.3 权限检查

```java
public boolean hasPermission(Long userId, Long projectId, String permission) {
    Set<String> permissions = getUserProjectPermissions(userId, projectId);
    return permissions.contains(permission);
}
```

**用途**: 在权限评估器中使用

#### 5.4 权限解析

```java
private List<String> parsePermissions(String permissionsStr) {
    // 支持多种格式：
    // 1. JSON 数组: ["view_issues", "add_issues"]
    // 2. YAML 序列化: "---\n- :view_issues\n- :add_issues"
    // 3. 逗号分隔: "view_issues,add_issues"
    // 4. 单个权限: "view_issues"
}
```

---

### 6. ProjectPermissionEvaluator - 项目权限评估器

**文件位置**: `src/main/java/com/github/jredmine/security/ProjectPermissionEvaluator.java`

**功能**:
- 实现 Spring Security 的 `PermissionEvaluator` 接口
- 在 `@PreAuthorize` 注解中评估权限表达式
- 支持项目级权限检查

**实现逻辑**:

```java
@Override
public boolean hasPermission(Authentication authentication, 
                            Object targetDomainObject, 
                            Object permission) {
    // 1. 获取 UserPrincipal
    UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
    
    // 2. 管理员拥有所有权限
    if (userPrincipal.isAdmin()) {
        return true;
    }
    
    // 3. 检查项目级权限
    if (targetDomainObject instanceof Long) {
        Long projectId = (Long) targetDomainObject;
        String permissionKey = permission.toString();
        return permissionService.hasPermission(
            userPrincipal.getId(), projectId, permissionKey
        );
    }
    
    return false;
}
```

**使用场景**:
- `@PreAuthorize("hasPermission(#id, 'Project', 'edit_projects')")`
- Spring Security 会自动调用此方法进行权限评估

---

### 7. SecurityUtils - 安全工具类

**文件位置**: `src/main/java/com/github/jredmine/util/SecurityUtils.java`

**功能**:
- 提供便捷方法获取当前登录用户信息
- 在 Service 层进行权限检查（如果需要在代码中检查）

**核心方法**:

```java
@Component
public class SecurityUtils {
    
    // 获取当前用户ID
    public Long getCurrentUserId() {
        UserPrincipal userPrincipal = getCurrentUserPrincipal();
        return userPrincipal.getId();
    }
    
    // 获取当前用户信息
    public User getCurrentUser() {
        // 从 SecurityContext 获取 UserPrincipal
        // 然后从数据库查询完整用户信息
    }
    
    // 获取当前用户主体
    public UserPrincipal getCurrentUserPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (UserPrincipal) auth.getPrincipal();
    }
    
    // 检查是否是管理员
    public boolean isAdmin() {
        UserPrincipal userPrincipal = getCurrentUserPrincipal();
        return userPrincipal.isAdmin();
    }
}
```

**使用场景**:
- Service 层需要获取当前用户时
- 需要在代码中进行权限判断时

---

## 文件结构说明

```
src/main/java/com/github/jredmine/
├── config/
│   ├── SecurityConfig.java              # Spring Security 主配置
│   └── JwtAuthenticationFilter.java     # JWT 认证过滤器
│
├── security/
│   ├── UserPrincipal.java               # 用户主体对象（实现 UserDetails）
│   ├── CustomUserDetailsService.java    # 用户详情服务（实现 UserDetailsService）
│   ├── PermissionService.java           # 权限服务（查询用户权限）
│   └── ProjectPermissionEvaluator.java  # 权限评估器（实现 PermissionEvaluator）
│
├── util/
│   └── SecurityUtils.java                # 安全工具类
│
└── controller/
    ├── ProjectController.java            # 使用 @PreAuthorize 注解
    ├── UserController.java               # 使用 @PreAuthorize 注解
    └── RoleController.java               # 使用 @PreAuthorize 注解
```

---

## 权限验证表达式

### 1. 管理员权限

```java
@PreAuthorize("hasRole('ADMIN')")
```

**说明**: 检查用户是否是系统管理员

**使用场景**: 
- 用户管理操作
- 角色管理操作
- 项目模板管理

---

### 2. 项目级权限

```java
@PreAuthorize("hasPermission(#id, 'Project', 'edit_projects')")
```

**说明**: 
- `#id`: 方法参数中的项目ID
- `'Project'`: 目标类型（固定为 'Project'）
- `'edit_projects'`: 权限键

**执行流程**:
1. Spring Security 调用 `ProjectPermissionEvaluator.hasPermission()`
2. 传入参数：`authentication`, `#id`, `'Project'`, `'edit_projects'`
3. 评估器检查用户是否在指定项目中拥有该权限

**使用场景**:
- 更新项目: `edit_projects`
- 删除项目: `delete_projects`
- 管理项目成员: `manage_projects`

---

### 3. 全局权限

```java
@PreAuthorize("hasRole('ADMIN') or authentication.principal.hasPermission('create_projects')")
```

**说明**:
- `authentication.principal`: 获取 `UserPrincipal` 对象
- `hasPermission('create_projects')`: 调用 `UserPrincipal.hasPermission()` 方法
- 检查用户是否在任何项目中拥有该权限

**使用场景**:
- 创建项目: `create_projects`（创建时还没有项目ID）

---

### 4. 组合表达式

```java
@PreAuthorize("hasRole('ADMIN') or authentication.principal.id == #id")
```

**说明**: 
- 管理员可以访问
- 或者用户ID等于参数ID（操作自己的数据）

**使用场景**:
- 查询/更新自己的用户信息
- 查询/更新自己的偏好设置

---

## 使用示例

### 示例 1: 项目更新接口

```java
@Operation(summary = "更新项目", security = @SecurityRequirement(name = "bearerAuth"))
@PreAuthorize("hasPermission(#id, 'Project', 'edit_projects')")
@PutMapping("/{id}")
public ApiResponse<ProjectDetailResponseDTO> updateProject(
        @PathVariable Long id,
        @Valid @RequestBody ProjectUpdateRequestDTO requestDTO) {
    // 只有拥有 edit_projects 权限的用户才能执行此方法
    ProjectDetailResponseDTO result = projectService.updateProject(id, requestDTO);
    return ApiResponse.success("项目更新成功", result);
}
```

**权限检查流程**:
1. 用户请求 `PUT /api/projects/123`
2. JWT 过滤器验证 Token 并加载用户权限
3. `@PreAuthorize` 拦截，调用 `ProjectPermissionEvaluator`
4. 评估器检查用户是否在项目 123 中拥有 `edit_projects` 权限
5. 有权限则继续执行，无权限则返回 403 Forbidden

---

### 示例 2: 创建项目接口

```java
@Operation(summary = "创建项目", security = @SecurityRequirement(name = "bearerAuth"))
@PreAuthorize("hasRole('ADMIN') or authentication.principal.hasPermission('create_projects')")
@PostMapping
public ApiResponse<ProjectDetailResponseDTO> createProject(
        @Valid @RequestBody ProjectCreateRequestDTO requestDTO) {
    // 管理员或拥有 create_projects 权限的用户可以执行
    ProjectDetailResponseDTO result = projectService.createProject(requestDTO);
    return ApiResponse.success("项目创建成功", result);
}
```

**权限检查流程**:
1. 用户请求 `POST /api/projects`
2. JWT 过滤器验证 Token 并加载用户权限
3. `@PreAuthorize` 拦截
4. 检查是否是管理员，或者用户是否在任何项目中拥有 `create_projects` 权限
5. 满足条件则继续执行

---

### 示例 3: 在 Service 层获取当前用户

```java
@Service
@RequiredArgsConstructor
public class ProjectService {
    
    private final SecurityUtils securityUtils;
    
    public void someMethod() {
        // 获取当前用户ID
        Long userId = securityUtils.getCurrentUserId();
        
        // 获取当前用户信息
        User user = securityUtils.getCurrentUser();
        
        // 检查是否是管理员
        if (securityUtils.isAdmin()) {
            // 管理员逻辑
        } else {
            // 普通用户逻辑
        }
    }
}
```

---

## 常见问题

### Q1: 为什么需要 PermissionService？

**A**: 
- JWT 认证时，需要加载用户的所有权限（用于全局权限检查）
- 权限评估时，需要检查用户在特定项目中的权限
- 权限存储在角色的 `permissions` 字段中，需要解析和查询

---

### Q2: UserPrincipal 中的 permissions 和项目级权限检查的关系？

**A**:
- `UserPrincipal.permissions`: 用户在所有项目中的权限并集（用于全局权限检查）
- 项目级权限检查: 每次调用 `PermissionService.hasPermission()` 实时查询（更准确）

**为什么这样设计**:
- 全局权限（如 `create_projects`）只需要知道用户是否在任何项目中拥有即可
- 项目级权限（如 `edit_projects`）需要精确检查用户在特定项目中的权限
- 避免在 JWT 中存储所有项目的权限（数据量大，且可能过期）

---

### Q3: 权限检查的性能如何优化？

**A**: 当前实现每次请求都会查询数据库，可以考虑：

1. **缓存用户权限**:
   ```java
   @Cacheable(value = "userPermissions", key = "#userId")
   public Set<String> getUserAllPermissions(Long userId) {
       // ...
   }
   ```

2. **缓存项目权限**:
   ```java
   @Cacheable(value = "projectPermissions", key = "#userId + '_' + #projectId")
   public Set<String> getUserProjectPermissions(Long userId, Long projectId) {
       // ...
   }
   ```

3. **权限变更时清除缓存**:
   - 当用户角色变更时
   - 当角色权限变更时

---

### Q4: 如何处理权限继承（子项目继承父项目角色）？

**A**: 
- 当前实现中，`member_roles` 表有 `inherited_from` 字段
- 在查询权限时，需要同时查询直接分配的角色和继承的角色
- 可以在 `PermissionService.getUserProjectPermissions()` 中实现递归查询

---

### Q5: 权限字符串格式支持哪些？

**A**: `PermissionService.parsePermissions()` 支持：

1. **JSON 数组**: `["view_issues", "add_issues"]`
2. **YAML 序列化**: `"---\n- :view_issues\n- :add_issues"`
3. **逗号分隔**: `"view_issues,add_issues"`
4. **单个权限**: `"view_issues"`

---

### Q6: 如何调试权限问题？

**A**: 

1. **启用日志**:
   ```java
   log.debug("检查权限 - 用户ID: {}, 项目ID: {}, 权限: {}", userId, projectId, permission);
   ```

2. **检查 SecurityContext**:
   ```java
   Authentication auth = SecurityContextHolder.getContext().getAuthentication();
   UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
   log.debug("当前用户: {}, 权限: {}", principal.getUsername(), principal.getPermissions());
   ```

3. **检查数据库**:
   - 查询 `members` 表确认用户是否是项目成员
   - 查询 `member_roles` 表确认用户角色
   - 查询 `roles` 表确认角色权限

---

## 总结

Spring Security 权限验证系统通过以下方式实现：

1. **JWT 认证**: 在每个请求中验证 Token 并加载用户权限
2. **方法级权限**: 使用 `@PreAuthorize` 注解进行声明式权限控制
3. **自定义评估器**: 实现 `PermissionEvaluator` 支持项目级权限检查
4. **权限服务**: 提供统一的权限查询接口

**优势**:
- ✅ 声明式权限控制，代码简洁
- ✅ 支持项目级权限，灵活精细
- ✅ 管理员自动拥有所有权限
- ✅ 易于扩展和维护

**注意事项**:
- ⚠️ 权限检查会查询数据库，注意性能优化
- ⚠️ 权限变更后需要重新登录或清除缓存
- ⚠️ 确保权限字符串格式正确

