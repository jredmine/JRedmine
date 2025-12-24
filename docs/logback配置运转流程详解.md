# Logback 配置运转流程详解

## 一、配置文件加载流程

### 1.1 Spring Boot 日志配置加载顺序

```
应用启动
    ↓
Spring Boot 自动配置
    ↓
查找日志配置文件（按优先级）：
    1. logback-spring.xml（支持 Spring Profile）
    2. logback.xml（不支持 Spring Profile）
    3. logback-test.xml（测试环境）
    4. 默认配置（如果以上都不存在）
    ↓
解析 XML 配置
    ↓
初始化 LoggerContext
    ↓
创建 Appender、Encoder、RollingPolicy 等组件
    ↓
应用配置到日志系统
    ↓
日志系统就绪
```

### 1.2 为什么使用 `logback-spring.xml`？

- ✅ **支持 Spring Profile**：可以使用 `<springProfile>` 标签根据环境切换配置
- ✅ **Spring Boot 特性**：支持使用 `${}` 引用 Spring 配置属性
- ✅ **延迟初始化**：可以延迟日志初始化，提高启动速度

## 二、配置文件结构解析

### 2.1 整体结构

```xml
<configuration>
    <!-- 1. 引入默认配置 -->
    <include resource="..."/>
    
    <!-- 2. 定义属性变量 -->
    <property name="LOG_HOME" value="..."/>
    
    <!-- 3. 定义 Appender（日志输出器） -->
    <appender name="CONSOLE" ...>
        <encoder>...</encoder>
    </appender>
    
    <!-- 4. 定义 Logger（日志记录器） -->
    <logger name="com.github.jredmine" level="DEBUG"/>
    
    <!-- 5. 定义 Root Logger（根日志记录器） -->
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
    </root>
</configuration>
```

## 三、详细组件解析

### 3.1 属性定义（Property）

```xml
<property name="LOG_HOME" value="${LOG_HOME:-logs}"/>
<property name="APP_NAME" value="jredmine"/>
```

**作用**：
- 定义可复用的变量
- 支持环境变量：`${LOG_HOME:-logs}` 表示如果环境变量 `LOG_HOME` 存在则使用，否则使用默认值 `logs`
- 可以在配置文件中通过 `${LOG_HOME}` 引用

**执行时机**：配置文件解析时立即执行

### 3.2 Appender（日志输出器）

Appender 负责将日志事件输出到目标位置（控制台、文件、网络等）。

#### 3.2.1 控制台 Appender（CONSOLE）

**开发/本地环境配置**：

```xml
<springProfile name="dev,local">
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{50} - %msg%n</pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>
</springProfile>
```

**运转流程**：
```
日志事件产生
    ↓
ConsoleAppender 接收
    ↓
Encoder 格式化日志（使用 Pattern）
    ↓
输出到 System.out
    ↓
控制台显示
```

**Pattern 格式说明**：
- `%d{yyyy-MM-dd HH:mm:ss.SSS}`：日期时间
- `[%thread]`：线程名
- `%-5level`：日志级别（左对齐，5个字符宽度）
- `%logger{50}`：Logger 名称（最多50个字符）
- `%msg`：日志消息
- `%n`：换行符

**生产环境配置**：

```xml
<springProfile name="prod">
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
            <providers>...</providers>
        </encoder>
    </appender>
</springProfile>
```

**运转流程**：
```
日志事件产生
    ↓
ConsoleAppender 接收
    ↓
LoggingEventCompositeJsonEncoder 处理
    ↓
各个 Provider 提取信息：
    - TimestampProvider：提取时间戳
    - LogLevelProvider：提取日志级别
    - MessageProvider：提取消息
    - MdcProvider：提取 MDC 上下文
    - StackTraceProvider：提取堆栈信息
    ↓
组装成 JSON 对象
    ↓
输出到 System.out（JSON 格式）
```

#### 3.2.2 文件 Appender（FILE）

```xml
<appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>${LOG_HOME}/${APP_NAME}.log</file>
    <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
        <providers>...</providers>
    </encoder>
    <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
        <fileNamePattern>${LOG_HOME}/${APP_NAME}-%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>
        <maxFileSize>100MB</maxFileSize>
        <maxHistory>30</maxHistory>
        <totalSizeCap>10GB</totalSizeCap>
    </rollingPolicy>
</appender>
```

**运转流程**：

**1. 日志写入流程**：
```
日志事件产生
    ↓
RollingFileAppender 接收
    ↓
检查当前文件是否需要滚动
    ↓
如果不需要滚动：
    ↓
    直接写入当前文件（jredmine.log）
    ↓
如果需要滚动：
    ↓
    触发滚动操作（见下方滚动流程）
    ↓
    写入新的当前文件
```

**2. 文件滚动流程**：
```
触发条件检查（每次写入时）：
    - 当前文件大小 >= maxFileSize（100MB）？
    - 日期是否变化？
    ↓
如果满足滚动条件：
    ↓
    关闭当前文件
    ↓
    重命名当前文件：
        jredmine.log → jredmine-2024-01-15.0.log
    ↓
    压缩文件：
        jredmine-2024-01-15.0.log → jredmine-2024-01-15.0.log.gz
    ↓
    创建新的 jredmine.log 文件
    ↓
    继续写入新文件
```

**3. 文件清理流程**：
```
应用启动时（cleanHistoryOnStart=true）：
    ↓
扫描日志目录
    ↓
计算所有日志文件的总大小
    ↓
如果总大小 > totalSizeCap（10GB）：
    ↓
    按时间排序，删除最旧的日志文件
    ↓
    直到总大小 <= totalSizeCap
    ↓
删除超过 maxHistory（30天）的日志文件
```

**滚动策略参数详解**：

| 参数 | 说明 | 示例 |
|------|------|------|
| `fileNamePattern` | 滚动后的文件命名模式 | `jredmine-%d{yyyy-MM-dd}.%i.log.gz` |
| `%d{yyyy-MM-dd}` | 日期占位符 | `2024-01-15` |
| `%i` | 同一天内的序号（从0开始） | `0, 1, 2, ...` |
| `.gz` | 压缩格式 | 自动压缩为 gzip |
| `maxFileSize` | 单个文件最大大小 | `100MB` |
| `maxHistory` | 保留天数 | `30` 天 |
| `totalSizeCap` | 所有日志文件总大小限制 | `10GB` |

**滚动场景示例**：

**场景1：文件大小触发滚动**
```
10:00:00 - jredmine.log (99MB) - 继续写入
10:00:01 - jredmine.log (100MB) - 触发滚动
          ↓
          jredmine.log (0MB) - 新文件
          jredmine-2024-01-15.0.log.gz - 已滚动
```

**场景2：日期变化触发滚动**
```
2024-01-15 23:59:59 - jredmine.log (50MB) - 继续写入
2024-01-16 00:00:00 - 日期变化，触发滚动
                    ↓
                    jredmine.log (0MB) - 新文件（新日期）
                    jredmine-2024-01-15.0.log.gz - 昨天的日志
```

**场景3：同一天多次滚动**
```
10:00 - jredmine.log (100MB) → 滚动 → jredmine-2024-01-15.0.log.gz
11:00 - jredmine.log (100MB) → 滚动 → jredmine-2024-01-15.1.log.gz
12:00 - jredmine.log (100MB) → 滚动 → jredmine-2024-01-15.2.log.gz
```

#### 3.2.3 错误日志 Appender（ERROR_FILE）

```xml
<appender name="ERROR_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>${LOG_HOME}/${APP_NAME}-error.log</file>
    <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
        <level>ERROR</level>
    </filter>
    ...
</appender>
```

**运转流程**：
```
日志事件产生
    ↓
ERROR_FILE Appender 接收
    ↓
ThresholdFilter 过滤：
    - 如果日志级别 >= ERROR：通过
    - 如果日志级别 < ERROR：拒绝
    ↓
通过过滤的日志：
    ↓
    写入错误日志文件
    ↓
    应用滚动策略
```

**Filter 工作原理**：
- `ThresholdFilter`：只允许指定级别及以上的日志通过
- `ERROR` 级别：只记录 ERROR 和 FATAL（如果有）

#### 3.2.4 异步 Appender（ASYNC_FILE）

```xml
<appender name="ASYNC_FILE" class="ch.qos.logback.classic.AsyncAppender">
    <discardingThreshold>0</discardingThreshold>
    <queueSize>512</queueSize>
    <appender-ref ref="FILE"/>
</appender>
```

**运转流程**：

**同步模式（不使用 AsyncAppender）**：
```
业务线程
    ↓
产生日志事件
    ↓
等待 Appender 处理（I/O 操作）
    ↓
日志写入完成
    ↓
继续执行业务逻辑
```
**问题**：I/O 操作阻塞业务线程，影响性能

**异步模式（使用 AsyncAppender）**：
```
业务线程
    ↓
产生日志事件
    ↓
放入异步队列（非阻塞，极快）
    ↓
立即返回，继续执行业务逻辑
    ↓
    ↓
后台线程（AsyncAppender 内部）
    ↓
从队列取出日志事件
    ↓
调用实际的 Appender（FILE）
    ↓
执行 I/O 操作
```

**异步 Appender 参数**：

| 参数 | 说明 | 默认值 | 配置值 |
|------|------|--------|--------|
| `queueSize` | 队列大小 | 256 | 512 |
| `discardingThreshold` | 队列剩余空间阈值，低于此值时丢弃日志 | 20% | 0（不丢弃） |
| `neverBlock` | 队列满时是否阻塞 | false | false（队列满时阻塞） |

**队列状态处理**：

```
队列状态：
    - 空闲：正常入队
    - 接近满（剩余 < discardingThreshold）：
        - discardingThreshold > 0：丢弃 TRACE/DEBUG/INFO 级别日志
        - discardingThreshold = 0：不丢弃，继续入队
    - 已满：
        - neverBlock = false：阻塞等待（可能影响性能）
        - neverBlock = true：丢弃日志（可能丢失日志）
```

**性能对比**：

| 模式 | 日志写入耗时 | 业务线程影响 |
|------|------------|------------|
| 同步 | ~1-5ms | 阻塞业务线程 |
| 异步 | ~0.01ms | 几乎无影响 |

### 3.3 Logger（日志记录器）

Logger 决定哪些日志事件应该被记录，以及记录到哪些 Appender。

#### 3.3.1 Logger 层次结构

```
Root Logger（根）
    ↓
com（包）
    ↓
com.github（包）
    ↓
com.github.jredmine（包）
    ↓
com.github.jredmine.service（包）
    ↓
com.github.jredmine.service.UserService（类）
```

**日志级别继承**：
- 子 Logger 继承父 Logger 的级别
- 如果子 Logger 未设置级别，使用父 Logger 的级别
- 最终继承到 Root Logger

#### 3.3.2 Logger 配置示例

```xml
<!-- 项目包日志级别 -->
<logger name="com.github.jredmine" level="DEBUG"/>
```

**运转流程**：

```
代码中调用：log.debug("调试信息")
    ↓
获取 Logger：LoggerFactory.getLogger(UserService.class)
    ↓
Logger 名称：com.github.jredmine.service.UserService
    ↓
查找匹配的 Logger 配置：
    - 精确匹配：com.github.jredmine.service.UserService（未找到）
    - 父级匹配：com.github.jredmine（找到，level=DEBUG）
    ↓
检查日志级别：
    - 请求级别：DEBUG
    - 配置级别：DEBUG
    - DEBUG >= DEBUG：✅ 通过
    ↓
创建日志事件
    ↓
传递给所有关联的 Appender
```

**Logger 匹配规则**：

| Logger 名称 | 匹配的配置 | 说明 |
|-----------|----------|------|
| `com.github.jredmine.service.UserService` | `com.github.jredmine` | 匹配父级配置 |
| `com.github.jredmine.mapper.UserMapper` | `com.github.jredmine` | 匹配父级配置 |
| `org.springframework.web` | `org.springframework` | 匹配父级配置 |
| `com.other.package` | Root Logger | 未匹配，使用 Root |

#### 3.3.3 Root Logger

```xml
<root level="INFO">
    <appender-ref ref="CONSOLE"/>
    <appender-ref ref="ASYNC_FILE"/>
    <appender-ref ref="ASYNC_ERROR_FILE"/>
</root>
```

**作用**：
- 所有未匹配到具体 Logger 的日志都使用 Root Logger
- 设置全局默认日志级别
- 关联所有 Appender

**运转流程**：

```
日志事件产生
    ↓
匹配 Logger 配置
    ↓
如果未匹配到具体 Logger：
    ↓
    使用 Root Logger
    ↓
    检查 Root Logger 级别
    ↓
    如果通过级别检查：
        ↓
        发送到所有 Root Logger 关联的 Appender：
            - CONSOLE
            - ASYNC_FILE
            - ASYNC_ERROR_FILE
```

### 3.4 Spring Profile 条件配置

```xml
<springProfile name="dev,local">
    <!-- 开发/本地环境配置 -->
</springProfile>

<springProfile name="prod">
    <!-- 生产环境配置 -->
</springProfile>
```

**运转流程**：

```
应用启动
    ↓
读取 application.yml 中的 spring.profiles.active
    ↓
例如：spring.profiles.active=dev
    ↓
解析 logback-spring.xml
    ↓
遇到 <springProfile name="dev,local">
    ↓
检查当前 Profile：dev
    ↓
dev 在 [dev, local] 中：✅ 匹配
    ↓
应用此配置块中的内容
    ↓
遇到 <springProfile name="prod">
    ↓
检查当前 Profile：dev
    ↓
dev != prod：❌ 不匹配
    ↓
忽略此配置块
```

**Profile 匹配规则**：
- `name="dev,local"`：匹配 dev 或 local
- `name="prod"`：只匹配 prod
- `name="!prod"`：匹配除 prod 外的所有环境

## 四、完整日志流转流程

### 4.1 开发环境（dev）日志流转

```
代码执行：log.info("用户注册成功")
    ↓
LoggerFactory.getLogger(UserService.class)
    ↓
获取 Logger：com.github.jredmine.service.UserService
    ↓
查找 Logger 配置：
    - 匹配到：com.github.jredmine (level=DEBUG)
    ↓
级别检查：INFO >= DEBUG ✅
    ↓
创建 LoggingEvent
    ↓
发送到所有关联的 Appender：
    ↓
    ┌─────────────────────────────────────┐
    │ 1. CONSOLE Appender                  │
    │    - 使用 PatternEncoder              │
    │    - 格式化：时间 [线程] 级别 类名 - 消息
    │    - 输出到：System.out               │
    │    - 结果：控制台显示彩色日志          │
    └─────────────────────────────────────┘
    ↓
    ┌─────────────────────────────────────┐
    │ 2. ASYNC_FILE Appender               │
    │    - 放入异步队列（非阻塞）            │
    │    - 后台线程处理                      │
    │    - 调用 FILE Appender                │
    │    - 使用 JSON Encoder                 │
    │    - 写入：logs/jredmine.log           │
    └─────────────────────────────────────┘
    ↓
    ┌─────────────────────────────────────┐
    │ 3. ASYNC_ERROR_FILE Appender         │
    │    - 放入异步队列                      │
    │    - 后台线程处理                      │
    │    - ThresholdFilter 检查：INFO < ERROR
    │    - 过滤：❌ 不通过，不写入错误日志    │
    └─────────────────────────────────────┘
```

### 4.2 生产环境（prod）日志流转

```
代码执行：log.error("系统异常", exception)
    ↓
LoggerFactory.getLogger(GlobalExceptionHandler.class)
    ↓
获取 Logger：com.github.jredmine.exception.handler.GlobalExceptionHandler
    ↓
查找 Logger 配置：
    - 匹配到：com.github.jredmine (level=INFO)
    ↓
级别检查：ERROR >= INFO ✅
    ↓
创建 LoggingEvent（包含异常堆栈）
    ↓
发送到所有关联的 Appender：
    ↓
    ┌─────────────────────────────────────┐
    │ 1. CONSOLE Appender                  │
    │    - 使用 JSON Encoder                │
    │    - 提取：时间戳、级别、消息、堆栈等
    │    - 组装 JSON 对象                   │
    │    - 输出到：System.out（JSON格式）   │
    │    - 结果：容器日志收集系统可解析      │
    └─────────────────────────────────────┘
    ↓
    ┌─────────────────────────────────────┐
    │ 2. ASYNC_FILE Appender               │
    │    - 放入异步队列                      │
    │    - 后台线程处理                      │
    │    - 调用 FILE Appender                │
    │    - 写入：logs/jredmine.log           │
    │    - 检查文件大小：>= 100MB？          │
    │    - 是：触发滚动，压缩旧文件           │
    └─────────────────────────────────────┘
    ↓
    ┌─────────────────────────────────────┐
    │ 3. ASYNC_ERROR_FILE Appender         │
    │    - 放入异步队列                      │
    │    - 后台线程处理                      │
    │    - ThresholdFilter 检查：ERROR >= ERROR
    │    - 过滤：✅ 通过                      │
    │    - 写入：logs/jredmine-error.log    │
    │    - 检查文件大小：>= 100MB？          │
    │    - 是：触发滚动                      │
    └─────────────────────────────────────┘
```

## 五、关键机制详解

### 5.1 MDC（Mapped Diagnostic Context）工作流程

```java
// 代码中
MDC.put("userId", "12345");
log.info("用户操作");
MDC.clear();
```

**运转流程**：

```
MDC.put("userId", "12345")
    ↓
将键值对存储到 ThreadLocal Map 中
    ↓
log.info("用户操作")
    ↓
创建 LoggingEvent
    ↓
LoggingEvent 包含：
    - 消息："用户操作"
    - MDC 上下文：{userId: "12345"}
    ↓
Encoder 处理时：
    ↓
MdcProvider 提取 MDC 上下文
    ↓
添加到 JSON 对象：
    {
        "message": "用户操作",
        "mdc": {
            "userId": "12345"
        }
    }
    ↓
MDC.clear()
    ↓
清理当前线程的 MDC 上下文
```

**注意事项**：
- MDC 使用 ThreadLocal，每个线程独立
- 必须及时清理，避免内存泄漏
- 建议使用 `try-finally` 确保清理

### 5.2 日志级别过滤流程

```
日志事件产生（例如：log.debug("调试信息")）
    ↓
获取 Logger 配置级别（例如：INFO）
    ↓
比较：
    - 请求级别：DEBUG
    - 配置级别：INFO
    - DEBUG < INFO：❌ 不通过
    ↓
丢弃日志事件（不创建 LoggingEvent）
    ↓
不发送到任何 Appender
    ↓
性能优化：级别检查在创建事件之前，避免不必要的对象创建
```

**级别优先级**：
```
TRACE < DEBUG < INFO < WARN < ERROR
```

### 5.3 文件滚动触发时机

**触发条件（任一满足即触发）**：
1. 当前文件大小 >= `maxFileSize`
2. 日期变化（根据 `fileNamePattern` 中的 `%d` 格式）

**检查时机**：
- 每次写入日志时检查
- 使用高效的检查机制，避免频繁 I/O

## 六、性能优化机制

### 6.1 异步日志性能提升

**同步模式性能**：
```
1000 条日志 × 2ms/条 = 2000ms（阻塞业务线程）
```

**异步模式性能**：
```
1000 条日志 × 0.01ms/条 = 10ms（几乎不阻塞）
后台线程批量处理：1000 条 × 2ms = 2000ms（不影响业务）
```

**性能提升**：约 200 倍（在业务线程视角）

### 6.2 日志级别提前过滤

```
代码：log.debug("复杂计算: {}", expensiveOperation())
    ↓
如果 Logger 级别 >= DEBUG：
    ↓
    执行 expensiveOperation()（耗时操作）
    ↓
    创建日志事件
    ↓
如果 Logger 级别 < DEBUG：
    ↓
    直接返回（不执行 expensiveOperation()）
    ↓
    性能优化：避免不必要的计算
```

**最佳实践**：
```java
// ❌ 不推荐：即使不记录日志也会执行计算
log.debug("结果: " + expensiveOperation());

// ✅ 推荐：使用 Lambda，延迟计算
log.debug("结果: {}", () -> expensiveOperation());
```

## 七、配置调试技巧

### 7.1 启用 Logback 内部日志

在 `logback-spring.xml` 中添加：

```xml
<configuration debug="true">
    <!-- 会输出 Logback 内部的调试信息 -->
</configuration>
```

### 7.2 验证配置

```java
@Slf4j
@RestController
public class LogTestController {
    @GetMapping("/test/log")
    public String testLog() {
        log.trace("TRACE 级别日志");
        log.debug("DEBUG 级别日志");
        log.info("INFO 级别日志");
        log.warn("WARN 级别日志");
        log.error("ERROR 级别日志");
        return "请查看控制台和日志文件";
    }
}
```

### 7.3 检查配置生效

```bash
# 检查日志文件
ls -lh logs/

# 查看 JSON 格式
head -1 logs/jredmine.log | jq .

# 查看日志级别分布
grep -o '"level":"[^"]*"' logs/jredmine.log | sort | uniq -c
```

---

**文档创建时间**：2024-01-15  
**适用版本**：Logback, Spring Boot 3.0.4

