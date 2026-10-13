import { useUserStore } from '@/stores/user'

// v-permission 指令：控制按钮级权限（不满足即移除元素）
export function setupPermissionDirective(app) {
  app.directive('permission', {
    mounted(el, binding) {
      const userStore = useUserStore()
      const codes = Array.isArray(binding.value) ? binding.value : [binding.value]
      const ok = codes.some((code) => userStore.hasPermission(code))
      if (!ok) {
        el.parentNode?.removeChild(el)
      }
    }
  })
}
