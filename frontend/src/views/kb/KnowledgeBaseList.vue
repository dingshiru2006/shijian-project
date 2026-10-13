<template>
  <el-card shadow="never">
    <!-- 查询表单 -->
    <el-form :inline="true" :model="query" class="query-form">
      <el-form-item label="名称">
        <el-input v-model="query.name" placeholder="知识库名称" clearable @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item label="编码">
        <el-input v-model="query.code" placeholder="知识库编码" clearable @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="Refresh" @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 工具栏 -->
    <div class="toolbar">
      <span class="toolbar-title">知识库列表</span>
      <el-button v-permission="'kb:kb:add'" type="primary" :icon="Plus" @click="openCreate">
        新增知识库
      </el-button>
    </div>

    <!-- 表格 -->
    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="70" align="center" />
      <el-table-column prop="name" label="名称" min-width="160" show-overflow-tooltip />
      <el-table-column prop="code" label="编码" min-width="130" />
      <el-table-column prop="description" label="简介" min-width="200" show-overflow-tooltip />
      <el-table-column label="可见性" width="90" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="row.visibility === 1 ? 'success' : 'info'">
            {{ row.visibility === 1 ? '公开' : '私有' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="文档数" width="90" align="center">
        <template #default="{ row }">
          <el-link type="primary" :underline="false" @click="goDocuments(row.id)">
            {{ row.docCount ?? 0 }}
          </el-link>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="80" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="row.status === 1 ? 'success' : 'danger'">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="170" align="center" />
      <el-table-column label="操作" width="160" align="center" fixed="right">
        <template #default="{ row }">
          <el-button
            v-permission="'kb:kb:edit'"
            type="primary" link
            @click="openEdit(row)"
          >编辑</el-button>
          <el-button
            v-permission="'kb:kb:delete'"
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
      :title="editingId ? '编辑知识库' : '新增知识库'"
      width="520px"
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="编码" prop="code">
          <el-input
            v-model="form.code"
            :disabled="!!editingId"
            placeholder="字母/数字/中划线/下划线"
          />
        </el-form-item>
        <el-form-item label="简介" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" maxlength="500" />
        </el-form-item>
        <el-form-item label="可见性" prop="visibility">
          <el-radio-group v-model="form.visibility">
            <el-radio :value="1">公开</el-radio>
            <el-radio :value="0">私有</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import { kbApi } from '@/api'

const router = useRouter()
const loading = ref(false)
const submitting = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10, name: '', code: '', status: null })

const dialogVisible = ref(false)
const editingId = ref(null)
const formRef = ref()
const form = reactive({
  name: '',
  code: '',
  description: '',
  visibility: 1,
  status: 1
})

const rules = {
  name: [{ required: true, message: '请输入知识库名称', trigger: 'blur' }],
  code: [
    { required: true, message: '请输入知识库编码', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_-]{2,50}$/, message: '编码只能包含字母、数字、中划线、下划线，长度 2-50', trigger: 'blur' }
  ],
  visibility: [{ required: true, message: '请选择可见性', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

async function loadList() {
  loading.value = true
  try {
    const res = await kbApi.page(query)
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
  query.name = ''
  query.code = ''
  query.status = null
  handleSearch()
}

function goDocuments(kbId) {
  router.push({ path: '/kb/documents', query: { kbId } })
}

function openCreate() {
  editingId.value = null
  dialogVisible.value = true
}

async function openEdit(row) {
  editingId.value = row.id
  const res = await kbApi.detail(row.id)
  Object.assign(form, {
    name: res.data.name,
    code: res.data.code,
    description: res.data.description,
    visibility: res.data.visibility,
    status: res.data.status
  })
  dialogVisible.value = true
}

function resetForm() {
  formRef.value?.resetFields()
  Object.assign(form, { name: '', code: '', description: '', visibility: 1, status: 1 })
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
      await kbApi.update(editingId.value, form)
      ElMessage.success('修改成功')
    } else {
      await kbApi.create(form)
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
  await ElMessageBox.confirm(
    `确认删除知识库「${row.name}」？其下文档将一并删除。`,
    '删除确认',
    { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
  ).catch(() => Promise.reject(new Error('cancel')))
  try {
    await kbApi.remove(row.id)
    ElMessage.success('删除成功')
    if (list.value.length === 1 && query.pageNum > 1) query.pageNum--
    loadList()
  } catch {
    // 拦截器已提示
  }
}

onMounted(loadList)
</script>

<style scoped>
.query-form {
  margin-bottom: 0;
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
