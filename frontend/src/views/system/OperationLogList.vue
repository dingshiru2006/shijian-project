<template>
  <el-card shadow="never">
    <el-form :inline="true" :model="query" class="query-form">
      <el-form-item label="模块">
        <el-input v-model="query.module" placeholder="功能模块" clearable @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item label="操作类型">
        <el-input v-model="query.operationType" placeholder="如 新增/修改" clearable @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item label="操作人">
        <el-input v-model="query.operator" placeholder="操作人" clearable @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item label="结果">
        <el-select v-model="query.status" placeholder="全部" clearable style="width: 100px">
          <el-option label="成功" :value="1" />
          <el-option label="失败" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item label="时间范围">
        <el-date-picker
          v-model="timeRange"
          type="datetimerange"
          range-separator="~"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          value-format="YYYY-MM-DD HH:mm:ss"
          style="width: 340px"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="Refresh" @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="toolbar">
      <span class="toolbar-title">操作日志（AOP 异步记录）</span>
    </div>

    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="70" align="center" />
      <el-table-column prop="module" label="模块" width="110" align="center" />
      <el-table-column prop="operationType" label="类型" width="90" align="center" />
      <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip />
      <el-table-column prop="operator" label="操作人" width="100" align="center" />
      <el-table-column prop="requestMethod" label="请求方式" width="90" align="center" />
      <el-table-column prop="requestUri" label="请求路径" min-width="160" show-overflow-tooltip />
      <el-table-column prop="operatorIp" label="IP" width="130" align="center" />
      <el-table-column label="结果" width="80" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="row.status === 1 ? 'success' : 'danger'">
            {{ row.status === 1 ? '成功' : '失败' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="耗时" width="90" align="center">
        <template #default="{ row }">
          {{ row.costTime != null ? row.costTime + ' ms' : '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="操作时间" width="170" align="center" />
      <el-table-column label="操作" width="90" align="center" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="openDetail(row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      class="pagination"
      v-model:current-page="query.pageNum"
      v-model:page-size="query.pageSize"
      :total="total"
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next, jumper"
      @size-change="loadList"
      @current-change="loadList"
    />

    <!-- 详情对话框 -->
    <el-dialog v-model="detailVisible" title="日志详情" width="620px">
      <el-descriptions v-if="detail" :column="1" border size="small">
        <el-descriptions-item label="模块">{{ detail.module }}</el-descriptions-item>
        <el-descriptions-item label="操作类型">{{ detail.operationType }}</el-descriptions-item>
        <el-descriptions-item label="描述">{{ detail.description }}</el-descriptions-item>
        <el-descriptions-item label="方法">{{ detail.method }}</el-descriptions-item>
        <el-descriptions-item label="请求">
          {{ detail.requestMethod }} {{ detail.requestUri }}
        </el-descriptions-item>
        <el-descriptions-item label="请求参数">
          <pre class="params-pre">{{ detail.requestParams || '无' }}</pre>
        </el-descriptions-item>
        <el-descriptions-item label="操作人">
          {{ detail.operator || '匿名' }}（ID: {{ detail.operatorId ?? '-' }}）
        </el-descriptions-item>
        <el-descriptions-item label="IP">{{ detail.operatorIp }}</el-descriptions-item>
        <el-descriptions-item label="耗时">
          {{ detail.costTime != null ? detail.costTime + ' ms' : '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="操作时间">{{ detail.createdAt }}</el-descriptions-item>
        <el-descriptions-item v-if="detail.status !== 1" label="错误信息">
          <span class="error-msg">{{ detail.errorMsg }}</span>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Search, Refresh } from '@element-plus/icons-vue'
import { logApi } from '@/api'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  module: '',
  operationType: '',
  operator: '',
  status: null,
  startTime: '',
  endTime: ''
})
const timeRange = ref(null)

const detailVisible = ref(false)
const detail = ref(null)

async function loadList() {
  loading.value = true
  try {
    const params = { ...query }
    if (params.status === null) delete params.status
    const res = await logApi.page(params)
    list.value = res.data.list
    total.value = res.data.total
  } catch {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  if (timeRange.value?.length === 2) {
    query.startTime = timeRange.value[0]
    query.endTime = timeRange.value[1]
  } else {
    query.startTime = ''
    query.endTime = ''
  }
  query.pageNum = 1
  loadList()
}

function handleReset() {
  query.module = ''
  query.operationType = ''
  query.operator = ''
  query.status = null
  query.startTime = ''
  query.endTime = ''
  timeRange.value = null
  handleSearch()
}

async function openDetail(row) {
  const res = await logApi.detail(row.id)
  detail.value = res.data
  detailVisible.value = true
}

onMounted(loadList)
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}
.toolbar-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}
.pagination {
  margin-top: 14px;
  justify-content: flex-end;
}
.params-pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
  font-size: 12px;
  max-height: 200px;
  overflow: auto;
}
.error-msg {
  color: #f56c6c;
}
</style>
