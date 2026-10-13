import request from './request'

// ==================== 认证 ====================
export const authApi = {
  login: (data) => request.post('/auth/login', data),
  info: () => request.get('/auth/info'),
  logout: () => request.post('/auth/logout'),
  updatePassword: (data) => request.put('/auth/password', data)
}

// ==================== 仪表盘 ====================
export const dashboardApi = {
  stats: () => request.get('/dashboard/stats')
}

// ==================== 知识库 ====================
export const kbApi = {
  page: (params) => request.get('/kb/knowledge-bases', { params }),
  all: () => request.get('/kb/knowledge-bases/all'),
  detail: (id) => request.get(`/kb/knowledge-bases/${id}`),
  create: (data) => request.post('/kb/knowledge-bases', data),
  update: (id, data) => request.put(`/kb/knowledge-bases/${id}`, data),
  remove: (id) => request.delete(`/kb/knowledge-bases/${id}`)
}

// ==================== 文档 ====================
export const documentApi = {
  page: (params) => request.get('/kb/documents', { params }),
  detail: (id) => request.get(`/kb/documents/${id}`),
  create: (data) => request.post('/kb/documents', data),
  update: (id, data) => request.put(`/kb/documents/${id}`, data),
  remove: (id) => request.delete(`/kb/documents/${id}`)
}

// ==================== 分类 ====================
export const categoryApi = {
  page: (params) => request.get('/kb/categories', { params }),
  all: () => request.get('/kb/categories/all'),
  detail: (id) => request.get(`/kb/categories/${id}`),
  create: (data) => request.post('/kb/categories', data),
  update: (id, data) => request.put(`/kb/categories/${id}`, data),
  remove: (id) => request.delete(`/kb/categories/${id}`)
}

// ==================== 标签 ====================
export const tagApi = {
  page: (params) => request.get('/kb/tags', { params }),
  all: () => request.get('/kb/tags/all'),
  detail: (id) => request.get(`/kb/tags/${id}`),
  create: (data) => request.post('/kb/tags', data),
  update: (id, data) => request.put(`/kb/tags/${id}`, data),
  remove: (id) => request.delete(`/kb/tags/${id}`)
}

// ==================== 附件 ====================
export const fileApi = {
  upload: (formData) => request.post('/kb/files', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }),
  downloadUrl: (id) => `/api/kb/files/${id}`,
  remove: (id) => request.delete(`/kb/files/${id}`)
}

// ==================== 全文检索 ====================
export const searchApi = {
  search: (params) => request.get('/search/documents', { params })
}

// ==================== 索引重建 ====================
export const rebuildApi = {
  submit: () => request.post('/rebuild/tasks'),
  page: (params) => request.get('/rebuild/tasks', { params }),
  detail: (id) => request.get(`/rebuild/tasks/${id}`)
}

// ==================== 用户管理 ====================
export const userApi = {
  page: (params) => request.get('/system/users', { params }),
  all: () => request.get('/system/users/all'),
  detail: (id) => request.get(`/system/users/${id}`),
  create: (data) => request.post('/system/users', data),
  update: (id, data) => request.put(`/system/users/${id}`, data),
  remove: (id) => request.delete(`/system/users/${id}`),
  assignRoles: (id, roleIds) => request.put(`/system/users/${id}/roles`, { roleIds }),
  resetPassword: (id, newPassword) =>
    request.put(`/system/users/${id}/password`, { newPassword })
}

// ==================== 角色管理 ====================
export const roleApi = {
  page: (params) => request.get('/system/roles', { params }),
  all: () => request.get('/system/roles/all'),
  detail: (id) => request.get(`/system/roles/${id}`),
  create: (data) => request.post('/system/roles', data),
  update: (id, data) => request.put(`/system/roles/${id}`, data),
  remove: (id) => request.delete(`/system/roles/${id}`),
  assignPermissions: (id, permissionIds) =>
    request.put(`/system/roles/${id}/permissions`, { permissionIds })
}

// ==================== 权限管理 ====================
export const permissionApi = {
  page: (params) => request.get('/system/permissions', { params }),
  modules: () => request.get('/system/permissions/modules'),
  detail: (id) => request.get(`/system/permissions/${id}`),
  create: (data) => request.post('/system/permissions', data),
  update: (id, data) => request.put(`/system/permissions/${id}`, data),
  remove: (id) => request.delete(`/system/permissions/${id}`)
}

// ==================== 操作日志 ====================
export const logApi = {
  page: (params) => request.get('/system/logs', { params }),
  detail: (id) => request.get(`/system/logs/${id}`),
  remove: (id) => request.delete(`/system/logs/${id}`)
}
