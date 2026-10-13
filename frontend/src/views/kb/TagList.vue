<template>
  <el-card shadow="never">
    <el-form :inline="true" :model="query" class="query-form">
      <el-form-item label="名称">
        <el-input v-model="query.name" placeholder="标签名称" clearable @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="Refresh" @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="toolbar">
      <span class="toolbar-title">标签列表</span>
      <el-button v-permission="'kb:tag:add'" type="primary" :icon="Plus" @click="openCreate">
        新增标签
      </el-button>
    </div>

    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="70" align="center" />
      <el-table-column label="标签" min-width="160">
        <template #default="{ row }">
          <el-tag :color="row.color || '#409eff'" effect="dark" style="border: none">
            {{ row.name }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="color" label="颜色值" width="140" align="center">
        <template #default="{ row }">
          <span class="color-cell">
            <span class="color-dot" :style="{ background: row.color || '#409eff' }"></span>
            {{ row.color || '-' }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="170" align="center" />
      <el-table-column label="操作" width="160" align="center" fixed="right">
        <template #default="{ row }">
          <el-button v-permission="'kb:tag:edit'" type="primary" link @click="openEdit(row)">
            编辑
          </el-button>
          <el-button v-permission="'kb:tag:delete'" type="danger" link @click="handleDelete(row)">
            删除
          </el-button>
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

    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑标签' : '新增标签'"
      width="440px"
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="颜色" prop="color">
          <div class="color-picker-row">
            <el-color-picker v-model="form.color" />
            <el-input v-model="form.color" placeholder="如 #409eff" style="flex: 1" />
          </div>
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import { tagApi } from '@/api'

const loading = ref(false)
const submitting = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10, name: '' })

const dialogVisible = ref(false)
const editingId = ref(null)
const formRef = ref()
const form = reactive({ name: '', color: '#409eff' })

const rules = {
  name: [{ required: true, message: '请输入标签名称', trigger: 'blur' }]
}

async function loadList() {
  loading.value = true
  try {
    const params = { ...query }
    if (!params.name) delete params.name
    const res = await tagApi.page(params)
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
  handleSearch()
}

function openCreate() {
  editingId.value = null
  dialogVisible.value = true
}

async function openEdit(row) {
  editingId.value = row.id
  const res = await tagApi.detail(row.id)
  Object.assign(form, res.data)
  dialogVisible.value = true
}

function resetForm() {
  formRef.value?.resetFields()
  Object.assign(form, { name: '', color: '#409eff' })
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
      await tagApi.update(editingId.value, form)
      ElMessage.success('修改成功')
    } else {
      await tagApi.create(form)
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
  await ElMessageBox.confirm(`确认删除标签「${row.name}」？`, '删除确认', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消'
  }).catch(() => Promise.reject(new Error('cancel')))
  try {
    await tagApi.remove(row.id)
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

.color-cell {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.color-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  display: inline-block;
}
.color-picker-row {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
}
</style>
