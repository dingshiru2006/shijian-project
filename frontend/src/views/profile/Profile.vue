<template>
  <div>
    <el-row :gutter="16">
      <!-- 个人信息 -->
      <el-col :span="10">
        <el-card shadow="never">
          <template #header>
            <span>个人信息</span>
          </template>
          <div class="profile-header">
            <el-avatar :size="64" :src="userStore.userInfo?.avatar || ''">
              {{ (userStore.nickname || 'U').charAt(0) }}
            </el-avatar>
            <div class="profile-brief">
              <h3>{{ userStore.userInfo?.nickname }}</h3>
              <p class="muted">{{ userStore.userInfo?.username }}</p>
            </div>
          </div>
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="用户 ID">
              {{ userStore.userInfo?.id }}
            </el-descriptions-item>
            <el-descriptions-item label="用户名">
              {{ userStore.userInfo?.username }}
            </el-descriptions-item>
            <el-descriptions-item label="昵称">
              {{ userStore.userInfo?.nickname }}
            </el-descriptions-item>
            <el-descriptions-item label="角色">
              <el-tag
                v-for="r in userStore.roles"
                :key="r"
                size="small"
                style="margin-right: 4px"
              >{{ r }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="权限数量">
              {{ userStore.permissions.length }} 项
            </el-descriptions-item>
          </el-descriptions>

          <div class="perm-panel">
            <div class="perm-panel-title">已授予权限编码</div>
            <div class="perm-tags">
              <el-tag
                v-for="p in userStore.permissions"
                :key="p"
                size="small"
                type="info"
                effect="plain"
                style="margin: 2px 4px 2px 0"
              >{{ p }}</el-tag>
              <span v-if="!userStore.permissions.length" class="muted">无</span>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 修改密码 -->
      <el-col :span="14">
        <el-card shadow="never">
          <template #header>
            <span>修改密码</span>
          </template>
          <el-form
            ref="pwdFormRef"
            :model="pwdForm"
            :rules="pwdRules"
            label-width="90px"
            style="max-width: 460px"
          >
            <el-form-item label="原密码" prop="oldPassword">
              <el-input v-model="pwdForm.oldPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="6-32 位" />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input v-model="pwdForm.confirmPassword" type="password" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="submitting" @click="handleUpdatePassword">
                确认修改
              </el-button>
              <el-button @click="resetPwdForm">重置</el-button>
            </el-form-item>
          </el-form>
          <el-alert type="warning" :closable="false" show-icon>
            <template #title>
              修改成功后当前登录状态将失效，需使用新密码重新登录。
            </template>
          </el-alert>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { authApi } from '@/api'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const submitting = ref(false)
const pwdFormRef = ref()
const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const validateConfirm = (rule, value, callback) => {
  if (value !== pwdForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度需在 6-32 个字符之间', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' }
  ]
}

function resetPwdForm() {
  pwdFormRef.value?.resetFields()
  pwdForm.oldPassword = ''
  pwdForm.newPassword = ''
  pwdForm.confirmPassword = ''
}

async function handleUpdatePassword() {
  try {
    await pwdFormRef.value.validate()
  } catch {
    return
  }
  submitting.value = true
  try {
    await authApi.updatePassword({
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword,
      confirmPassword: pwdForm.confirmPassword
    })
    ElMessage.success('密码修改成功，请重新登录')
    userStore.clearSession()
    router.push('/login')
  } catch {
    // 拦截器已提示
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.profile-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 18px;
}
.profile-brief h3 {
  margin: 0 0 4px;
  font-size: 18px;
  color: #303133;
}
.profile-brief p {
  margin: 0;
  font-size: 13px;
}
.muted {
  color: #909399;
}

.perm-panel {
  margin-top: 18px;
}
.perm-panel-title {
  font-size: 13px;
  color: #606266;
  margin-bottom: 8px;
}
.perm-tags {
  max-height: 220px;
  overflow: auto;
}
</style>
