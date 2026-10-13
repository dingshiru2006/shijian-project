import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

// 统一请求实例：baseURL 走 Vite 代理 /api -> http://localhost:8080/api
const request = axios.create({
  baseURL: '/api',
  timeout: 30000
})

// 请求拦截：附加 JWT
request.interceptors.request.use(
  (config) => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截：统一处理业务码 / 401 / 网络错误
request.interceptors.response.use(
  (response) => {
    const res = response.data
    // 文件下载等场景直接返回原始响应
    if (res === null || res === undefined || typeof res !== 'object' || !('code' in res)) {
      return res
    }
    if (res.code === 200) {
      return res
    }
    // 业务异常（含 600 业务错误 / 401 未认证 / 403 无权限）
    if (res.code === 401) {
      handleUnauthorized()
      return Promise.reject(new Error(res.message || '登录已失效，请重新登录'))
    }
    ElMessage.error(res.message || '请求失败')
    return Promise.reject(new Error(res.message || '请求失败'))
  },
  (error) => {
    const status = error.response?.status
    const body = error.response?.data
    if (status === 401 || body?.code === 401) {
      handleUnauthorized()
      return Promise.reject(new Error('登录已失效，请重新登录'))
    }
    if (status === 403) {
      ElMessage.error(body?.message || '没有访问权限')
      return Promise.reject(error)
    }
    ElMessage.error(body?.message || error.message || '网络异常，请稍后重试')
    return Promise.reject(error)
  }
)

let redirectingToLogin = false
function handleUnauthorized() {
  const userStore = useUserStore()
  userStore.clearSession()
  const current = window.location.hash
  if (!redirectingToLogin && !current.startsWith('#/login')) {
    redirectingToLogin = true
    ElMessage.warning('登录状态已失效，请重新登录')
    window.location.hash = '#/login'
    setTimeout(() => { redirectingToLogin = false }, 1000)
  }
}

export default request
