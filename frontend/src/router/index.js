import { createRouter, createWebHashHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

// 主布局子路由：meta.perm 为菜单可见所需权限（同时用于按钮级判断）
export const mainRoutes = [
  {
    path: 'dashboard',
    name: 'Dashboard',
    component: () => import('@/views/Dashboard.vue'),
    meta: { title: '工作台', icon: 'Odometer', group: '工作台' }
  },
  {
    path: 'kb/knowledge-bases',
    name: 'KnowledgeBaseList',
    component: () => import('@/views/kb/KnowledgeBaseList.vue'),
    meta: { title: '知识库管理', icon: 'Reading', group: '知识库', perm: 'kb:kb:view' }
  },
  {
    path: 'kb/documents',
    name: 'DocumentList',
    component: () => import('@/views/kb/DocumentList.vue'),
    meta: { title: '文档管理', icon: 'Document', group: '知识库', perm: 'kb:doc:view' }
  },
  {
    path: 'kb/categories',
    name: 'CategoryList',
    component: () => import('@/views/kb/CategoryList.vue'),
    meta: { title: '分类管理', icon: 'Folder', group: '知识库', perm: 'kb:category:view' }
  },
  {
    path: 'kb/tags',
    name: 'TagList',
    component: () => import('@/views/kb/TagList.vue'),
    meta: { title: '标签管理', icon: 'PriceTag', group: '知识库', perm: 'kb:tag:view' }
  },
  {
    path: 'search',
    name: 'DocumentSearch',
    component: () => import('@/views/search/DocumentSearch.vue'),
    meta: { title: '全文检索', icon: 'Search', group: '检索中心', perm: 'kb:doc:view' }
  },
  {
    path: 'rebuild',
    name: 'IndexRebuild',
    component: () => import('@/views/rebuild/IndexRebuild.vue'),
    meta: { title: '索引重建', icon: 'RefreshRight', group: '检索中心', perm: 'system:perm:view' }
  },
  {
    path: 'system/users',
    name: 'UserList',
    component: () => import('@/views/system/UserList.vue'),
    meta: { title: '用户管理', icon: 'User', group: '系统管理', perm: 'system:user:view' }
  },
  {
    path: 'system/roles',
    name: 'RoleList',
    component: () => import('@/views/system/RoleList.vue'),
    meta: { title: '角色管理', icon: 'UserFilled', group: '系统管理', perm: 'system:role:view' }
  },
  {
    path: 'system/permissions',
    name: 'PermissionList',
    component: () => import('@/views/system/PermissionList.vue'),
    meta: { title: '权限管理', icon: 'Key', group: '系统管理', perm: 'system:perm:view' }
  },
  {
    path: 'system/logs',
    name: 'OperationLogList',
    component: () => import('@/views/system/OperationLogList.vue'),
    meta: { title: '操作日志', icon: 'Tickets', group: '系统管理', perm: 'system:log:view' }
  },
  {
    path: 'profile',
    name: 'Profile',
    component: () => import('@/views/profile/Profile.vue'),
    meta: { title: '个人中心', icon: 'Setting', group: '工作台' }
  }
]

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录', public: true }
  },
  {
    path: '/',
    component: () => import('@/layout/AppLayout.vue'),
    redirect: '/dashboard',
    children: mainRoutes
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

// 全局守卫：未登录跳转登录页；已登录访问登录页跳转工作台
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  if (to.meta.public) {
    next()
  } else if (!userStore.isLoggedIn) {
    next({ path: '/login', query: { redirect: to.fullPath } })
  } else if (!userStore.userInfo) {
    // 有 Token 但未加载用户信息：加载后再放行
    userStore.fetchUserInfo()
      .then(() => next())
      .catch(() => {
        userStore.clearSession()
        next('/login')
      })
  } else {
    next()
  }
})

export default router
