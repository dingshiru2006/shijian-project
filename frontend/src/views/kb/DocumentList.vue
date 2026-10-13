<template>
  <el-card shadow="never">
    <!-- 查询表单 -->
    <el-form :inline="true" :model="query" class="query-form">
      <el-form-item label="标题">
        <el-input v-model="query.title" placeholder="文档标题" clearable @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item label="知识库">
        <el-select v-model="query.kbId" placeholder="全部" clearable style="width: 160px">
          <el-option v-for="kb in kbOptions" :key="kb.id" :label="kb.name" :value="kb.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="分类">
        <el-select v-model="query.categoryId" placeholder="全部" clearable style="width: 140px">
          <el-option v-for="c in categoryOptions" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width: 110px">
          <el-option label="已发布" :value="1" />
          <el-option label="草稿" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="Refresh" @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 工具栏 -->
    <div class="toolbar">
      <span class="toolbar-title">文档列表</span>
      <el-button v-permission="'kb:doc:add'" type="primary" :icon="Plus" @click="openCreate">
        新增文档
      </el-button>
    </div>

    <!-- 表格 -->
    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="70" align="center" />
      <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip>
        <template #default="{ row }">
          <el-link type="primary" :underline="false" @click="openDetail(row)">{{ row.title }}</el-link>
        </template>
      </el-table-column>
      <el-table-column prop="kbName" label="所属知识库" min-width="130" show-overflow-tooltip />
      <el-table-column prop="categoryName" label="分类" width="110" align="center" />
      <el-table-column label="标签" min-width="140">
        <template #default="{ row }">
          <el-tag
            v-for="name in row.tagNames || []"
            :key="name"
            size="small"
            style="margin-right: 4px"
          >{{ name }}</el-tag>
          <span v-if="!row.tagNames?.length" class="muted">-</span>
        </template>
      </el-table-column>
      <el-table-column prop="authorName" label="作者" width="90" align="center" />
      <el-table-column label="状态" width="80" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="row.status === 1 ? 'success' : 'info'">
            {{ row.status === 1 ? '已发布' : '草稿' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="viewCount" label="浏览量" width="90" align="center" />
      <el-table-column prop="createdAt" label="创建时间" width="170" align="center" />
      <el-table-column label="操作" width="160" align="center" fixed="right">
        <template #default="{ row }">
          <el-button
            v-permission="'kb:doc:edit'"
            type="primary" link
            @click="openEdit(row)"
          >编辑</el-button>
          <el-button
            v-permission="'kb:doc:delete'"
            type="danger" link
            @click="handleDelete(row)"
          >删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
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

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑文档' : '新增文档'"
      width="680px"
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="所属知识库" prop="kbId">
              <el-select v-model="form.kbId" placeholder="请选择知识库" style="width: 100%">
                <el-option v-for="kb in kbOptions" :key="kb.id" :label="kb.name" :value="kb.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属分类" prop="categoryId">
              <el-select v-model="form.categoryId" placeholder="请选择分类" style="width: 100%">
                <el-option v-for="c in categoryOptions" :key="c.id" :label="c.name" :value="c.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="摘要" prop="summary">
          <el-input v-model="form.summary" type="textarea" :rows="2" maxlength="500" />
        </el-form-item>
        <el-form-item label="正文" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="8" />
        </el-form-item>
        <el-form-item label="标签">
          <el-select
            v-model="form.tagIds"
            multiple
            filterable
            placeholder="请选择标签"
            style="width: 100%"
          >
            <el-option v-for="t in tagOptions" :key="t.id" :label="t.name" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">已发布</el-radio>
            <el-radio :value="0">草稿</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 详情抽屉 -->
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
          <el-descriptions-item label="状态">
            {{ detail.status === 1 ? '已发布' : '草稿' }}
          </el-descriptions-item>
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
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import { documentApi, kbApi, categoryApi, tagApi } from '@/api'

const route = useRoute()
const loading = ref(false)
const submitting = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  title: '',
  kbId: null,
  categoryId: null,
  status: null
})

const kbOptions = ref([])
const categoryOptions = ref([])
const tagOptions = ref([])

const dialogVisible = ref(false)
const editingId = ref(null)
const formRef = ref()
const form = reactive({
  kbId: null,
  categoryId: null,
  title: '',
  summary: '',
  content: '',
  status: 1,
  tagIds: []
})

const rules = {
  kbId: [{ required: true, message: '请选择所属知识库', trigger: 'change' }],
  categoryId: [{ required: true, message: '请选择所属分类', trigger: 'change' }],
  title: [{ required: true, message: '请输入文档标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入文档正文', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

const detailVisible = ref(false)
const detail = ref(null)

async function loadOptions() {
  const [kbRes, catRes, tagRes] = await Promise.all([
    kbApi.all(),
    categoryApi.all(),
    tagApi.all()
  ])
  kbOptions.value = kbRes.data
  categoryOptions.value = catRes.data
  tagOptions.value = tagRes.data
}

async function loadList() {
  loading.value = true
  try {
    const params = { ...query }
    Object.keys(params).forEach((k) => {
      if (params[k] === '' || params[k] === null) delete params[k]
    })
    const res = await documentApi.page(params)
    list.value = res.data.list
    total.value = res.data.total
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
  query.title = ''
  query.kbId = null
  query.categoryId = null
  query.status = null
  handleSearch()
}

function openCreate() {
  editingId.value = null
  dialogVisible.value = true
}

async function openEdit(row) {
  editingId.value = row.id
  const res = await documentApi.detail(row.id)
  Object.assign(form, {
    kbId: res.data.kbId,
    categoryId: res.data.categoryId,
    title: res.data.title,
    summary: res.data.summary,
    content: res.data.content,
    status: res.data.status,
    tagIds: res.data.tagIds || []
  })
  dialogVisible.value = true
}

async function openDetail(row) {
  const res = await documentApi.detail(row.id)
  detail.value = res.data
  detailVisible.value = true
}

function resetForm() {
  formRef.value?.resetFields()
  Object.assign(form, {
    kbId: null,
    categoryId: null,
    title: '',
    summary: '',
    content: '',
    status: 1,
    tagIds: []
  })
}

async function handleSubmit() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  submitting.value = true
  try {
    if (editingId.value) {
      await documentApi.update(editingId.value, form)
      ElMessage.success('修改成功')
    } else {
      await documentApi.create(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadList()
  } catch {
    // 拦截器已提示
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确认删除文档「${row.title}」？`, '删除确认', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消'
  }).catch(() => Promise.reject(new Error('cancel')))
  try {
    await documentApi.remove(row.id)
    ElMessage.success('删除成功')
    if (list.value.length === 1 && query.pageNum > 1) query.pageNum--
    loadList()
  } catch {
    // 拦截器已提示
  }
}

onMounted(async () => {
  // 支持从知识库列表跳转带入 kbId 过滤
  if (route.query.kbId) {
    query.kbId = Number(route.query.kbId)
  }
  await loadOptions()
  await loadList()
})
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
.muted {
  color: #c0c4cc;
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
