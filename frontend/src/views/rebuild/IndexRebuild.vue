<template>
  <el-card shadow="never">
    <div class="intro">
      <el-alert type="info" :closable="false" show-icon>
        <template #title>
          索引重建通过 RabbitMQ 异步执行：提交任务后消息进入队列，后台消费者清空并全量重建
          Elasticsearch 索引，任务进度与结果可在此查询。
        </template>
      </el-alert>
    </div>

    <div class="toolbar">
      <span class="toolbar-title">重建任务列表</span>
      <el-button
        v-permission="'system:perm:edit'"
        type="primary"
        :icon="RefreshRight"
        :loading="submitting"
        @click="handleSubmit"
      >
        提交重建任务
      </el-button>
    </div>

    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="70" align="center" />
      <el-table-column prop="taskNo" label="任务编号" min-width="240" show-overflow-tooltip />
      <el-table-column label="触发方式" width="100" align="center">
        <template #default="{ row }">
          {{ row.triggerType === 1 ? '手动触发' : '系统启动' }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="110" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="statusType(row.status)">
            {{ statusText(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="进度" min-width="180">
        <template #default="{ row }">
          <el-progress
            :percentage="progress(row)"
            :status="row.status === 3 ? 'exception' : row.status === 2 ? 'success' : undefined"
            :stroke-width="14"
          />
        </template>
      </el-table-column>
      <el-table-column label="成功/总数" width="110" align="center">
        <template #default="{ row }">
          {{ row.successCount }} / {{ row.totalCount }}
        </template>
      </el-table-column>
      <el-table-column prop="createdBy" label="提交人" width="90" align="center" />
      <el-table-column prop="createdAt" label="提交时间" width="170" align="center" />
      <el-table-column label="耗时" width="100" align="center">
        <template #default="{ row }">
          {{ formatCost(row) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="90" align="center" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="openDetail(row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      class="pagination"
      v-model:current-page="pageNum"
      v-model:page-size="pageSize"
      :total="total"
      :page-sizes="[10, 20]"
      layout="total, sizes, prev, pager, next"
      @size-change="loadList"
      @current-change="loadList"
    />

    <!-- 详情对话框 -->
    <el-dialog v-model="detailVisible" title="任务详情" width="560px">
      <el-descriptions v-if="detail" :column="1" border size="small">
        <el-descriptions-item label="任务编号">{{ detail.taskNo }}</el-descriptions-item>
        <el-descriptions-item label="触发方式">
          {{ detail.triggerType === 1 ? '手动触发' : '系统启动' }}
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag size="small" :type="statusType(detail.status)">{{ statusText(detail.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="文档总数">{{ detail.totalCount }}</el-descriptions-item>
        <el-descriptions-item label="成功条数">{{ detail.successCount }}</el-descriptions-item>
        <el-descriptions-item label="失败条数">{{ detail.failCount }}</el-descriptions-item>
        <el-descriptions-item label="提交时间">{{ detail.createdAt }}</el-descriptions-item>
        <el-descriptions-item label="开始时间">{{ detail.startedAt || '-' }}</el-descriptions-item>
        <el-descriptions-item label="完成时间">{{ detail.finishedAt || '-' }}</el-descriptions-item>
        <el-descriptions-item label="执行信息">{{ detail.message || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { RefreshRight } from '@element-plus/icons-vue'
import { rebuildApi } from '@/api'

const loading = ref(false)
const submitting = ref(false)
const list = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const detailVisible = ref(false)
const detail = ref(null)

function statusText(s) {
  return ['待执行', '执行中', '成功', '失败'][s] ?? '未知'
}
function statusType(s) {
  return ['info', 'warning', 'success', 'danger'][s] ?? 'info'
}
function progress(row) {
  if (!row.totalCount) return 0
  const done = (row.successCount || 0) + (row.failCount || 0)
  return Math.min(100, Math.round((done / row.totalCount) * 100))
}
function formatCost(row) {
  if (!row.startedAt || !row.finishedAt) return '-'
  const ms = new Date(row.finishedAt) - new Date(row.startedAt)
  return ms < 1000 ? ms + ' ms' : (ms / 1000).toFixed(1) + ' s'
}

async function loadList() {
  loading.value = true
  try {
    const res = await rebuildApi.page({ pageNum: pageNum.value, pageSize: pageSize.value })
    list.value = res.data.list
    total.value = res.data.total
  } catch {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
}

async function handleSubmit() {
  submitting.value = true
  try {
    const res = await rebuildApi.submit()
    ElMessage.success(`重建任务已提交：${res.data.taskNo}`)
    pageNum.value = 1
    await loadList()
    // 简单轮询：等待本次任务完成
    pollTask(res.data.id)
  } catch {
    // 拦截器已提示
  } finally {
    submitting.value = false
  }
}

function pollTask(taskId) {
  let times = 0
  const timer = setInterval(async () => {
    times++
    try {
      const res = await rebuildApi.detail(taskId)
      if (res.data.status === 2) {
        clearInterval(timer)
        ElMessage.success('索引重建完成')
        loadList()
      } else if (res.data.status === 3) {
        clearInterval(timer)
        ElMessage.error('索引重建失败：' + (res.data.message || ''))
        loadList()
      } else if (times > 60) {
        clearInterval(timer)
      }
    } catch {
      clearInterval(timer)
    }
  }, 2000)
}

async function openDetail(row) {
  const res = await rebuildApi.detail(row.id)
  detail.value = res.data
  detailVisible.value = true
}

onMounted(loadList)
</script>

<style scoped>
.intro {
  margin-bottom: 14px;
}
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
</style>
