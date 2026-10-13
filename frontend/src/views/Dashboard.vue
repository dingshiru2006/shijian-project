<template>
  <div v-loading="loading">
    <!-- 统计卡片 -->
    <el-row :gutter="16">
      <el-col :span="4" v-for="card in statCards" :key="card.label">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value" :style="{ color: card.color }">{{ card.value ?? '-' }}</div>
          <div class="stat-label">
            <el-icon><component :is="card.icon" /></el-icon>
            {{ card.label }}
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <!-- 分类文档分布 -->
      <el-col :span="10">
        <el-card shadow="never">
          <template #header>
            <span>分类文档分布</span>
          </template>
          <div v-if="!stats?.categoryChart?.length" class="empty">暂无数据</div>
          <div v-else class="bar-list">
            <div v-for="item in stats.categoryChart" :key="item.name" class="bar-row">
              <span class="bar-name" :title="item.name">{{ item.name }}</span>
              <div class="bar-track">
                <div
                  class="bar-fill"
                  :style="{ width: barWidth(item.value, maxCategory) + '%' }"
                ></div>
              </div>
              <span class="bar-value">{{ item.value }}</span>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 知识库文档分布 -->
      <el-col :span="10">
        <el-card shadow="never">
          <template #header>
            <span>知识库文档分布</span>
          </template>
          <div v-if="!stats?.kbChart?.length" class="empty">暂无数据</div>
          <div v-else class="bar-list">
            <div v-for="item in stats.kbChart" :key="item.name" class="bar-row">
              <span class="bar-name" :title="item.name">{{ item.name }}</span>
              <div class="bar-track">
                <div
                  class="bar-fill kb"
                  :style="{ width: barWidth(item.value, maxKb) + '%' }"
                ></div>
              </div>
              <span class="bar-value">{{ item.value }}</span>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 最近操作日志 -->
      <el-col :span="4">
        <el-card shadow="never" class="log-card">
          <template #header>
            <span>最近操作</span>
          </template>
          <ul class="recent-logs">
            <li v-for="log in stats?.recentLogs || []" :key="log.id">
              <el-tag size="small" :type="log.status === 1 ? 'success' : 'danger'" effect="plain">
                {{ log.operationType }}
              </el-tag>
              <span class="log-desc" :title="log.description">{{ log.description }}</span>
            </li>
            <li v-if="!stats?.recentLogs?.length" class="empty">暂无记录</li>
          </ul>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { dashboardApi } from '@/api'

const loading = ref(false)
const stats = ref(null)

const statCards = computed(() => [
  { label: '知识库', value: stats.value?.kbCount, icon: 'Reading', color: '#409eff' },
  { label: '文档总数', value: stats.value?.documentCount, icon: 'Document', color: '#67c23a' },
  { label: '已发布', value: stats.value?.publishedCount, icon: 'Check', color: '#409eff' },
  { label: '草稿', value: stats.value?.draftCount, icon: 'EditPen', color: '#e6a23c' },
  { label: 'ES 索引文档', value: stats.value?.esDocCount, icon: 'Search', color: '#909399' },
  { label: '用户数', value: stats.value?.userCount, icon: 'User', color: '#409eff' },
  { label: '分类数', value: stats.value?.categoryCount, icon: 'Folder', color: '#67c23a' },
  { label: '今日操作', value: stats.value?.todayLogCount, icon: 'Tickets', color: '#e6a23c' }
])

const maxCategory = computed(() =>
  Math.max(1, ...(stats.value?.categoryChart || []).map((i) => i.value))
)
const maxKb = computed(() =>
  Math.max(1, ...(stats.value?.kbChart || []).map((i) => i.value))
)

function barWidth(value, max) {
  return Math.round((value / max) * 100)
}

async function loadStats() {
  loading.value = true
  try {
    const res = await dashboardApi.stats()
    stats.value = res.data
  } catch {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
}

onMounted(loadStats)
</script>

<style scoped>
.stat-card {
  text-align: center;
  padding: 6px 0;
}
.stat-value {
  font-size: 26px;
  font-weight: 700;
}
.stat-label {
  margin-top: 6px;
  font-size: 13px;
  color: #606266;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
}

.bar-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.bar-row {
  display: flex;
  align-items: center;
  gap: 10px;
}
.bar-name {
  width: 72px;
  font-size: 13px;
  color: #606266;
  text-align: right;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.bar-track {
  flex: 1;
  height: 14px;
  background: #ebeef5;
  border-radius: 7px;
  overflow: hidden;
}
.bar-fill {
  height: 100%;
  background: linear-gradient(90deg, #79bbff, #409eff);
  border-radius: 7px;
  transition: width 0.4s;
}
.bar-fill.kb {
  background: linear-gradient(90deg, #95d475, #67c23a);
}
.bar-value {
  width: 32px;
  font-size: 13px;
  color: #303133;
  font-weight: 600;
}

.log-card :deep(.el-card__body) {
  padding: 10px 12px;
}
.recent-logs {
  margin: 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.recent-logs li {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
}
.log-desc {
  color: #606266;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.empty {
  color: #909399;
  font-size: 13px;
  text-align: center;
  padding: 20px 0;
}
</style>
