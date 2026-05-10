## 修复说明：ID字段0值问题

### 问题描述
在调用甘特图接口时，前端如果传入了ID类型参数的默认值0（如`versionId=0&trackerId=0&statusId=0&assignedToId=0`），会导致查询条件错误地添加了`WHERE version_id = 0`等条件，从而查不到任何数据。

### 原因分析
原代码只判断了参数是否为`null`，没有考虑前端可能传入`0`作为"不筛选"的语义：

```java
// 原代码 - 有问题
if (requestDTO.getVersionId() != null) {
    queryWrapper.eq(Issue::getFixedVersionId, requestDTO.getVersionId());
}
```

当前端传入`versionId=0`时，`requestDTO.getVersionId()`不为null，会添加`WHERE fixed_version_id = 0`条件，但数据库中不存在ID为0的版本。

### 修复方案
修改判断条件，同时检查不为`null`且不为`0`：

```java
// 修复后的代码
if (requestDTO.getVersionId() != null && requestDTO.getVersionId() > 0) {
    queryWrapper.eq(Issue::getFixedVersionId, requestDTO.getVersionId());
}
if (requestDTO.getTrackerId() != null && requestDTO.getTrackerId() > 0) {
    queryWrapper.eq(Issue::getTrackerId, requestDTO.getTrackerId());
}
if (requestDTO.getStatusId() != null && requestDTO.getStatusId() > 0) {
    queryWrapper.eq(Issue::getStatusId, requestDTO.getStatusId());
}
if (requestDTO.getAssignedToId() != null && requestDTO.getAssignedToId() > 0) {
    queryWrapper.eq(Issue::getAssignedToId, requestDTO.getAssignedToId());
}
```

### 影响的参数
- `versionId` (Long)
- `trackerId` (Integer)
- `statusId` (Integer)
- `assignedToId` (Long)

### 测试验证

#### 测试1：传入0值不筛选
```bash
curl -X GET "http://localhost:8080/api/issues/gantt?projectId=1&versionId=0&trackerId=0&statusId=0&assignedToId=0" \
  -H "Authorization: Bearer {token}"
```

**预期结果**：
- ✅ 返回项目1的所有任务（不进行任何ID字段筛选）
- ✅ totalCount > 0

#### 测试2：传入null不筛选
```bash
curl -X GET "http://localhost:8080/api/issues/gantt?projectId=1" \
  -H "Authorization: Bearer {token}"
```

**预期结果**：
- ✅ 返回项目1的所有任务（不进行任何ID字段筛选）
- ✅ totalCount > 0

#### 测试3：传入有效ID进行筛选
```bash
curl -X GET "http://localhost:8080/api/issues/gantt?projectId=1&versionId=1&trackerId=2&statusId=1" \
  -H "Authorization: Bearer {token}"
```

**预期结果**：
- ✅ 只返回满足条件的任务
- ✅ 所有返回的任务的versionId=1, trackerId=2, statusId=1

#### 测试4：混合0值和有效值
```bash
curl -X GET "http://localhost:8080/api/issues/gantt?projectId=1&versionId=1&trackerId=0&statusId=0" \
  -H "Authorization: Bearer {token}"
```

**预期结果**：
- ✅ 只按versionId=1筛选，不按trackerId和statusId筛选
- ✅ 返回versionId=1的所有任务

### 兼容性说明

此修复完全向后兼容：

1. **原有行为保持不变**：
   - 不传参数 → 不筛选 ✓
   - 传null → 不筛选 ✓
   - 传有效ID → 正常筛选 ✓

2. **新增支持**：
   - 传0 → 不筛选（新增支持）✓

### 最佳实践建议

#### 前端调用建议

**方式1：不传不需要的参数（推荐）**
```javascript
// 只查询项目1的任务，不进行其他筛选
const params = {
  projectId: 1
};
```

**方式2：传0表示不筛选（也支持）**
```javascript
// 明确传0表示不筛选
const params = {
  projectId: 1,
  versionId: 0,
  trackerId: 0,
  statusId: 0,
  assignedToId: 0
};
```

**方式3：动态构建参数对象（推荐）**
```javascript
const params = {
  projectId: 1
};

// 只在有值时添加筛选条件
if (selectedVersionId > 0) {
  params.versionId = selectedVersionId;
}
if (selectedTrackerId > 0) {
  params.trackerId = selectedTrackerId;
}
if (selectedStatusId > 0) {
  params.statusId = selectedStatusId;
}
```

### 修复文件列表
- ✅ `IssueService.java` - 修改筛选条件判断逻辑
- ✅ `任务甘特图接口使用指南.md` - 更新参数说明
- ✅ `任务甘特图功能实现总结.md` - 添加ID字段处理说明
- ✅ `ID字段0值问题修复说明.md` - 本文档

### 版本信息
- **修复日期**: 2026-01-11
- **修复版本**: v1.0.1
- **问题发现**: 用户测试反馈
- **修复人**: AI Assistant
