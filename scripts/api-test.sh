#!/usr/bin/env bash
# =====================================================================
# 知库 · 企业知识库平台 —— API 全链路自动化测试脚本
# 用法：bash scripts/api-test.sh [BASE_URL] [MYSQL_PORT] [REDIS_PORT]
#   默认 BASE_URL=http://localhost:8080/api
# 依赖：curl、python3（仅用于解析 JSON）、docker（Redis 断言，可选）
# =====================================================================
set -u

BASE="${1:-http://localhost:8080/api}"
MYSQL_PORT="${2:-3306}"
REDIS_PORT="${3:-6379}"
REDIS_PASS="${REDIS_PASS:-redis1234}"

PASS=0
FAIL=0

check() { # check <描述> <实际> <期望包含>
    local desc="$1" actual="$2" expect="$3"
    if echo "$actual" | grep -q "$expect"; then
        echo "  [PASS] $desc"
        PASS=$((PASS + 1))
    else
        echo "  [FAIL] $desc"
        echo "         期望包含: $expect"
        echo "         实际返回: $(echo "$actual" | head -c 300)"
        FAIL=$((FAIL + 1))
    fi
}

jq_get() { # jq_get <json> <python 表达式，作用于 d>
    echo "$1" | python3 -c "import sys,json
try:
    d = json.load(sys.stdin)
except Exception:
    print('PARSE_ERROR'); sys.exit()
print($2)" 2>/dev/null || echo "EXPR_ERROR"
}

echo "=============================================="
echo " 知库 API 全链路测试  BASE=$BASE"
echo "=============================================="

# ---------- 1. 认证 ----------
echo ""
echo "[1] 登录认证（JWT + Redis）"
R=$(curl -s -X POST "$BASE/auth/login" -H "Content-Type: application/json" \
    -d '{"username":"admin","password":"wrongpass"}')
check "错误密码返回业务错误" "$R" '"code":600'

R=$(curl -s -X POST "$BASE/auth/login" -H "Content-Type: application/json" \
    -d '{"username":"admin","password":"Admin@123"}')
check "正确登录返回 Token" "$R" '"token":"ey'
TOKEN=$(jq_get "$R" "d['data']['token']")
[ -z "$TOKEN" ] && { echo "Token 获取失败，终止测试"; exit 1; }
echo "  Token: ${TOKEN:0:40}..."

R=$(curl -s "$BASE/auth/info" -H "Authorization: Bearer $TOKEN")
check "携带 Token 获取用户信息" "$R" '"username":"admin"'
check "返回角色列表" "$R" '"roles"'
check "返回权限列表" "$R" '"permissions"'

R=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/system/users")
check "无 Token 访问返回 401" "$R" "401"

# Redis 登录态断言
if command -v docker >/dev/null 2>&1; then
    KEYS=$(docker exec kb-redis redis-cli --no-auth-warning -a "$REDIS_PASS" keys "auth:*" 2>/dev/null | tr '\n' ' ')
    check "Redis 中存在登录态键" "$KEYS" "auth:token:1"
fi

# ---------- 2. RBAC ----------
echo ""
echo "[2] RBAC 权限控制"
R=$(curl -s -X POST "$BASE/auth/login" -H "Content-Type: application/json" \
    -d '{"username":"lisi","password":"Li@123"}')
VTOKEN=$(jq_get "$R" "d['data']['token']")
check "访客账号登录成功" "$R" '"token":"ey'
R=$(curl -s -X POST "$BASE/kb/documents" -H "Authorization: Bearer $VTOKEN" \
    -H "Content-Type: application/json" \
    -d '{"kbId":1,"categoryId":1,"title":"越权测试","content":"x","status":1}')
check "访客无写权限返回 403" "$R" '403\|FORBIDDEN\|没有访问权限'
R=$(curl -s "$BASE/kb/documents?pageNum=1&pageSize=5" -H "Authorization: Bearer $VTOKEN")
check "访客有读权限可查文档" "$R" '"total"'

# ---------- 3. 业务 CRUD ----------
echo ""
echo "[3] 核心业务 CRUD（知识库 + 文档）"
R=$(curl -s -X POST "$BASE/kb/knowledge-bases" -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d '{"name":"API测试知识库","code":"api-test-kb","description":"自动化测试创建","visibility":1,"status":1}')
check "新增知识库成功" "$R" '"code":200'
KBID=$(jq_get "$R" "d['data']")

R=$(curl -s -X POST "$BASE/kb/documents" -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d "{\"kbId\":$KBID,\"categoryId\":1,\"title\":\"Redis 测试文档\",\"summary\":\"摘要\",\"content\":\"Redis 缓存是高性能内存数据库，JWT 认证配合 Elasticsearch 检索。\",\"status\":1,\"tagIds\":[1,2]}")
check "新增文档成功" "$R" '"code":200'
DOCID=$(jq_get "$R" "d['data']")

R=$(curl -s "$BASE/kb/documents?pageNum=1&pageSize=10&title=Redis" -H "Authorization: Bearer $TOKEN")
check "文档标题模糊查询" "$R" "Redis 测试文档"

R=$(curl -s -X PUT "$BASE/kb/documents/$DOCID" -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d "{\"kbId\":$KBID,\"categoryId\":1,\"title\":\"Redis 测试文档(改)\",\"summary\":\"摘要2\",\"content\":\"RabbitMQ 消息队列与 MySQL 索引优化。\",\"status\":1,\"tagIds\":[1]}")
check "修改文档成功" "$R" '"code":200'

R=$(curl -s "$BASE/kb/documents/$DOCID" -H "Authorization: Bearer $TOKEN")
check "文档详情返回正文" "$R" "RabbitMQ 消息队列"

R=$(curl -s -X DELETE "$BASE/kb/documents/$DOCID" -H "Authorization: Bearer $TOKEN")
check "删除文档成功" "$R" '"code":200'

R=$(curl -s "$BASE/kb/documents/$DOCID" -H "Authorization: Bearer $TOKEN")
check "删除后查询不存在" "$R" '文档不存在'

R=$(curl -s -X DELETE "$BASE/kb/knowledge-bases/$KBID" -H "Authorization: Bearer $TOKEN")
check "删除知识库成功" "$R" '"code":200'

# ---------- 4. 用户/角色/权限管理 ----------
echo ""
echo "[4] 用户/角色/权限管理"
R=$(curl -s -X POST "$BASE/system/users" -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d '{"username":"testuser","password":"Test@123","nickname":"测试用户","status":1,"roleIds":[3]}')
check "新增用户成功" "$R" '"code":200'
NEWUID=$(jq_get "$R" "d['data']")

R=$(curl -s "$BASE/system/users/$NEWUID" -H "Authorization: Bearer $TOKEN")
check "用户详情含角色" "$R" '知识库访客'

R=$(curl -s -X PUT "$BASE/system/users/$NEWUID/roles" -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" -d '{"roleIds":[2]}')
check "分配角色成功" "$R" '"code":200'

R=$(curl -s -X POST "$BASE/system/roles" -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d '{"roleCode":"TEST_ROLE","roleName":"测试角色","description":"API测试","status":1,"sort":9}')
check "新增角色成功" "$R" '"code":200'
ROLEID=$(jq_get "$R" "d['data']")

R=$(curl -s -X PUT "$BASE/system/roles/$ROLEID/permissions" -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" -d '{"permissionIds":[18,19,30,31]}')
check "角色分配权限成功" "$R" '"code":200'

R=$(curl -s -X DELETE "$BASE/system/roles/$ROLEID" -H "Authorization: Bearer $TOKEN")
check "删除角色成功" "$R" '"code":200'

R=$(curl -s -X DELETE "$BASE/system/users/$NEWUID" -H "Authorization: Bearer $TOKEN")
check "删除测试用户成功" "$R" '"code":200'

# ---------- 5. 操作日志 ----------
echo ""
echo "[5] 操作日志（AOP）"
R=$(curl -s -G "$BASE/system/logs" --data-urlencode "pageNum=1" --data-urlencode "pageSize=5" --data-urlencode "module=文档管理" -H "Authorization: Bearer $TOKEN")
check "按模块查询操作日志" "$R" '"total"'
R=$(curl -s "$BASE/system/logs?pageNum=1&pageSize=20" -H "Authorization: Bearer $TOKEN")
check "日志含操作人与IP" "$R" 'operatorIp'

# ---------- 6. 全文检索 ----------
echo ""
echo "[6] Elasticsearch 全文检索（关键词 + 高亮 + 组合筛选）"
R=$(curl -s "$BASE/search/documents?keyword=Redis&pageNum=1&pageSize=5" -H "Authorization: Bearer $TOKEN")
check "关键词检索有结果" "$R" '"total":[1-9]'
check "检索结果含高亮" "$R" '<em>'

R=$(curl -s "$BASE/search/documents?keyword=Elasticsearch&pageNum=1&pageSize=5" -H "Authorization: Bearer $TOKEN")
check "英文关键词检索" "$R" 'Elasticsearch'

R=$(curl -s "$BASE/search/documents?keyword=Redis&kbId=1&pageNum=1&pageSize=5" -H "Authorization: Bearer $TOKEN")
check "组合条件筛选(知识库)" "$R" '"kbName":"技术架构库"'

R=$(curl -s "$BASE/search/documents?pageNum=1&pageSize=5&tagName=Redis" -H "Authorization: Bearer $TOKEN")
check "组合条件筛选(标签)" "$R" 'Redis'

R=$(curl -s -G "$BASE/search/documents" --data-urlencode "keyword=缓存" --data-urlencode "pageNum=1" --data-urlencode "pageSize=5" -H "Authorization: Bearer $TOKEN")
check "中文关键词检索" "$R" '"total":[1-9]'

# ---------- 7. 异步索引重建 ----------
echo ""
echo "[7] RabbitMQ 异步索引重建"
R=$(curl -s -X POST "$BASE/rebuild/tasks" -H "Authorization: Bearer $TOKEN")
check "提交重建任务成功" "$R" '"taskNo":"REBUILD'
TASKID=$(jq_get "$R" "d['data']['id']")

SUCCESS=0
for i in $(seq 1 30); do
    sleep 2
    R=$(curl -s "$BASE/rebuild/tasks/$TASKID" -H "Authorization: Bearer $TOKEN")
    ST=$(jq_get "$R" "d['data']['status']")
    if [ "$ST" = "2" ]; then SUCCESS=1; break; fi
    if [ "$ST" = "3" ]; then break; fi
done
if [ "$SUCCESS" = "1" ]; then
    echo "  [PASS] 重建任务执行成功（MQ 异步消费）"
    PASS=$((PASS + 1))
    check "重建统计文档数" "$R" '"successCount":[1-9]'
else
    echo "  [FAIL] 重建任务未成功: $R"
    FAIL=$((FAIL + 1))
fi

# ---------- 8. 退出登录 ----------
echo ""
echo "[8] 退出登录（Token 立即失效）"
R=$(curl -s -X POST "$BASE/auth/logout" -H "Authorization: Bearer $TOKEN")
check "退出登录成功" "$R" '"code":200'
R=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/auth/info" -H "Authorization: Bearer $TOKEN")
check "退出后原 Token 失效(401)" "$R" "401"
if command -v docker >/dev/null 2>&1; then
    KEYS=$(docker exec kb-redis redis-cli --no-auth-warning -a "$REDIS_PASS" keys "auth:*" 2>/dev/null | tr '\n' ' ')
    if echo "$KEYS" | grep -q "auth:token:1"; then
        echo "  [FAIL] Redis 登录态未清除"
        FAIL=$((FAIL + 1))
    else
        echo "  [PASS] Redis 登录态已清除"
        PASS=$((PASS + 1))
    fi
fi

echo ""
echo "=============================================="
echo " 测试结果：PASS=$PASS  FAIL=$FAIL"
echo "=============================================="
[ "$FAIL" -eq 0 ] && echo "全部通过 ✔" || echo "存在失败项 ✘"
exit $FAIL
