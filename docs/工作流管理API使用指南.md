# 工作流管理 API 使用指南

## 📋 概述

工作流管理模块提供了完整的 REST API 接口，用于管理工作流规则和查询任务状态。

## 🔗 API 端点

### 1. 工作流管理

#### 1.1 查询工作流列表

**接口**: `GET /api/workflows`

**权限**: 需要管理员权限

**请求参数**:
- `current` (Integer, 可选): 当前页码，默认 1
- `size` (Integer, 可选): 每页数量，默认 10
- `trackerId` (Integer, 可选): 跟踪器ID
- `oldStatusId` (Integer, 可选): 旧状态ID
- `newStatusId` (Integer, 可选): 新状态ID
- `roleId` (Integer, 可选): 角色ID
- `type` (String, 可选): 类型（'transition' 或 'field'）

**响应示例**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "records": [
      {
        "id": 1,
        "trackerId": 1,
        "trackerName": "Bug",
        "oldStatusId": 1,
        "oldStatusName": "新建",
        "newStatusId": 2,
        "newStatusName": "进行中",
        "roleId": 3,
        "roleName": "开发者",
        "assignee": false,
        "author": false,
        "type": "transition"
      }
    ],
    "total": 1,
    "current": 1,
    "size": 10
  }
}
```

#### 1.2 查询工作流详情

**接口**: `GET /api/workflows/{id}`

**权限**: 需要管理员权限

**路径参数**:
- `id` (Integer): 工作流ID

**响应示例**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "trackerId": 1,
    "trackerName": "Bug",
    "oldStatusId": 1,
    "oldStatusName": "新建",
    "newStatusId": 2,
    "newStatusName": "进行中",
    "roleId": 3,
    "roleName": "开发者",
    "assignee": false,
    "author": false,
    "type": "transition"
  }
}
```

#### 1.3 创建工作流规则

**接口**: `POST /api/workflows`

**权限**: 需要管理员权限

**请求体**:
```json
{
  "trackerId": 1,
  "oldStatusId": 1,
  "newStatusId": 2,
  "roleId": 3,
  "assignee": false,
  "author": false,
  "type": "transition",
  "fieldName": null,
  "rule": null
}
```

**字段说明**:
- `trackerId` (Integer, 必填): 跟踪器ID（0表示所有跟踪器）
- `oldStatusId` (Integer, 必填): 旧状态ID（0表示所有状态）
- `newStatusId` (Integer, 必填): 新状态ID
- `roleId` (Integer, 必填): 角色ID（0表示所有角色）
- `assignee` (Boolean, 可选): 是否只有指派人可以执行，默认 false
- `author` (Boolean, 可选): 是否只有创建者可以执行，默认 false
- `type` (String, 必填): 类型（'transition'=状态转换，'field'=字段规则）
- `fieldName` (String, 可选): 字段名（仅用于字段规则）
- `rule` (String, 可选): 规则（仅用于字段规则：'required'=必填，'readonly'=只读，'hidden'=隐藏）

**响应示例**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "trackerId": 1,
    "trackerName": "Bug",
    "oldStatusId": 1,
    "oldStatusName": "新建",
    "newStatusId": 2,
    "newStatusName": "进行中",
    "roleId": 3,
    "roleName": "开发者",
    "assignee": false,
    "author": false,
    "type": "transition"
  }
}
```

#### 1.4 更新工作流规则

**接口**: `PUT /api/workflows/{id}`

**权限**: 需要管理员权限

**路径参数**:
- `id` (Integer): 工作流ID

**请求体**:
```json
{
  "trackerId": 1,
  "oldStatusId": 1,
  "newStatusId": 2,
  "roleId": 3,
  "assignee": true,
  "author": false,
  "type": "transition"
}
```

**响应示例**: 同创建接口

#### 1.5 删除工作流规则

**接口**: `DELETE /api/workflows/{id}`

**权限**: 需要管理员权限

**路径参数**:
- `id` (Integer): 工作流ID

**响应示例**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": null
}
```

### 2. 状态转换查询

#### 2.1 获取可用的状态转换

**接口**: `GET /api/workflows/transitions`

**权限**: 需要认证

**请求参数**:
- `trackerId` (Integer, 必填): 跟踪器ID
- `currentStatusId` (Integer, 必填): 当前状态ID
- `roleIds` (String, 可选): 用户角色ID列表（逗号分隔，如 "3,4"）

**响应示例**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "currentStatusId": 1,
    "currentStatusName": "新建",
    "availableTransitions": [
      {
        "statusId": 2,
        "statusName": "进行中",
        "assignee": false,
        "author": false
      },
      {
        "statusId": 5,
        "statusName": "已拒绝",
        "assignee": false,
        "author": true
      }
    ]
  }
}
```

### 3. 任务状态管理

#### 3.1 获取所有任务状态列表

**接口**: `GET /api/workflows/statuses`

**权限**: 需要认证

**响应示例**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "name": "新建",
      "description": "任务刚创建",
      "isClosed": false,
      "position": 1,
      "defaultDoneRatio": 0
    },
    {
      "id": 2,
      "name": "进行中",
      "description": "任务正在处理",
      "isClosed": false,
      "position": 2,
      "defaultDoneRatio": 50
    }
  ]
}
```

#### 3.2 查询任务状态详情

**接口**: `GET /api/workflows/statuses/{id}`

**权限**: 需要认证

**路径参数**:
- `id` (Integer): 状态ID

**响应示例**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "name": "新建",
    "description": "任务刚创建",
    "isClosed": false,
    "position": 1,
    "defaultDoneRatio": 0
  }
}
```

## 💡 使用示例

### 示例1：创建 Bug 处理工作流

```bash
# 1. 开发者可以将"新建"转换为"进行中"
curl -X POST http://localhost:8080/api/workflows \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "trackerId": 1,
    "oldStatusId": 1,
    "newStatusId": 2,
    "roleId": 3,
    "type": "transition"
  }'

# 2. 开发者可以将"进行中"转换为"已解决"
curl -X POST http://localhost:8080/api/workflows \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "trackerId": 1,
    "oldStatusId": 2,
    "newStatusId": 3,
    "roleId": 3,
    "type": "transition"
  }'

# 3. 测试人员可以将"已解决"转换为"已关闭"
curl -X POST http://localhost:8080/api/workflows \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "trackerId": 1,
    "oldStatusId": 3,
    "newStatusId": 4,
    "roleId": 4,
    "type": "transition"
  }'
```

### 示例2：创建字段规则

```bash
# 从"新建"转换到"进行中"时，"指派人"字段必填
curl -X POST http://localhost:8080/api/workflows \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "trackerId": 1,
    "oldStatusId": 1,
    "newStatusId": 2,
    "roleId": 3,
    "type": "field",
    "fieldName": "assigned_to",
    "rule": "required"
  }'

# 从"新建"转换到"进行中"时，"描述"字段只读
curl -X POST http://localhost:8080/api/workflows \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "trackerId": 1,
    "oldStatusId": 1,
    "newStatusId": 2,
    "roleId": 3,
    "type": "field",
    "fieldName": "description",
    "rule": "readonly"
  }'
```

### 示例3：查询可用的状态转换

```bash
# 查询 Bug 跟踪器，当前状态为"新建"，用户角色为"开发者"时可用的状态转换
curl -X GET "http://localhost:8080/api/workflows/transitions?trackerId=1&currentStatusId=1&roleIds=3" \
  -H "Authorization: Bearer <token>"
```

## 🔒 权限说明

- **工作流管理接口**（创建、更新、删除、查询列表和详情）：需要管理员权限（`ROLE_ADMIN`）
- **状态转换查询接口**：需要认证，所有已认证用户都可以查询
- **任务状态查询接口**：需要认证，所有已认证用户都可以查询

## 📝 注意事项

1. **工作流类型**：
   - `transition`: 状态转换规则，定义哪些角色可以执行状态转换
   - `field`: 字段规则，定义在状态转换时字段的必填、只读或隐藏规则

2. **特殊值**：
   - `trackerId = 0`: 表示所有跟踪器
   - `oldStatusId = 0`: 表示所有状态
   - `roleId = 0`: 表示所有角色

3. **字段规则**：
   - 只有 `type = 'field'` 时，`fieldName` 和 `rule` 才有效
   - `rule` 的值必须是：`'required'`（必填）、`'readonly'`（只读）或 `'hidden'`（隐藏）

4. **用户限制**：
   - `assignee = true`: 只有任务的指派人可以执行此转换
   - `author = true`: 只有任务的创建者可以执行此转换
   - 两者可以同时为 `true`，表示只有既是创建者又是指派人的用户才能执行

5. **状态转换查询**：
   - 系统会根据跟踪器、当前状态和用户角色，自动匹配所有符合条件的工作流规则
   - 返回所有可以转换到的目标状态列表

