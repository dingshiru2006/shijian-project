-- =====================================================================
-- 知库 · 企业知识库平台 —— 数据库初始化脚本
-- 数据库：MySQL 8.0+  字符集：utf8mb4
--
-- !! 警告：本脚本会删除并重建本项目的 13 张表（DROP TABLE IF EXISTS）。
-- !! 请勿在含有其他重要数据的数据库上执行；执行前请先备份。
--
-- 执行方式：
--   mysql -u root -p kb_system < sql/init.sql
-- 或（Docker）：
--   docker exec -i kb-mysql mysql -uroot -proot1234 kb_system < sql/init.sql
--
-- 测试账号（密码均已 BCrypt 加密存储）：
--   admin    / Admin@123  超级管理员（全部权限）
--   zhangsan / Zhang@123  知识库编辑（知识库域读写 + 检索）
--   lisi     / Li@123     知识库访客（只读 + 检索）
-- =====================================================================

CREATE DATABASE IF NOT EXISTS kb_system
    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE kb_system;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ---------- 删除旧表（按外键依赖反序） ----------
DROP TABLE IF EXISTS es_rebuild_task;
DROP TABLE IF EXISTS sys_operation_log;
DROP TABLE IF EXISTS kb_document_tag;
DROP TABLE IF EXISTS kb_document;
DROP TABLE IF EXISTS kb_file;
DROP TABLE IF EXISTS sys_role_permission;
DROP TABLE IF EXISTS sys_user_role;
DROP TABLE IF EXISTS sys_permission;
DROP TABLE IF EXISTS sys_role;
DROP TABLE IF EXISTS sys_user;
DROP TABLE IF EXISTS kb_tag;
DROP TABLE IF EXISTS kb_category;
DROP TABLE IF EXISTS kb_knowledge_base;

-- =====================================================================
-- 一、系统管理域
-- =====================================================================

CREATE TABLE sys_user (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    username      VARCHAR(50)  NOT NULL COMMENT '登录账号',
    password      VARCHAR(100) NOT NULL COMMENT 'BCrypt 加密密码',
    nickname      VARCHAR(50)  DEFAULT NULL COMMENT '昵称',
    email         VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    phone         VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    avatar        VARCHAR(255) DEFAULT NULL COMMENT '头像地址',
    status        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
    last_login_at DATETIME     DEFAULT NULL COMMENT '最近登录时间',
    last_login_ip VARCHAR(50)  DEFAULT NULL COMMENT '最近登录 IP',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_user_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '系统用户表';

CREATE TABLE sys_role (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    role_code   VARCHAR(50) NOT NULL COMMENT '角色编码：ADMIN/EDITOR/VIEWER',
    role_name   VARCHAR(50) NOT NULL COMMENT '角色名称',
    description VARCHAR(255) DEFAULT NULL COMMENT '角色描述',
    status      TINYINT     NOT NULL DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
    sort        INT         NOT NULL DEFAULT 0 COMMENT '显示排序',
    created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_role_code (role_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '系统角色表';

CREATE TABLE sys_permission (
    id         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    perm_code  VARCHAR(100) NOT NULL COMMENT '权限编码，如 kb:doc:add',
    perm_name  VARCHAR(50) NOT NULL COMMENT '权限名称',
    module     VARCHAR(50) NOT NULL COMMENT '所属模块',
    type       TINYINT     NOT NULL DEFAULT 2 COMMENT '类型：1-菜单，2-按钮',
    route_path VARCHAR(100) DEFAULT NULL COMMENT '前端路由路径',
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_permission_code (perm_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '系统功能权限表';

CREATE TABLE sys_user_role (
    id         BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id    BIGINT   NOT NULL COMMENT '用户 ID',
    role_id    BIGINT   NOT NULL COMMENT '角色 ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_user_role (user_id, role_id),
    KEY idx_sys_user_role_user (user_id),
    KEY idx_sys_user_role_role (role_id),
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES sys_role (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户与角色关联表';

CREATE TABLE sys_role_permission (
    id            BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    role_id       BIGINT   NOT NULL COMMENT '角色 ID',
    permission_id BIGINT   NOT NULL COMMENT '权限 ID',
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_role_permission (role_id, permission_id),
    KEY idx_sys_role_permission_role (role_id),
    KEY idx_sys_role_permission_perm (permission_id),
    CONSTRAINT fk_role_perm_role FOREIGN KEY (role_id) REFERENCES sys_role (id) ON DELETE CASCADE,
    CONSTRAINT fk_role_perm_perm FOREIGN KEY (permission_id) REFERENCES sys_permission (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '角色与权限关联表';

CREATE TABLE sys_operation_log (
    id             BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    module         VARCHAR(50)  DEFAULT NULL COMMENT '功能模块',
    operation_type VARCHAR(20)  DEFAULT NULL COMMENT '操作类型',
    description    VARCHAR(255) DEFAULT NULL COMMENT '操作描述',
    method         VARCHAR(200) DEFAULT NULL COMMENT '请求方法',
    request_uri    VARCHAR(255) DEFAULT NULL COMMENT '请求 URI',
    request_method VARCHAR(10)  DEFAULT NULL COMMENT '请求方式',
    request_params TEXT COMMENT '请求参数（JSON，超长截断）',
    operator       VARCHAR(50)  DEFAULT NULL COMMENT '操作人账号',
    operator_id    BIGINT       DEFAULT NULL COMMENT '操作人 ID',
    operator_ip    VARCHAR(50)  DEFAULT NULL COMMENT '操作人 IP',
    status         TINYINT      NOT NULL DEFAULT 1 COMMENT '结果：1-成功，0-失败',
    error_msg      VARCHAR(500) DEFAULT NULL COMMENT '异常信息',
    cost_time      BIGINT       DEFAULT NULL COMMENT '耗时（毫秒）',
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_sys_operation_log_created (created_at),
    KEY idx_sys_operation_log_operator (operator_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '系统操作日志表（AOP 记录）';

-- =====================================================================
-- 二、知识库业务域
-- =====================================================================

CREATE TABLE kb_knowledge_base (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    name        VARCHAR(100) NOT NULL COMMENT '知识库名称',
    code        VARCHAR(50) NOT NULL COMMENT '知识库编码（唯一）',
    description VARCHAR(500) DEFAULT NULL COMMENT '知识库简介',
    owner_id    BIGINT      NOT NULL COMMENT '负责人 ID',
    visibility  TINYINT     NOT NULL DEFAULT 1 COMMENT '可见性：1-公开，2-私有',
    doc_count   INT         NOT NULL DEFAULT 0 COMMENT '文档数量',
    status      TINYINT     NOT NULL DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
    created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_kb_code (code),
    KEY idx_kb_owner (owner_id),
    CONSTRAINT fk_kb_owner FOREIGN KEY (owner_id) REFERENCES sys_user (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '知识库表';

CREATE TABLE kb_category (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    name        VARCHAR(50) NOT NULL COMMENT '分类名称（唯一）',
    description VARCHAR(255) DEFAULT NULL COMMENT '分类描述',
    sort        INT         NOT NULL DEFAULT 0 COMMENT '显示排序',
    status      TINYINT     NOT NULL DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
    created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_kb_category_name (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '知识库分类表';

CREATE TABLE kb_tag (
    id         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    name       VARCHAR(50) NOT NULL COMMENT '标签名称（唯一）',
    color      VARCHAR(20) DEFAULT '#409EFF' COMMENT '标签颜色',
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_kb_tag_name (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '知识库标签表';

CREATE TABLE kb_file (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    original_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
    stored_name   VARCHAR(100) NOT NULL COMMENT '存储文件名（UUID）',
    file_path     VARCHAR(500) NOT NULL COMMENT '存储路径（相对上传目录）',
    file_size     BIGINT       NOT NULL COMMENT '文件大小（字节）',
    file_type     VARCHAR(100) DEFAULT NULL COMMENT '文件类型（MIME）',
    uploader_id   BIGINT       NOT NULL COMMENT '上传人 ID',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_kb_file_uploader (uploader_id),
    CONSTRAINT fk_kb_file_uploader FOREIGN KEY (uploader_id) REFERENCES sys_user (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '附件文件元数据表';

CREATE TABLE kb_document (
    id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    kb_id       BIGINT        NOT NULL COMMENT '所属知识库 ID',
    category_id BIGINT        NOT NULL COMMENT '所属分类 ID',
    title       VARCHAR(200)  NOT NULL COMMENT '文档标题',
    summary     VARCHAR(500)  DEFAULT NULL COMMENT '文档摘要',
    content     LONGTEXT      COMMENT '文档正文',
    file_id     BIGINT        DEFAULT NULL COMMENT '关联附件文件 ID',
    author_id   BIGINT        NOT NULL COMMENT '作者 ID',
    view_count  INT           NOT NULL DEFAULT 0 COMMENT '浏览次数',
    status      TINYINT       NOT NULL DEFAULT 1 COMMENT '状态：1-已发布，0-草稿',
    created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_kb_document_kb (kb_id),
    KEY idx_kb_document_category (category_id),
    KEY idx_kb_document_author (author_id),
    KEY idx_kb_document_status (status),
    KEY idx_kb_document_created (created_at),
    CONSTRAINT fk_doc_kb FOREIGN KEY (kb_id) REFERENCES kb_knowledge_base (id) ON DELETE CASCADE,
    CONSTRAINT fk_doc_category FOREIGN KEY (category_id) REFERENCES kb_category (id),
    CONSTRAINT fk_doc_author FOREIGN KEY (author_id) REFERENCES sys_user (id),
    CONSTRAINT fk_doc_file FOREIGN KEY (file_id) REFERENCES kb_file (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '知识库文档表';

CREATE TABLE kb_document_tag (
    id          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    document_id BIGINT   NOT NULL COMMENT '文档 ID',
    tag_id      BIGINT   NOT NULL COMMENT '标签 ID',
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_kb_document_tag (document_id, tag_id),
    KEY idx_kb_document_tag_doc (document_id),
    KEY idx_kb_document_tag_tag (tag_id),
    CONSTRAINT fk_doc_tag_doc FOREIGN KEY (document_id) REFERENCES kb_document (id) ON DELETE CASCADE,
    CONSTRAINT fk_doc_tag_tag FOREIGN KEY (tag_id) REFERENCES kb_tag (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '文档与标签关联表';

CREATE TABLE es_rebuild_task (
    id            BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    task_no       VARCHAR(50) NOT NULL COMMENT '任务编号（唯一）',
    trigger_type  TINYINT     NOT NULL DEFAULT 1 COMMENT '触发方式：1-手动触发',
    status        TINYINT     NOT NULL DEFAULT 0 COMMENT '状态：0-待执行，1-进行中，2-成功，3-失败',
    total_count   INT         NOT NULL DEFAULT 0 COMMENT '待索引文档总数',
    success_count INT         NOT NULL DEFAULT 0 COMMENT '索引成功数量',
    fail_count    INT         NOT NULL DEFAULT 0 COMMENT '索引失败数量',
    message       VARCHAR(500) DEFAULT NULL COMMENT '任务信息',
    started_at    DATETIME    DEFAULT NULL COMMENT '开始执行时间',
    finished_at   DATETIME    DEFAULT NULL COMMENT '执行完成时间',
    created_by    BIGINT       DEFAULT NULL COMMENT '任务创建人 ID',
    created_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_es_rebuild_task_no (task_no),
    KEY idx_es_rebuild_task_status (status),
    CONSTRAINT fk_rebuild_creator FOREIGN KEY (created_by) REFERENCES sys_user (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'ES 索引全量重建任务表';

SET FOREIGN_KEY_CHECKS = 1;

-- =====================================================================
-- 三、种子数据
-- =====================================================================

-- ---------- 3.1 权限（37 项） ----------
INSERT INTO sys_permission (id, perm_code, perm_name, module, type, route_path) VALUES
(1,  'system:user:view',        '用户查询',   '用户管理',   1, '/user'),
(2,  'system:user:add',         '新增用户',   '用户管理',   2, NULL),
(3,  'system:user:edit',        '修改用户',   '用户管理',   2, NULL),
(4,  'system:user:delete',      '删除用户',   '用户管理',   2, NULL),
(5,  'system:user:assign-role', '分配角色',   '用户管理',   2, NULL),
(6,  'system:user:reset-pwd',   '重置密码',   '用户管理',   2, NULL),
(7,  'system:role:view',        '角色查询',   '角色管理',   1, '/role'),
(8,  'system:role:add',         '新增角色',   '角色管理',   2, NULL),
(9,  'system:role:edit',        '修改角色',   '角色管理',   2, NULL),
(10, 'system:role:delete',      '删除角色',   '角色管理',   2, NULL),
(11, 'system:role:assign-perm', '分配权限',   '角色管理',   2, NULL),
(12, 'system:perm:view',        '权限查询',   '权限管理',   1, '/permission'),
(13, 'system:perm:add',         '新增权限',   '权限管理',   2, NULL),
(14, 'system:perm:edit',        '修改权限',   '权限管理',   2, NULL),
(15, 'system:perm:delete',      '删除权限',   '权限管理',   2, NULL),
(16, 'system:log:view',         '日志查询',   '操作日志',   1, '/log'),
(17, 'system:log:delete',       '删除日志',   '操作日志',   2, NULL),
(18, 'kb:kb:view',              '知识库查询', '知识库管理', 1, '/kb'),
(19, 'kb:kb:add',               '新增知识库', '知识库管理', 2, NULL),
(20, 'kb:kb:edit',              '修改知识库', '知识库管理', 2, NULL),
(21, 'kb:kb:delete',            '删除知识库', '知识库管理', 2, NULL),
(22, 'kb:category:view',        '分类查询',   '分类管理',   1, '/category'),
(23, 'kb:category:add',         '新增分类',   '分类管理',   2, NULL),
(24, 'kb:category:edit',        '修改分类',   '分类管理',   2, NULL),
(25, 'kb:category:delete',      '删除分类',   '分类管理',   2, NULL),
(26, 'kb:tag:view',             '标签查询',   '标签管理',   1, '/tag'),
(27, 'kb:tag:add',              '新增标签',   '标签管理',   2, NULL),
(28, 'kb:tag:edit',             '修改标签',   '标签管理',   2, NULL),
(29, 'kb:tag:delete',           '删除标签',   '标签管理',   2, NULL),
(30, 'kb:doc:view',             '文档查询',   '文档管理',   1, '/document'),
(31, 'kb:doc:add',              '新增文档',   '文档管理',   2, NULL),
(32, 'kb:doc:edit',             '修改文档',   '文档管理',   2, NULL),
(33, 'kb:doc:delete',           '删除文档',   '文档管理',   2, NULL),
(34, 'kb:file:upload',          '上传附件',   '附件管理',   2, NULL),
(35, 'kb:file:delete',          '删除附件',   '附件管理',   2, NULL),
(36, 'kb:search',               '全文检索',   '全文检索',   1, '/search'),
(37, 'kb:rebuild',              '索引重建',   '全文检索',   2, NULL);

-- ---------- 3.2 角色 ----------
INSERT INTO sys_role (id, role_code, role_name, description, status, sort) VALUES
(1, 'ADMIN',  '超级管理员', '拥有系统全部功能权限',                 1, 1),
(2, 'EDITOR', '知识库编辑', '负责知识库内容维护与全文检索',          1, 2),
(3, 'VIEWER', '知识库访客', '只读查看知识库内容与检索，无写操作权限', 1, 3);

-- ---------- 3.3 角色-权限关联 ----------
-- ADMIN：全部 37 项
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission;

-- EDITOR：知识库域读写 + 检索 + 重建 + 日志查看（21 项）
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 2, id FROM sys_permission WHERE perm_code IN (
    'kb:kb:view','kb:kb:add','kb:kb:edit','kb:kb:delete',
    'kb:category:view','kb:category:add','kb:category:edit','kb:category:delete',
    'kb:tag:view','kb:tag:add','kb:tag:edit','kb:tag:delete',
    'kb:doc:view','kb:doc:add','kb:doc:edit','kb:doc:delete',
    'kb:file:upload','kb:file:delete','kb:search','kb:rebuild','system:log:view'
);

-- VIEWER：各模块查询权限 + 检索（9 项）
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 3, id FROM sys_permission WHERE perm_code IN (
    'system:user:view','system:role:view','system:perm:view','system:log:view',
    'kb:kb:view','kb:category:view','kb:tag:view','kb:doc:view','kb:search'
);

-- ---------- 3.4 用户（密码为 BCrypt 加密，非明文） ----------
INSERT INTO sys_user (id, username, password, nickname, email, phone, status, last_login_at, last_login_ip) VALUES
(1, 'admin',    '$2b$10$8DL5Peccw364IaR89GepKe0Q.jYhYUBymDOz6Vp5cRaHTj4cJZdZW', '系统管理员', 'admin@zhiku.com',    '13800000001', 1, '2026-10-13 09:12:33', '127.0.0.1'),
(2, 'zhangsan', '$2b$10$FabuV/cbqpvAwuDqNcLr5uRZz4dq0daF5jv2g/xaMqd2osoVzKBOG', '张三',       'zhangsan@zhiku.com', '13800000002', 1, '2026-10-13 08:45:07', '127.0.0.1'),
(3, 'lisi',     '$2b$10$tPb94Emz8w5oeyi5vbJDFem4bhDV8AIQyj57CYK5arbBZDLCmcgk.', '李四',       'lisi@zhiku.com',      '13800000003', 1, '2026-10-12 17:20:41', '127.0.0.1');

-- ---------- 3.5 用户-角色关联 ----------
INSERT INTO sys_user_role (user_id, role_id) VALUES
(1, 1),
(2, 2),
(3, 3);

-- ---------- 3.6 知识库 ----------
INSERT INTO kb_knowledge_base (id, name, code, description, owner_id, visibility, doc_count, status) VALUES
(1, '技术架构库',   'tech-arch',       '系统架构、技术方案与最佳实践文档',   1, 1, 7, 1),
(2, '产品设计库',   'product-design',  '产品需求、原型与交互设计说明',       1, 1, 2, 1),
(3, '运维手册库',   'ops-manual',      '部署、监控、故障处理操作手册',       2, 2, 1, 1),
(4, '新人培训库',   'training',        '新员工入职学习路线与培训材料',       1, 1, 2, 1);

-- ---------- 3.7 分类 ----------
INSERT INTO kb_category (id, name, description, sort, status) VALUES
(1, '后端技术',   '服务端开发相关技术文档', 1, 1),
(2, '前端技术',   '前端框架与工程化实践',   2, 1),
(3, '数据库',     '关系型与非关系型数据库', 3, 1),
(4, '中间件',     '缓存、消息队列等中间件', 4, 1),
(5, '运维部署',   'CI/CD 与容器化部署',    5, 1),
(6, '系统设计',   '架构设计与权限模型',     6, 1),
(7, '团队协作',   '流程规范与协作工具',     7, 1);

-- ---------- 3.8 标签 ----------
INSERT INTO kb_tag (id, name, color) VALUES
(1, 'Redis',         '#E34D59'),
(2, 'JWT',           '#7C3AED'),
(3, 'Elasticsearch', '#F59E0B'),
(4, 'RabbitMQ',      '#10B981'),
(5, 'MySQL',         '#0EA5E9'),
(6, 'Vue',           '#42B883'),
(7, 'Docker',        '#2563EB'),
(8, 'Spring Boot',   '#65A30D');

-- ---------- 3.9 附件文件 ----------
INSERT INTO kb_file (id, original_name, stored_name, file_path, file_size, file_type, uploader_id) VALUES
(1, '知识库平台部署清单.pdf', 'b3f2c1a4e5d64f8a9b0c1d2e3f4a5b6c.pdf', '2026/10/b3f2c1a4e5d64f8a9b0c1d2e3f4a5b6c.pdf', 245760, 'application/pdf', 2);

-- ---------- 3.10 文档（含可检索正文） ----------
INSERT INTO kb_document (id, kb_id, category_id, title, summary, content, file_id, author_id, view_count, status, created_at, updated_at) VALUES
(1, 1, 4, 'Redis 分布式缓存设计与实践',
 '从缓存选型、Key 设计到集群方案的完整落地指南',
 'Redis 作为高性能的内存数据库，是企业知识库系统中最常用的缓存中间件。本文介绍 Redis 的几种典型使用场景：缓存加速、分布式锁、排行榜与消息通知。在缓存设计上，需要重点考虑 Key 的命名规范，建议采用 业务:模块:标识 的三段式结构，并设置合理的过期时间，避免缓存永不过期导致的内存膨胀。集群方案推荐使用 Redis Cluster 模式，通过哈希槽实现数据分片，配合读写分离与哨兵机制保障高可用。实践中还要警惕缓存穿透、缓存击穿与缓存雪崩三大问题：穿透可用布隆过滤器或空值缓存解决，击穿可用互斥锁，雪崩则通过随机过期时间与多级缓存缓解。',
 NULL, 2, 328, 1, '2026-10-08 10:15:00', '2026-10-08 10:15:00'),

(2, 1, 6, 'JWT 无状态认证方案详解',
 'Access Token 结构、刷新机制与安全实践',
 'JWT（JSON Web Token）由 Header、Payload、Signature 三部分组成，是前后端分离架构下最常用的无状态认证方案。签名部分采用 HMAC-SHA256 或 RSA 算法，保证 Token 在传输过程中不被篡改。Payload 中不应存放敏感信息，因为该部分仅做 Base64Url 编码，任何人都可以解码。为了提升安全性，Access Token 的有效期应尽量短（如 2 小时），并配合 Refresh Token 机制实现无感续期。更进一步，可以把 Token 的唯一标识 jti 存入 Redis，实现退出登录后 Token 立即失效，以及单点登录踢下线能力，这正是本平台登录模块的设计。',
 NULL, 1, 512, 1, '2026-10-08 14:30:00', '2026-10-08 14:30:00'),

(3, 2, 4, 'RabbitMQ 消息可靠性投递机制',
 '生产者确认、消费者 ACK 与消息持久化三重保障',
 'RabbitMQ 是部署最广泛的开源消息队列，基于 AMQP 协议。保证消息不丢失需要三个环节同时发力：生产者端开启 publisher confirm 机制，确保消息到达交换机；消息与队列都设置为持久化（durable + persistent），防止 Broker 重启丢失；消费者采用手动 ACK，只有在业务处理成功后才确认，失败则进入重试或死信队列。此外，通过 TTL 与死信交换机（DLX）可以实现延迟消息。本平台的全量索引重建任务即通过 RabbitMQ 异步消费执行：用户提交任务后立即返回，后台消费者分页读取 MySQL 文档数据并批量写入 Elasticsearch，避免阻塞请求线程。',
 NULL, 2, 276, 1, '2026-10-09 09:05:00', '2026-10-09 09:05:00'),

(4, 1, 6, 'Elasticsearch 分词与检索原理',
 '倒排索引、分词器与相关性评分机制剖析',
 'Elasticsearch 的核心是倒排索引：文档写入时经过分析器（Analyzer）分词，建立 词条 -> 文档列表 的映射；检索时同样对查询词分词，快速定位候选文档。标准分词器对英文按空格与标点切分，对中文则按单字切分，因此中文检索常需引入 IK 分词器获得更精确的结果。相关性评分使用 BM25 算法，综合考虑词频、文档长度与逆文档频率。本平台的知识库文档检索采用 multi_match 查询，对标题、摘要、正文分别设置 3、2、1 的权重，并配合 highlight 高亮返回命中的片段，同时支持按知识库、分类、标签与创建时间范围组合筛选。',
 NULL, 1, 445, 1, '2026-10-09 16:40:00', '2026-10-09 16:40:00'),

(5, 1, 3, 'MySQL 索引优化实战笔记',
 'B+ 树索引结构、最左前缀原则与执行计划分析',
 'MySQL InnoDB 引擎的索引基于 B+ 树组织，叶子节点通过双向链表连接，范围查询效率高。设计索引需遵循最左前缀原则：联合索引 (a, b, c) 在查询条件包含 a 时才能命中。避免在索引列上使用函数或隐式类型转换，这会导致索引失效。分析慢查询时使用 EXPLAIN 查看 type 列，至少达到 range 级别，理想是 const/ref。覆盖索引可以避免回表，是常见的优化手段。对于知识库平台的文档表，我们对 知识库 ID、分类 ID、状态、创建时间 等高频筛选字段建立了组合索引，使分页列表查询保持在毫秒级。',
 NULL, 2, 389, 1, '2026-10-10 11:20:00', '2026-10-10 11:20:00'),

(6, 2, 2, 'Vue 3 组合式 API 最佳实践',
 'setup、响应式系统与组件复用的工程化经验',
 'Vue 3 的组合式 API 将同一功能的逻辑聚合在一起，解决了选项式 API 中逻辑关注点分散的问题。ref 与 reactive 的选择原则：基本类型用 ref，对象用 reactive。计算属性 derived 应当是无副作用的纯函数。组件复用优先使用组合式函数（Composable），而非 mixin。项目结构上，API 层统一封装 axios 拦截器，处理 Token 注入、统一响应格式解包与 401 跳转登录；路由层使用动态菜单，根据用户权限过滤可访问页面；状态管理使用 Pinia，持久化 Token 到 localStorage。Element Plus 作为组件库，通过 CSS 变量定制主题色，保证视觉统一。',
 NULL, 1, 298, 1, '2026-10-10 15:55:00', '2026-10-10 15:55:00'),

(7, 3, 5, 'Docker 容器化部署指南',
 '从镜像构建到 compose 编排的一站式部署手册',
 'Docker 通过容器镜像实现环境一致性交付，一次构建、到处运行。多容器应用使用 Docker Compose 编排：一个 YAML 文件定义全部服务、网络与卷。部署知识库平台时，MySQL、Redis、RabbitMQ、Elasticsearch 四个中间件各一个容器，通过服务名互相访问，数据落到命名卷实现持久化。Elasticsearch 需要调整 nofile 限制并预留至少 512MB 堆内存；单节点模式通过 discovery.type=single-node 简化集群配置。健康检查（healthcheck）让依赖服务就绪后才启动应用，配合 depends_on 的 condition: service_healthy 可以避免启动竞态。',
 1, 2, 356, 1, '2026-10-11 09:30:00', '2026-10-11 09:30:00'),

(8, 1, 1, 'Spring Boot 统一异常处理设计',
 '全局异常处理器、统一响应格式与业务异常划分',
 'Spring Boot 通过 @RestControllerAdvice 实现全局异常处理。设计要点：业务异常（BusinessException）返回 HTTP 200 + 统一响应体中的业务码，让前端通过响应体判断结果；参数校验异常（MethodArgumentNotValidException）提取字段错误信息；认证与授权异常分别返回 401 与 403；未知异常记录堆栈并返回 500。统一响应格式采用 {code, message, data, timestamp} 四段式，所有接口保持一致。分页接口使用 PageResult 包装 list、total、pageNum、pageSize。结合 AOP 切面，重要写操作的请求参数、耗时、操作人与结果会自动落入操作日志表。',
 NULL, 1, 267, 1, '2026-10-11 14:10:00', '2026-10-11 14:10:00'),

(9, 1, 6, 'RBAC 权限模型设计与实现',
 '用户-角色-权限三级模型与按钮级权限控制',
 'RBAC（基于角色的访问控制）将权限分配给角色，角色再分配给用户，避免用户与权限直接耦合。本平台采用经典三级模型：sys_user、sys_role、sys_permission 三张表通过关联表连接。权限分为菜单权限与按钮权限，菜单决定前端展示哪些页面，按钮权限控制页面内的操作入口。后端使用 Spring Security 的方法级安全注解 @PreAuthorize("hasAuthority(''kb:doc:add'')") 做接口级拦截，前端通过自定义指令 v-permission 控制按钮显隐，双层防护确保越权请求无法穿透。',
 NULL, 1, 421, 1, '2026-10-12 10:00:00', '2026-10-12 10:00:00'),

(10, 1, 6, '知识库系统全文检索架构',
 'MySQL 存储 + Elasticsearch 检索的双写架构实践',
 '知识库平台采用 MySQL 作为主存储、Elasticsearch 作为检索引擎的双写架构。文档新增、修改、删除时实时同步 ES 索引，保证检索结果与主库一致；同时提供全量重建入口，通过 RabbitMQ 异步消费，分页批量重建索引，用于索引损坏或初始化场景。索引结构包含标题（权重 3）、摘要（权重 2）、正文（权重 1）三个文本字段，以及知识库、分类、标签等关键字字段用于组合筛选。检索结果通过 highlight 返回高亮片段，前端用 em 标签渲染。仪表盘同时展示 MySQL 文档总数与 ES 索引文档数，便于运维核对双写一致性。',
 NULL, 2, 233, 1, '2026-10-12 16:25:00', '2026-10-12 16:25:00'),

(11, 4, 4, 'Redis 缓存穿透与雪崩的解决方案',
 '三大缓存异常场景的成因与对应防护策略',
 '缓存三大问题是 Redis 使用的必修课。缓存穿透指查询根本不存在的数据，请求每次都打到数据库，解决方案是布隆过滤器或对空结果也做短期缓存。缓存击穿指热点 Key 过期的瞬间大量请求直击数据库，使用互斥锁（分布式锁）只允许一个线程回源。缓存雪崩指大量 Key 同时过期或 Redis 宕机，通过给过期时间加随机值、构建多级缓存（本地缓存 + Redis）、开启哨兵与集群来缓解。在知识库场景中，用户信息与权限列表适合缓存在 Redis，设置与 Token 一致的有效期，既减轻数据库压力，又支持退出登录即时失效。',
 NULL, 2, 189, 1, '2026-10-12 17:40:00', '2026-10-12 17:40:00'),

(12, 4, 2, '前端工程化构建优化实践',
 'Vite 构建速度优化与产物体积控制（草稿）',
 'Vite 基于 esbuild 与 Rollup，开发环境毫秒级冷启动。生产构建优化手段包括：路由懒加载分割代码块、Element Plus 等重型依赖采用按需引入或独立 chunk、ECharts 仅在使用页面引入、gzip 与 brotli 压缩、静态资源哈希命名与长期缓存。本文整理实际项目中的构建优化清单与对比数据（草稿状态，待补充构建耗时截图）。',
 NULL, 1, 76, 0, '2026-10-13 08:30:00', '2026-10-13 08:30:00');

-- ---------- 3.11 文档-标签关联 ----------
INSERT INTO kb_document_tag (document_id, tag_id) VALUES
(1, 1), (2, 2), (3, 4), (4, 3), (5, 5), (6, 6), (7, 7),
(8, 8), (9, 2), (10, 3), (10, 8), (11, 1), (12, 6), (12, 7);

-- ---------- 3.12 操作日志（演示 AOP 记录效果） ----------
INSERT INTO sys_operation_log (module, operation_type, description, method, request_uri, request_method, request_params, operator, operator_id, operator_ip, status, error_msg, cost_time, created_at) VALUES
('认证中心', 'LOGIN',  '用户登录', 'com.kb.system.module.system.auth.controller.AuthController#login', '/api/auth/login', 'POST', '[{"username":"admin","password":"***"}]', '系统管理员', 1, '127.0.0.1', 1, NULL, 86, '2026-10-10 09:00:12'),
('知识库管理', 'ADD',   '新增知识库', 'com.kb.system.module.kb.knowledgebase.controller.KbKnowledgeBaseController#create', '/api/kb/knowledge-bases', 'POST', '[{"name":"技术架构库","code":"tech-arch"}]', '系统管理员', 1, '127.0.0.1', 1, NULL, 34, '2026-10-10 09:05:41'),
('文档管理', 'ADD',   '新增文档', 'com.kb.system.module.kb.document.controller.KbDocumentController#create', '/api/kb/documents', 'POST', '[{"title":"Redis 分布式缓存设计与实践","kbId":1}]', '张三', 2, '127.0.0.1', 1, NULL, 152, '2026-10-10 10:15:22'),
('文档管理', 'UPDATE', '修改文档', 'com.kb.system.module.kb.document.controller.KbDocumentController#update', '/api/kb/documents/1', 'PUT', '[{"title":"Redis 分布式缓存设计与实践（修订）"}]', '张三', 2, '127.0.0.1', 1, NULL, 128, '2026-10-10 10:42:08'),
('角色管理', 'ASSIGN', '分配权限', 'com.kb.system.module.system.role.controller.SysRoleController#assignPermissions', '/api/system/roles/2/permissions', 'PUT', '[{"permissionIds":[18,19,20,21,30,31,32,33,36,37]}]', '系统管理员', 1, '127.0.0.1', 1, NULL, 95, '2026-10-10 11:20:33'),
('用户管理', 'ADD',   '新增用户', 'com.kb.system.module.system.user.controller.SysUserController#create', '/api/system/users', 'POST', '[{"username":"lisi","nickname":"李四"}]', '系统管理员', 1, '127.0.0.1', 1, NULL, 67, '2026-10-10 14:08:19'),
('索引管理', 'REBUILD', '提交索引全量重建任务', 'com.kb.system.module.search.controller.IndexRebuildController#submit', '/api/rebuild/tasks', 'POST', '[]', '系统管理员', 1, '127.0.0.1', 1, NULL, 21, '2026-10-10 15:30:00'),
('认证中心', 'LOGOUT', '退出登录', 'com.kb.system.module.system.auth.controller.AuthController#logout', '/api/auth/logout', 'POST', '[]', '系统管理员', 1, '127.0.0.1', 1, NULL, 12, '2026-10-10 18:05:44'),
('认证中心', 'LOGIN',  '用户登录', 'com.kb.system.module.system.auth.controller.AuthController#login', '/api/auth/login', 'POST', '[{"username":"zhangsan","password":"***"}]', '张三', 2, '127.0.0.1', 1, NULL, 74, '2026-10-11 08:45:02'),
('文档管理', 'ADD',   '新增文档', 'com.kb.system.module.kb.document.controller.KbDocumentController#create', '/api/kb/documents', 'POST', '[{"title":"RabbitMQ 消息可靠性投递机制","kbId":1}]', '张三', 2, '127.0.0.1', 1, NULL, 141, '2026-10-11 09:05:33'),
('分类管理', 'UPDATE', '修改分类', 'com.kb.system.module.kb.category.controller.KbCategoryController#update', '/api/kb/categories/3', 'PUT', '[{"name":"数据库","sort":3}]', '张三', 2, '127.0.0.1', 1, NULL, 45, '2026-10-11 11:12:50'),
('附件管理', 'UPLOAD', '上传附件', 'com.kb.system.module.kb.file.controller.KbFileController#upload', '/api/kb/files', 'POST', '[file]', '张三', 2, '127.0.0.1', 1, NULL, 213, '2026-10-11 09:28:41'),
('知识库管理', 'DELETE', '删除知识库', 'com.kb.system.module.kb.knowledgebase.controller.KbKnowledgeBaseController#delete', '/api/kb/knowledge-bases/9', 'DELETE', '[]', '系统管理员', 1, '127.0.0.1', 1, NULL, 289, '2026-10-11 16:40:12'),
('用户管理', 'DELETE', '删除用户', 'com.kb.system.module.system.user.controller.SysUserController#delete', '/api/system/users/8', 'DELETE', '[]', '系统管理员', 1, '127.0.0.1', 1, NULL, 58, '2026-10-11 17:02:31'),
('认证中心', 'LOGIN',  '用户登录', 'com.kb.system.module.system.auth.controller.AuthController#login', '/api/auth/login', 'POST', '[{"username":"lisi","password":"***"}]', '李四', 3, '127.0.0.1', 1, NULL, 79, '2026-10-12 17:20:41'),
('文档管理', 'DELETE', '删除文档', 'com.kb.system.module.kb.document.controller.KbDocumentController#delete', '/api/kb/documents/6', 'DELETE', '[]', '李四', 3, '127.0.0.1', 0, '没有访问权限，请联系管理员', 15, '2026-10-12 17:22:03'),
('标签管理', 'ADD',   '新增标签', 'com.kb.system.module.kb.tag.controller.KbTagController#create', '/api/kb/tags', 'POST', '[{"name":"性能优化","color":"#F59E0B"}]', '张三', 2, '127.0.0.1', 1, NULL, 38, '2026-10-12 09:15:26'),
('权限管理', 'ADD',   '新增权限', 'com.kb.system.module.system.permission.controller.SysPermissionController#create', '/api/system/permissions', 'POST', '[{"permCode":"kb:doc:export","permName":"导出文档"}]', '系统管理员', 1, '127.0.0.1', 1, NULL, 42, '2026-10-12 10:33:18'),
('索引管理', 'REBUILD', '提交索引全量重建任务', 'com.kb.system.module.search.controller.IndexRebuildController#submit', '/api/rebuild/tasks', 'POST', '[]', '张三', 2, '127.0.0.1', 1, NULL, 19, '2026-10-12 15:10:55'),
('认证中心', 'LOGIN',  '用户登录', 'com.kb.system.module.system.auth.controller.AuthController#login', '/api/auth/login', 'POST', '[{"username":"admin","password":"wrong"}]', 'admin', NULL, '127.0.0.1', 0, '用户名或密码错误', 63, '2026-10-13 07:58:20'),
('认证中心', 'LOGIN',  '用户登录', 'com.kb.system.module.system.auth.controller.AuthController#login', '/api/auth/login', 'POST', '[{"username":"admin","password":"***"}]', '系统管理员', 1, '127.0.0.1', 1, NULL, 71, '2026-10-13 09:12:33'),
('文档管理', 'UPDATE', '修改文档', 'com.kb.system.module.kb.document.controller.KbDocumentController#update', '/api/kb/documents/12', 'PUT', '[{"title":"前端工程化构建优化实践"}]', '系统管理员', 1, '127.0.0.1', 1, NULL, 117, '2026-10-13 10:26:47');

-- ---------- 3.13 索引重建任务历史 ----------
INSERT INTO es_rebuild_task (task_no, trigger_type, status, total_count, success_count, fail_count, message, started_at, finished_at, created_by, created_at) VALUES
('REBUILD-20261012151055-3F2A1B', 1, 2, 11, 11, 0, '索引重建完成，共索引 11 个文档', '2026-10-12 15:10:57', '2026-10-12 15:11:04', 2, '2026-10-12 15:10:55'),
('REBUILD-20261010153000-9C4D2E', 1, 3, 11, 0, 11, '索引重建失败: Elasticsearch 连接超时', '2026-10-10 15:30:02', '2026-10-10 15:30:22', 1, '2026-10-10 15:30:00');

-- =====================================================================
-- 完成
-- =====================================================================
