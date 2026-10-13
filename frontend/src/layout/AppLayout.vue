<template>
  <div class="layout">
    <!-- 侧边栏 -->
    <aside class="sidebar" :class="{ collapsed: collapsed }">
      <div class="logo">
        <el-icon :size="22"><Reading /></el-icon>
        <span v-show="!collapsed" class="logo-text">知库 KB-PLATFORM</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        :collapse="collapsed"
        background-color="#001529"
        text-color="#b7c0cd"
        active-text-color="#ffffff"
        router
      >
        <template v-for="group in menuGroups" :key="group.name">
          <div v-show="!collapsed" class="menu-group-title">{{ group.name }}</div>
          <el-menu-item
            v-for="item in group.items"
            :key="item.path"
            :index="item.path"
          >
            <el-icon><component :is="item.icon" /></el-icon>
            <template #title>{{ item.title }}</template>
          </el-menu-item>
        </template>
      </el-menu>
    </aside>

    <!-- 主区域 -->
    <div class="main">
      <header class="header">
        <div class="header-left">
          <el-icon class="collapse-btn" :size="20" @click="collapsed = !collapsed">
            <Fold v-if="!collapsed" />
            <Expand v-else />
          </el-icon>
          <span class="page-title">{{ currentTitle }}</span>
        </div>
        <div class="header-right">
          <el-tag size="small" effect="plain" type="info" class="role-tag">
            {{ rolesText }}
          </el-tag>
          <el-dropdown trigger="click" @command="handleCommand">
            <span class="user-entry">
              <el-avatar :size="30" :src="userStore.userInfo?.avatar || ''">
                {{ (userStore.nickname || 'U').charAt(0) }}
              </el-avatar>
              <span class="username">{{ userStore.nickname || userStore.userInfo?.username }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon> 个人中心
                </el-dropdown-item>
                <el-dropdown-item command="logout" divided>
                  <el-icon><SwitchButton /></el-icon> 退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main class="content">
        <router-view v-slot="{ Component }">
          <keep-alive :include="['DocumentList']">
            <component :is="Component" />
          </keep-alive>
        </router-view>
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { mainRoutes } from '@/router/index'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const collapsed = ref(false)

// 菜单分组：按 meta.group 聚合，并按 meta.perm 过滤
const GROUP_ORDER = ['工作台', '知识库', '检索中心', '系统管理']
const menuGroups = computed(() => {
  const groups = new Map()
  for (const r of mainRoutes) {
    const meta = r.meta
    if (!meta?.group) continue
    if (meta.perm && !userStore.hasPermission(meta.perm)) continue
    if (!groups.has(meta.group)) groups.set(meta.group, [])
    groups.get(meta.group).push({
      path: '/' + r.path,
      title: meta.title,
      icon: meta.icon || 'Menu'
    })
  }
  return GROUP_ORDER
    .filter((name) => groups.has(name))
    .map((name) => ({ name, items: groups.get(name) }))
})

const activeMenu = computed(() => route.path)
const currentTitle = computed(() => route.meta.title || '工作台')
const rolesText = computed(() => userStore.roles.join(' / ') || '访客')

async function handleCommand(command) {
  if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'logout') {
    await ElMessageBox.confirm('确认退出登录吗？', '提示', {
      type: 'warning',
      confirmButtonText: '退出',
      cancelButtonText: '取消'
    }).catch(() => Promise.reject(new Error('cancel')))
    await userStore.logout()
    router.push('/login')
  }
}
</script>

<style scoped>
.layout {
  display: flex;
  height: 100vh;
}

.sidebar {
  width: 220px;
  background-color: #001529;
  transition: width 0.2s;
  overflow-x: hidden;
  display: flex;
  flex-direction: column;
  height: 100vh;
}
.sidebar.collapsed {
  width: 64px;
}

/* 菜单区域独立滚动，避免条目过多时溢出视口 */
.sidebar :deep(.el-menu) {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
}
.sidebar :deep(.el-menu--collapse) {
  overflow-y: visible;
}

.logo {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #fff;
  font-weight: 600;
  white-space: nowrap;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}
.logo-text {
  font-size: 15px;
  letter-spacing: 0.5px;
}

.menu-group-title {
  padding: 14px 16px 6px;
  font-size: 12px;
  color: #5a6b7f;
  white-space: nowrap;
}

.sidebar :deep(.el-menu) {
  border-right: none;
}

.main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.header {
  height: 56px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  position: relative;
  z-index: 10;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 14px;
}
.collapse-btn {
  cursor: pointer;
  color: #606266;
}
.page-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 14px;
}
.role-tag {
  text-transform: uppercase;
}
.user-entry {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  outline: none;
}
.username {
  font-size: 14px;
  color: #303133;
}

.content {
  flex: 1;
  padding: 16px;
  overflow: auto;
}
</style>
