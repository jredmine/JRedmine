# MyBatis-Plus联合主键问题修复说明

## 问题描述

启动应用时报错：
```
@TableId can't more than one in Class: "com.github.jredmine.entity.ProjectTracker".
```

## 问题原因

**MyBatis-Plus 不支持在一个实体类中定义多个 `@TableId` 注解！**

对于联合主键表，MyBatis-Plus 提供了两种解决方案：
1. **方案1（推荐）**: 不使用 `@TableId`，将所有字段作为普通字段，查询时手动指定所有主键条件
2. **方案2**: 创建单独的主键类（较复杂，适用于需要大量 CRUD 操作的场景）

## 修复方案

采用**方案1**，移除 `@TableId` 注解，保留 `@TableName` 注解。

### 修复前

```java
@Data
@TableName("projects_trackers")
public class ProjectTracker {
    @TableId  // ❌ 错误：不能有多个@TableId
    private Long projectId;
    
    @TableId  // ❌ 错误：不能有多个@TableId
    private Long trackerId;
}
```

### 修复后

```java
@Data
@TableName(value = "projects_trackers", autoResultMap = true)
public class ProjectTracker {
    // ✅ 正确：不添加@TableId注解
    private Long projectId;
    private Long trackerId;
}
```

## 修复的实体类

1. **ProjectTracker** - 项目跟踪器关联表
   - 联合主键：`project_id` + `tracker_id`
   
2. **ProjectTemplateRole** - 项目模板角色关联表
   - 联合主键：`project_id` + `role_id`
   
3. **RolesManagedRole** - 角色管理关系表
   - 联合主键：`role_id` + `managed_role_id`

## 使用注意事项

对于没有 `@TableId` 的联合主键表，在进行 CRUD 操作时：

### 1. 查询示例

```java
// 查询时必须同时指定两个主键字段
LambdaQueryWrapper<ProjectTracker> queryWrapper = new LambdaQueryWrapper<>();
queryWrapper.eq(ProjectTracker::getProjectId, projectId)
            .eq(ProjectTracker::getTrackerId, trackerId);
ProjectTracker result = projectTrackerMapper.selectOne(queryWrapper);
```

### 2. 插入示例

```java
// 插入时两个字段都需要设置
ProjectTracker projectTracker = new ProjectTracker();
projectTracker.setProjectId(projectId);
projectTracker.setTrackerId(trackerId);
projectTrackerMapper.insert(projectTracker);
```

### 3. 删除示例

```java
// 删除时必须指定所有主键字段
LambdaQueryWrapper<ProjectTracker> queryWrapper = new LambdaQueryWrapper<>();
queryWrapper.eq(ProjectTracker::getProjectId, projectId)
            .eq(ProjectTracker::getTrackerId, trackerId);
projectTrackerMapper.delete(queryWrapper);
```

### 4. 不支持的操作

```java
// ❌ 错误：不能使用 updateById()，因为没有单一主键
projectTrackerMapper.updateById(projectTracker);

// ❌ 错误：不能使用 selectById()，因为没有单一主键
projectTrackerMapper.selectById(id);

// ❌ 错误：不能使用 deleteById()，因为没有单一主键
projectTrackerMapper.deleteById(id);
```

## MyBatis-Plus 联合主键的两种方案对比

### 方案1：不使用 @TableId（当前采用）

**优点**：
- ✅ 简单直接
- ✅ 适用于简单的关联表
- ✅ 无需额外代码

**缺点**：
- ❌ 不能使用 `xxxById()` 系列方法
- ❌ 需要手动构建查询条件

**适用场景**：
- 多对多关系的中间表
- 不需要频繁更新的关联表
- 主要用于关联查询的表

### 方案2：使用复合主键类

**示例代码**：
```java
// 主键类
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProjectTrackerKey implements Serializable {
    private Long projectId;
    private Long trackerId;
}

// 实体类
@Data
@TableName("projects_trackers")
public class ProjectTracker {
    @TableId(type = IdType.INPUT)
    private ProjectTrackerKey id;
}
```

**优点**：
- ✅ 更符合 JPA 规范
- ✅ 可以使用部分 `xxxById()` 方法
- ✅ 更清晰的主键概念

**缺点**：
- ❌ 需要创建额外的主键类
- ❌ 代码复杂度增加
- ❌ MyBatis-Plus 对此支持有限

## 验证结果

✅ **编译成功** - 移除多余的 `@TableId` 注解后编译通过  
✅ **MyBatis-Plus初始化正常** - 不再报联合主键错误  
✅ **应用可以正常启动**

## 总结

对于JRedmine项目中的联合主键表：
- 采用**方案1**（不使用 @TableId）
- 这些表主要用于关联查询和简单的增删操作
- 在 Mapper 层查询时手动指定所有主键条件即可
- 如果后续需要频繁的复杂操作，可以考虑升级到方案2

## 相关文档

- [MyBatis-Plus官方文档 - 主键策略](https://baomidou.com/pages/223848/)
- [实体类主键注解修复说明](./实体类主键注解修复说明.md)
