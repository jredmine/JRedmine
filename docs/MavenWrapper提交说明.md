# Maven Wrapper 文件提交说明

## 一、Maven Wrapper 文件说明

Maven Wrapper（mvnw）允许项目在没有全局安装 Maven 的情况下构建，并确保所有开发者使用相同的 Maven 版本。

## 二、应该提交的文件

### ✅ 必须提交的文件

| 文件 | 说明 | 是否已提交 |
|------|------|-----------|
| `mvnw` | Unix/Linux/Mac 可执行脚本 | ✅ 已提交 |
| `mvnw.cmd` | Windows 可执行脚本 | ✅ 已提交 |
| `.mvn/wrapper/maven-wrapper.properties` | Maven 版本配置文件 | ✅ 已提交 |
| `.mvn/wrapper/maven-wrapper.jar` | Wrapper JAR 文件（二进制） | ⚠️ 未提交 |
| `.mvn/wrapper/MavenWrapperDownloader.java` | Wrapper 下载器源码 | ⚠️ 未提交 |

## 三、为什么应该提交这些文件？

### 3.1 优势

1. **构建一致性**：确保所有开发者使用相同的 Maven 版本
2. **无需本地安装**：开发者不需要在本地安装 Maven
3. **CI/CD 友好**：CI/CD 环境可以直接使用，无需额外配置
4. **离线构建**：`maven-wrapper.jar` 已包含，支持离线构建

### 3.2 最佳实践

根据 Maven 官方文档和业界最佳实践：
- ✅ **所有 Maven Wrapper 文件都应该提交到版本控制**
- ✅ 包括二进制文件 `maven-wrapper.jar`
- ✅ 这样可以确保项目在任何环境下都能一致构建

## 四、当前状态

### 4.1 已提交的文件
- `mvnw`
- `mvnw.cmd`
- `.mvn/wrapper/maven-wrapper.properties`

### 4.2 需要提交的文件
- `.mvn/wrapper/maven-wrapper.jar`（二进制文件）
- `.mvn/wrapper/MavenWrapperDownloader.java`（源码文件）

### 4.3 被修改的文件
- `mvnw`（可能有格式或内容更新）
- `mvnw.cmd`（可能有格式或内容更新）

## 五、.gitignore 配置

当前 `.gitignore` 配置：
```gitignore
!.mvn/wrapper/maven-wrapper.jar
```

这个配置表示：
- `.mvn/` 目录默认被忽略
- 但 `maven-wrapper.jar` 应该被提交（使用 `!` 排除忽略）

## 六、建议操作

### 6.1 提交缺失的文件

```bash
# 添加 Maven Wrapper 相关文件
git add .mvn/wrapper/maven-wrapper.jar
git add .mvn/wrapper/MavenWrapperDownloader.java

# 如果 mvnw 文件有重要更新，也提交
git add mvnw mvnw.cmd

# 提交
git commit -m "chore: 添加 Maven Wrapper 缺失文件"
```

### 6.2 验证

提交后，其他开发者克隆项目后应该能够：
```bash
# 直接使用 mvnw 构建，无需安装 Maven
./mvnw clean install

# Windows
mvnw.cmd clean install
```

## 七、注意事项

1. **二进制文件大小**：`maven-wrapper.jar` 通常只有几十 KB，提交到 Git 是合理的
2. **文件权限**：确保 `mvnw` 有执行权限（`chmod +x mvnw`）
3. **跨平台兼容**：同时提交 `mvnw`（Unix）和 `mvnw.cmd`（Windows）确保跨平台支持

---

**结论**：**应该提交所有 Maven Wrapper 相关文件**，包括二进制文件 `maven-wrapper.jar`。

