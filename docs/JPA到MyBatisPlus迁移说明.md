# JPA 到 MyBatis Plus 迁移说明

> 本文档说明从 Spring Data JPA 迁移到 MyBatis Plus 的变更内容和使用方式。

**迁移时间**: 2025年2月

---

## 一、迁移概述

### 1.1 变更内容

- ✅ 移除 Spring Data JPA 和 Hibernate 依赖
- ✅ 移除 QueryDSL 依赖和构建配置
- ✅ 添加 MyBatis Plus 依赖
- ✅ 更新实体类注解（JPA → MyBatis Plus）
- ✅ 重构 Repository 层（JPA Repository → MyBatis Plus Mapper）
- ✅ 更新 Service 层使用 Lambda QueryWrapper

### 1.2 主要优势

- ✅ **类型安全查询**: 使用 Lambda QueryWrapper，编译时类型检查
- ✅ **SQL 控制**: 完全控制 SQL 语句，性能优化更灵活
- ✅ **学习曲线**: 基于 SQL，学习成本低
- ✅ **调试方便**: SQL 直观易懂，便于调试

---

## 二、依赖变更

### 2.1 移除的依赖

```xml
<!-- 移除 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>com.querydsl</groupId>
    <artifactId>querydsl-jpa</artifactId>
    <version>5.0.0</version>
</dependency>
<dependency>
    <groupId>com.querydsl</groupId>
    <artifactId>querydsl-apt</artifactId>
    <version>5.0.0</version>
</dependency>
```

### 2.2 新增的依赖

```xml
<!-- 新增 -->
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-boot-starter</artifactId>
    <version>3.5.7</version>
</dependency>
```

### 2.3 移除的构建配置

```xml
<!-- 移除 QueryDSL 注解处理器 -->
<plugin>
    <groupId>com.mysema.maven</groupId>
    <artifactId>apt-maven-plugin</artifactId>
    ...
</plugin>
```

---

## 三、配置变更

### 3.1 移除的配置

```yaml
# 移除 JPA 配置
spring:
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
```

### 3.2 新增的配置

```yaml
# MyBatis Plus 配置
mybatis-plus:
  # 配置扫描 Mapper XML 文件路径
  mapper-locations: classpath*:/mapper/**/*.xml
  # 配置实体类包路径
  type-aliases-package: com.github.jredmine.entity
  # 配置
  configuration:
    # 开启驼峰命名转换
    map-underscore-to-camel-case: true
    # 开启 SQL 日志
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  # 全局配置
  global-config:
    db-config:
      # 主键类型：AUTO(数据库自增)
      id-type: AUTO
      # 逻辑删除字段名
      logic-delete-field: deleted
      # 逻辑删除值（已删除）
      logic-delete-value: 1
      # 逻辑未删除值（未删除）
      logic-not-delete-value: 0
```

### 3.3 应用主类变更

```java
@SpringBootApplication
@MapperScan("com.github.jredmine.repository")  // 新增：扫描 Mapper 接口
public class JRedmineApplication {
    // ...
}
```

---

## 四、代码变更

### 4.1 实体类变更

#### 之前（JPA）

```java
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String login;
    
    @Column(name = "hashed_password")
    private String hashedPassword;
    // ...
}
```

#### 现在（MyBatis Plus）

```java
@TableName("users")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String login;
    
    private String hashedPassword;  // 驼峰命名自动映射到 hashed_password
    // ...
}
```

**变更说明**:
- ✅ 移除 `@Entity`、`@Table`、`@Column` 等 JPA 注解
- ✅ 使用 `@TableName` 指定表名
- ✅ 使用 `@TableId` 指定主键和生成策略
- ✅ 字段名使用驼峰命名，自动映射到数据库下划线命名

### 4.2 Repository 层变更

#### 之前（JPA Repository）

```java
@Repository
public interface UserRepository extends JpaRepository<User, Long>, QuerydslPredicateExecutor<User> {
    Optional<User> findByLogin(String login);
}
```

#### 现在（MyBatis Plus Mapper）

```java
@Mapper
public interface UserRepository extends BaseMapper<User> {
    // 基础 CRUD 方法已由 BaseMapper 提供
    // 复杂查询使用 Lambda QueryWrapper 在 Service 层实现
}
```

**变更说明**:
- ✅ 继承 `BaseMapper<T>` 获得基础 CRUD 方法
- ✅ 使用 `@Mapper` 注解替代 `@Repository`
- ✅ 方法命名查询改为使用 Lambda QueryWrapper

### 4.3 Service 层变更

#### 之前（JPA）

```java
@Service
public class UserService {
    private final UserRepository userRepository;
    
    public UserRegisterResponseDTO register(UserRegisterRequestDTO requestDTO) {
        // 检查用户是否存在
        Optional<User> existsUser = userRepository.findByLogin(requestDTO.getLogin());
        if (existsUser.isPresent()) {
            throw new Exception("User already exists");
        }
        
        User user = new User();
        // ... 设置属性
        userRepository.save(user);  // JPA save 方法
        
        return UserMapper.INSTANCE.toUserRegisterResponseDTO(user);
    }
}
```

#### 现在（MyBatis Plus + Lambda QueryWrapper）

```java
@Service
public class UserService {
    private final UserRepository userRepository;
    
    public UserRegisterResponseDTO register(UserRegisterRequestDTO requestDTO) {
        // 使用 Lambda QueryWrapper 检查用户是否存在（类型安全）
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getLogin, requestDTO.getLogin());
        User existsUser = userRepository.selectOne(queryWrapper);
        
        if (existsUser != null) {
            throw new Exception("User already exists");
        }
        
        User user = new User();
        // ... 设置属性
        userRepository.insert(user);  // MyBatis Plus insert 方法
        
        return UserMapper.INSTANCE.toUserRegisterResponseDTO(user);
    }
}
```

**变更说明**:
- ✅ 使用 `LambdaQueryWrapper` 进行类型安全查询
- ✅ 使用 `selectOne()` 替代 `findByLogin()`
- ✅ 使用 `insert()` 替代 `save()`
- ✅ 返回类型从 `Optional<User>` 改为 `User`

---

## 五、常用操作对比

### 5.1 查询操作

#### 简单查询

**JPA**:
```java
Optional<User> user = userRepository.findById(1L);
List<User> users = userRepository.findAll();
```

**MyBatis Plus**:
```java
User user = userRepository.selectById(1L);
List<User> users = userRepository.selectList(null);
```

#### 条件查询（类型安全）

**JPA**:
```java
// 需要定义方法
Optional<User> user = userRepository.findByLogin("admin");
```

**MyBatis Plus**:
```java
// 使用 Lambda QueryWrapper（类型安全）
LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
wrapper.eq(User::getLogin, "admin");
User user = userRepository.selectOne(wrapper);
```

#### 多条件查询

**JPA**:
```java
// 需要定义方法或使用 QueryDSL
List<User> users = userRepository.findByStatusAndAdmin(1, true);
```

**MyBatis Plus**:
```java
// 使用 Lambda QueryWrapper（类型安全）
LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
wrapper.eq(User::getStatus, 1)
       .eq(User::getAdmin, true);
List<User> users = userRepository.selectList(wrapper);
```

#### 模糊查询

**JPA**:
```java
List<User> users = userRepository.findByLoginContaining("admin");
```

**MyBatis Plus**:
```java
LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
wrapper.like(User::getLogin, "admin");
List<User> users = userRepository.selectList(wrapper);
```

#### 分页查询

**JPA**:
```java
Pageable pageable = PageRequest.of(0, 10);
Page<User> page = userRepository.findAll(pageable);
```

**MyBatis Plus**:
```java
// 需要配置分页插件
Page<User> page = new Page<>(1, 10);
LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
Page<User> result = userRepository.selectPage(page, wrapper);
```

### 5.2 插入操作

**JPA**:
```java
User user = new User();
// ... 设置属性
userRepository.save(user);  // 新增或更新
```

**MyBatis Plus**:
```java
User user = new User();
// ... 设置属性
userRepository.insert(user);  // 仅插入
// 或
userRepository.insertOrUpdate(user);  // 插入或更新
```

### 5.3 更新操作

**JPA**:
```java
User user = userRepository.findById(1L).orElseThrow();
user.setStatus(2);
userRepository.save(user);  // 自动更新
```

**MyBatis Plus**:
```java
// 方式1：先查询再更新
User user = userRepository.selectById(1L);
user.setStatus(2);
userRepository.updateById(user);

// 方式2：直接更新（推荐）
LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
updateWrapper.eq(User::getId, 1L)
             .set(User::getStatus, 2);
userRepository.update(null, updateWrapper);
```

### 5.4 删除操作

**JPA**:
```java
userRepository.deleteById(1L);
userRepository.delete(user);
```

**MyBatis Plus**:
```java
userRepository.deleteById(1L);
userRepository.delete(new LambdaQueryWrapper<User>().eq(User::getId, 1L));
```

---

## 六、Lambda QueryWrapper 使用指南

### 6.1 基本用法

```java
LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();

// 等于
wrapper.eq(User::getStatus, 1);

// 不等于
wrapper.ne(User::getStatus, 0);

// 大于
wrapper.gt(User::getId, 100);

// 大于等于
wrapper.ge(User::getId, 100);

// 小于
wrapper.lt(User::getId, 1000);

// 小于等于
wrapper.le(User::getId, 1000);

// 模糊查询
wrapper.like(User::getLogin, "admin");
wrapper.likeLeft(User::getLogin, "admin");   // %admin
wrapper.likeRight(User::getLogin, "admin");  // admin%

// IN 查询
wrapper.in(User::getId, Arrays.asList(1, 2, 3));

// NOT IN
wrapper.notIn(User::getId, Arrays.asList(4, 5, 6));

// IS NULL
wrapper.isNull(User::getDeletedAt);

// IS NOT NULL
wrapper.isNotNull(User::getDeletedAt);

// BETWEEN
wrapper.between(User::getId, 1, 100);

// 排序
wrapper.orderByAsc(User::getCreatedOn);
wrapper.orderByDesc(User::getCreatedOn);

// 限制数量
wrapper.last("LIMIT 10");
```

### 6.2 组合条件

```java
LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();

// AND 条件
wrapper.eq(User::getStatus, 1)
       .eq(User::getAdmin, true);

// OR 条件
wrapper.eq(User::getStatus, 1)
       .or()
       .eq(User::getStatus, 2);

// 嵌套条件
wrapper.and(w -> w.eq(User::getStatus, 1).eq(User::getAdmin, true))
       .or(w -> w.eq(User::getStatus, 2).eq(User::getAdmin, false));
```

### 6.3 复杂查询示例

```java
// 多条件组合查询
LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
wrapper.eq(User::getStatus, 1)
       .like(User::getLogin, "admin")
       .ge(User::getCreatedOn, startDate)
       .le(User::getCreatedOn, endDate)
       .orderByDesc(User::getCreatedOn)
       .last("LIMIT 20");

List<User> users = userRepository.selectList(wrapper);
```

---

## 七、注意事项

### 7.1 字段映射

- ✅ MyBatis Plus 默认开启驼峰命名转换
- ✅ 实体类字段使用驼峰命名（如 `hashedPassword`）
- ✅ 数据库字段使用下划线命名（如 `hashed_password`）
- ✅ 自动映射，无需 `@Column` 注解

### 7.2 主键策略

- ✅ 使用 `@TableId(type = IdType.AUTO)` 表示数据库自增
- ✅ 其他策略：`ASSIGN_ID`（雪花算法）、`ASSIGN_UUID`（UUID）等

### 7.3 逻辑删除

- ✅ 配置了逻辑删除字段 `deleted`
- ✅ 删除操作会自动转换为更新 `deleted = 1`
- ✅ 查询操作会自动过滤 `deleted = 0` 的记录

### 7.4 分页插件

如果需要分页功能，需要配置分页插件：

```java
@Configuration
public class MyBatisPlusConfig {
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
```

---

## 八、迁移检查清单

- [x] 更新 pom.xml 依赖
- [x] 更新配置文件
- [x] 更新实体类注解
- [x] 重构 Repository 接口
- [x] 更新 Service 层查询逻辑
- [x] 添加 @MapperScan 注解
- [ ] 测试所有 CRUD 操作
- [ ] 测试复杂查询
- [ ] 测试分页功能
- [ ] 性能测试

---

## 九、参考资源

- [MyBatis Plus 官方文档](https://baomidou.com/)
- [Lambda QueryWrapper 使用指南](https://baomidou.com/pages/10c804/)
- [MyBatis Plus 代码生成器](https://baomidou.com/pages/generator/)

---

**迁移完成时间**: 2025年2月

