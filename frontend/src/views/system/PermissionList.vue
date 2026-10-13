<template>
  <el-card shadow="never">
    <el-form :inline="true" :model="query" class="query-form">
      <el-form-item label="模块">
        <el-select v-model="query.module" placeholder="全部模块" clearable style="width: 150px">
          <el-option v-for="m in modules" :key="m" :label="m" :value="m" />
        </el-select>
      </el-form-item>
      <el-form-item label="关键词">
        <el-input v-model="query.keyword" placeholder="权限编码/名称" clearable @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item label="类型">
        <el-select v-model="query.type" placeholder="全部" clearable style="width: 110px">
          <el-option label="菜单" :value="1" />
          <el-option label="按钮" :value="2" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="Refresh" @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="toolbar">
      <span class="toolbar-title">权限列表</span>
      <el-button v-permission="'system:perm:add'" type="primary" :icon="Plus" @click="openCreate">
        新增权限
      </el-button>
    </div>

    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="70" align="center" />
      <el-table-column prop="permCode" label="权限编码" min-width="160" />
      <el-table-column prop="permName" label="权限名称" min-width="130" />
      <el-table-column prop="module" label="所属模块" min-width="120" />
      <el-table-column label="类型" width="80" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="row.type === 1 ? '' : 'info'">
            {{ row.type === 1 ? '菜单' : '按钮' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="routePath" label="路由路径" min-width="140" show-overflow-tooltip />
      <el-table-column prop="createdAt" label="创建时间" width="170" align="center" />
      <el-table-column label="操作" width="160" align="center" fixed="right">
        <template #default="{ row }">
          <el-button v-permission="'system:perm:edit'" type="primary" link @click="openEdit(row)">
            编辑
          </el-button>
          <el-button v-permission="'system:perm:delete'" type="danger" link @click="handleDelete(row)">
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

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑权限' : '新增权限'"
      width="480px"
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item v-if="!editingId" label="权限编码" prop="permCode">
          <el-input v-model="form.permCode" placeholder="如 kb:doc:add" />
        </el-form-item>
        <el-form-item label="权限名称" prop="permName">
          <el-input v-model="form.permName" maxlength="50" />
        </el-form-item>
        <el-form-item label="所属模块" prop="module">
          <el-input v-model="form.module" maxlength="50" placeholder="如 文档管理" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio :value="1">菜单</el-radio>
            <el-radio :value="2">按钮</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="路由路径" prop="routePath">
          <el-input v-model="form.routePath" maxlength="100" placeholder="如 /kb/documents（可空）" />
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
import { permissionApi } from '@/api'

const loading = ref(false)
const submitting = ref(false)
const list = ref([])
const total = ref(0)
const modules = ref([])
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  module: '',
  keyword: '',
  type: null
})

const dialogVisible = ref(false)
const editingId = ref(null)
const formRef = ref()
const form = reactive({
  permCode: '',
  permName: '',
  module: '',
  type: 2,
  routePath: ''
})

const rules = {
  permCode: [
    { required: true, message: '请输入权限编码', trigger: 'blur' },
    { pattern: /^[a-zA-Z]+(:[a-zA-Z]+)+$/, message: '格式如 kb:doc:add', trigger: 'blur' }
  ],
  permName: [{ required: true, message: '请输入权限名称', trigger: 'blur' }],
  module: [{ required: true, message: '请输入所属模块', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }]
}

async function loadModules() {
  try {
    const res = await permissionApi.modules()
    modules.value = res.data
  } catch {
    // 拦截器已提示
  }
}

async function loadList() {
  loading.value = true
  try {
    const params = { ...query }
    if (params.type === null) delete params.type
    const res = await permissionApi.page(params)
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
  query.module = ''
  query.keyword = ''
  query.type = null
  handleSearch()
}

function openCreate() {
  editingId.value = null
  dialogVisible.value = true
}

async function openEdit(row) {
  editingId.value = row.id
  const res = await permissionApi.detail(row.id)
  Object.assign(form, res.data)
  dialogVisible.value = true
}

function resetForm() {
  formRef.value?.resetFields()
  Object.assign(form, { permCode: '', permName: '', module: '', type: 2, routePath: '' })
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
      await permissionApi.update(editingId.value, form)
      ElMessage.success('修改成功')
    } else {
      await permissionApi.create(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadModules()
    loadList()
  } catch {
    // 拦截器已提示
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row) {
  await ElMessageBox.confirm(
    `确认删除权限「${row.permName}」？已分配给该权限的角色将同步移除。`,
    '删除确认',
    { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
  ).catch(() => Promise.reject(new Error('cancel')))
  try {
    await permissionApi.remove(row.id)
    ElMessage.success('删除成功')
    if (list.value.length === 1 && query.pageNum > 1) query.pageNum--
    loadList()
  } catch {
    // 拦截器已提示
  }
}

onMounted(() => {
  loadModules()
  loadList()
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
</style>
