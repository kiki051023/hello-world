import { computed } from 'vue'
import { useUserStore } from '@/store/user'

/** 事务管理角色：可审批/办理/登记 */
export function useCanManage() {
  const userStore = useUserStore()
  return computed(() =>
    ['ADMIN', 'EDUCATION_ADMIN', 'DEPARTMENT_LEADER', 'COUNSELOR'].includes(userStore.role))
}

/** 是否学生角色 */
export function useIsStudent() {
  const userStore = useUserStore()
  return computed(() => userStore.role === 'STUDENT')
}
