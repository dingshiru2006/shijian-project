<template>
  <el-card shadow="never">
    <!-- 检索表单 -->
    <el-form :inline="true" :model="query" class="query-form">
      <el-form-item>
        <el-input
          v-model="query.keyword"
          placeholder="输入关键词，检索标题 / 摘要 / 正文"
          clearable
          size="large"
          style="width: 380px"
          :prefix-icon="Search"
          @keyup.enter="handleSearch"
        />
      </el-form-item>
      <el-form-item>
        <el-select v-model="query.kbId" placeholder="全部知识库" clearable style="width: 150px">
          <el-option v-for="kb in kbOptions" :key="kb.id" :label="kb.name" :value="kb.id" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-select v-model="query.categoryId" placeholder="全部分类" clearable style="width: 130px">
          <el-option v-for="c in categoryOptions" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-input v-model="query.tagName" placeholder="标签名" clearable style="width: 110px" />
      </el-form-item>
      <el-form-item>
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 100px">
          <el-option label="已发布" :value="1" />
          <el-option label="草稿" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" size="large" :icon="Search" @click="handleSearch">检索</el-button>
        <el-button size="large" :icon="Refresh" @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="result-meta" v-if="searched">
      共找到 <b>{{ total }}</b> 条结果，耗时信息见各条目相关度得分
    </div>

    <!-- 检索结果 -->
    <div v-loading="loading" class="result-list">
      <el-empty v-if="searched && !list.length" description="没有找到匹配的文档" />

      <el-card
        v-for="hit in list"
        :key="hit.id"
        shadow="hover"
        class="hit-card"
        :body-style="{ padding: '16px 20px' }"
      >
        <div class="hit-header">
          <h3 class="hit-title" v-html="hit.highlightTitle || hit.title"></h3>
          <span class="hit-score">相关度 {{ hit.score?.toFixed(2) ?? '-' }}</span>
        </div>
        <div class="hit-tags">
          <el-tag size="small" type="primary" effect="plain">{{ hit.kbName }}</el-tag>
          <el-tag size="small" type="info" effect="plain">{{ hit.categoryName }}</el-tag>
          <el-tag
            v-for="name in hit.tagNames || []"
            :key="name"
            size="small"
            type="warning"
            effect="plain"
          >{{ name }}</el-tag>
          <el-tag size="small" :type="hit.status === 1 ? 'success' : 'info'" effect="plain">
            {{ hit.status === 1 ? '已发布' : '草稿' }}
          </el-tag>
          <span class="hit-author">{{ hit.authorName }} · {{ hit.createdAtText }}</span>
        </div>
        <p v-if="hit.highlightSummary" class="hit-summary" v-html="hit.highlightSummary"></p>
        <p v-else-if="hit.summary" class="hit-summary plain">{{ hit.summary }}</p>
        <p class="hit-content" v-html="hit.highlightContent || hit.content"></p>
        <div class="hit-footer">
          <el-button type="primary" link @click="openDocument(hit.id)">查看文档</el-button>
        </div>
      </el-card>
    </div>

    <!-- 分页 -->
    <el-pagination
      v-if="total > 0"
      class="pagination"
      v-model:current-page="query.pageNum"
      v-model:page-size="query.pageSize"
      :total="total"
      :page-sizes="[5, 10, 20]"
      layout="total, sizes, prev, pager, next, jumper"
      @size-change="loadList"
      @current-change="loadList"
    />

    <!-- 文档详情抽屉 -->
    <el-drawer v-model="detailVisible" title="文档详情" size="480px">
      <template v-if="detail">
        <h3 class="detail-title">{{ detail.title }}</h3>
        <div class="detail-meta">
          <el-tag size="small">{{ detail.kbName }}</el-tag>
          <el-tag size="small" type="info">{{ detail.categoryName }}</el-tag>
          <el-tag
            v-for="name in detail.tagNames || []"
            :key="name"
            size="small"
            type="warning"
          >{{ name }}</el-tag>
        </div>
        <el-descriptions :column="2" border size="small" style="margin: 14px 0">
          <el-descriptions-item label="作者">{{ detail.authorName }}</el-descriptions-item>
          <el-descriptions-item label="浏览量">{{ detail.viewCount }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ detail.createdAt }}</el-descriptions-item>
        </el-descriptions>
        <div class="detail-summary"><b>摘要：</b>{{ detail.summary || '无' }}</div>
        <div class="detail-content">{{ detail.content }}</div>
      </template>
    </el-drawer>
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Search, Refresh } from '@element-plus/icons-vue'
import { searchApi, kbApi, categoryApi, documentApi } from '@/api'

const loading = ref(false)
const searched = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  kbId: null,
  categoryId: null,
  tagName: '',
  status: null
})

const kbOptions = ref([])
const categoryOptions = ref([])

const detailVisible = ref(false)
const detail = ref(null)

async function loadList() {
  loading.value = true
  try {
    const params = { ...query }
    Object.keys(params).forEach((k) => {
      if (params[k] === '' || params[k] === null) delete params[k]
    })
    const res = await searchApi.search(params)
    list.value = res.data.list
    total.value = res.data.total
    searched.value = true
  } catch {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.pageNum = 1
  loadList()
}

function handleReset() {
  query.keyword = ''
  query.kbId = null
  query.categoryId = null
  query.tagName = ''
  query.status = null
  handleSearch()
}

async function openDocument(id) {
  const res = await documentApi.detail(id)
  detail.value = res.data
  detailVisible.value = true
}

onMounted(async () => {
  const [kbRes, catRes] = await Promise.all([kbApi.all(), categoryApi.all()])
  kbOptions.value = kbRes.data
  categoryOptions.value = catRes.data
  await loadList()
})
</script>

<style scoped>
.result-meta {
  margin-bottom: 14px;
  font-size: 13px;
  color: #606266;
}

.result-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-height: 200px;
}

.hit-header {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 12px;
}
.hit-title {
  margin: 0;
  font-size: 17px;
  color: #1f5bb4;
}
.hit-title :deep(em) {
  color: #d4380d;
  font-style: normal;
  font-weight: 700;
}
.hit-score {
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
}

.hit-tags {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  margin: 10px 0;
}
.hit-author {
  font-size: 12px;
  color: #909399;
  margin-left: auto;
}

.hit-summary,
.hit-content {
  margin: 6px 0;
  font-size: 13px;
  color: #606266;
  line-height: 1.7;
}
.hit-summary :deep(em),
.hit-content :deep(em) {
  color: #d4380d;
  font-style: normal;
  font-weight: 700;
}
.hit-summary.plain {
  color: #909399;
}
.hit-content {
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.hit-footer {
  margin-top: 8px;
  text-align: right;
}

.pagination {
  margin-top: 14px;
  justify-content: flex-end;
}

.detail-title {
  margin: 0 0 10px;
  font-size: 18px;
  color: #303133;
}
.detail-meta {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}
.detail-summary {
  font-size: 13px;
  color: #606266;
  background: #f4f4f5;
  padding: 10px 12px;
  border-radius: 6px;
  margin-bottom: 12px;
}
.detail-content {
  font-size: 14px;
  color: #303133;
  line-height: 1.8;
  white-space: pre-wrap;
}
</style>
