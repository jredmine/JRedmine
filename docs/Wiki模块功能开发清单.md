# Wiki 模块功能开发清单

基于现有数据库结构（`wikis`、`wiki_pages`、`wiki_contents`、`wiki_content_versions`、`wiki_redirects`）、实体与 Mapper、权限枚举及搜索集成，整理 Wiki 模块需开发的功能与接口。

---

## 一、现有基础

| 项目 | 状态 | 说明 |
|------|------|------|
| 实体 | ✅ | `Wiki`、`WikiPage`、`WikiContent` |
| Mapper | ✅ | `WikiMapper`、`WikiPageMapper`、`WikiContentMapper` |
| 权限 | ✅ | `view_wiki_pages`、`edit_wiki_pages`、`delete_wiki_pages`、`manage_wiki` |
| 项目模块 | ✅ | `ProjectModule.WIKI`，项目可启用/禁用 Wiki |
| 搜索 | ✅ | 全局搜索已支持按标题、内容搜 Wiki |
| 附件 | ✅ | 附件支持 `containerType=WikiPage` |
| 版本历史表 | ✅ 表存在 | `wiki_content_versions`（需实体 + 业务） |
| 重定向表 | ✅ 表存在 | `wiki_redirects`（需实体 + 业务） |

---

## 二、需开发功能概览

### 1. Wiki 初始化与设置（按项目）

- **项目启用 Wiki 时创建/获取 Wiki**
  - 项目启用 `wiki` 模块时，若该项目尚无 `wikis` 记录，则创建一条 `wikis`（关联 `project_id`，可设 `start_page`、`status`）。
  - 提供「获取项目 Wiki 信息」：返回当前项目的 Wiki 主记录（含 `start_page`、`status` 等），供前端展示与入口页跳转。

- **设置 Wiki 首页（start_page）**
  - 更新 `wikis.start_page`（即首页对应的页面标题）。
  - 权限建议：`manage_wiki` 或项目管理员。

### 2. Wiki 页面（CRUD + 列表）

- **创建页面**
  - 在指定项目的 Wiki 下创建 `wiki_pages`（`wiki_id`、`title`、`parent_id`、`protected` 等）。
  - 可选同时创建首条 `wiki_contents`（内容、评论、version=1）。
  - 同一 `wiki_id` 下 `title` 唯一；若支持层级，则同一 `parent_id` 下子页面标题唯一（按你们产品约定）。
  - 权限：`edit_wiki_pages` 或 `manage_wiki`。

- **获取单个页面（含最新内容）**
  - 按项目标识 + 页面标题（或 slug）查询 `wiki_pages`，并返回最新一条 `wiki_contents`（正文、版本号、作者、更新时间、评论）。
  - 支持「项目标识 + 标题」或「项目 ID + 页面 ID」两种方式之一或都支持。
  - 权限：`view_wiki_pages`。

- **更新页面**
  - 更新页面元数据（如 `parent_id`、`protected`）；并新增一条 `wiki_contents`（新 version），可选写入 `wiki_content_versions` 做历史。
  - 权限：`edit_wiki_pages` 或 `manage_wiki`；若页面 `protected`，可限制为 `manage_wiki`。

- **删除页面**
  - 软删或硬删 `wiki_pages`，并决定是否级联处理 `wiki_contents`、附件、重定向。
  - 权限：`delete_wiki_pages` 或 `manage_wiki`。

- **页面列表（树形或平铺）**
  - 按项目查该 Wiki 下所有 `wiki_pages`，支持平铺列表或按 `parent_id` 树形结构；可分页。
  - 列表项可含：标题、父页面、是否保护、创建/更新时间、作者等（按需）。
  - 权限：`view_wiki_pages`。

### 3. Wiki 版本历史

- **查看某页面的版本列表**
  - 按 `page_id` 查 `wiki_contents` 的多个版本（或关联 `wiki_content_versions`），返回版本号、作者、更新时间、评论。
  - 权限：`view_wiki_pages`。

- **查看指定版本内容**
  - 按 `page_id` + `version` 返回该版本的正文（及作者、时间、评论）。
  - 权限：`view_wiki_pages`。

- **可选：回滚到某版本**
  - 将指定版本内容作为当前内容，插入新一条 `wiki_contents`（version+1），并可选写入 `wiki_content_versions`。
  - 权限：`edit_wiki_pages` 或 `manage_wiki`。

### 4. Wiki 重定向

- **创建重定向**
  - 插入 `wiki_redirects`：`wiki_id`、原 `title`、`redirects_to`（目标标题）、`redirects_to_wiki_id`（同项目一般等于 `wiki_id`）。
  - 访问「原标题」时，接口层或服务层解析为重定向目标并返回目标页面或 302。
  - 权限：`manage_wiki` 或 `edit_wiki_pages`（按你们策略）。

- **解析重定向**
  - 获取页面时，若当前标题在 `wiki_redirects` 中存在，则返回重定向信息或直接返回目标页内容。
  - 可与「获取单个页面」合并实现。

- **删除/列表重定向**
  - 列出某 Wiki 的重定向；删除某条重定向。
  - 权限：`manage_wiki`。

### 5. 权限与可见性

- **项目级**
  - 仅当项目已启用 `wiki` 模块时，所有 Wiki 接口才对该项目有效；否则 404 或 403。
  - 使用现有 `EnabledModule`/`ProjectModule.WIKI` 判断。

- **权限校验**
  - 所有接口按角色/权限校验：`view_wiki_pages`、`edit_wiki_pages`、`delete_wiki_pages`、`manage_wiki`（与现有权限体系一致）。
  - 保护页（`wiki_pages.protected`）：仅 `manage_wiki` 可编辑/删除（可选策略）。

- **私有项目**
  - 若项目为私有，则 Wiki 仅项目成员可见（与现有项目可见性一致）。

### 6. 附件与全文搜索（已具备，需约定）

- **附件**
  - 上传/下载/删除附件时，`containerType=WikiPage`、`containerId=wiki_pages.id`；编辑页时展示该页附件列表。
  - 无需新增接口，只需前端与文档说明。

- **搜索**
  - 全局搜索已支持类型 `wiki`，按标题、内容检索；保持现有接口不变，确保 Wiki 创建/更新后能搜到即可。

### 7. 其他可选功能

- **页面层级（父子）**
  - 列表/树接口中已包含 `parent_id`；创建/更新时支持指定 `parent_id`，前端可展示为树或面包屑。

- **Wiki 导出**
  - 按项目导出全部页面为 HTML/Markdown/PDF（可选，后续迭代）。

- **Wiki 模板**
  - 新建页时可选模板（如「会议纪要」「需求说明」），即预填内容模板（可选）。

---

## 三、建议的 API 清单（REST）

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/api/projects/{projectIdOrIdentifier}/wiki` | 获取项目 Wiki 信息（含 start_page） | view_wiki_pages |
| PUT | `/api/projects/{projectIdOrIdentifier}/wiki` | 设置 Wiki 首页等 | manage_wiki |
| GET | `/api/projects/{projectIdOrIdentifier}/wiki/pages` | 页面列表（树或平铺，分页） | view_wiki_pages |
| POST | `/api/projects/{projectIdOrIdentifier}/wiki/pages` | 创建页面 | edit_wiki_pages |
| GET | `/api/projects/{projectIdOrIdentifier}/wiki/pages/{titleOrId}` | 获取页面（含最新内容，支持重定向） | view_wiki_pages |
| PUT | `/api/projects/{projectIdOrIdentifier}/wiki/pages/{titleOrId}` | 更新页面（新增内容版本） | edit_wiki_pages |
| DELETE | `/api/projects/{projectIdOrIdentifier}/wiki/pages/{titleOrId}` | 删除页面 | delete_wiki_pages |
| GET | `/api/projects/{projectIdOrIdentifier}/wiki/pages/{titleOrId}/versions` | 版本列表 | view_wiki_pages |
| GET | `/api/projects/{projectIdOrIdentifier}/wiki/pages/{titleOrId}/versions/{version}` | 指定版本内容 | view_wiki_pages |
| POST | `/api/projects/{projectIdOrIdentifier}/wiki/pages/{titleOrId}/versions/{version}/revert` | 回滚到某版本（可选） | edit_wiki_pages |
| GET | `/api/projects/{projectIdOrIdentifier}/wiki/redirects` | 重定向列表（可选） | manage_wiki |
| POST | `/api/projects/{projectIdOrIdentifier}/wiki/redirects` | 创建重定向 | manage_wiki |
| DELETE | `/api/projects/{projectIdOrIdentifier}/wiki/redirects/{id}` | 删除重定向（可选） | manage_wiki |

说明：`titleOrId` 可用 slug（URL 友好标题）或数字 ID；重定向在「获取页面」时由后端解析即可，前端可无感。

---

## 四、实体与表补充建议

- **wiki_content_versions**  
  - 若要做完整历史与回滚，建议增加实体 `WikiContentVersion` 和 Mapper，在每次保存内容时写入一条版本快照（或仅对「发布」版本写入，视产品而定）。

- **wiki_redirects**  
  - 建议增加实体 `WikiRedirect` 和 Mapper，用于重定向的 CRUD 与解析。

---

## 五、开发顺序建议

1. **Phase 1：基础 CRUD**
   - Wiki 初始化（项目启用模块时创建/获取 wikis）。
   - 页面列表、创建、获取（含最新内容）、更新、删除。
   - 权限与项目模块校验。

2. **Phase 2：版本历史**
   - 版本列表、查看指定版本；可选回滚与 `wiki_content_versions` 落库。

3. **Phase 3：重定向**
   - `WikiRedirect` 实体 + Mapper，创建/解析/列表/删除。

4. **Phase 4：增强**
   - 树形列表、保护页策略、Wiki 首页设置、导出等。

---

## 六、小结

| 类别 | 功能 | 优先级 |
|------|------|--------|
| Wiki 设置 | 获取/设置项目 Wiki、首页 | P0 |
| 页面 | 列表、创建、获取、更新、删除 | P0 |
| 权限 | 项目模块启用 + view/edit/delete/manage_wiki | P0 |
| 版本历史 | 版本列表、查看版本、回滚（可选） | P1 |
| 重定向 | 创建、解析、列表/删除 | P1 |
| 附件/搜索 | 沿用现有附件与搜索，约定 container 与类型 | P0（约定） |
| 增强 | 树形、保护页、导出、模板 | P2 |

当前代码中已有 Wiki 实体、Mapper、权限与搜索集成，缺失的是 **WikiController、WikiService、DTO 以及可选的 WikiContentVersion/WikiRedirect 实体与版本与重定向业务逻辑**。按上述清单可实现完整的 Wiki 模块并与现有项目、权限、搜索、附件体系一致。
