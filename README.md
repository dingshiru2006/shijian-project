# 知库 · 企业知识库平台（KB-PLATFORM）

一个基于 **Spring Boot 3.3 + Vue 3 + Elasticsearch + RabbitMQ + Redis + MySQL** 的全栈企业知识库管理系统。
覆盖 JWT 无状态认证、RBAC 权限模型、AOP 操作日志、ES 全文检索（高亮）、MQ 异步索引重建等完整后端技术栈。

## 一、技术栈

| 层 | 技术 |
|---|---|
| 前端 | Vue 3.5 + Element Plus 2.9 + Vue Router 4 + Pinia + Axios + Vite 5 |
| 后端 | Spring Boot 3.3.5 + Spring Security 6.3 + Spring Data JPA + JJWT 0.12 |
| 数据库 | MySQL 8.0（Hibernate 6.5，`ddl-auto=none` + 手工 SQL） |
| 缓存/登录态 | Redis 7.2（Token 黑名单式 jti 校验 + 登录用户缓存） |
| 消息队列 | RabbitMQ 3.13（直连交换机 + 持久队列 + 手动 ACK） |
| 搜索引擎 | Elasticsearch 8.18.1（elasticsearch-java Client） |
| 中间件编排 | Docker Compose（端口参数化，避免与宿主冲突） |
| JDK / Node | Java 17+（开发用 21）/ Node 18+（开发用 22） |

## 二、功能模块与作业要求对照

### 1. 项目骨架与规范
- **统一响应格式**：`Result{code, message, data, timestamp}` / `PageResult{list, total, pageNum, pageSize, pages}`（`common/` 包）
- **参数校验**：Jakarta Validation + `GlobalExceptionHandler` 全局异常处理（业务异常 / 校验失败 / 401 / 403 / 500 分流）
- **Spring Data JPA**：Specification 组合条件查询、投影接口统计、审计字段自动填充（`BaseEntity`）

### 2. 自设计数据库（13 张表）
- 系统域：`sys_user`、`sys_role`、`sys_permission`、`sys_role_permission`、`sys_user_role`、`sys_operation_log`
- 业务域：`kb_knowledge_base`、`kb_category`、`kb_tag`、`kb_document`、`kb_document_tag`、`kb_file`
- 检索域：`es_rebuild_task`
- 完整 DDL + 种子数据见 `sql/init.sql`（37 权限 / 3 角色 / 67 角色-权限关联 / 3 用户 / 12 文档 / 22 条日志）

### 3. 核心业务 CRUD + 分页（两个以上模块）
- 知识库管理：CRUD、删除级联清理文档与 ES 索引、文档数动态统计
- 文档管理：CRUD、标签替换、ES 实时同步、知识库迁移时文档数联动
- 分类管理 / 标签管理：CRUD、被引用拒删保护
- 全部列表支持组合条件 + 分页（`pageNum/pageSize` + 各模块筛选字段）

### 4. 密码加密 + JWT + Redis 登录态 + 退出立即失效
- BCrypt 加密存储（`PasswordEncoder`）
- JWT：jjwt HS384，载荷含 `sub/uid/jti/iat/exp`，有效期 2 小时
- **登录态存 Redis**：`auth:token:{userId}=jti`、`auth:user:{userId}=LoginUser` 缓存
- **退出后 Token 立即失效**：过滤器每次请求比对 Redis jti，logout 删除键即失效
- 单点登录：同一用户再次登录将使旧 Token 失效（jti 被覆盖）

### 5. RBAC（用户/角色/权限管理及分配）
- 用户：CRUD、分配角色、管理员重置密码、禁删自己/超级管理员
- 角色：CRUD、树形分配权限、被用户引用拒删
- 权限：CRUD、模块列表、被角色引用拒删
- 后端 `@PreAuthorize` 方法级鉴权 + 前端菜单/按钮双重控制（`v-permission` 指令）

### 6. AOP 操作日志
- `@OperationLog` 注解 + `@Around` 切面 + `@Async` 异步落库
- 记录：模块、类型、描述、方法、URI、请求方式、参数（截断 2000 字）、操作人、IP、耗时、成败、异常
- 登录接口回退反射 DTO 取用户名；支持按模块/类型/操作人/结果/时间范围组合查询

### 7. Elasticsearch 全文检索
- 关键词权重检索（标题^3、摘要^2、正文）+ 知识库/分类/标签/状态/时间范围组合筛选
- `<em>` 高亮（标题/摘要/正文分别配置片段数）
- 文档增删改实时同步索引；启动时索引自检（空索引自动全量索引）

### 8. RabbitMQ 异步索引全量重建
- 提交任务 → 落库 → 发 MQ 消息 → 消费者手动 ACK → 清空索引 → 分批 bulk 全量索引 → 刷新
- 幂等控制（仅处理待执行任务）、进度/结果可查询（任务列表 + 详情）

## 三、快速开始

### 1. 启动中间件（Docker Compose）

```bash
docker compose up -d
# 如需避开宿主端口冲突：
KB_MYSQL_PORT=3307 KB_REDIS_PORT=6380 KB_RABBIT_PORT=5673 KB_RABBIT_MGMT_PORT=15673 KB_ES_PORT=9201 docker compose up -d
```

默认端口：MySQL 3306 / Redis 6379 / RabbitMQ 5672（管理台 15672）/ ES 9200。
凭据：MySQL `root/root1234`，Redis `redis1234`，RabbitMQ `kbadmin/kb123456`，ES 关闭安全认证。

### 2. 初始化数据库

```bash
docker exec -i kb-mysql mysql -uroot -proot1234 < sql/init.sql
```

> ⚠️ `init.sql` 含 `DROP TABLE IF EXISTS`，执行前请备份已有数据。

### 3. 启动后端

```bash
cd backend
mvn clean package -DskipTests
java -jar target/kb-system.jar
# 如中间件端口有调整：
MYSQL_PORT=3307 REDIS_PORT=6380 java -jar target/kb-system.jar
```

- 服务地址：`http://localhost:8080`，context-path `/api`
- 连接参数均支持环境变量覆盖：`MYSQL_HOST/MYSQL_PORT/MYSQL_USER/MYSQL_PASS`、`REDIS_HOST/REDIS_PORT/REDIS_PASS`、`ES_URIS`、`RABBIT_HOST/RABBIT_PORT/...`
- 启动后自动创建 ES 索引；索引为空且库中有文档时自动全量索引

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev        # http://localhost:5173（/api 代理到 8080）
# 生产构建：
npm run build      # 产物在 dist/，可用 npx vite preview 预览
```

### 5. 测试账号

| 账号 | 密码 | 角色 | 说明 |
|---|---|---|---|
| admin | Admin@123 | ADMIN | 超级管理员，全部权限 |
| zhangsan | Editor@123 | EDITOR | 知识库编辑 |
| lisi | Li@123 | VIEWER | 知识库访客（只读） |

### 6. API 全链路自动化测试

```bash
# 先后端已启动
bash scripts/api-test.sh http://localhost:8080/api 3307 6380
```

覆盖 8 大组共 39 项断言：认证（错误密码/正确登录/携带 Token/无 Token 401/Redis 登录态）、RBAC（访客 403/读权限）、CRUD（知识库+文档增删改查）、用户/角色/权限管理（含分配）、AOP 日志、ES 检索（关键词/英文/组合筛选/中文）、MQ 异步重建、退出 Token 失效。

## 四、项目结构

```
├── docker-compose.yml          # 4 中间件一键编排
├── sql/init.sql                # 13 表 DDL + 种子数据
├── scripts/api-test.sh         # API 全链路测试（39 断言）
├── backend/                    # Spring Boot 后端
│   ├── pom.xml
│   └── src/main/java/com/kb/system/
│       ├── common/             # 统一响应/分页/异常/审计基类
│       ├── config/             # Security/Redis/RabbitMQ/Jackson/Async
│       ├── security/           # JWT 工具/过滤器/登录主体
│       ├── aop/                # 操作日志切面
│       └── module/
│           ├── system/         # auth/user/role/permission/log/dashboard
│           ├── kb/             # knowledgebase/document/category/tag/file
│           └── search/         # ES 检索/索引重建/消费者
└── frontend/                   # Vue 3 前端
    └── src/
        ├── api/                # 请求实例 + 各域 API
        ├── router/             # 路由 + 权限元信息
        ├── stores/             # Pinia 用户会话
        ├── layout/             # 主布局（侧边栏菜单按权限过滤）
        ├── utils/              # v-permission 指令
        └── views/              # 13 个页面
```

## 五、API 摘要

| 模块 | 端点 |
|---|---|
| 认证 | `POST /api/auth/login`、`GET /api/auth/info`、`POST /api/auth/logout`、`PUT /api/auth/password` |
| 仪表盘 | `GET /api/dashboard/stats` |
| 知识库 | `GET/POST /api/kb/knowledge-bases`、`GET/PUT/DELETE /api/kb/knowledge-bases/{id}`、`GET /api/kb/knowledge-bases/all` |
| 文档 | `GET/POST /api/kb/documents`、`GET/PUT/DELETE /api/kb/documents/{id}` |
| 分类/标签 | `GET/POST /api/kb/categories`、`.../{id}`；`GET/POST /api/kb/tags`、`.../{id}` |
| 检索 | `GET /api/search/documents?keyword=&kbId=&categoryId=&tagName=&status=&startDate=&endDate=` |
| 索引重建 | `POST /api/rebuild/tasks`、`GET /api/rebuild/tasks`、`GET /api/rebuild/tasks/{id}` |
| 用户 | `GET/POST /api/system/users`、`PUT /api/system/users/{id}`、`DELETE .../{id}`、`PUT .../{id}/roles`、`PUT .../{id}/password` |
| 角色 | `GET/POST /api/system/roles`、`PUT .../{id}`、`DELETE .../{id}`、`PUT .../{id}/permissions` |
| 权限 | `GET/POST /api/system/permissions`、`GET /api/system/permissions/modules`、`PUT/DELETE .../{id}` |
| 日志 | `GET /api/system/logs`、`GET .../{id}`、`DELETE .../{id}` |

所有请求经 `Authorization: Bearer <token>` 鉴权（登录接口除外）。
