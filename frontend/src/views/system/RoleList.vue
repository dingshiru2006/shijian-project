<template>
  <el-card shadow="never">
    <el-form :inline="true" :model="query" class="query-form">
      <el-form-item label="角色编码">
        <el-input v-model="query.roleCode" placeholder="角色编码" clearable @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item label="角色名称">
        <el-input v-model="query.roleName" placeholder="角色名称" clearable @keyup.enter="handleSearch" />
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

    <div class="toolbar">
      <span class="toolbar-title">角色列表</span>
      <el-button v-permission="'system:role:add'" type="primary" :icon="Plus" @click="openCreate">
        新增角色
      </el-button>
    </div>

    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="70" align="center" />
      <el-table-column prop="roleCode" label="角色编码" min-width="130" />
      <el-table-column prop="roleName" label="角色名称" min-width="120" />
      <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
      <el-table-column prop="sort" label="排序" width="80" align="center" />
      <el-table-column label="状态" width="80" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="row.status === 1 ? 'success' : 'danger'">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="170" align="center" />
      <el-table-column label="操作" width="240" align="center" fixed="right">
        <template #default="{ row }">
          <el-button v-permission="'system:role:edit'" type="primary" link @click="openEdit(row)">
            编辑
          </el-button>
          <el-button
            v-permission="'system:role:edit'"
            type="primary" link
            @click="openAssignPermissions(row)"
          >分配权限</el-button>
          <el-button v-permission="'system:role:delete'" type="danger" link @click="handleDelete(row)">
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
      :title="editingId ? '编辑角色' : '新增角色'"
      width="480px"
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item v-if="!editingId" label="角色编码" prop="roleCode">
          <el-input v-model="form.roleCode" placeholder="大写字母开头，可含数字、下划线" />
        </el-form-item>
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" maxlength="50" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" maxlength="255" />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="form.sort" :min="0" :max="999" />
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

    <!-- 分配权限对话框 -->
    <el-dialog v-model="permVisible" title="分配权限" width="520px">
      <template #header>
        <span>为「{{ assignRow?.roleName }}」分配权限</span>
      </template>
      <div class="perm-tree-wrapper" v-loading="permLoading">
        <el-tree
          ref="permTreeRef"
          :data="permTree"
          show-checkbox
          node-key="id"
          :default-checked-keys="assignPermIds"
          :props="{ label: 'label', children: 'children' }"
        />
      </div>
      <template #footer>
        <el-button @click="permVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleAssignPermissions">确定</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import { roleApi, permissionApi } from '@/api'

const loading = ref(false)
const submitting = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  roleCode: '',
  roleName: '',
  status: null
})

const dialogVisible = ref(false)
const editingId = ref(null)
const formRef = ref()
const form = reactive({
  roleCode: '',
  roleName: '',
  description: '',
  sort: 0,
  status: 1
})

const rules = {
  roleCode: [
    { required: true, message: '请输入角色编码', trigger: 'blur' },
    { pattern: /^[A-Z][A-Z0-9_]{1,49}$/, message: '需大写字母开头，可含数字、下划线，长度 2-50', trigger: 'blur' }
  ],
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  sort: [{ required: true, message: '请输入排序', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

// 分配权限
const permVisible = ref(false)
const permLoading = ref(false)
const assignRow = ref(null)
const assignPermIds = ref([])
const permTreeRef = ref()
const allPermissions = ref([])

// 按模块分组的权限树（叶子为具体权限）
const permTree = computed(() => {
  const groups = new Map()
  for (const p of allPermissions.value) {
    if (!groups.has(p.module)) groups.set(p.module, [])
    groups.get(p.module).push({
      id: p.id,
      label: `${p.permName}（${p.permCode}）`,
      type: p.type
    })
  }
  return [...groups.entries()].map(([module, children]) => ({
    id: `module-${module}`,
    label: module,
    children
  }))
})

async function loadList() {
  loading.value = true
  try {
    const params = { ...query }
    if (params.status === null) delete params.status
    const res = await roleApi.page(params)
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
  query.roleCode = ''
  query.roleName = ''
  query.status = null
  handleSearch()
}

function openCreate() {
  editingId.value = null
  dialogVisible.value = true
}

async function openEdit(row) {
  editingId.value = row.id
  const res = await roleApi.detail(row.id)
  Object.assign(form, {
    roleCode: res.data.roleCode,
    roleName: res.data.roleName,
    description: res.data.description,
    sort: res.data.sort,
    status: res.data.status
  })
  dialogVisible.value = true
}

function resetForm() {
  formRef.value?.resetFields()
  Object.assign(form, { roleCode: '', roleName: '', description: '', sort: 0, status: 1 })
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
      await roleApi.update(editingId.value, form)
      ElMessage.success('修改成功')
    } else {
      await roleApi.create(form)
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

async function openAssignPermissions(row) {
  assignRow.value = row
  assignPermIds.value = []
  permVisible.value = true
  permLoading.value = true
  try {
    // 并行加载全部权限（树）与当前角色已有权限（回显）
    const [allRes, detailRes] = await Promise.all([
      permissionApi.page({ pageNum: 1, pageSize: 100 }),
      roleApi.detail(row.id)
    ])
    allPermissions.value = allRes.data.list
    assignPermIds.value = detailRes.data.permissionIds || []
  } catch {
    // 拦截器已提示
  } finally {
    permLoading.value = false
  }
}

async function handleAssignPermissions() {
  // 仅收集叶子节点（真实权限），排除模块分组节点
  const checked = permTreeRef.value.getCheckedKeys()
  const halfChecked = permTreeRef.value.getHalfCheckedKeys()
  const leafIds = [...checked, ...halfChecked].filter(
    (id) => typeof id === 'number'
  )
  submitting.value = true
  try {
    await roleApi.assignPermissions(assignRow.value.id, leafIds)
    ElMessage.success('权限分配成功')
    permVisible.value = false
    loadList()
  } catch {
    // 拦截器已提示
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确认删除角色「${row.roleName}」？`, '删除确认', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消'
  }).catch(() => Promise.reject(new Error('cancel')))
  try {
    await roleApi.remove(row.id)
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
.perm-tree-wrapper {
  max-height: 420px;
  overflow: auto;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 10px;
}
</style>
