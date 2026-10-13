<template>
  <el-card shadow="never">
    <el-form :inline="true" :model="query" class="query-form">
      <el-form-item label="用户名">
        <el-input v-model="query.username" placeholder="用户名" clearable @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item label="昵称">
        <el-input v-model="query.nickname" placeholder="昵称" clearable @keyup.enter="handleSearch" />
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
      <span class="toolbar-title">用户列表</span>
      <el-button v-permission="'system:user:add'" type="primary" :icon="Plus" @click="openCreate">
        新增用户
      </el-button>
    </div>

    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="70" align="center" />
      <el-table-column prop="username" label="用户名" min-width="120" />
      <el-table-column prop="nickname" label="昵称" min-width="110" />
      <el-table-column prop="email" label="邮箱" min-width="160" show-overflow-tooltip />
      <el-table-column prop="phone" label="手机号" width="130" align="center" />
      <el-table-column label="角色" min-width="150">
        <template #default="{ row }">
          <el-tag
            v-for="name in row.roleNames || []"
            :key="name"
            size="small"
            style="margin-right: 4px"
          >{{ name }}</el-tag>
          <span v-if="!row.roleNames?.length" class="muted">-</span>
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
      <el-table-column label="操作" width="230" align="center" fixed="right">
        <template #default="{ row }">
          <el-button v-permission="'system:user:edit'" type="primary" link @click="openEdit(row)">
            编辑
          </el-button>
          <el-button
            v-permission="'system:user:edit'"
            type="primary" link
            @click="openAssignRoles(row)"
          >分配角色</el-button>
          <el-button
            v-permission="'system:user:edit'"
            type="warning" link
            @click="openResetPassword(row)"
          >重置密码</el-button>
          <el-button v-permission="'system:user:delete'" type="danger" link @click="handleDelete(row)">
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
      :title="editingId ? '编辑用户' : '新增用户'"
      width="500px"
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item v-if="!editingId" label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="字母/数字/下划线，2-50 位" />
        </el-form-item>
        <el-form-item v-if="!editingId" label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="6-32 位" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" maxlength="50" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" maxlength="11" />
        </el-form-item>
        <el-form-item v-if="!editingId" label="初始角色">
          <el-select v-model="form.roleIds" multiple placeholder="可多选" style="width: 100%">
            <el-option v-for="r in roleOptions" :key="r.id" :label="r.roleName" :value="r.id" />
          </el-select>
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

    <!-- 分配角色对话框 -->
    <el-dialog v-model="assignVisible" title="分配角色" width="440px">
      <el-form label-width="70px">
        <el-form-item label="用户">
          <span>{{ assignRow?.nickname }}（{{ assignRow?.username }}）</span>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="assignRoleIds" multiple style="width: 100%">
            <el-option v-for="r in roleOptions" :key="r.id" :label="r.roleName" :value="r.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleAssignRoles">确定</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码对话框 -->
    <el-dialog v-model="pwdVisible" title="重置密码" width="420px">
      <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="80px">
        <el-form-item label="用户">
          <span>{{ pwdRow?.nickname }}（{{ pwdRow?.username }}）</span>
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="6-32 位" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleResetPassword">确定</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import { userApi, roleApi } from '@/api'

const loading = ref(false)
const submitting = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  username: '',
  nickname: '',
  status: null
})

const roleOptions = ref([])

const dialogVisible = ref(false)
const editingId = ref(null)
const formRef = ref()
const form = reactive({
  username: '',
  password: '',
  nickname: '',
  email: '',
  phone: '',
  status: 1,
  roleIds: []
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]{2,50}$/, message: '只能包含字母、数字、下划线，长度 2-50', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度需在 6-32 个字符之间', trigger: 'blur' }
  ],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

// 分配角色
const assignVisible = ref(false)
const assignRow = ref(null)
const assignRoleIds = ref([])

// 重置密码
const pwdVisible = ref(false)
const pwdRow = ref(null)
const pwdFormRef = ref()
const pwdForm = reactive({ newPassword: '' })
const pwdRules = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度需在 6-32 个字符之间', trigger: 'blur' }
  ]
}

async function loadList() {
  loading.value = true
  try {
    const params = { ...query }
    if (params.status === null) delete params.status
    const res = await userApi.page(params)
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
  query.username = ''
  query.nickname = ''
  query.status = null
  handleSearch()
}

function openCreate() {
  editingId.value = null
  dialogVisible.value = true
}

async function openEdit(row) {
  editingId.value = row.id
  const res = await userApi.detail(row.id)
  Object.assign(form, {
    username: res.data.username,
    password: '',
    nickname: res.data.nickname,
    email: res.data.email,
    phone: res.data.phone,
    status: res.data.status,
    roleIds: res.data.roleIds || []
  })
  dialogVisible.value = true
}

function resetForm() {
  formRef.value?.resetFields()
  Object.assign(form, {
    username: '',
    password: '',
    nickname: '',
    email: '',
    phone: '',
    status: 1,
    roleIds: []
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
      await userApi.update(editingId.value, {
        nickname: form.nickname,
        email: form.email,
        phone: form.phone,
        status: form.status
      })
      ElMessage.success('修改成功')
    } else {
      await userApi.create({ ...form })
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

async function openAssignRoles(row) {
  assignRow.value = row
  assignRoleIds.value = row.roleIds || []
  assignVisible.value = true
}

async function handleAssignRoles() {
  if (!assignRoleIds.value.length) {
    ElMessage.warning('请至少选择一个角色')
    return
  }
  submitting.value = true
  try {
    await userApi.assignRoles(assignRow.value.id, assignRoleIds.value)
    ElMessage.success('角色分配成功')
    assignVisible.value = false
    loadList()
  } catch {
    // 拦截器已提示
  } finally {
    submitting.value = false
  }
}

function openResetPassword(row) {
  pwdRow.value = row
  pwdForm.newPassword = ''
  pwdVisible.value = true
}

async function handleResetPassword() {
  try {
    await pwdFormRef.value.validate()
  } catch {
    return
  }
  submitting.value = true
  try {
    await userApi.resetPassword(pwdRow.value.id, pwdForm.newPassword)
    ElMessage.success('密码重置成功')
    pwdVisible.value = false
  } catch {
    // 拦截器已提示
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确认删除用户「${row.username}」？`, '删除确认', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消'
  }).catch(() => Promise.reject(new Error('cancel')))
  try {
    await userApi.remove(row.id)
    ElMessage.success('删除成功')
    if (list.value.length === 1 && query.pageNum > 1) query.pageNum--
    loadList()
  } catch {
    // 拦截器已提示
  }
}

onMounted(async () => {
  const roleRes = await roleApi.all()
  roleOptions.value = roleRes.data
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
</style>
