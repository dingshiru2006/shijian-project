<template>
  <div class="login-page">
    <div class="login-panel">
      <div class="brand">
        <el-icon :size="40" color="#409eff"><Reading /></el-icon>
        <h1>知库 · 企业知识库平台</h1>
        <p class="subtitle">Spring Boot + Vue 3 + Elasticsearch + RabbitMQ + Redis</p>
      </div>
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        size="large"
        @keyup.enter="handleLogin"
      >
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>
        <el-button
          type="primary"
          class="login-btn"
          size="large"
          :loading="loading"
          @click="handleLogin"
        >
          登 录
        </el-button>
      </el-form>
      <div class="tips">
        <el-tag size="small" type="info">admin / Admin@123</el-tag>
        <el-tag size="small" type="info">editor / Editor@123</el-tag>
        <el-tag size="small" type="info">lisi / Li@123</el-tag>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref()
const loading = ref(false)
const form = reactive({ username: 'admin', password: 'Admin@123' })

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  loading.value = true
  try {
    await userStore.login(form.username, form.password)
    ElMessage.success('登录成功')
    const redirect = route.query.redirect || '/dashboard'
    router.push(redirect)
  } catch {
    // 错误提示由请求拦截器统一处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1e3a5f 0%, #0f2440 50%, #001529 100%);
}

.login-panel {
  width: 400px;
  padding: 40px 36px 28px;
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.25);
}

.brand {
  text-align: center;
  margin-bottom: 28px;
}
.brand h1 {
  margin: 12px 0 6px;
  font-size: 22px;
  color: #303133;
}
.subtitle {
  margin: 0;
  font-size: 12px;
  color: #909399;
}

.login-btn {
  width: 100%;
  margin-top: 4px;
}

.tips {
  margin-top: 18px;
  display: flex;
  justify-content: center;
  gap: 8px;
  flex-wrap: wrap;
}
</style>
