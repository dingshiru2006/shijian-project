import { defineStore } from 'pinia'
import { authApi } from '@/api'

const TOKEN_KEY = 'kb_token'

// 用户会话状态：Token + 用户信息（角色/权限，驱动菜单与按钮级 RBAC）
export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    userInfo: null
  }),

  getters: {
    isLoggedIn: (state) => !!state.token,
    roles: (state) => state.userInfo?.roles || [],
    permissions: (state) => state.userInfo?.permissions || [],
    nickname: (state) => state.userInfo?.nickname || state.userInfo?.username || ''
  },

  actions: {
    setToken(token) {
      this.token = token
      localStorage.setItem(TOKEN_KEY, token)
    },

    async login(username, password) {
      const res = await authApi.login({ username, password })
      this.setToken(res.data.token)
      this.userInfo = res.data.userInfo
      return res
    },

    async fetchUserInfo() {
      const res = await authApi.info()
      this.userInfo = res.data
      return res.data
    },

    async logout() {
      try {
        if (this.token) {
          await authApi.logout()
        }
      } finally {
        this.clearSession()
      }
    },

    clearSession() {
      this.token = ''
      this.userInfo = null
      localStorage.removeItem(TOKEN_KEY)
    },

    // 判断是否拥有某权限编码
    hasPermission(code) {
      if (this.roles.includes('ADMIN')) return true
      return this.permissions.includes(code)
    }
  }
})
